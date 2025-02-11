package com.mycompany.ecommerce.servlet;

import com.mycompany.ecommerce.model.Tenant;
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
public class TenantFilter implements Filter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    static final Random random = new Random();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // todo implement real business logic to extract the tenant-id from the request
        Tenant tenant = Tenant.nextRandomTenant();

        logger.atDebug()
                .addKeyValue("tenant.id", tenant.getId())
                .addKeyValue("tenant.shortcode", tenant.getShortCode())
                .log("Setting tenant");

        Tenant.setCurrent(tenant);
        // The BaggageSpanProcessor promotes baggage to span attributes on span creation
        // and the HTTP Server span is already created by the time this filter is called
        // set the tenant.id & tenant.shortcode attributes on the current span
        Span.current()
                .setAttribute("tenant.id", tenant.getId())
                .setAttribute("tenant.shortcode", tenant.getShortCode());
        Baggage baggage = Baggage.builder()
                .put("tenant.id", tenant.getId())
                .put("tenant.shortcode", tenant.getShortCode())
                .build();
        try (var ignored = baggage.makeCurrent()) {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            Tenant.clearCurrent();
        }
    }
}
