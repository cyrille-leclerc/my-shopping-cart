package com.mycompany.ecommerce.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mycompany.ecommerce.exception.ResourceNotFoundException;
import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.repository.ProductRepository;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private ProductRepository productRepository;
    Cache<Long, Product> productCache;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
        this.productCache = CacheBuilder.newBuilder().maximumSize(2).recordStats().build();
        GuavaCacheMetrics.monitor(Metrics.globalRegistry, this.productCache, "productCache");
    }

    @Override
    public Iterable<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProduct(long id) throws ResourceNotFoundException {
        try {
            return productCache.get(id, () -> productRepository
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product '" + id + "' not found")));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }
}
