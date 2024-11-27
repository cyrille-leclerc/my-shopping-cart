package com.mycompany.ecommerce.servlet;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;

@Component
@Order(1)
public class TenantIdFilter implements Filter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    static final Random random = new Random();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // todo implement real business logic to extract the tenant-id from the request
        String tenantId = "tenant-" + random.nextInt(3);

        logger.atDebug().addKeyValue("tenant_id", tenantId).log("Setting tenant id");
        // The com.mycompany.opentelemetry.BaggageSpanProcessor promotes baggage to span attributes on span creation
        // and the HTTP Server span is already created by the time this filter is called
        // set the tenant_id attribute on the current span
        Span.current().setAttribute("tenant_id", tenantId); // th
        var baggage = Baggage.builder().put("tenant_id", tenantId).build();
        try (var ignored = baggage.makeCurrent()) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
