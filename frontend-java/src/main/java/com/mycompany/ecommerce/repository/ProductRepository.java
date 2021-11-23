package com.mycompany.ecommerce.repository;

import com.mycompany.ecommerce.model.Product;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ProductRepository extends CrudRepository<Product, Long>, CustomizedProductRepository {
}
