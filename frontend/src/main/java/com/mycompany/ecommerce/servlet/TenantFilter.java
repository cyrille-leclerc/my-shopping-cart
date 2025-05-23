package com.mycompany.ecommerce.servlet;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Component
@Order(1)
public class TenantFilter implements Filter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    static final Random random = new Random();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest httpServletRequest) {
            doFilter(filterChain, httpServletRequest, (HttpServletResponse) servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private void doFilter(FilterChain filterChain, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Tenant tenant = getTenant(request);

        logger.atDebug()
                //.addKeyValue("tenant.id", tenant.getId())
                .addKeyValue("tenant.short_code", tenant.getShortCode())
                .addKeyValue("http.request.method", request.getMethod())
                .addKeyValue("url.path", request.getRequestURI())
                .addKeyValue("request['tenant']", request.getParameter("tenant"))
                .log("Setting tenant");

        // The BaggageSpanProcessor promotes baggage to span attributes on span creation
        // and the HTTP Server span is already created by the time this filter is called
        // set the tenant.id & tenant.short_code attributes on the current span
        Span.current()
                //.setAttribute("tenant.id", tenant.getId())
                .setAttribute("tenant.short_code", tenant.getShortCode());
        Baggage baggage = Baggage.builder()
                //.put("tenant.id", tenant.getId())
                .put("tenant.short_code", tenant.getShortCode())
                .build();
        Tenant.setCurrent(tenant);
        try (var ignored = baggage.makeCurrent()) {
            filterChain.doFilter(request, response);
        } finally {
            Tenant.clearCurrent();
        }
    }

    // todo implement real business logic to extract the tenant-id from the request
    private static Tenant getTenant(HttpServletRequest request) {
        // retrieve tenant from HTTP session or from request parameter or generate a random one
        HttpSession session = request.getSession();

        return Optional.ofNullable(session.getAttribute("tenant"))
                .map(value -> (Tenant) value)
                .orElseGet(() -> {
                    Tenant t = Optional.ofNullable(request.getParameter("tenant"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Tenant::newFromShortCode)
                            .orElseGet(Tenant::nextRandomTenant);
                    session.setAttribute("tenant", t);
                    return t;
                });
    }

    public static class Tenant {
        private final static Random random = new Random();

        private static final List<Tenant> tenants = List.of(
                newFromShortCode("T_ALPHA"),
                newFromShortCode("T_BETA"),
                newFromShortCode("T_GAMMA"),
                newFromShortCode("T_DELTA")
        );

        public static Tenant nextRandomTenant() {
            return tenants.get(random.nextInt(tenants.size()));
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
