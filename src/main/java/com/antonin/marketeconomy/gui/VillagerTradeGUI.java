package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.ReputationManager;
import com.antonin.marketeconomy.model.MarketCategory;
import com.antonin.marketeconomy.model.MarketItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class VillagerTradeGUI {

    public static boolean hasTradableItems(Villager villager, MarketManager marketManager) {
        MarketCategory category = professionCategory(villager.getProfession());
        return marketManager.getItems().values().stream().anyMatch(item -> item.getCategory() == category);
    }

    public static void open(Player player, Villager villager, MarketManager marketManager, ReputationManager reputationManager) {
        MarketCategory category = professionCategory(villager.getProfession());
        List<MarketItem> tradable = new ArrayList<>();
        for (MarketItem item : marketManager.getItems().values()) {
            if (item.getCategory() == category) {
                tradable.add(item);
            }
        }

        int size = Math.max(9, ((tradable.size() - 1) / 9 + 1) * 9);
        Map<Integer, Material> materialBySlot = new HashMap<>();
        VillagerTradeHolder holder = new VillagerTradeHolder(materialBySlot);
        Inventory inv = Bukkit.createInventory(holder, size, "§8§l" + villagerLabel(villager.getProfession()));
        holder.setInventory(inv);

        UUID uuid = player.getUniqueId();
        double buyMultiplier = reputationManager.getBuyMultiplier(uuid);
        double sellMultiplier = reputationManager.getSellMultiplier(uuid);
        String trustLabel = reputationManager.getTrustLabel(uuid);

        int slot = 0;
        for (MarketItem item : tradable) {
            ItemStack stack = new ItemStack(item.getMaterial());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + item.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add("§7Prix d'achat: §a" + round2(item.getBuyPrice() * buyMultiplier));
                lore.add("§7Prix de vente: §c" + round2(item.getSellPrice() * sellMultiplier));
                lore.add("§7Stock disponible: §f" + item.getStock());
                lore.add("");
                lore.add("§7Ta réputation ici: §f" + trustLabel);
                lore.add("§eClic gauche §7pour acheter 1");
                lore.add("§eClic droit §7pour vendre 1");
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inv.setItem(slot, stack);
            materialBySlot.put(slot, item.getMaterial());
            slot++;
        }

        player.openInventory(inv);
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // Villager.Profession n'est plus un enum classique dans l'API Paper recente (type
    // "Keyed"), donc on ne peut plus faire switch(profession) directement : on bascule
    // sur sa cle texte (ex. "farmer"), stable quelle que soit la forme du type en API.
    public static MarketCategory professionCategory(Villager.Profession profession) {
        switch (profession.getKey().getKey()) {
            case "farmer":
            case "fisherman":
            case "butcher":
                return MarketCategory.CONSUMABLES;
            case "toolsmith":
            case "weaponsmith":
            case "armorer":
                return MarketCategory.ORES;
            case "cartographer":
            case "librarian":
            case "cleric":
                return MarketCategory.RARE;
            case "leatherworker":
            case "shepherd":
            case "mason":
                return MarketCategory.RAW_MATERIALS;
            case "fletcher":
                return MarketCategory.MOB_DROPS;
            default:
                return MarketCategory.OTHER;
        }
    }

    public static String villagerLabel(Villager.Profession profession) {
        switch (profession.getKey().getKey()) {
            case "farmer":
                return "Le Fermier";
            case "fisherman":
                return "Le Pêcheur";
            case "butcher":
                return "Le Boucher";
            case "toolsmith":
                return "L'Outilleur";
            case "weaponsmith":
                return "L'Armurier";
            case "armorer":
                return "Le Forgeron";
            case "cartographer":
                return "Le Cartographe";
            case "librarian":
                return "Le Bibliothécaire";
            case "cleric":
                return "Le Clerc";
            case "leatherworker":
                return "Le Tanneur";
            case "shepherd":
                return "Le Berger";
            case "mason":
                return "Le Maçon";
            case "fletcher":
                return "Le Fléchier";
            default:
                return "Le Villageois";
        }
    }
}
