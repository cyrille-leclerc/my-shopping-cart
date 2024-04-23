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
        logger.log(Level.INFO, "OpenTelemetry SDK configuration: Add " + BaggageSpanProcessor.class + " and " + BaggageLogRecordProcessor.class);
        autoConfigurationCustomizer
                .addTracerProviderCustomizer(
                        (sdkTracerProviderBuilder, configProperties) -> sdkTracerProviderBuilder.addSpanProcessor(new BaggageSpanProcessor()));
        autoConfigurationCustomizer
                .addLoggerProviderCustomizer(
                        (sdkLoggerProviderBuilder, configProperties) -> sdkLoggerProviderBuilder.addLogRecordProcessor(new BaggageLogRecordProcessor()));
    }
}
