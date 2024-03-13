package com.mycompany.ecommerce;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.service.ProductService;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
import java.util.Objects;

@SpringBootApplication
public class EcommerceApplication {

    public static final String AMQP_EXCHANGE = "ecommerce-exchange";
    public static final String AMQP_ROUTING_KEY = "queue.order";
    public static final String AMQP_QUEUE = "order";

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }

    @Bean
    CommandLineRunner runner(ProductService productService) {
        return args -> {
            productService.save(new Product(1L, "TV Set", 300.00, "http://placehold.it/200x100"));
            productService.save(new Product(2L, "Game Console", 200.00, "http://placehold.it/200x100"));
            productService.save(new Product(3L, "Sofa", 100.00, "http://placehold.it/200x100"));
            productService.save(new Product(4L, "Ice cream", 5.00, "http://placehold.it/200x100"));
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
    public Queue queue() {
        return new Queue(AMQP_QUEUE, false);
    }

    @Bean
    public Exchange exchange() {
        return new TopicExchange(AMQP_EXCHANGE, false, true);
    }

    @Bean
    Binding binding(Queue queue, Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(AMQP_ROUTING_KEY).noargs();
    }

    @Bean
    public MessageConverter amqpMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    CommandLineRunner initRabbitMq(ConnectionFactory connectionFactory, Queue queue, Exchange exchange, Binding binding) {
        return args ->
        {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
            rabbitAdmin.declareQueue(queue);
            rabbitAdmin.declareExchange(exchange);
            rabbitAdmin.declareBinding(binding);
        };
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplateBuilder().setReadTimeout(Duration.of(1200, ChronoUnit.MILLIS)).build();
    }

    @Bean
    public Module getJacksonHibernate5Module() {
        return new Hibernate6Module();
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
        OpenTelemetryUtils.observeRedisCache(Objects.requireNonNull(productCache), meter);
        return productCache;
    }
}
