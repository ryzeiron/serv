package com.antonin.marketeconomy.job;

public enum JobType {
    HACKER("§5Hacker", 10),
    MINEUR("§6Mineur", 45);

    private final String displayName;
    private final int maxLevel;

    JobType(String displayName, int maxLevel) {
        this.displayName = displayName;
        this.maxLevel = maxLevel;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getMaxLevel() {
        return this.maxLevel;
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
