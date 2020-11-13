package com.mycompany.ecommerce;

import com.google.common.cache.Cache;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.api.metrics.Meter;


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
    public static void observeGoogleGuavaCache(Cache cache, String cacheName) {
        Meter.getDefault().longValueObserverBuilder("guava_cache_hit_total")
                .setDescription("Cache hit totals").build()
                .setCallback(result -> result.observe(cache.stats().hitCount(), Labels.of("cache", cacheName)));
        Meter.getDefault().longValueObserverBuilder("guava_cache_miss_total")
                .setDescription("Cache miss totals").build()
                .setCallback(result -> result.observe(cache.stats().missCount(), Labels.of("cache", cacheName)));
        Meter.getDefault().longValueObserverBuilder("guava_cache_requests_total")
                .setDescription("Cache requests totals").build()
                .setCallback(result -> result.observe(cache.stats().requestCount(), Labels.of("cache", cacheName)));
        Meter.getDefault().longValueObserverBuilder("guava_cache_eviction_total")
                .setDescription("Cache evictions totals").build()
                .setCallback(result -> result.observe(cache.stats().evictionCount(), Labels.of("cache", cacheName)));
        Meter.getDefault().longUpDownSumObserverBuilder("guava_cache_size")
                .setDescription("Cache size").build()
                .setCallback(result -> result.observe(cache.size(), Labels.of("cache", cacheName)));
    }
}
