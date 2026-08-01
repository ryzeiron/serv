package com.antonin.marketeconomy.model;

import java.util.LinkedList;
import org.bukkit.Material;

public class MarketItem {
    private final Material material;
    private final String displayName;
    private final double basePrice;
    private double currentPrice;
    private long stock;
    private long recentBought = 0L;
    private long recentSold = 0L;
    private final LinkedList<Double> priceHistory = new LinkedList();
    private final int historyMaxLength;

    public MarketItem(Material material, String displayName, double basePrice, long initialStock, int historyMaxLength) {
        this.material = material;
        this.displayName = displayName;
        this.basePrice = basePrice;
        this.currentPrice = basePrice;
        this.stock = initialStock;
        this.historyMaxLength = historyMaxLength;
        this.priceHistory.add(basePrice);
    }

    public Material getMaterial() {
        return this.material;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public double getBasePrice() {
        return this.basePrice;
    }

    public double getCurrentPrice() {
        return this.currentPrice;
    }

    public long getStock() {
        return this.stock;
    }

    public double getBuyPrice() {
        return this.round2(this.currentPrice * 1.05);
    }

    public double getSellPrice() {
        return this.round2(this.currentPrice * 0.95);
    }

    public void registerBuy(long amount) {
        this.recentBought += amount;
        this.stock = Math.max(0L, this.stock - amount);
    }

    public void registerSell(long amount) {
        this.recentSold += amount;
        this.stock += amount;
    }

    public void recalculatePrice(double sensitivity, double minMultiplier, double maxMultiplier) {
        long net = this.recentBought - this.recentSold;
        long totalActivity = this.recentBought + this.recentSold;
        if (totalActivity > 0L) {
            double pressure = (double)net / (double)Math.max(1L, totalActivity);
            double change = 1.0 + pressure * sensitivity * (double)Math.min(totalActivity, 200L);
            this.currentPrice *= change;
        }
        if (totalActivity == 0L) {
            this.currentPrice += (this.basePrice - this.currentPrice) * 0.01;
        }
        double min = this.basePrice * minMultiplier;
        double max = this.basePrice * maxMultiplier;
        this.currentPrice = Math.max(min, Math.min(max, this.currentPrice));
        this.recentBought = 0L;
        this.recentSold = 0L;
        this.priceHistory.add(this.round2(this.currentPrice));
        if (this.priceHistory.size() > this.historyMaxLength) {
            this.priceHistory.removeFirst();
        }
    }

    public LinkedList<Double> getPriceHistory() {
        return this.priceHistory;
    }

    public String getTrendArrow() {
        if (this.priceHistory.size() < 2) {
            return "\u2192";
        }
        double previous = this.priceHistory.get(this.priceHistory.size() - 2);
        if (this.currentPrice > previous) {
            return "\u2191";
        }
        if (this.currentPrice < previous) {
            return "\u2193";
        }
        return "\u2192";
    }

    private double round2(double value) {
        return (double)Math.round(value * 100.0) / 100.0;
    }
}

