package com.mycompany.ecommerce.service;

import com.mycompany.ecommerce.exception.ResourceNotFoundException;
import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.repository.ProductRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private ProductRepository productRepository;

    private Cache productCache;

    private boolean cacheMissAttack = false;

    public ProductServiceImpl(ProductRepository productRepository, Cache productCache) {
        this.productRepository = productRepository;
        this.productCache = productCache;
    }

    @Override
    public Iterable<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    @WithSpan
    public Product getProduct(@SpanAttribute("productId") long id) throws ResourceNotFoundException {
        Object cacheKey = cacheMissAttack? UUID.randomUUID().toString() : id;

        final Product product = productCache.get(cacheKey, () -> {
            long beforeInNanos = System.nanoTime();
            try {
                return productRepository.doFindByIdWithThrottle(id).orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
            } finally {
                Span.current().setAttribute("cache.miss", true);
                logger.info("Cache miss for product " + id + ", load from database in " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - beforeInNanos, TimeUnit.NANOSECONDS) + "ms");
            }
        });
        if (product == null) {
            throw new ResourceNotFoundException("product " + id);
        }
        return product;
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    public boolean isCacheMissAttack() {
        return cacheMissAttack;
    }

    public void setCacheMissAttack(boolean cacheMissAttack) {
        this.cacheMissAttack = cacheMissAttack;
    }

    /**
     * Hold dummy ballast to saturate the cache
     */
    private static class ProductReference {
        final static Random RANDOM = new Random();

        Product product;
        byte[] ballast;

        public ProductReference() {
        }

        public ProductReference(Product product) {
            this.product = product;
            this.ballast = new byte[5 * 1024];
            RANDOM.nextBytes(ballast);
        }

        public Product getProduct() {
            return product;
        }

        public void setProduct(Product product) {
            this.product = product;
        }

        public byte[] getBallast() {
            return ballast;
        }

        public void setBallast(byte[] ballast) {
            this.ballast = ballast;
        }
    }
}
