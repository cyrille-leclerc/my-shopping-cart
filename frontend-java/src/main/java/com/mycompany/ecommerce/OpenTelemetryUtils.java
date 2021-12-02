package com.mycompany.ecommerce;

import com.google.common.cache.Cache;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.RedisCache;

public class OpenTelemetryUtils {

    /**
     * https://github.com/prometheus/client_java/blob/master/simpleclient_guava/src/main/java/io/prometheus/client/guava/cache/CacheMetricsCollector.java
     * <pre>{@code
     * guava_cache_hit_total{cache="mycache"} 10.0
     * guava_cache_miss_total{cache="mycache"} 3.0
     * guava_cache_requests_total{cache="mycache"} 13.0
     * guava_cache_eviction_total{cache="mycache"} 1.0
     * guava_cache_size{cache="mycache"} 5.0
     * }
     */
    public static void observeGoogleGuavaCache(Cache cache, String cacheName, Meter meter) {
        final Attributes cacheNameAttribute = Attributes.of(AttributeKey.stringKey("cache"), cacheName);
        meter.counterBuilder("guava_cache_hit_total")
                .setDescription("Cache hit totals")
                .buildWithCallback(longResult -> longResult.observe(cache.stats().hitCount(), cacheNameAttribute));
        meter.counterBuilder("guava_cache_miss_total")
                .setDescription("Cache miss totals")
                .buildWithCallback(longResult -> longResult.observe(cache.stats().missCount(), cacheNameAttribute));
        meter.counterBuilder("guava_cache_requests_total")
                .setDescription("Cache requests totals")
                .buildWithCallback(longResult -> longResult.observe(cache.stats().requestCount(), cacheNameAttribute));
        meter.counterBuilder("guava_cache_eviction_total")
                .setDescription("Cache evictions totals")
                .buildWithCallback(longResult -> longResult.observe(cache.stats().evictionCount(), cacheNameAttribute));
        meter.counterBuilder("guava_cache_size")
                .setDescription("Cache size")
                .buildWithCallback(longResult -> longResult.observe(cache.size(), cacheNameAttribute));
    }

    public static void observeRedisCache(RedisCache redisCache, Meter meter){
        final Attributes cacheNameAttribute = Attributes.of(AttributeKey.stringKey("cache"), redisCache.getName());

        meter.counterBuilder("redis_cache_gets")
                .setDescription("Cache gets totals")
                .buildWithCallback(longResult -> longResult.observe(redisCache.getStatistics().getGets(), cacheNameAttribute));
        meter.counterBuilder("redis_cache_puts")
                .setDescription("Cache puts totals")
                .buildWithCallback(longResult -> longResult.observe(redisCache.getStatistics().getPuts(), cacheNameAttribute));
        meter.counterBuilder("redis_cache_misses")
                .setDescription("Cache misses totals")
                .buildWithCallback(longResult -> longResult.observe(redisCache.getStatistics().getMisses(), cacheNameAttribute));
        // TODO overlap between gets, hits,and misses
        meter.counterBuilder("redis_cache_hits")
                .setDescription("Cache hits totals")
                .buildWithCallback(longResult -> longResult.observe(redisCache.getStatistics().getHits(), cacheNameAttribute));
    }
}
