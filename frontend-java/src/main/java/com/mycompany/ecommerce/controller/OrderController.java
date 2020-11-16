package com.mycompany.ecommerce.controller;

import com.mycompany.ecommerce.dto.OrderProductDto;
import com.mycompany.ecommerce.exception.ResourceNotFoundException;
import com.mycompany.ecommerce.model.Order;
import com.mycompany.ecommerce.model.OrderProduct;
import com.mycompany.ecommerce.model.OrderStatus;
import com.mycompany.ecommerce.service.OrderProductService;
import com.mycompany.ecommerce.service.OrderService;
import com.mycompany.ecommerce.service.ProductService;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleValueRecorder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
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
    RestTemplate restTemplate;
    String antiFraudServiceBaseUrl;
    final DoubleValueRecorder orderValueRecorder;
    final DoubleCounter orderSumCounter;
    final DoubleCounter orderValueWithTagsCounter;
    final LongCounter orderCountCounter;
    final DoubleValueRecorder orderWithTagsValueRecorder;


    public OrderController(ProductService productService, OrderService orderService, OrderProductService orderProductService, Meter meter) {
        this.productService = productService;
        this.orderService = orderService;
        this.orderProductService = orderProductService;
        // (!) ValueRecorders (histograms) are ignored by Otel Collector Exporter for Elastic v0.14
        orderValueRecorder = meter.doubleValueRecorderBuilder("order").setUnit("usd").build();

        // Meters below are used for testing and compare with orderValueRecorder
        orderSumCounter = meter.doubleCounterBuilder("order_sum").setUnit("usd").build();
        orderCountCounter = meter.longCounterBuilder("order_count").build();
        orderWithTagsValueRecorder = meter.doubleValueRecorderBuilder("order_with_tags").setUnit("usd").build();
        orderValueWithTagsCounter = meter.doubleCounterBuilder("order_value_with_tags_counter").setUnit("usd").build();
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public @NotNull Iterable<Order> list() {
        return this.orderService.getAllOrders();
    }


    @PostMapping
    public ResponseEntity<Order> create(@RequestBody OrderForm form, HttpServletRequest request) {
        Span span = Span.current();

        List<OrderProductDto> formDtos = form.getProductOrders();
        validateProductsExistence(formDtos);

        String customerId = "customer-" + RANDOM.nextInt(100);
        span.setAttribute("customer_id", customerId);

        double orderPrice = formDtos.stream().mapToDouble(po -> po.getQuantity() * po.getProduct().getPrice()).sum();
        span.setAttribute("order_price", orderPrice);
        span.setAttribute("order_price_range", getPriceRange(orderPrice));

        String shippingCountry = getCountryCode(request.getRemoteAddr());
        String shippingMethod = randomShippingMethod();
        String paymentMethod = randomPaymentMethod();
        Labels labels = Labels.of(
                "shipping_country", shippingCountry,
                "shipping_method", shippingMethod,
                "payment_method", paymentMethod);
        orderWithTagsValueRecorder.record(orderPrice, labels);
        labels.forEach((key, value) -> {
            span.setAttribute(key, value);
        });

        ResponseEntity<String> antiFraudResult;
        try {
            antiFraudResult = restTemplate.getForEntity(
                    this.antiFraudServiceBaseUrl + "fraud/checkOrder?orderPrice={q}&customerIpAddress={q}&shippingCountry={q}",
                    String.class,
                    orderPrice, request.getRemoteAddr(), shippingCountry);
        } catch (RestClientException e) {
            String exceptionShortDescription = e.getClass().getName();
            span.recordException(e);

            if (e.getCause() != null) {
                exceptionShortDescription += " / " + e.getCause().getClass().getName();
            }
            logger.info("Failure createOrder({}): totalPrice: {}, fraud.exception:{}", form, orderPrice, exceptionShortDescription);
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
            orderProducts.add(orderProductService.create(new OrderProduct(order, productService.getProduct(dto
                    .getProduct()
                    .getId()), dto.getQuantity())));
        }

        order.setOrderProducts(orderProducts);

        this.orderService.update(order);

        // UPDATE METRICS
        this.orderValueRecorder.record(orderPrice);

        // Meters below are used for testing and compare with orderValueRecorder
        this.orderSumCounter.add(orderPrice);
        this.orderCountCounter.add(1);
        this.orderWithTagsValueRecorder.record(orderPrice, labels);
        this.orderValueWithTagsCounter.add(orderPrice, labels);

        logger.info("SUCCESS createOrder({}): totalPrice: {}, id:{}", form, orderPrice, order.getId());

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
            return new ToStringCreator(this).append(this.productOrders).toString();
        }
    }
}
