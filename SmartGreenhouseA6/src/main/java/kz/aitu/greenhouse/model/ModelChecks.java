package kz.aitu.greenhouse.model;

import java.util.Objects;

/** Shared validation keeps the immutable data records small. */
final class ModelChecks {
    private ModelChecks() {
    }

    static void text(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    static void nonNegative(double value, String field) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(field + " must be finite and non-negative");
        }
    }
}
