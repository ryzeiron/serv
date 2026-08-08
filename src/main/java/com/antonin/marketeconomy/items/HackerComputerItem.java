package com.antonin.marketeconomy.items;

import java.util.Arrays;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

// L'Ordinateur : bloc posable (reskin de jukebox, qui a un bloc-entite exploitable via PDC et
// une forme de cube simple, ideale pour un reskin de texture propre) qui donne acces a toutes
// les capacites du metier Hacker en clic droit une fois pose
public class HackerComputerItem {
    public static final Material BASE_MATERIAL = Material.JUKEBOX;
    public static final int CUSTOM_MODEL_DATA = 5003;

    private HackerComputerItem() {
    }

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "meco_computer");
    }

    public static ItemStack create(Plugin plugin) {
        ItemStack stack = new ItemStack(BASE_MATERIAL);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lOrdinateur");
            meta.setLore(Arrays.asList(
                    "§7Terminal d'accès aux capacités",
                    "§7du métier Hacker.",
                    "§eClic droit §7une fois posé pour l'ouvrir."
            ));
            meta.setCustomModelData(CUSTOM_MODEL_DATA);
            meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static boolean isComputerItem(Plugin plugin, ItemStack stack) {
        if (stack == null || stack.getType() != BASE_MATERIAL || !stack.hasItemMeta()) {
            return false;
        }
        Byte value = stack.getItemMeta().getPersistentDataContainer().get(key(plugin), PersistentDataType.BYTE);
        return value != null && value == 1;
    }
}
