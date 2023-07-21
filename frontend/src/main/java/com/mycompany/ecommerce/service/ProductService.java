package com.mycompany.ecommerce.service;

import com.mycompany.ecommerce.exception.ResourceNotFoundException;
import com.mycompany.ecommerce.model.Product;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
public interface ProductService {

    @NotNull Iterable<Product> getAllProducts();

    /**
     * @throws ResourceNotFoundException
     */
    @NotNull
    Product getProduct(@Min(value = 1L, message = "Invalid product ID.") long id) throws ResourceNotFoundException;

    Product save(Product product);
}
