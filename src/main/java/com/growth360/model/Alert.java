package com.growth360.model;

import java.time.LocalDateTime;

/**
 * Record representing a system-detected business risk alert.
 */
public record Alert(
        String id,
        AlertType type,
        Severity severity,
        String title,
        String description,
        double metricValue,
        LocalDateTime timestamp
) {
    public enum AlertType {
        INVENTORY,
        REVENUE,
        EXPENSE,
        CUSTOMER
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    public Alert(String id, AlertType type, Severity severity, String title, String description, double metricValue) {
        this(id, type, severity, title, description, metricValue, LocalDateTime.now());
    }
}
