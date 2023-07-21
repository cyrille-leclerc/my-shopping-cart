package com.mycompany.ecommerce.controller;

import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    final Logger logger = LoggerFactory.getLogger(getClass());

    ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;

    }

    @GetMapping(value = {"", "/"})
    public @Nonnull
    Iterable<Product> getProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public @Nonnull Product getProduct(@PathVariable long id, HttpServletRequest request) {
        return productService.getProduct(id);
    }
}
