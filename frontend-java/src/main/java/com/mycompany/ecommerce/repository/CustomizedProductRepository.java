package com.mycompany.ecommerce.repository;

import com.mycompany.ecommerce.model.Product;

import java.util.Optional;

public interface CustomizedProductRepository {
     Optional<Product> doFindByIdWithThrottle(Long id);
}
