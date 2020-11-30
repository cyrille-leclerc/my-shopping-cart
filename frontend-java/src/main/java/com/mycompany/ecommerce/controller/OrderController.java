package com.mycompany.ecommerce.controller;

import co.elastic.apm.api.CaptureTransaction;
import co.elastic.apm.api.ElasticApm;
import com.mycompany.ecommerce.dto.OrderProductDto;
import com.mycompany.ecommerce.exception.ResourceNotFoundException;
import com.mycompany.ecommerce.model.Order;
import com.mycompany.ecommerce.model.OrderProduct;
import com.mycompany.ecommerce.model.OrderStatus;
import com.mycompany.ecommerce.service.OrderProductService;
import com.mycompany.ecommerce.service.OrderService;
import com.mycompany.ecommerce.service.ProductService;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
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
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    final static Random RANDOM = new Random();

    final Logger logger = LoggerFactory.getLogger(getClass());

    ProductService productService;
    OrderService orderService;
    OrderProductService orderProductService;
    RestTemplate restTemplate;
    String antiFraudServiceBaseUrl;

    public OrderController(ProductService productService, OrderService orderService, OrderProductService orderProductService) {
        this.productService = productService;
        this.orderService = orderService;
        this.orderProductService = orderProductService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public @NotNull Iterable<Order> list() {
        return this.orderService.getAllOrders();
    }


    @PostMapping
    public ResponseEntity<Order> create(@RequestBody OrderForm form, HttpServletRequest request) {
        ElasticApm.currentSpan().setName("createOrder");
        List<OrderProductDto> formDtos = form.getProductOrders();
        validateProductsExistence(formDtos);

        String customerId = "customer-" + RANDOM.nextInt(100); // TODO better demo
        ElasticApm.currentSpan().setLabel("customerId", customerId);

        double orderPrice = formDtos.stream().mapToDouble(po -> po.getQuantity() * po.getProduct().getPrice()).sum();
        ElasticApm.currentSpan().setLabel("orderPrice", orderPrice);
        String priceRange = getPriceRange(orderPrice);
        ElasticApm.currentSpan().setLabel("orderPriceRange", priceRange);

        String shippingCountryCode = getCountryCode(request.getRemoteAddr());
        ElasticApm.currentSpan().setLabel("shippingCountry", shippingCountryCode);
        ResponseEntity<String> antiFraudResult;
        try {
            antiFraudResult = restTemplate.getForEntity(
                    this.antiFraudServiceBaseUrl + "fraud/checkOrder?orderPrice={q}&customerIpAddress={q}&shippingCountry={q}",
                    String.class,
                    orderPrice, request.getRemoteAddr(), shippingCountryCode);

        } catch (RestClientException e) {
            String exceptionShortDescription = e.getClass().getName();
            ElasticApm.currentSpan().setLabel("antiFraud.exception", exceptionShortDescription);
            ElasticApm.currentSpan().captureException(e);
            if (e.getCause() != null) { // capture SockerTimeoutException...
                ElasticApm.currentSpan().setLabel("antiFraud.exception.cause", e.getCause().getClass().getName());
                exceptionShortDescription += " / " + e.getCause().getClass().getName();
            }
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-orderCreationFailureCause", "auti-fraud_" + exceptionShortDescription);
            logger.info("Failure createOrder({}): orderPrice: {}, fraud.exception:{}", form, orderPrice, exceptionShortDescription);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (antiFraudResult.getStatusCode() != HttpStatus.OK) {
            String exceptionShortDescription = "status-" + antiFraudResult.getStatusCode();
            ElasticApm.currentSpan().setLabel("antiFraud.exception", exceptionShortDescription);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-orderCreationFailureCause", "auti-fraud_" + exceptionShortDescription);
            logger.info("Failure createOrder({}): orderPrice: {}, fraud.exception:{}", form, orderPrice, exceptionShortDescription);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!"OK".equals(antiFraudResult.getBody())) {
            String exceptionShortDescription = "response-" + antiFraudResult.getBody();
            ElasticApm.currentSpan().setLabel("antiFraud.exception", exceptionShortDescription);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-orderCreationFailureCause", "auti-fraud_" + exceptionShortDescription);
            logger.info("Failure createOrder({}): orderPrice: {}, fraud.exception:{}", form, orderPrice, exceptionShortDescription);
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

        DistributionSummary.builder("order")
                .publishPercentileHistogram()
                .publishPercentiles(0.75, 0.95)
                .register(Metrics.globalRegistry)
                .record(orderPrice);

        Metrics.counter("order_value_counter").increment(orderPrice);
        Metrics.counter("order_count_counter").increment();

        DistributionSummary.builder("order_per_country")
                .tags("shipping_country", shippingCountryCode)
                .publishPercentileHistogram()
                .publishPercentiles(0.75, 0.95)
                .register(Metrics.globalRegistry)
                .record(orderPrice);

        logger.info("SUCCESS createOrder({}): price: {}, id:{}", form, orderPrice, order.getId());

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
        String[] countries = {"US", "FR", "GB",};
        return countries[RANDOM.nextInt(countries.length)];
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