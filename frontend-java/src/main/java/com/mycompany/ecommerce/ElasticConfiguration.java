package com.mycompany.ecommerce;

import co.elastic.apm.attach.ElasticApmAttacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:cyrille@cyrilleleclerc.com">Cyrille Le Clerc</a>
 */
@Configuration
public class ElasticConfiguration {

    protected final Logger logger = LoggerFactory.getLogger(ElasticConfiguration.class);

    /**
     * @see org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration
     */
    private BuildProperties buildProperties;

    @PostConstruct
    public void postConstruct() throws IOException {
        String serviceVersion = buildProperties.getVersion();
        String serviceName = buildProperties.getGroup().replace('.', '-') + "_" + buildProperties.getArtifact().replace('.', '-');
        String applicationPackages = getClass().getPackage().getName();

        Map<String, String> configuration = new HashMap<>();
        // https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-service-name
        configuration.put("service_name", serviceName);
        // https://www.elastic.co/guide/en/apm/agent/java/current/config-core.html#config-service-version
        configuration.put("service_version", serviceVersion);
        // https://www.elastic.co/guide/en/apm/agent/java/current/config-stacktrace.html#config-application-packages
        configuration.put("application_packages", applicationPackages);
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("elasticapm.properties");
        if (in != null) {
            Properties properties = new Properties();
            properties.load(in);
            for (final String name : properties.stringPropertyNames())
                configuration.put(name, properties.getProperty(name));
        }

        // https://www.elastic.co/guide/en/apm/agent/java/current/config-logging.html#config-enable-log-correlation
        configuration.put("enable_log_correlation", "true");
        // warning may contain secret `secret_token`
        logger.debug("Load ElasticAPM with configuration {}", configuration);
        ElasticApmAttacher.attach(configuration);
    }

    @Autowired
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }
}