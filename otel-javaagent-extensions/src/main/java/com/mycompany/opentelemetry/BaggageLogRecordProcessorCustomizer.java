package com.mycompany.opentelemetry;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;

import java.util.List;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class BaggageLogRecordProcessorCustomizer implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
        autoConfigurationCustomizer.addLoggerProviderCustomizer(
                (sdkLoggerProviderBuilder, config) -> {
                    addLogRecordProcessor(sdkLoggerProviderBuilder, config);
                    return sdkLoggerProviderBuilder;
                });
    }

    private static void addLogRecordProcessor(
            SdkLoggerProviderBuilder sdkLoggerProviderBuilder, ConfigProperties config) {
        List<String> keys =
                config.getList("otel.java.experimental.log-attributes.copy-from-baggage.include");

        if (keys.isEmpty()) {
            return;
        }

        sdkLoggerProviderBuilder.addLogRecordProcessor(createProcessor(keys));
    }

    static LogRecordProcessor createProcessor(List<String> keys) {
        if (keys.size() == 1 && keys.get(0).equals("*")) {
            return BaggageLogRecordProcessor.allowAllBaggageKeys();
        }
        return new BaggageLogRecordProcessor(keys::contains);
    }
}
