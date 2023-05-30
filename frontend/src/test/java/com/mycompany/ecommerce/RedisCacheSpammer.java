package com.mycompany.ecommerce;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.types.Expiration;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RedisCacheSpammer {
    public static void main(String[] args) {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration());
        connectionFactory.afterPropertiesSet();
        Random random = new Random();

        final RedisConnection connection = connectionFactory.getConnection();
        for (int i = 0; i < 3_000; i++) {
            final byte[] value = new byte[1024];
            random.nextBytes(value);
            connection.set(
                    ("spam-key-" + i).getBytes(StandardCharsets.UTF_8),
                    value/*,
                    Expiration.from(365, TimeUnit.DAYS),
                    RedisStringCommands.SetOption.upsert()*/);
        }



        // connection.flushDb();
    }
}
