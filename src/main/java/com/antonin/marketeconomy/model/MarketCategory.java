package com.antonin.marketeconomy.model;

public enum MarketCategory {
    RAW_MATERIALS("Matières premières"),
    ORES("Minerais"),
    CONSUMABLES("Consommables"),
    MOB_DROPS("Butin de mobs"),
    RARE("Objets rares"),
    OTHER("Autre");

    private final String displayName;

    MarketCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static MarketCategory fromConfig(String raw) {
        if (raw == null) {
            return OTHER;
        }
        try {
            return MarketCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
