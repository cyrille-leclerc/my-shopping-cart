package com.mycompany.ecommerce.controller;

import co.elastic.apm.api.ElasticApm;
import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping(value = { "", "/" })
    public @NotNull Iterable<Product> getProducts() {
        ElasticApm.currentSpan().setName("products");
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public @NotNull Product getProduct(@PathVariable long id) {
        ElasticApm.currentSpan().setName("product");
        ElasticApm.currentSpan().addLabel("product.id", id);
        return productService.getProduct(id);
    }
}