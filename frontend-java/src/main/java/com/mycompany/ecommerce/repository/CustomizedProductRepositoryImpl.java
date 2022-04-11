package com.mycompany.ecommerce.repository;

import com.mycompany.ecommerce.RandomUtils;
import com.mycompany.ecommerce.model.Product;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.extension.annotations.SpanAttribute;
import io.opentelemetry.extension.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;
import java.util.PrimitiveIterator;
import java.util.concurrent.TimeUnit;

public class CustomizedProductRepositoryImpl implements CustomizedProductRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager em;
    private PrimitiveIterator.OfDouble getProductDistribution = RandomUtils.positiveDoubleGaussianDistribution(0.1, 0.05);
    private boolean latencyAttack;

    @Override
    public Optional<Product> doFindByIdWithThrottle(@SpanAttribute("productId") Long id) {
        Span.current().updateName("ProductRepository.doFindById");
        long nanosBefore = System.nanoTime();
        try {
            if (latencyAttack) {
                Query getProductByIdQuery = em.createNativeQuery("select pg_sleep(:sleep), product.* from product where product.id= :id", Product.class);
                getProductByIdQuery.setParameter("sleep", getProductDistribution.nextDouble());
                getProductByIdQuery.setParameter("id", id);
                final List resultList = getProductByIdQuery.getResultList();
                if (resultList.isEmpty()) {
                    return Optional.empty();
                } else if (resultList.size() == 1) {
                    return Optional.of((Product) resultList.get(0));
                } else {
                    throw new NonUniqueResultException(resultList.size() + " entries found for product id " + id + ", 1 was expected");
                }
            } else {
                try {
                    return Optional.of(em.find(Product.class, id));
                } catch (NoResultException e) {
                    return Optional.empty();
                }
            }
        } finally {
            logger.trace("doFindByIdWithThrottle({}): {}ms", id, TimeUnit.MILLISECONDS.convert(System.nanoTime() - nanosBefore, TimeUnit.NANOSECONDS));
        }
    }

    public boolean isLatencyAttack() {
        return latencyAttack;
    }

    public void setLatencyAttack(boolean latencyAttack) {
        this.latencyAttack = latencyAttack;
    }

}
