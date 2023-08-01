package com.mycompany.ecommerce.controller;

import com.mycompany.ecommerce.repository.CustomizedProductRepositoryImpl;
import com.mycompany.ecommerce.service.ProductServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chaos")
public class ChaosController {

    ProductServiceImpl productServiceImpl;
    CustomizedProductRepositoryImpl customizedProductRepositoryImpl;

    public ChaosController(
            ProductServiceImpl productServiceImpl,
            CustomizedProductRepositoryImpl customizedProductRepositoryImpl) {
        this.productServiceImpl = productServiceImpl;
        this.customizedProductRepositoryImpl = customizedProductRepositoryImpl;;
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
