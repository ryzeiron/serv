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

    public static MarketCategory professionCategory(Villager.Profession profession) {
        switch (profession) {
            case FARMER:
            case FISHERMAN:
            case BUTCHER:
                return MarketCategory.CONSUMABLES;
            case TOOLSMITH:
            case WEAPONSMITH:
            case ARMORER:
                return MarketCategory.ORES;
            case CARTOGRAPHER:
            case LIBRARIAN:
            case CLERIC:
                return MarketCategory.RARE;
            case LEATHERWORKER:
            case SHEPHERD:
            case MASON:
                return MarketCategory.RAW_MATERIALS;
            case FLETCHER:
                return MarketCategory.MOB_DROPS;
            default:
                return MarketCategory.OTHER;
        }
    }

    public static String villagerLabel(Villager.Profession profession) {
        switch (profession) {
            case FARMER:
                return "Le Fermier";
            case FISHERMAN:
                return "Le Pêcheur";
            case BUTCHER:
                return "Le Boucher";
            case TOOLSMITH:
                return "L'Outilleur";
            case WEAPONSMITH:
                return "L'Armurier";
            case ARMORER:
                return "Le Forgeron";
            case CARTOGRAPHER:
                return "Le Cartographe";
            case LIBRARIAN:
                return "Le Bibliothécaire";
            case CLERIC:
                return "Le Clerc";
            case LEATHERWORKER:
                return "Le Tanneur";
            case SHEPHERD:
                return "Le Berger";
            case MASON:
                return "Le Maçon";
            case FLETCHER:
                return "Le Fléchier";
            default:
                return "Le Villageois";
        }
    }
}
