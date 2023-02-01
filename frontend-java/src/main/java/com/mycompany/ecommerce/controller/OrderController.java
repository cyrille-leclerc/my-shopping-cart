package com.mycompany.ecommerce.controller;

import com.mycompany.checkout.PlaceOrderReply;
import com.mycompany.checkout.PlaceOrderRequest;
import com.mycompany.ecommerce.OpenTelemetryAttributes;
import com.mycompany.ecommerce.dto.OrderProductDto;
import com.mycompany.ecommerce.exception.ResourceNotFoundException;
import com.mycompany.ecommerce.model.Order;
import com.mycompany.ecommerce.model.OrderProduct;
import com.mycompany.ecommerce.model.OrderStatus;
import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.service.CheckoutService;
import com.mycompany.ecommerce.service.OrderProductService;
import com.mycompany.ecommerce.service.OrderService;
import com.mycompany.ecommerce.service.ProductService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
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
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    RestTemplate restTemplate;
    String antiFraudServiceBaseUrl;
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
        logger.info("Anti fraud service base url: {}", this.antiFraudServiceBaseUrl);
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
        //validateProductsExistence(formDtos);

        String customerId = "customer-" + RANDOM.nextInt(100);
        span.setAttribute(OpenTelemetryAttributes.CUSTOMER_ID, customerId);

        double orderPrice = formDtos.stream().mapToDouble(po -> po.getQuantity() * po.getProduct().getPrice()).sum();
        String shippingCountry = getCountryCode(request.getRemoteAddr());
        String shippingMethod = randomShippingMethod();
        String paymentMethod = randomPaymentMethod();

        span.addEvent("order-creation", Attributes.of(
                OpenTelemetryAttributes.CUSTOMER_ID, customerId,
                OpenTelemetryAttributes.ORDER_PRICE, orderPrice,
                OpenTelemetryAttributes.PAYMENT_METHOD, paymentMethod,
                OpenTelemetryAttributes.SHIPPING_METHOD, shippingMethod,
                OpenTelemetryAttributes.SHIPPING_COUNTRY, shippingCountry));

        span.setAttribute(OpenTelemetryAttributes.ORDER_PRICE_RANGE, getPriceRange(orderPrice));

        span.setAttribute(OpenTelemetryAttributes.SHIPPING_COUNTRY.getKey(), shippingCountry);
        span.setAttribute(OpenTelemetryAttributes.SHIPPING_METHOD.getKey(), shippingMethod);
        span.setAttribute(OpenTelemetryAttributes.PAYMENT_METHOD.getKey(), paymentMethod);

        ResponseEntity<String> antiFraudResult;
        String url = this.antiFraudServiceBaseUrl + (antiFraudServiceBaseUrl.endsWith("/") ? "" : "/") + "fraud/checkOrder?orderPrice={q}&customerIpAddress={q}&shippingCountry={q}";
        try {
            antiFraudResult = restTemplate.getForEntity(
                    url,
                    String.class,
                    orderPrice, request.getRemoteAddr(), shippingCountry);
        } catch (RestClientException e) {
            String exceptionShortDescription = e.getClass().getSimpleName();
            span.recordException(e);

            if (e.getCause() != null) {
                exceptionShortDescription += " / " + e.getCause().getClass().getSimpleName();
            }
            logger.info("Failure createOrder({}): price: {}, fraud.exception: {} with URL {}", form, orderPrice, exceptionShortDescription, url);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (antiFraudResult.getStatusCode() != HttpStatus.OK) {
            String exceptionShortDescription = "status-" + antiFraudResult.getStatusCode();
            span.recordException(new Exception(exceptionShortDescription));
            logger.info("Failure createOrder({}): totalPrice: {}, fraud.exception:{}", form, orderPrice, exceptionShortDescription);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!"OK".equals(antiFraudResult.getBody())) {
            String exceptionShortDescription = "response-" + antiFraudResult.getBody();
            span.recordException(new Exception(exceptionShortDescription));
            logger.info("Failure createOrder({}): totalPrice: {}, fraud.exception:{}", form, orderPrice, exceptionShortDescription);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
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

        final PlaceOrderReply placeOrderReply = this.checkoutService.placeOrder(PlaceOrderRequest.newBuilder().setName(customerId).build());

        // UPDATE METRICS
        this.orderValueHistogram.record(orderPrice);

        Attributes attributes = Attributes.of(
                OpenTelemetryAttributes.SHIPPING_COUNTRY, shippingCountry,
                OpenTelemetryAttributes.SHIPPING_METHOD, shippingMethod,
                OpenTelemetryAttributes.PAYMENT_METHOD, paymentMethod);
        this.orderWithTagsHistogram.record(orderPrice, attributes);

        long durationInNanos = System.nanoTime() - beforeInNanos;

        logger.info("SUCCESS placeOrder orderId={} customerId={} price={} paymentMethod={} shippingMethod={} shippingCountry={} durationInNanos={}", order.getId(), customerId, orderPrice, paymentMethod, shippingMethod, shippingCountry, durationInNanos);

        String uri = ServletUriComponentsBuilder
                .fromCurrentServletMapping()
                .path("/orders/{id}")
                .buildAndExpand(order.getId())
                .toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", uri);

        return new ResponseEntity<>(order, headers, HttpStatus.CREATED);
    }

    private void validateProductsExistence(List<OrderProductDto> orderProducts) {
        List<OrderProductDto> list = orderProducts
                .stream()
                .filter(op -> Objects.isNull(productService.getProduct(op
                        .getProduct()
                        .getId())))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(list)) {
            new ResourceNotFoundException("Product not found");
        }
    }

    public String getCountryCode(String ip) {
        String[] countries = {"US", "FR", "GB"};
        return countries[RANDOM.nextInt(countries.length)];
    }

    public String randomPaymentMethod() {
        String[] paymentMethods = {"credit_cart", "paypal"};
        return paymentMethods[RANDOM.nextInt(paymentMethods.length)];
    }

    public String randomShippingMethod() {
        String[] shippingMethods = {"standard", "express"};
        return shippingMethods[RANDOM.nextInt(shippingMethods.length)];
    }

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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

        public List<OrderProductDto> getProductOrders() {
            return productOrders;
        }

        public void setProductOrders(List<OrderProductDto> productOrders) {
            this.productOrders = productOrders;
        }

        @Override
        public String toString() {
            return "OrderForm{" + this.productOrders.stream().map(OrderProductDto::toString).collect(Collectors.joining(",")) + "}";
        }
    }
}
