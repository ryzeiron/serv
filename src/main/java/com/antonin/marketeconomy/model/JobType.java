package com.antonin.marketeconomy.model;

public enum JobType {
    HACKER("§5Hacker");

    private final String displayName;

    JobType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static JobType fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return JobType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
