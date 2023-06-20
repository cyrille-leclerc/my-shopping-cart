package com.mycompany.ecommerce;

import com.google.common.collect.Lists;
import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.service.ProductServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { EcommerceApplication.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductServiceImplTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductServiceImpl productServiceImpl;

    @Test
    public void testFindAllProducts(){
        List<Product> products = Lists.newArrayList(productServiceImpl.getAllProducts());
        System.out.println(products);
    }

    @Test
    public void testFindOneProduct (){
        Product product = productServiceImpl.getProduct(1);
        System.out.println(product);
    }

}
