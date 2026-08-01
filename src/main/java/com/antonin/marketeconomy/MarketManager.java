package com.antonin.marketeconomy;

import com.antonin.marketeconomy.model.MarketCategory;
import com.antonin.marketeconomy.model.MarketEvent;
import com.antonin.marketeconomy.model.MarketItem;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class MarketManager {
    private final Map<Material, MarketItem> items = new LinkedHashMap<>();
    private final double sensitivity;
    private final double minMultiplier;
    private final double maxMultiplier;
    private final double categorySensitivity;

    private final boolean eventsEnabled;
    private final double eventCheckChance;
    private final int eventDurationCycles;
    private final double crashShock;
    private final double boomShock;
    private final double eventVolatilityMultiplier;

    private final Random random = new Random();
    private MarketEvent activeEvent;

    private long totalBought = 0L;
    private long totalSold = 0L;
    private double previousIndexValue = 1.0;
    private double lastIndexValue = 1.0;

    public MarketManager(MarketEconomyPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        this.sensitivity = config.getDouble("market-sensitivity", 0.02);
        this.minMultiplier = config.getDouble("min-price-multiplier", 0.2);
        this.maxMultiplier = config.getDouble("max-price-multiplier", 5.0);
        this.categorySensitivity = config.getDouble("category-sensitivity", 0.01);
        int historyLength = config.getInt("price-history-length", 50);

        this.eventsEnabled = config.getBoolean("events.enabled", true);
        this.eventCheckChance = config.getDouble("events.check-chance", 0.05);
        this.eventDurationCycles = Math.max(1, config.getInt("events.duration-cycles", 3));
        this.crashShock = config.getDouble("events.crash-shock", 0.75);
        this.boomShock = config.getDouble("events.boom-shock", 1.30);
        this.eventVolatilityMultiplier = config.getDouble("events.volatility-multiplier", 2.0);

        if (config.isConfigurationSection("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null) {
                    plugin.getLogger().warning("Materiau inconnu dans config.yml: " + key);
                    continue;
                }
                double basePrice = config.getDouble("items." + key + ".base-price", 10.0);
                long initialStock = config.getLong("items." + key + ".initial-stock", 100L);
                String displayName = config.getString("items." + key + ".display-name", MarketManager.defaultDisplayName(material));
                MarketCategory category = MarketCategory.fromConfig(config.getString("items." + key + ".category"));
                this.items.put(material, new MarketItem(material, displayName, category, basePrice, initialStock, historyLength));
            }
        }
        this.lastIndexValue = this.computeIndex();
        this.previousIndexValue = this.lastIndexValue;
    }

    private static String defaultDisplayName(Material material) {
        String name = material.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public Map<Material, MarketItem> getItems() {
        return this.items;
    }

    public MarketItem getItem(Material material) {
        return this.items.get(material);
    }

    public boolean isTradable(Material material) {
        return this.items.containsKey(material);
    }

    public MarketEvent getActiveEvent() {
        return this.activeEvent;
    }

    public void recordPurchase(MarketItem item, long amount) {
        item.registerBuy(amount);
        this.totalBought += amount;
    }

    public void recordSale(MarketItem item, long amount) {
        item.registerSell(amount);
        this.totalSold += amount;
    }

    public long getTotalBought() {
        return this.totalBought;
    }

    public long getTotalSold() {
        return this.totalSold;
    }

    public double getMarketIndexChangePercent() {
        if (this.previousIndexValue == 0.0) {
            return 0.0;
        }
        return (this.lastIndexValue - this.previousIndexValue) / this.previousIndexValue * 100.0;
    }

    public String getMarketTrendArrow() {
        double change = this.getMarketIndexChangePercent();
        if (change > 0.01) {
            return "↑";
        }
        if (change < -0.01) {
            return "↓";
        }
        return "→";
    }

    private double computeIndex() {
        if (this.items.isEmpty()) {
            return 1.0;
        }
        double sum = 0.0;
        for (MarketItem item : this.items.values()) {
            sum += item.getCurrentPrice() / item.getBasePrice();
        }
        return sum / this.items.size();
    }

    public void recalculateAll() {
        Map<MarketCategory, long[]> categoryTotals = new EnumMap<>(MarketCategory.class);
        for (MarketItem item : this.items.values()) {
            long[] totals = categoryTotals.computeIfAbsent(item.getCategory(), c -> new long[2]);
            totals[0] += item.getRecentBought();
            totals[1] += item.getRecentSold();
        }

        for (MarketItem item : this.items.values()) {
            long[] totals = categoryTotals.get(item.getCategory());
            long categoryActivity = totals[0] + totals[1];
            double categoryPressure = categoryActivity > 0L
                    ? (double) (totals[0] - totals[1]) / (double) categoryActivity
                    : 0.0;
            boolean underEvent = this.activeEvent != null
                    && (this.activeEvent.getCategory() == null || this.activeEvent.getCategory() == item.getCategory());
            double itemSensitivity = underEvent ? this.sensitivity * this.eventVolatilityMultiplier : this.sensitivity;
            item.recalculatePrice(itemSensitivity, this.minMultiplier, this.maxMultiplier, categoryPressure, this.categorySensitivity);
        }

        this.tickEvent();

        this.previousIndexValue = this.lastIndexValue;
        this.lastIndexValue = this.computeIndex();
    }

    private void tickEvent() {
        if (this.activeEvent != null) {
            if (this.activeEvent.tick()) {
                String cause = this.activeEvent.getType() == MarketEvent.Type.CRASH ? "le krach" : "la ruée";
                Bukkit.broadcastMessage("§6[Marché] §eLe marché se stabilise après " + cause + ".");
                this.activeEvent = null;
            }
            return;
        }
        if (!this.eventsEnabled || this.random.nextDouble() >= this.eventCheckChance) {
            return;
        }
        this.startRandomEvent();
    }

    private void startRandomEvent() {
        List<MarketCategory> categories = this.items.values().stream()
                .map(MarketItem::getCategory)
                .distinct()
                .collect(Collectors.toList());
        if (categories.isEmpty()) {
            return;
        }

        MarketEvent.Type type = this.random.nextBoolean() ? MarketEvent.Type.BOOM : MarketEvent.Type.CRASH;
        MarketCategory targetCategory = this.random.nextDouble() < 0.2 ? null : categories.get(this.random.nextInt(categories.size()));
        double shock = type == MarketEvent.Type.BOOM ? this.boomShock : this.crashShock;

        for (MarketItem item : this.items.values()) {
            if (targetCategory == null || item.getCategory() == targetCategory) {
                item.applyShock(shock, this.minMultiplier, this.maxMultiplier);
            }
        }

        this.activeEvent = new MarketEvent(type, targetCategory, this.eventDurationCycles);
        Bukkit.broadcastMessage(this.buildEventMessage(type, targetCategory));
    }

    private String buildEventMessage(MarketEvent.Type type, MarketCategory category) {
        String scope = category == null ? "tout le marché" : category.getDisplayName().toLowerCase();
        if (type == MarketEvent.Type.CRASH) {
            return "§c[Marché] §eKrach boursier ! Les prix de " + scope + " s'effondrent.";
        }
        return "§6[Marché] §eRuée sur le marché ! Les prix de " + scope + " s'envolent.";
    }
}
