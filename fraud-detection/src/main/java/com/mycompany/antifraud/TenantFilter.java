package com.mycompany.antifraud;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Component
@Order(1)
public class TenantFilter implements Filter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest request) {
            doFilter(filterChain, request, (HttpServletResponse) servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private void doFilter(FilterChain filterChain, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        //String tenantId = Baggage.current().getEntryValue("tenant.id");
        String tenantShortCode = Baggage.current().getEntryValue("tenant.short_code");
        Tenant tenant = Optional.ofNullable(tenantShortCode)
                .map(id -> Tenant.newFromShortCode(tenantShortCode))
                .orElseGet(Tenant::unknown);

        logger.debug("Tenant: {}", tenant);

        // The BaggageSpanProcessor promotes baggage to span attributes on span creation
        // and the HTTP Server span is already created by the time this filter is called
        // set the tenant.id & tenant.short_code attributes on the current span
        Span.current()
                //.setAttribute("tenant.id", tenant.getId())
                .setAttribute("tenant.short_code", tenant.getShortCode());
        Tenant.setCurrent(tenant);
        try{
            filterChain.doFilter(request, response);
        } finally {
            Tenant.clearCurrent();
        }
    }

    public static class Tenant {

        public static Tenant unknown() {
            return new Tenant("#unknown#", "#unknown#");
        }

        public static Tenant newFromShortCode(String shortCode) {
            return new Tenant("tid_" + shortCode.hashCode(), shortCode);
        }

        private final static ThreadLocal<Tenant> currentTenant = new ThreadLocal<>();

        public static Tenant current() {
            return currentTenant.get();
        }
        public static void setCurrent(Tenant tenant) {
            currentTenant.set(tenant);
        }
        public static void clearCurrent() {
            currentTenant.remove();
        }

        final String id;
        final String shortCode;

        private Tenant(String id, String shortCode) {
            this.id = id;
            this.shortCode = shortCode;
        }

        public String getId() {
            return id;
        }

        public String getShortCode() {
            return shortCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Tenant tenant = (Tenant) o;
            return Objects.equals(id, tenant.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }

        @Override
        public String toString() {
            return "Tenant{" +
                    "id='" + id + '\'' +
                    ", shortCode='" + shortCode + '\'' +
                    '}';
        }
    }
}
