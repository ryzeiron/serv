package com.antonin.marketeconomy.items;

import com.antonin.marketeconomy.model.FuturesContract;
import com.antonin.marketeconomy.model.MarketItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class FuturesItem {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM HH:mm");

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "futures_contract_id");
    }

    public static ItemStack createContract(Plugin plugin, FuturesContract contract, MarketItem item) {
        return build(plugin, contract, item, Material.PAPER, "§6§lContrat Scellé",
                "§7Cet objet est échangeable : la mise",
                "§7appartient à qui le détient à l'échéance.");
    }

    public static ItemStack createSeed(Plugin plugin, FuturesContract contract, MarketItem item) {
        return build(plugin, contract, item, Material.WHEAT_SEEDS, "§a§lGraine Spéculative",
                "§7Une graine pas comme les autres...",
                "§7Sa vraie valeur se révèle à la récolte.");
    }

    private static ItemStack build(Plugin plugin, FuturesContract contract, MarketItem item, Material material, String name, String... flavorLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList<>();
            lore.add("§7Type: §e" + (contract.getType() == FuturesContract.Type.LONG ? "LONG (hausse)" : "SHORT (baisse)"));
            lore.add("§7Sous-jacent: §f" + item.getDisplayName());
            lore.add("§7Mise: §f" + round2(contract.getStake()));
            lore.add("§7Prix de référence: §f" + round2(contract.getPriceAtCreation()));
            lore.add("§7Échéance: §f" + DATE_FORMAT.format(new Date(contract.getMaturityAtMillis())));
            lore.add("");
            for (String line : flavorLines) {
                lore.add(line);
            }
            lore.add("§eClic droit après échéance §7pour encaisser");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, contract.getId().toString());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static UUID readContractId(Plugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        String raw = stack.getItemMeta().getPersistentDataContainer().get(key(plugin), PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
