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
        ElasticApm.currentSpan().addLabel("customerId", customerId);

        double totalPrice = formDtos.stream().mapToDouble(po -> po.getQuantity() * po.getProduct().getPrice()).sum();
        // FIXME shouldn't orderTotalPrice be log message rather than tag / label?
        ElasticApm.currentSpan().addLabel("orderTotalPrice", totalPrice);
        String priceRange = getPriceRange(totalPrice);
        ElasticApm.currentSpan().addLabel("orderTotalPriceRange", priceRange);

        String shippingCountry = "FR"; // TODO better demo
        ElasticApm.currentSpan().addLabel("shippingCountry", shippingCountry);
        ResponseEntity<String> antiFraudResult;
        try {
            antiFraudResult = restTemplate.getForEntity(
                    this.antiFraudServiceBaseUrl + "fraud/checkOrder?totalPrice={q}&customerIpAddress={q}&shippingCountry={q}",
                    String.class,
                    totalPrice, request.getRemoteAddr(), shippingCountry);

        } catch (RestClientException e) {
            String exceptionShortDescription = e.getClass().getName();
            ElasticApm.currentSpan().addLabel("antiFraud.exception", exceptionShortDescription);
            ElasticApm.currentSpan().captureException(e);
            if (e.getCause() != null) { // capture SockerTimeoutException...
                ElasticApm.currentSpan().addLabel("antiFraud.exception.cause", e.getCause().getClass().getName());
                exceptionShortDescription += " / " + e.getCause().getClass().getName();
            }
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-orderCreationFailureCause", "auti-fraud_" + exceptionShortDescription);
            logger.info("Failure createOrder({}): totalPrice: {}, fraud.exception:{}", form, totalPrice, exceptionShortDescription);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (antiFraudResult.getStatusCode() != HttpStatus.OK) {
            String exceptionShortDescription = "status-" + antiFraudResult.getStatusCode();
            ElasticApm.currentSpan().addLabel("antiFraud.exception", exceptionShortDescription);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-orderCreationFailureCause", "auti-fraud_" + exceptionShortDescription);
            logger.info("Failure createOrder({}): totalPrice: {}, fraud.exception:{}", form, totalPrice, exceptionShortDescription);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!"OK".equals(antiFraudResult.getBody())) {
            String exceptionShortDescription = "response-" + antiFraudResult.getBody();
            ElasticApm.currentSpan().addLabel("antiFraud.exception", exceptionShortDescription);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("x-orderCreationFailureCause", "auti-fraud_" + exceptionShortDescription);
            logger.info("Failure createOrder({}): totalPrice: {}, fraud.exception:{}", form, totalPrice, exceptionShortDescription);
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

        Metrics.counter("order.totalPrice", "priceRange", priceRange, "shippingCountry", shippingCountry).increment(totalPrice);
        logger.info("SUCCESS createOrder({}): totalPrice: {}, id:{}", form, totalPrice, order.getId());

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