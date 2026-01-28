package com.devsecwatch.backend.model.enums;

public enum Severity {
    CRITICAL, HIGH, MEDIUM, LOW;

    public boolean isHighOrCritical() {
        return this == HIGH || this == CRITICAL;
    }
}
