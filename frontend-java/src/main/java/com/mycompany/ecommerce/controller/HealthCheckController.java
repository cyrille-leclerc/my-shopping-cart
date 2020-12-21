package com.mycompany.ecommerce.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@ManagedResource
@RestController
public class HealthCheckController {
    private final static Random RANDOM = new Random();
    /**
     * from zero to 100%
     */
    private int successRatioInPercentage = 95;

    @RequestMapping("/health-check")
    public ResponseEntity<String> healthCheck(){
        if(this.successRatioInPercentage - RANDOM.nextInt(100) >= 0) {
            return ResponseEntity.ok().body("up");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("down");
        }
    }

    @ManagedAttribute
    public int getSuccessRatioInPercentage() {
        return successRatioInPercentage;
    }

    @ManagedAttribute
    public void setSuccessRatioInPercentage(int successRatioInPercentage) {
        this.successRatioInPercentage = successRatioInPercentage;
    }
}
