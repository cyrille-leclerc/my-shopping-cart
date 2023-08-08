package com.mycompany.ecommerce;

import com.mycompany.ecommerce.controller.OrderController;
import com.mycompany.ecommerce.controller.ProductController;
import com.mycompany.ecommerce.dto.OrderProductDto;
import com.mycompany.ecommerce.model.Order;
import com.mycompany.ecommerce.model.Product;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { EcommerceApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EcommerceApplicationIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Autowired private ProductController productController;

    @Autowired private OrderController orderController;

    @Test
    public void contextLoads() {
        Assertions
          .assertThat(productController)
          .isNotNull();
        Assertions
          .assertThat(orderController)
          .isNotNull();
    }

    @Test
    public void givenGetProductsApiCall_whenProductListRetrieved_thenSizeMatchAndListContainsProductNames() {
        ResponseEntity<Iterable<Product>> responseEntity = restTemplate.exchange("http://localhost:" + port + "/api/products", HttpMethod.GET, null, new ParameterizedTypeReference<Iterable<Product>>() {
        });
        Iterable<Product> products = responseEntity.getBody();
        Assertions
          .assertThat(products)
          .hasSize(12);

        assertThat(products, hasItem(hasProperty("name", is("TV Set"))));
        assertThat(products, hasItem(hasProperty("name", is("Game Console"))));
        assertThat(products, hasItem(hasProperty("name", is("Sofa"))));
        assertThat(products, hasItem(hasProperty("name", is("Icecream"))));
        assertThat(products, hasItem(hasProperty("name", is("Beer"))));
        assertThat(products, hasItem(hasProperty("name", is("Phone"))));
        assertThat(products, hasItem(hasProperty("name", is("Watch"))));
        assertThat(products, hasItem(hasProperty("name", is("USB Cable"))));

        assertThat(products, hasItem(hasProperty("name", is("USB-C Cable"))));
        assertThat(products, hasItem(hasProperty("name", is("Micro USB Cable"))));
        assertThat(products, hasItem(hasProperty("name", is("Lightning Cable"))));
        assertThat(products, hasItem(hasProperty("name", is("USB C adapter"))));
    }

    @Ignore("the postgresql database is not reset")
    @Test
    public void givenGetOrdersApiCall_whenProductListRetrieved_thenSizeMatchAndListContainsProductNames() {
        ResponseEntity<Iterable<Order>> responseEntity = restTemplate.exchange("http://localhost:" + port + "/api/orders", HttpMethod.GET, null, new ParameterizedTypeReference<Iterable<Order>>() {
        });

        Iterable<Order> orders = responseEntity.getBody();
        Assertions
          .assertThat(orders)
          .hasSize(0);
    }

    @Ignore("Not testable without the mock antiFraud service")
    @Test
    public void givenPostOrder_whenBodyRequestMatcherJson_thenResponseContainsEqualObjectProperties() {
        final ResponseEntity<Order> postResponse = restTemplate.postForEntity("http://localhost:" + port + "/api/orders", prepareOrderForm(), Order.class);
        Order order = postResponse.getBody();
        Assertions
          .assertThat(postResponse.getStatusCode())
          .isEqualTo(HttpStatus.CREATED);

        assertThat(order, hasProperty("status", is("PAID")));
        assertThat(order.getOrderProducts(), hasItem(hasProperty("quantity", is(2))));
    }

    private OrderController.OrderForm prepareOrderForm() {
        OrderController.OrderForm orderForm = new OrderController.OrderForm();
        OrderProductDto productDto = new OrderProductDto();
        productDto.setProduct(new Product(1L, "TV Set", 300.00, "http://placehold.it/200x100"));
        productDto.setQuantity(2);
        orderForm.setProductOrders(Collections.singletonList(productDto));
        orderForm.setPaymentMethod("visa");
        orderForm.setShippingCountry("FR");
        orderForm.setShippingMethod("standard");

        return orderForm;
    }
}
