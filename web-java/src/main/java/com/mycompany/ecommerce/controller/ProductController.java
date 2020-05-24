package com.mycompany.ecommerce.controller;

import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.service.ProductService;
import io.opentelemetry.context.Scope;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    ProductService productService;

    Tracer tracer;

    public ProductController(ProductService productService, Tracer tracer) {
        this.productService = productService;
        this.tracer = tracer;
    }

    @GetMapping(value = {"", "/"})
    public @NotNull Iterable<Product> getProducts() {
        Span span = tracer.spanBuilder("products").startSpan();
        try (Scope scope = tracer.withSpan(span)) {
            return productService.getAllProducts();
        } finally {
            span.end();
        }

    }

    @GetMapping("/{id}")
    public @NotNull Product getProduct(@PathVariable long id) {
        Span span = tracer.spanBuilder("products").startSpan();
        try {
            span.setAttribute("product.id", id);
            return productService.getProduct(id);
        } finally {
            span.end();
        }
    }
}
