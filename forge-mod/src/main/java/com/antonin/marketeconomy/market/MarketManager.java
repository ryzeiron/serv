package com.antonin.marketeconomy.market;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

// Moteur de prix offre/demande du marche. Version noyau du MarketManager du plugin Paper :
// achat/vente, indice de marche et evenements krach/ruee. La manipulation de marche, les primes,
// les ecoutes du metier Hacker, les contrats a terme et le journal boursier viendront avec les
// prochaines etapes du portage (ils dependent de systemes pas encore reecrits en mod).
public class MarketManager {
    private static final int HISTORY_LENGTH = 50;
    private static final double SENSITIVITY = 0.02;
    private static final double MIN_MULTIPLIER = 0.2;
    private static final double MAX_MULTIPLIER = 5.0;
    private static final double CATEGORY_SENSITIVITY = 0.01;

    private static final boolean EVENTS_ENABLED = true;
    private static final double EVENT_CHECK_CHANCE = 0.05;
    private static final int EVENT_DURATION_CYCLES = 3;
    private static final double CRASH_SHOCK = 0.75;
    private static final double BOOM_SHOCK = 1.30;
    private static final double EVENT_VOLATILITY_MULTIPLIER = 2.0;

    private final Map<Item, MarketItem> items = new LinkedHashMap<>();
    private final Random random = new Random();
    private MarketEvent activeEvent;

    private long totalBought = 0L;
    private long totalSold = 0L;
    private double totalSpent = 0.0;
    private double totalEarned = 0.0;
    private double previousIndexValue = 1.0;
    private double lastIndexValue = 1.0;

    public MarketManager() {
        this.registerDefaultItems();
        this.lastIndexValue = this.computeIndex();
        this.previousIndexValue = this.lastIndexValue;
    }

    private void registerDefaultItems() {
        this.register("oak_log", "Bois de Chêne", MarketCategory.RAW_MATERIALS, 3.0, 4000L);
        this.register("cobblestone", "Pierre", MarketCategory.RAW_MATERIALS, 1.0, 8000L);
        this.register("iron_ingot", "Lingot de Fer", MarketCategory.ORES, 8.0, 2000L);
        this.register("gold_ingot", "Lingot d'Or", MarketCategory.ORES, 15.0, 1200L);
        this.register("diamond", "Diamant", MarketCategory.RARE, 100.0, 500L);
        this.register("emerald", "Émeraude", MarketCategory.RARE, 40.0, 800L);
        this.register("netherite_ingot", "Lingot de Netherite", MarketCategory.RARE, 500.0, 50L);
        this.register("wheat", "Blé", MarketCategory.CONSUMABLES, 2.0, 5000L);
        this.register("cooked_beef", "Steak Cuit", MarketCategory.CONSUMABLES, 5.0, 2000L);
        this.register("rotten_flesh", "Chair Pourrie", MarketCategory.MOB_DROPS, 0.5, 5000L);
        this.register("ender_pearl", "Perle d'Ender", MarketCategory.MOB_DROPS, 25.0, 300L);
    }

    private void register(String vanillaItemId, String displayName, MarketCategory category, double basePrice, long initialStock) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(vanillaItemId));
        if (item == null) {
            return;
        }
        this.items.put(item, new MarketItem(item, displayName, category, basePrice, initialStock, HISTORY_LENGTH));
    }

    public Map<Item, MarketItem> getItems() {
        return this.items;
    }

    public MarketItem getItem(Item item) {
        return this.items.get(item);
    }

    public boolean isTradable(Item item) {
        return this.items.containsKey(item);
    }

    public MarketEvent getActiveEvent() {
        return this.activeEvent;
    }

    public void recordPurchase(MarketItem item, long amount, double totalPrice) {
        item.registerBuy(amount);
        this.totalBought += amount;
        this.totalSpent += totalPrice;
    }

    public void recordSale(MarketItem item, long amount, double totalPrice) {
        item.registerSell(amount);
        this.totalSold += amount;
        this.totalEarned += totalPrice;
    }

    public long getTotalBought() {
        return this.totalBought;
    }

    public long getTotalSold() {
        return this.totalSold;
    }

    public double getTotalSpent() {
        return this.totalSpent;
    }

    public double getTotalEarned() {
        return this.totalEarned;
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

    public void recalculateAll(MinecraftServer server) {
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
            double itemSensitivity = underEvent ? SENSITIVITY * EVENT_VOLATILITY_MULTIPLIER : SENSITIVITY;
            item.recalculatePrice(itemSensitivity, MIN_MULTIPLIER, MAX_MULTIPLIER, categoryPressure, CATEGORY_SENSITIVITY);
        }

        this.tickEvent(server);

        this.previousIndexValue = this.lastIndexValue;
        this.lastIndexValue = this.computeIndex();
    }

    private void tickEvent(MinecraftServer server) {
        if (this.activeEvent != null) {
            if (this.activeEvent.tick()) {
                String cause = this.activeEvent.getType() == MarketEvent.Type.CRASH ? "le krach" : "la ruée";
                broadcast(server, "§6[Marché] §eLe marché se stabilise après " + cause + ".");
                this.activeEvent = null;
            }
            return;
        }
        if (!EVENTS_ENABLED || this.random.nextDouble() >= EVENT_CHECK_CHANCE) {
            return;
        }
        this.startRandomEvent(server);
    }

    private void startRandomEvent(MinecraftServer server) {
        List<MarketCategory> categories = this.items.values().stream()
                .map(MarketItem::getCategory)
                .distinct()
                .collect(Collectors.toList());
        if (categories.isEmpty()) {
            return;
        }

        MarketEvent.Type type = this.random.nextBoolean() ? MarketEvent.Type.BOOM : MarketEvent.Type.CRASH;
        MarketCategory targetCategory = this.random.nextDouble() < 0.2 ? null : categories.get(this.random.nextInt(categories.size()));
        double shock = type == MarketEvent.Type.BOOM ? BOOM_SHOCK : CRASH_SHOCK;

        for (MarketItem item : this.items.values()) {
            if (targetCategory == null || item.getCategory() == targetCategory) {
                item.applyShock(shock, MIN_MULTIPLIER, MAX_MULTIPLIER);
            }
        }

        this.activeEvent = new MarketEvent(type, targetCategory, EVENT_DURATION_CYCLES);
        String scope = targetCategory == null ? "tout le marché" : targetCategory.getDisplayName().toLowerCase();
        broadcast(server, buildEventMessage(type, scope));
    }

    private static String buildEventMessage(MarketEvent.Type type, String scope) {
        if (type == MarketEvent.Type.CRASH) {
            return "§c[Marché] §eKrach boursier ! Les prix de " + scope + " s'effondrent.";
        }
        return "§6[Marché] §eRuée sur le marché ! Les prix de " + scope + " s'envolent.";
    }

    private static void broadcast(MinecraftServer server, String message) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }
}
