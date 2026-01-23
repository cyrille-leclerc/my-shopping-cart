package com.mycompany.ecommerce;

import com.mycompany.ecommerce.controller.OrderController;
import com.mycompany.ecommerce.controller.ProductController;
import com.mycompany.ecommerce.dto.OrderProductDto;
import com.mycompany.ecommerce.model.Order;
import com.mycompany.ecommerce.model.OrderProduct;
import com.mycompany.ecommerce.model.Product;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Disabled
@SpringBootTest(classes = { EcommerceApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EcommerceApplicationIntegrationTest {

    private RestTestClient restTestClient;

    @LocalServerPort
    private int port;

    @Autowired private ProductController productController;

    @Autowired private OrderController orderController;

    @BeforeEach
    public void setUp() {
        this.restTestClient = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    public void contextLoads() {
        assertThat(productController, CoreMatchers.notNullValue());
        assertThat(orderController, CoreMatchers.notNullValue());
    }

    @Disabled
    public void givenGetProductsApiCall_whenProductListRetrieved_thenSizeMatchAndListContainsProductNames() {

        ParameterizedTypeReference<Iterable<Product>> bodyType = new ParameterizedTypeReference<>() {
        };
        Iterable<Product> products = restTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody(bodyType)
                .returnResult()
                .getResponseBody();


        Assertions
          .assertThat(products)
          .hasSize(12);
    }

    @Disabled("the postgresql database is not reset")
    @Test
    public void givenGetOrdersApiCall_whenProductListRetrieved_thenSizeMatchAndListContainsProductNames() {


        ParameterizedTypeReference<Iterable<Order>> bodyType = new ParameterizedTypeReference<>() {
        };
        Iterable<Order> orders = restTestClient.get()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(bodyType)
                .returnResult()
                .getResponseBody();


        Assertions
          .assertThat(orders)
          .hasSize(0);
    }

    @Disabled("Not testable without the mock antiFraud service")
    @Test
    public void givenPostOrder_whenBodyRequestMatcherJson_thenResponseContainsEqualObjectProperties() {

        restTestClient.post()
                .uri( "/api/orders")
                .body(prepareOrderForm())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Order.class)
                .consumeWith(response -> {
                    Order order = response.getResponseBody();
                    assert order != null;
                    assertThat(order.getStatus(), is("PAID"));
                    List<OrderProduct> orderProducts = order.getOrderProducts();
                    assertThat(orderProducts.size(), is(1));
                    assertThat(orderProducts.getFirst().getQuantity(),  is(2));
                });


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
