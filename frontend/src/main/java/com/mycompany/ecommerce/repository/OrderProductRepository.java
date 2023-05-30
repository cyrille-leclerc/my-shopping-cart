package com.mycompany.ecommerce.repository;

import com.mycompany.ecommerce.model.OrderProduct;
import com.mycompany.ecommerce.model.OrderProductPK;
import org.springframework.data.repository.CrudRepository;

public interface OrderProductRepository extends CrudRepository<OrderProduct, OrderProductPK> {
}
