package com.mycompany.ecommerce.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.ForwardingCache;
import com.mycompany.ecommerce.exception.ResourceNotFoundException;
import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.repository.ProductRepository;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    private ProductRepository productRepository;

    private NoopableCache<Long, Product> productCache;


    public ProductServiceImpl(ProductRepository productRepository, Meter meter) {
        this.productRepository = productRepository;
        this.productCache = new NoopableCache(CacheBuilder.newBuilder().maximumSize(20).expireAfterAccess(10, TimeUnit.SECONDS).build());
        // FIXME record cache statistics
        // OpenTelemetryUtils.observeGoogleGuavaCache(productCache, "product", meter);
    }

    @Override
    public Iterable<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Product getProduct(long id) throws ResourceNotFoundException {
        try {
            return productCache.get(id, () -> {
                long beforeInNanos = System.nanoTime();
                try {
                    return productRepository.doFindByIdWithThrottle(id).orElseThrow(() -> new ResourceNotFoundException("Product " + id + " not found"));
                } finally {
                    Span.current().addEvent("cache.product", Attributes.of(AttributeKey.longKey("id"), id, AttributeKey.booleanKey("miss"), Boolean.TRUE));
                    logger.info("Cache miss for product " + id + ", load from database in " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - beforeInNanos, TimeUnit.NANOSECONDS) + "ms");
                }
            });
        } catch (ExecutionException e) {
            throw new RuntimeException("Failure to load Product '" + id + "'");
        }
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void startCacheAttack() {
        this.productCache.noop = true;
    }

    public void stopCacheAttack() {
        this.productCache.noop = false;
    }

    public boolean isCacheAttackActive() {
        return this.productCache.noop;
    }

    static class NoopableCache<K, V> extends ForwardingCache<K, V> {

        final Cache<K, V> delegate;
        final Cache<K, V> noopCache = CacheBuilder.newBuilder().maximumSize(0).recordStats().build();
        boolean noop;

        public NoopableCache(Cache delegate) {
            this.delegate = delegate;

        }

        @Override
        protected Cache<K, V> delegate() {
            return noop ? noopCache : delegate;
        }
    }
}
