package com.mycompany.ecommerce.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ProductControllerTest {

    @Disabled("UnsatisfiedLink no asyncProfiler in java.library.path")
    @Test
    void getImage() throws Exception {
        ProductController productController = new ProductController(null);
        long nanosBefore = System.nanoTime();
        productController.getImage(1);
        System.out.println("conversion duration = " + TimeUnit.MILLISECONDS.convert((System.nanoTime()-nanosBefore), TimeUnit.NANOSECONDS) + "ms");
    }
}