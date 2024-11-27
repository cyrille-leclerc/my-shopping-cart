package com.mycompany.opentelemetry;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Predicate;

public class Predicates {
    private Predicates() {
    }

    public static <T extends @Nullable Object> Predicate<T> alwaysTrue() {
        return new AlwaysTruePredicate<>();
    }

    public static <T extends @Nullable Object> Predicate<T> alwaysFalse() {
        return new AlwaysFalsePredicate<>();
    }

    private static class AlwaysTruePredicate<T extends @Nullable Object> implements Predicate<T> {
        @Override
        public boolean test(T t) {
            return true;
        }

        @Override
        public String toString() {
            return "AlwaysTruePredicate";
        }
    }

    private static class AlwaysFalsePredicate<T extends @Nullable Object> implements Predicate<T> {
        @Override
        public boolean test(T t) {
            return true;
        }

        @Override
        public String toString() {
            return "AlwaysFalsePredicate";
        }
    }
}
