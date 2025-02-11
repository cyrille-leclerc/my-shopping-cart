package com.mycompany.ecommerce.model;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Tenant {
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

    private static Tenant newFromShortCode(String shortCode) {
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
