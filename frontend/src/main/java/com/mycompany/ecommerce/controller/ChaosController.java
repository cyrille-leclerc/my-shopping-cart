package com.mycompany.ecommerce.controller;

import com.mycompany.ecommerce.repository.CustomizedProductRepositoryImpl;
import com.mycompany.ecommerce.service.ProductServiceImpl;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chaos")
public class ChaosController {

    ProductServiceImpl productServiceImpl;
    CustomizedProductRepositoryImpl customizedProductRepositoryImpl;
    ToxiproxyClient toxiproxyClient;
    RedisConnectionFactory redisConnectionFactory;

    public ChaosController(
            ProductServiceImpl productServiceImpl,
            CustomizedProductRepositoryImpl customizedProductRepositoryImpl,
            ToxiproxyClient toxiproxyClient) {
        this.productServiceImpl = productServiceImpl;
        this.customizedProductRepositoryImpl = customizedProductRepositoryImpl;
        this.toxiproxyClient = toxiproxyClient;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @RequestMapping("/attack/cache/enable")
    public String enableCacheAttack(){
        productServiceImpl.setCacheMissAttack(true);
        return "Cache miss attack started";
    }
    @RequestMapping("/attack/cache/disable")
    public String disableCacheAttack(){
        productServiceImpl.setCacheMissAttack(false);
        return "Cache miss attack stopped";
    }

    @RequestMapping("/attack/latency/enable")
    public String enableLatencyAttack(){
        customizedProductRepositoryImpl.setLatencyAttack(true);
        return "Latency attack started";
    }
    @RequestMapping("/attack/latency/disable")
    public String disableLatencyAttack(){
        customizedProductRepositoryImpl.setLatencyAttack(false);
        return "Latency attack stopped";
    }
}
