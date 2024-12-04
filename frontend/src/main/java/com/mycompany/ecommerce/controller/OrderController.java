package com.mycompany.ecommerce.controller;

import com.mycompany.checkout.PlaceOrderReply;
import com.mycompany.checkout.PlaceOrderRequest;
import com.mycompany.ecommerce.EcommerceApplication;
import com.mycompany.ecommerce.OpenTelemetryAttributes;
import com.mycompany.ecommerce.dto.OrderProductDto;
import com.mycompany.ecommerce.model.Order;
import com.mycompany.ecommerce.model.OrderProduct;
import com.mycompany.ecommerce.model.OrderStatus;
import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.service.CheckoutService;
import com.mycompany.ecommerce.service.OrderProductService;
import com.mycompany.ecommerce.service.OrderService;
import com.mycompany.ecommerce.service.ProductService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    final static Random RANDOM = new Random();

    final Logger logger = LoggerFactory.getLogger(getClass());

    final ProductService productService;
    final OrderService orderService;
    final OrderProductService orderProductService;
    final CheckoutService checkoutService;
    RabbitTemplate rabbitTemplate;
    RestTemplate restTemplate;
    String antiFraudServiceBaseUrl;
    String unInstrumentedServiceUrl;
    final DoubleHistogram orderValueHistogram;
    final DoubleHistogram orderWithTagsHistogram;

    public OrderController(ProductService productService, OrderService orderService, OrderProductService orderProductService, CheckoutService checkoutService, Meter meter) {
        this.productService = productService;
        this.orderService = orderService;
        this.orderProductService = orderProductService;
        this.checkoutService = checkoutService;

        // Meters below are used for testing and compare with orderValueRecorder
        orderValueHistogram = meter.histogramBuilder("order").setUnit("usd").build();
        orderWithTagsHistogram = meter.histogramBuilder("orderWithTags").setUnit("usd").build();
    }

    @PostConstruct
    public void init() {
        logger.atInfo()
                .addKeyValue("fraud.url", this.antiFraudServiceBaseUrl)
                .addKeyValue("unInstrumentedService.url", this.unInstrumentedServiceUrl)
                .log("initialized");
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public @Nonnull
    Iterable<Order> list() {
        return this.orderService.getAllOrders();
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody OrderForm form, HttpServletRequest request) {
        long beforeInNanos = System.nanoTime();
        Span span = Span.current();

        List<OrderProductDto> formDtos = form.getProductOrders();

        String customerId = "customer-" + RANDOM.nextInt(100);
        span.setAttribute(OpenTelemetryAttributes.CUSTOMER_ID, customerId);

        double orderPrice = formDtos.stream().mapToDouble(po -> po.getQuantity() * po.getProduct().getPrice()).sum();
        String orderPriceRange = getPriceRange(orderPrice);
        String shippingCountry = form.getShippingCountry();
        String shippingMethod = form.getShippingMethod();
        String paymentMethod = form.getPaymentMethod();

        span.setAttribute(OpenTelemetryAttributes.CUSTOMER_ID, customerId);
        span.setAttribute(OpenTelemetryAttributes.ORDER_PRICE_RANGE, orderPriceRange);
        span.setAttribute(OpenTelemetryAttributes.SHIPPING_COUNTRY.getKey(), shippingCountry);
        span.setAttribute(OpenTelemetryAttributes.SHIPPING_METHOD.getKey(), shippingMethod);
        span.setAttribute(OpenTelemetryAttributes.PAYMENT_METHOD.getKey(), paymentMethod);

        ResponseEntity<String> fraudDetectionResult;
        String url = this.antiFraudServiceBaseUrl + (antiFraudServiceBaseUrl.endsWith("/") ? "" : "/") + "fraud/checkOrder?orderPrice={q}&customerIpAddress={q}&shippingCountry={q}";
        try {
            fraudDetectionResult = restTemplate.getForEntity(
                    url,
                    String.class,
                    orderPrice, request.getRemoteAddr(), shippingCountry);
        } catch (RestClientException e) {
            String exceptionShortDescription = e.getClass().getSimpleName();
            span.recordException(e);

            if (e.getCause() != null) {
                exceptionShortDescription += " / " + e.getCause().getClass().getSimpleName();
            }
            logger.atWarn()
                    .addKeyValue("order", form)
                    .addKeyValue("orderPrice", orderPrice)
                    .addKeyValue("fraud.url", url)
                    .addKeyValue("fraud.exception", exceptionShortDescription)
                    .log("Fraud detection failure");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (fraudDetectionResult.getStatusCode().isError()) {
            String exceptionShortDescription = "fraudDetection-status-" + fraudDetectionResult.getStatusCode();
            span.recordException(new Exception(exceptionShortDescription));
            logger.atWarn()
                    .addKeyValue("fraud.score", fraudDetectionResult.getBody())
                    .addKeyValue("fraud.status_code", fraudDetectionResult.getStatusCode())
                    .addKeyValue("order", form)
                    .addKeyValue("orderPrice", orderPrice)
                    .addKeyValue("fraud.exception", exceptionShortDescription)
                    .log("Fraud detected");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!"approved".equals(fraudDetectionResult.getBody())) {
            String exceptionShortDescription = "fraudDetection-" + fraudDetectionResult.getBody();
            span.recordException(new Exception(exceptionShortDescription));
            logger.atWarn()
                    .addKeyValue("fraud.score", fraudDetectionResult.getBody())
                    .addKeyValue("order", form)
                    .addKeyValue("orderPrice", orderPrice)
                    .log("Fraud detected");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // invoke un-instrumented service.
        ResponseEntity<String> unInstrumentedServiceResponse = restTemplate.getForEntity(this.unInstrumentedServiceUrl, String.class);
        if (unInstrumentedServiceResponse.getStatusCode() != HttpStatus.OK) {
            String exceptionShortDescription = unInstrumentedServiceUrl + "-status-" + fraudDetectionResult.getStatusCode();
            span.recordException(new Exception(exceptionShortDescription));
        }

        Order order = new Order();
        order.setStatus(OrderStatus.PAID.name());
        order = this.orderService.create(order);

        List<OrderProduct> orderProducts = new ArrayList<>();
        for (OrderProductDto dto : formDtos) {
            Product product = productService.getProduct(dto
                    .getProduct()
                    .getId());
            orderProducts.add(orderProductService.create(new OrderProduct(order, product, dto.getQuantity())));
        }

        order.setOrderProducts(orderProducts);

        this.orderService.update(order);

        rabbitTemplate.convertAndSend(EcommerceApplication.AMQP_EXCHANGE, EcommerceApplication.AMQP_ROUTING_KEY, order.toString());

        final PlaceOrderReply placeOrderReply = this.checkoutService.placeOrder(PlaceOrderRequest.newBuilder().setName(customerId).build());

        // UPDATE METRICS
        this.orderValueHistogram.record(orderPrice); // would like the tenant_id to be added

        Attributes attributes = Attributes.of(
                OpenTelemetryAttributes.SHIPPING_COUNTRY, shippingCountry,
                OpenTelemetryAttributes.SHIPPING_METHOD, shippingMethod,
                OpenTelemetryAttributes.PAYMENT_METHOD, paymentMethod);
        this.orderWithTagsHistogram.record(orderPrice, attributes);

        long durationInNanos = System.nanoTime() - beforeInNanos;

        logger.atInfo().addKeyValue("orderId", order.getId())
                .addKeyValue("customerId", customerId)
                .addKeyValue("price", orderPrice)
                .addKeyValue("paymentMethod", paymentMethod)
                .addKeyValue("shippingMethod", shippingMethod)
                .addKeyValue("shippingCountry", shippingCountry)
                .addKeyValue("durationInNanos", durationInNanos)
                .log("Success placeOrder");

        span.addEvent("order-creation", Attributes.builder()
                .put(OpenTelemetryAttributes.OUTCOME, "success")
                .put(OpenTelemetryAttributes.CUSTOMER_ID, customerId)
                .put(OpenTelemetryAttributes.ORDER_PRICE, orderPrice)
                .put(OpenTelemetryAttributes.ORDER_PRICE_RANGE, orderPriceRange)
                .put(OpenTelemetryAttributes.PAYMENT_METHOD, paymentMethod)
                .put(OpenTelemetryAttributes.SHIPPING_METHOD, shippingMethod)
                .put(OpenTelemetryAttributes.SHIPPING_COUNTRY, shippingCountry)
                .build());

        String uri = ServletUriComponentsBuilder
                .fromCurrentServletMapping()
                .path("/orders/{id}")
                .buildAndExpand(order.getId())
                .toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", uri);

        return new ResponseEntity<>(order, headers, HttpStatus.CREATED);
    }

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${unInstrumentedService.url}")
    public void setUnInstrumentedServiceUrl(String unInstrumentedServiceUrl) {
        this.unInstrumentedServiceUrl = unInstrumentedServiceUrl;
    }

    @Value("${antiFraudService.baseUrl}")
    public void setAntiFraudServiceBaseUrl(String antiFraudServiceBaseUrl) {
        this.antiFraudServiceBaseUrl = antiFraudServiceBaseUrl;
    }

    public String getPriceRange(double price) {
        if (price < 10) {
            return "small";
        } else if (price < 100) {
            return "medium";
        } else {
            return "large";
        }
    }

    public static class OrderForm {

        private List<OrderProductDto> productOrders;

        private String paymentMethod;

        private String shippingCountry;

        private String shippingMethod;

        public List<OrderProductDto> getProductOrders() {
            return productOrders;
        }

        public void setProductOrders(List<OrderProductDto> productOrders) {
            this.productOrders = productOrders;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getShippingCountry() {
            return shippingCountry;
        }

        public void setShippingCountry(String shippingCountry) {
            this.shippingCountry = shippingCountry;
        }

        public String getShippingMethod() {
            return shippingMethod;
        }

        public void setShippingMethod(String shippingMethod) {
            this.shippingMethod = shippingMethod;
        }

        @Override
        public String toString() {
            return "OrderForm{" +
                    "paymentMethod='" + paymentMethod + '\'' +
                    ", shippingCountry='" + shippingCountry + '\'' +
                    ", shippingMethod='" + shippingMethod + '\'' +
                    ", productOrders=" + productOrders.stream().map(OrderProductDto::toString).collect(Collectors.joining(",")) +
                    '}';
        }
    }
}
