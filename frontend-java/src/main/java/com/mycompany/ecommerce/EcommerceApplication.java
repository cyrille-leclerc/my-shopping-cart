package com.mycompany.ecommerce;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.service.ProductService;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }

    @Bean
    CommandLineRunner runner(ProductService productService) {
        return args -> {
            productService.save(new Product(1L, "TV Set", 300.00, "http://placehold.it/200x100"));
            productService.save(new Product(2L, "Game Console", 200.00, "http://placehold.it/200x100"));
            productService.save(new Product(3L, "Sofa", 100.00, "http://placehold.it/200x100"));
            productService.save(new Product(4L, "Icecream", 5.00, "http://placehold.it/200x100"));
            productService.save(new Product(5L, "Beer", 3.00, "http://placehold.it/200x100"));
            productService.save(new Product(6L, "Phone", 500.00, "http://placehold.it/200x100"));
            productService.save(new Product(7L, "Watch", 30.00, "http://placehold.it/200x100"));
            productService.save(new Product(8L, "USB Cable", 4.00, "http://placehold.it/200x100"));
            productService.save(new Product(9L, "USB-C Cable", 5.00, "http://placehold.it/200x100"));
            productService.save(new Product(10L, "Micro USB Cable", 3.00, "http://placehold.it/200x100"));
            productService.save(new Product(11L, "Lightning Cable", 9.00, "http://placehold.it/200x100"));
            productService.save(new Product(12L, "USB C adapter", 5.00, "http://placehold.it/200x100"));
        };
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplateBuilder().setReadTimeout(Duration.of(1200, ChronoUnit.MILLIS)).build();
    }

    @Bean
    public Module getJacksonHibernate5Module() {
        return new Hibernate5Module();
    }

    @Bean
    public Meter getOpenTelemetryMeter() {
        return GlobalOpenTelemetry.get().getMeter("frontend");
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, RedisCacheConfiguration redisCacheConfiguration) {
        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration)
                .enableStatistics()
                .build();
    }

    @Bean("productCache")
    public RedisCache getProductCache(RedisCacheManager cacheManager, Meter meter) {
        RedisCache productCache = (RedisCache) cacheManager.getCache("productCache");
        OpenTelemetryUtils.observeRedisCache(productCache, meter);
        return productCache;
    }

    @Bean
    public ToxiproxyClient toxiproxyClient() {
        // FIXME get hostname and port from cfg
        return new ToxiproxyClient("127.0.0.1", 8474);
    }
}
