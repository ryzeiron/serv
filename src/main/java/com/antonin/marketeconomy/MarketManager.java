package com.antonin.marketeconomy;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.MarketItem;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class MarketManager {
    private final Map<Material, MarketItem> items = new LinkedHashMap<Material, MarketItem>();
    private final double sensitivity;
    private final double minMultiplier;
    private final double maxMultiplier;

    public MarketManager(MarketEconomyPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        this.sensitivity = config.getDouble("market-sensitivity", 0.02);
        this.minMultiplier = config.getDouble("min-price-multiplier", 0.2);
        this.maxMultiplier = config.getDouble("max-price-multiplier", 5.0);
        int historyLength = config.getInt("price-history-length", 50);
        if (config.isConfigurationSection("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                Material material = Material.matchMaterial((String)key);
                if (material == null) {
                    plugin.getLogger().warning("Materiau inconnu dans config.yml: " + key);
                    continue;
                }
                double basePrice = config.getDouble("items." + key + ".base-price", 10.0);
                long initialStock = config.getLong("items." + key + ".initial-stock", 100L);
                String displayName = config.getString("items." + key + ".display-name", MarketManager.defaultDisplayName(material));
                this.items.put(material, new MarketItem(material, displayName, basePrice, initialStock, historyLength));
            }
        }
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

    public void recalculateAll() {
        for (MarketItem item : this.items.values()) {
            item.recalculatePrice(this.sensitivity, this.minMultiplier, this.maxMultiplier);
        }
    }
}

