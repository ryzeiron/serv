package com.antonin.marketeconomy.items;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.gui.VillagerTradeGUI;
import com.antonin.marketeconomy.model.MarketCategory;
import com.antonin.marketeconomy.model.MarketItem;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

public class MerchantCompassTracker implements Runnable {
    private static final double SEARCH_RADIUS = 256.0;

    private final MarketEconomyPlugin plugin;
    private final MarketManager marketManager;

    public MerchantCompassTracker(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.marketManager = plugin.getMarketManager();
    }

    @Override
    public void run() {
        MarketCategory bestCategory = this.findBestDiscountCategory();
        if (bestCategory == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.updateIfHolding(player, player.getInventory().getItemInMainHand(), bestCategory);
            this.updateIfHolding(player, player.getInventory().getItemInOffHand(), bestCategory);
        }
    }

    private void updateIfHolding(Player player, ItemStack stack, MarketCategory category) {
        if (!MerchantCompassItem.isMerchantCompass(this.plugin, stack)) {
            return;
        }
        Villager target = this.findNearestVillager(player, category);
        if (target == null) {
            return;
        }
        CompassMeta meta = (CompassMeta) stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.setLodestoneTracked(false);
        meta.setLodestone(target.getLocation());
        stack.setItemMeta(meta);
    }

    private Villager findNearestVillager(Player player, MarketCategory category) {
        Villager nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity entity : player.getNearbyEntities(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
            if (!(entity instanceof Villager)) {
                continue;
            }
            Villager villager = (Villager) entity;
            if (VillagerTradeGUI.professionCategory(villager.getProfession()) != category) {
                continue;
            }
            double distance = villager.getLocation().distanceSquared(player.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = villager;
            }
        }
        return nearest;
    }

    // La categorie la plus "en solde" : ratio moyen prix actuel/prix de base le plus bas
    private MarketCategory findBestDiscountCategory() {
        Map<MarketCategory, double[]> sums = new EnumMap<>(MarketCategory.class);
        for (MarketItem item : this.marketManager.getItems().values()) {
            double[] totals = sums.computeIfAbsent(item.getCategory(), c -> new double[2]);
            totals[0] += item.getCurrentPrice() / item.getBasePrice();
            totals[1] += 1.0;
        }
        MarketCategory best = null;
        double bestRatio = Double.MAX_VALUE;
        for (Map.Entry<MarketCategory, double[]> entry : sums.entrySet()) {
            double ratio = entry.getValue()[0] / entry.getValue()[1];
            if (ratio < bestRatio) {
                bestRatio = ratio;
                best = entry.getKey();
            }
        }
        return best;
    }
}
