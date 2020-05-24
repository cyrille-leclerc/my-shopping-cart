package com.mycompany.ecommerce;

import io.opentelemetry.exporters.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author <a href="mailto:cyrille@cyrilleleclerc.com">Cyrille Le Clerc</a>
 */
@Configuration
public class OpenTelemetryConfiguration {
    protected final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfiguration.class);
    private Tracer tracer;

    /**
     * @see org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration
     */
    private BuildProperties buildProperties;

    @PostConstruct
    public void PostConstruct() {
        String serviceVersion = buildProperties.getVersion();
        String serviceName = buildProperties.getGroup().replace('.', '-') + "_" + buildProperties.getArtifact().replace('.', '-');
        String applicationPackages = getClass().getPackage().getName();


        // Configure the LoggingExporter as our exporter.
        LoggingSpanExporter spanExporter = new LoggingSpanExporter();

        SpanProcessor spanProcessor = SimpleSpansProcessor.create(spanExporter);
        TracerSdkProvider tracerSdkProvider = OpenTelemetrySdk.getTracerProvider();
        tracerSdkProvider.addSpanProcessor(spanProcessor);


        // Create the tracer
        this.tracer = tracerSdkProvider.get(serviceName);
    }

    @Bean
    public Tracer getTracer() {
        return tracer;
    }

    @PreDestroy
    public void preDestroy(){
        OpenTelemetrySdk.getTracerProvider().shutdown();
    }

    @Autowired
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

}
