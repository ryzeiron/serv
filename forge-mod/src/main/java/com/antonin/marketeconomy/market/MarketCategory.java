package com.antonin.marketeconomy.market;

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
        return this.displayName;
    }
}
