package com.antonin.marketeconomy.reputation;

public class PlayerReputation {
    private double trustScore = 50.0;
    private long totalTrades = 0L;
    private double totalSpent = 0.0;

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

    public long getTotalTrades() {
        return this.totalTrades;
    }

    public double getTotalSpent() {
        return this.totalSpent;
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
}
