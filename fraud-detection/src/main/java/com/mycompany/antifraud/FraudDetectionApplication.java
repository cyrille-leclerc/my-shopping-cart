package com.mycompany.antifraud;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FraudDetectionApplication {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionApplication.class, args);
    }

    @Bean
    public Meter getOpenTelemetryMeter() {
        return GlobalOpenTelemetry.get().getMeter("anti-fraud");
    }
}
