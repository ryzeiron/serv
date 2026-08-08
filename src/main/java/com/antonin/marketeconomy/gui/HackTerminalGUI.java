package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.PlayerJob;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

// Mini-jeu "terminal de piratage" : une grille 3x3 cache un noeud correct, 3 essais, indice de
// distance a chaque echec. Trouve le bon noeud -> loot ; les 3 essais rates -> echec.
public final class HackTerminalGUI {
    public static final int[] GRID_SLOTS = {3, 4, 5, 12, 13, 14, 21, 22, 23};
    private static final Random RANDOM = new Random();

    private HackTerminalGUI() {
    }

    public static void open(MarketEconomyPlugin plugin, Player player, PlayerJob job) {
        int correctIndex = RANDOM.nextInt(GRID_SLOTS.length);
        HackTerminalHolder holder = new HackTerminalHolder(correctIndex, job.getLevel(), 3);
        Inventory inventory = Bukkit.createInventory(holder, 27, "§5Terminal de piratage");
        holder.setInventory(inventory);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
        for (int slot : GRID_SLOTS) {
            inventory.setItem(slot, namedItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§bNoeud ?",
                    new String[]{"§7Clique pour tenter une intrusion", "§7Essais restants: §f" + holder.getAttemptsLeft()}));
        }

        player.openInventory(inventory);
        player.sendMessage("§5[Hack] §dTerminal ouvert — trouve le bon noeud en 3 essais. Un indice de distance s'affiche à chaque tentative.");
    }

    static ItemStack namedItem(Material material, String name, String[] lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(java.util.Arrays.asList(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
