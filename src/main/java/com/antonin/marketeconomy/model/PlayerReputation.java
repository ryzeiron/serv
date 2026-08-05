package com.antonin.marketeconomy.model;

public class PlayerReputation {
    private double trustScore = 50.0;
    private long totalTrades = 0L;
    private double totalSpent = 0.0;
    private long lastInteractionMillis = 0L;

    public double getTrustScore() {
        return this.trustScore;
    }

    public void addTrust(double delta) {
        this.trustScore = Math.max(0.0, Math.min(100.0, this.trustScore + delta));
    }

    public void recordSpend(double amount) {
        this.totalTrades++;
        this.totalSpent += amount;
    }

    public void touchInteraction() {
        this.lastInteractionMillis = System.currentTimeMillis();
    }

    public long getTotalTrades() {
        return this.totalTrades;
    }

    public double getTotalSpent() {
        return this.totalSpent;
    }

    public long getLastInteractionMillis() {
        return this.lastInteractionMillis;
    }

    public void setTrustScore(double trustScore) {
        this.trustScore = trustScore;
    }

    public void setTotalTrades(long totalTrades) {
        this.totalTrades = totalTrades;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public void setLastInteractionMillis(long lastInteractionMillis) {
        this.lastInteractionMillis = lastInteractionMillis;
    }
}
