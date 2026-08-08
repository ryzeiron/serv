package com.antonin.marketeconomy.items;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

// Composants technologiques ajoutes au jeu, utilises par le craft de l'Ordinateur du Hacker.
// Chaque item est un materiau vanilla reskinne (nom + lore) et tague via PDC, pour que le craft
// ne puisse pas etre triche avec un item vanilla ordinaire du meme materiau
public class CustomMaterials {
    private static final String LITHIUM_ID = "lithium_ingot";
    private static final String PLASTIC_ID = "plastic";

    private CustomMaterials() {
    }

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "meco_custom_id");
    }

    public static ItemStack createLithiumIngot(Plugin plugin) {
        return build(plugin, LITHIUM_ID, Material.AMETHYST_SHARD, "§dLingot de Lithium",
                "§7Un metal leger et conducteur,",
                "§7trouve en traces pres des gisements de diamant.");
    }

    public static ItemStack createPlastic(Plugin plugin) {
        return build(plugin, PLASTIC_ID, Material.SLIME_BALL, "§aPlastique",
                "§7Un dechet flottant remonte de l'eau...",
                "§7Composant cle de l'electronique moderne.");
    }

    public static boolean isLithiumIngot(Plugin plugin, ItemStack stack) {
        return isCustom(plugin, stack, LITHIUM_ID);
    }

    public static boolean isPlastic(Plugin plugin, ItemStack stack) {
        return isCustom(plugin, stack, PLASTIC_ID);
    }

    private static boolean isCustom(Plugin plugin, ItemStack stack, String id) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        String value = stack.getItemMeta().getPersistentDataContainer().get(key(plugin), PersistentDataType.STRING);
        return id.equals(value);
    }

    private static ItemStack build(Plugin plugin, String id, Material material, String name, String... loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(line);
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, id);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
