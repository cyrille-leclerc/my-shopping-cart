package com.mycompany.opentelemetry;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class MyAutoConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {

    private final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
        logger.log(Level.FINE,() -> "OpenTelemetry SDK configuration: Add " + BaggageSpanProcessor.class + " and " + BaggageLogRecordProcessor.class);
        autoConfigurationCustomizer
                .addTracerProviderCustomizer(
                        (tracerProviderBuilder, configProperties) -> tracerProviderBuilder.addSpanProcessor(new BaggageSpanProcessor()));
        autoConfigurationCustomizer
                .addLoggerProviderCustomizer(
                        (loggerProviderBuilder, configProperties) -> loggerProviderBuilder.addLogRecordProcessor(new BaggageLogRecordProcessor()));
    }
}
