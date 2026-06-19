package com.autocase.entity;

/**
 * 严重程度枚举
 */
public enum Severity {
    P0("P0 - 致命"),
    P1("P1 - 严重"),
    P2("P2 - 一般"),
    P3("P3 - 轻微");

    private final String displayName;

    Severity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
