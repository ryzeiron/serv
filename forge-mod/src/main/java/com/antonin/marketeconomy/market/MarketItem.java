package com.antonin.marketeconomy.market;

import java.util.LinkedList;
import net.minecraft.world.item.Item;

public class MarketItem {
    private final Item item;
    private final String displayName;
    private final MarketCategory category;
    private final double basePrice;
    private double currentPrice;
    private long stock;
    private long recentBought = 0L;
    private long recentSold = 0L;
    private final LinkedList<Double> priceHistory = new LinkedList<>();
    private final int historyMaxLength;

    public MarketItem(Item item, String displayName, MarketCategory category, double basePrice, long initialStock, int historyMaxLength) {
        this.item = item;
        this.displayName = displayName;
        this.category = category;
        this.basePrice = basePrice;
        this.currentPrice = basePrice;
        this.stock = initialStock;
        this.historyMaxLength = historyMaxLength;
        this.priceHistory.add(basePrice);
    }

    public Item getItem() {
        return this.item;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public MarketCategory getCategory() {
        return this.category;
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

    public long getRecentBought() {
        return this.recentBought;
    }

    public long getRecentSold() {
        return this.recentSold;
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

    public void recalculatePrice(double sensitivity, double minMultiplier, double maxMultiplier, double categoryPressure, double categorySensitivity) {
        long net = this.recentBought - this.recentSold;
        long totalActivity = this.recentBought + this.recentSold;
        if (totalActivity > 0L) {
            double pressure = (double) net / (double) Math.max(1L, totalActivity);
            double change = 1.0 + pressure * sensitivity * (double) Math.min(totalActivity, 200L);
            this.currentPrice *= change;
        } else {
            this.currentPrice += (this.basePrice - this.currentPrice) * 0.01;
        }
        if (categoryPressure != 0.0) {
            this.currentPrice *= 1.0 + categoryPressure * categorySensitivity;
        }
        this.clampToRange(minMultiplier, maxMultiplier);
        this.recentBought = 0L;
        this.recentSold = 0L;
        this.pushHistory();
    }

    public void applyShock(double factor, double minMultiplier, double maxMultiplier) {
        this.currentPrice *= factor;
        this.clampToRange(minMultiplier, maxMultiplier);
        this.pushHistory();
    }

    private void clampToRange(double minMultiplier, double maxMultiplier) {
        double min = this.basePrice * minMultiplier;
        double max = this.basePrice * maxMultiplier;
        this.currentPrice = Math.max(min, Math.min(max, this.currentPrice));
    }

    private void pushHistory() {
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
            return "→";
        }
        double previous = this.priceHistory.get(this.priceHistory.size() - 2);
        if (this.currentPrice > previous) {
            return "↑";
        }
        if (this.currentPrice < previous) {
            return "↓";
        }
        return "→";
    }

    private double round2(double value) {
        return (double) Math.round(value * 100.0) / 100.0;
    }
}
