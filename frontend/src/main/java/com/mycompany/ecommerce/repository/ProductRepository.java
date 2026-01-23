package com.mycompany.ecommerce.repository;

import com.mycompany.ecommerce.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, CrudRepository<Product, Long>, CustomizedProductRepository {
}
