package com.antonin.marketeconomy.market;

public class MarketEvent {

    public enum Type {
        CRASH,
        BOOM
    }

    private final Type type;
    private final MarketCategory category;
    private int remainingCycles;

    public MarketEvent(Type type, MarketCategory category, int durationCycles) {
        this.type = type;
        this.category = category;
        this.remainingCycles = durationCycles;
    }

    public Type getType() {
        return this.type;
    }

    // category == null signifie que tout le marche est touche, pas une seule categorie
    public MarketCategory getCategory() {
        return this.category;
    }

    public boolean tick() {
        this.remainingCycles--;
        return this.remainingCycles <= 0;
    }
}
