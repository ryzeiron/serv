package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.model.MarketCategory;
import com.antonin.marketeconomy.model.MarketItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MarketGUI {
    public static final String MAIN_TITLE = "§8§lMarché";
    private static final Material BACK_BUTTON_MATERIAL = Material.BARRIER;

    private static final Map<MarketCategory, Material> CATEGORY_ICONS = new EnumMap<>(MarketCategory.class);

    static {
        CATEGORY_ICONS.put(MarketCategory.RAW_MATERIALS, Material.OAK_LOG);
        CATEGORY_ICONS.put(MarketCategory.ORES, Material.IRON_INGOT);
        CATEGORY_ICONS.put(MarketCategory.CONSUMABLES, Material.BREAD);
        CATEGORY_ICONS.put(MarketCategory.MOB_DROPS, Material.BONE);
        CATEGORY_ICONS.put(MarketCategory.RARE, Material.DIAMOND);
        CATEGORY_ICONS.put(MarketCategory.OTHER, Material.CHEST);
    }

    public static Inventory buildMainMenu(MarketManager manager) {
        Map<MarketCategory, List<MarketItem>> byCategory = groupByCategory(manager);
        int size = Math.max(9, ((byCategory.size() - 1) / 9 + 1) * 9);

        Map<Integer, MarketCategory> categoryBySlot = new HashMap<>();
        MarketMenuHolder holder = MarketMenuHolder.mainMenu(categoryBySlot);
        Inventory inv = Bukkit.createInventory(holder, size, MAIN_TITLE);
        holder.setInventory(inv);

        int slot = 0;
        for (Map.Entry<MarketCategory, List<MarketItem>> entry : byCategory.entrySet()) {
            MarketCategory category = entry.getKey();
            List<MarketItem> categoryItems = entry.getValue();
            Material icon = CATEGORY_ICONS.getOrDefault(category, Material.CHEST);

            ItemStack stack = new ItemStack(icon);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e§l" + category.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add("§7" + categoryItems.size() + " item(s) échangeable(s)");
                lore.add("");
                lore.add("§eClique pour ouvrir le rayon");
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inv.setItem(slot, stack);
            categoryBySlot.put(slot, category);
            slot++;
        }

        return inv;
    }

    public static Inventory buildCategoryMenu(MarketManager manager, MarketCategory category) {
        List<MarketItem> categoryItems = groupByCategory(manager).getOrDefault(category, List.of());
        int size = Math.max(9, (categoryItems.size() / 9 + 1) * 9);
        int backSlot = size - 1;

        Map<Integer, Material> materialBySlot = new HashMap<>();
        MarketMenuHolder holder = MarketMenuHolder.categoryMenu(category, materialBySlot, backSlot);
        Inventory inv = Bukkit.createInventory(holder, size, "§8§lMarché §7- §e" + category.getDisplayName());
        holder.setInventory(inv);

        int slot = 0;
        for (MarketItem item : categoryItems) {
            ItemStack stack = new ItemStack(item.getMaterial());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + item.getDisplayName() + " " + item.getTrendArrow());
                List<String> lore = new ArrayList<>();
                lore.add("§7Prix d'achat: §a" + item.getBuyPrice());
                lore.add("§7Prix de vente: §c" + item.getSellPrice());
                lore.add("§7Stock disponible: §f" + item.getStock());
                lore.add("");
                lore.add("§eClic gauche §7pour acheter 1");
                lore.add("§eClic droit §7pour vendre 1");
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inv.setItem(slot, stack);
            materialBySlot.put(slot, item.getMaterial());
            slot++;
        }

        ItemStack backButton = new ItemStack(BACK_BUTTON_MATERIAL);
        ItemMeta backMeta = backButton.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§cRetour");
            backButton.setItemMeta(backMeta);
        }
        inv.setItem(backSlot, backButton);

        return inv;
    }

    private static Map<MarketCategory, List<MarketItem>> groupByCategory(MarketManager manager) {
        Map<MarketCategory, List<MarketItem>> byCategory = new EnumMap<>(MarketCategory.class);
        for (MarketItem item : manager.getItems().values()) {
            byCategory.computeIfAbsent(item.getCategory(), c -> new ArrayList<>()).add(item);
        }
        return byCategory;
    }

    public static void openMainMenu(Player player, MarketManager manager) {
        player.openInventory(buildMainMenu(manager));
    }

    public static void openCategoryMenu(Player player, MarketManager manager, MarketCategory category) {
        player.openInventory(buildCategoryMenu(manager, category));
    }
}
