package com.mycompany.antifraud;

import com.lightstep.tracer.jre.JRETracer;
import com.lightstep.tracer.shared.Options;
import io.opentracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:cyrille@cyrilleleclerc.com">Cyrille Le Clerc</a>
 */
@Configuration
public class LightStepConfiguration {

    protected final Logger logger = LoggerFactory.getLogger(LightStepConfiguration.class);

    private JRETracer tracer;

    /**
     * @see org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration
     */
    private BuildProperties buildProperties;

    private String accessToken;

    @PostConstruct
    public void postConstruct() throws MalformedURLException, UnknownHostException {
        String serviceName = buildProperties.getGroup().replace('.', '-') + "_" + buildProperties.getArtifact().replace('-', '_');
        Options opts = new Options.OptionsBuilder()
                .withAccessToken(accessToken)
                .withComponentName(serviceName)
                .withVerbosity(Options.VERBOSITY_DEBUG) /* TODO REMOVE ME */
                .withMaxReportingIntervalMillis(500) /* TODO REMOVE ME */
                .withTag("service.version", buildProperties.getVersion())
                .withTag("hostname", InetAddress.getLocalHost().getHostName())
                .build();
        logger.info("Start LightStep monitoring for service '{}'", serviceName);
        tracer = new JRETracer(opts);
    }

    @PreDestroy
    public void preDestroy() {
        tracer.flush(500);
    }

    @Bean
    public Tracer getTracer() {
        return tracer;
    }

    @Autowired
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Value("${lightStep.accessToken}")
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
