package com.antonin.marketeconomy.items;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class MerchantCompassItem {

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "merchant_compass");
    }

    public static ItemStack create(Plugin plugin) {
        ItemStack stack = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lBoussole du Marchand");
            meta.setLore(List.of(
                    "§7Pointe vers le marchand le plus proche",
                    "§7offrant la meilleure affaire du moment."
            ));
            meta.setLodestoneTracked(false);
            meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static boolean isMerchantCompass(Plugin plugin, ItemStack stack) {
        if (stack == null || stack.getType() != Material.COMPASS || !stack.hasItemMeta()) {
            return false;
        }
        return stack.getItemMeta().getPersistentDataContainer().has(key(plugin), PersistentDataType.BYTE);
    }
}
