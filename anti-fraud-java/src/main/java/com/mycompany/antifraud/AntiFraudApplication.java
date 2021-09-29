package com.mycompany.antifraud;

import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AntiFraudApplication {

    public static void main(String[] args) {
        SpringApplication.run(AntiFraudApplication.class, args);
    }

    @Bean
    public Meter getOpenTelemetryMeter() {
        return GlobalMeterProvider.get().get("anti-fraud");
    }
}
