package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.model.MarketCategory;
import com.antonin.marketeconomy.model.MarketItem;
import com.antonin.marketeconomy.storage.EconomyHook;
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

    private static final int MAIN_MENU_SIZE = 27;
    private static final int CATEGORY_ROW = 1;
    private static final int FOOTER_ROW = 2;
    private static final int TREND_SLOT = FOOTER_ROW * 9 + 4;
    private static final int BOUGHT_TOTAL_SLOT = FOOTER_ROW * 9 + 7;
    private static final int SOLD_TOTAL_SLOT = FOOTER_ROW * 9 + 8;

    private static final Map<MarketCategory, Material> CATEGORY_ICONS = new EnumMap<>(MarketCategory.class);

    static {
        CATEGORY_ICONS.put(MarketCategory.RAW_MATERIALS, Material.OAK_LOG);
        CATEGORY_ICONS.put(MarketCategory.ORES, Material.IRON_INGOT);
        CATEGORY_ICONS.put(MarketCategory.CONSUMABLES, Material.BREAD);
        CATEGORY_ICONS.put(MarketCategory.MOB_DROPS, Material.BONE);
        CATEGORY_ICONS.put(MarketCategory.RARE, Material.DIAMOND);
        CATEGORY_ICONS.put(MarketCategory.OTHER, Material.CHEST);
    }

    public static Inventory buildMainMenu(MarketManager manager, EconomyHook economyHook) {
        Map<MarketCategory, List<MarketItem>> byCategory = groupByCategory(manager);
        List<MarketCategory> categories = new ArrayList<>(byCategory.keySet());

        Map<Integer, MarketCategory> categoryBySlot = new HashMap<>();
        MarketMenuHolder holder = MarketMenuHolder.mainMenu(categoryBySlot);
        Inventory inv = Bukkit.createInventory(holder, MAIN_MENU_SIZE, MAIN_TITLE);
        holder.setInventory(inv);

        int[] slots = centeredSlots(categories.size(), CATEGORY_ROW, 1);
        for (int i = 0; i < categories.size(); i++) {
            MarketCategory category = categories.get(i);
            List<MarketItem> categoryItems = byCategory.get(category);
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
            int slot = slots[i];
            inv.setItem(slot, stack);
            categoryBySlot.put(slot, category);
        }

        inv.setItem(TREND_SLOT, buildTrendItem(manager));
        inv.setItem(BOUGHT_TOTAL_SLOT, buildStatItem(Material.GOLD_INGOT, "§6Total acheté", manager.getTotalSpent(), manager.getTotalBought(), economyHook));
        inv.setItem(SOLD_TOTAL_SLOT, buildStatItem(Material.EMERALD, "§aTotal vendu", manager.getTotalEarned(), manager.getTotalSold(), economyHook));

        return inv;
    }

    public static Inventory buildCategoryMenu(MarketManager manager, MarketCategory category) {
        List<MarketItem> categoryItems = groupByCategory(manager).getOrDefault(category, List.of());
        int rowsNeeded = Math.max(1, (categoryItems.size() + 8) / 9);
        int totalRows = rowsNeeded + 2;
        int size = totalRows * 9;
        int backSlot = (totalRows - 1) * 9 + 4;

        Map<Integer, Material> materialBySlot = new HashMap<>();
        MarketMenuHolder holder = MarketMenuHolder.categoryMenu(category, materialBySlot, backSlot);
        Inventory inv = Bukkit.createInventory(holder, size, "§8§lMarché §7- §e" + category.getDisplayName());
        holder.setInventory(inv);

        int[] slots = centeredSlots(categoryItems.size(), 1, rowsNeeded);
        for (int i = 0; i < categoryItems.size(); i++) {
            MarketItem item = categoryItems.get(i);
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
            int slot = slots[i];
            inv.setItem(slot, stack);
            materialBySlot.put(slot, item.getMaterial());
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

    // Centre "count" items sur des rangees de 9 slots max, a partir de la rangee rowOffset
    private static int[] centeredSlots(int count, int rowOffset, int availableRows) {
        int[] slots = new int[count];
        int rowsNeeded = count == 0 ? 0 : Math.min(availableRows, Math.max(1, (count + 8) / 9));
        int index = 0;
        int remaining = count;
        for (int row = 0; row < rowsNeeded && remaining > 0; row++) {
            int itemsInRow = Math.min(9, remaining);
            int startCol = (9 - itemsInRow) / 2;
            int slotRow = rowOffset + row;
            for (int col = 0; col < itemsInRow; col++) {
                slots[index++] = slotRow * 9 + startCol + col;
            }
            remaining -= itemsInRow;
        }
        return slots;
    }

    private static ItemStack buildTrendItem(MarketManager manager) {
        ItemStack stack = new ItemStack(Material.COMPASS);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            double change = manager.getMarketIndexChangePercent();
            String arrow = manager.getMarketTrendArrow();
            String color = change > 0.01 ? "§a" : change < -0.01 ? "§c" : "§7";
            meta.setDisplayName("§6§lTendance générale " + color + arrow);
            List<String> lore = new ArrayList<>();
            lore.add("§7Variation: " + color + String.format("%.2f", change) + "%");
            lore.add("§7Depuis le dernier recalcul de prix");
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack buildStatItem(Material material, String name, double totalMoney, long totalItems, EconomyHook economyHook) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String amount = economyHook.isEnabled() ? economyHook.format(totalMoney) : String.format("%.2f$", totalMoney);
            meta.setDisplayName(name + " §f" + amount);
            List<String> lore = new ArrayList<>();
            lore.add("§7" + totalItems + " item(s) depuis le démarrage");
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Map<MarketCategory, List<MarketItem>> groupByCategory(MarketManager manager) {
        Map<MarketCategory, List<MarketItem>> byCategory = new EnumMap<>(MarketCategory.class);
        for (MarketItem item : manager.getItems().values()) {
            byCategory.computeIfAbsent(item.getCategory(), c -> new ArrayList<>()).add(item);
        }
        return byCategory;
    }

    public static void openMainMenu(Player player, MarketManager manager, EconomyHook economyHook) {
        player.openInventory(buildMainMenu(manager, economyHook));
    }

    public static void openCategoryMenu(Player player, MarketManager manager, MarketCategory category) {
        player.openInventory(buildCategoryMenu(manager, category));
    }
}
