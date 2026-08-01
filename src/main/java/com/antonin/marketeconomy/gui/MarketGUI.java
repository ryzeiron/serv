package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.model.MarketItem;
import java.util.ArrayList;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MarketGUI {
    public static final String TITLE = "\u00a78\u00a7lMarch\u00e9";

    public static Inventory build(MarketManager manager) {
        Map<Material, MarketItem> items = manager.getItems();
        int size = Math.max(9, (items.size() / 9 + 1) * 9);
        Inventory inv = Bukkit.createInventory(null, (int)size, (String)TITLE);
        int slot = 0;
        for (MarketItem item : items.values()) {
            ItemStack stack = new ItemStack(item.getMaterial());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("\u00a7e" + item.getDisplayName() + " " + item.getTrendArrow());
                ArrayList<Object> lore = new ArrayList<Object>();
                lore.add("\u00a77Prix d'achat: \u00a7a" + item.getBuyPrice());
                lore.add("\u00a77Prix de vente: \u00a7c" + item.getSellPrice());
                lore.add("\u00a77Stock disponible: \u00a7f" + item.getStock());
                lore.add("");
                lore.add("\u00a7eClic gauche \u00a77pour acheter 1");
                lore.add("\u00a7eClic droit \u00a77pour vendre 1");
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            inv.setItem(slot, stack);
            ++slot;
        }
        return inv;
    }

    public static void open(Player player, MarketManager manager) {
        player.openInventory(MarketGUI.build(manager));
    }
}

