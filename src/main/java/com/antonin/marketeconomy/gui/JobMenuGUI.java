package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.JobType;
import com.antonin.marketeconomy.model.PlayerJob;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

// Menu /metier : une icone par metier (pour l'instant seul Hacker existe), clic -> vue progression
public final class JobMenuGUI {
    private JobMenuGUI() {
    }

    private static Material iconFor(JobType type) {
        if (type == JobType.HACKER) {
            return Material.SPYGLASS;
        }
        return Material.PAPER;
    }

    public static void openMain(MarketEconomyPlugin plugin, Player player) {
        JobMenuHolder holder = new JobMenuHolder(JobMenuHolder.Mode.MAIN, null);
        Inventory inventory = Bukkit.createInventory(holder, 27, "§6§lMétiers");
        holder.setInventory(inventory);

        ItemStack filler = HackerComputerGUI.namedItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        PlayerJob current = plugin.getJobManager().getJob(player.getUniqueId());
        int slot = 11;
        for (JobType type : JobType.values()) {
            List<String> lore = new ArrayList<>();
            if (current != null && current.getType() == type) {
                lore.add("§aMétier actuel §7— niveau " + current.getLevel());
            } else {
                lore.add("§7Non commencé");
            }
            lore.add("");
            lore.add("§eClique §7pour voir la progression");
            inventory.setItem(slot, HackerComputerGUI.namedItem(iconFor(type), type.getDisplayName(), lore.toArray(new String[0])));
            slot += 2;
        }

        player.openInventory(inventory);
    }

    public static void openProgression(MarketEconomyPlugin plugin, Player player, JobType type) {
        JobMenuHolder holder = new JobMenuHolder(JobMenuHolder.Mode.PROGRESSION, type);
        Inventory inventory = Bukkit.createInventory(holder, 27, "§6" + type.getDisplayName() + " §6- Progression");
        holder.setInventory(inventory);

        ItemStack filler = HackerComputerGUI.namedItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        PlayerJob job = plugin.getJobManager().getJob(player.getUniqueId());
        boolean isCurrent = job != null && job.getType() == type;
        double xpPerLevelBase = plugin.getConfig().getDouble("jobs.xp-per-level-base", 100.0);

        List<String> lore = new ArrayList<>();
        if (isCurrent) {
            int level = job.getLevel();
            boolean maxed = level >= PlayerJob.MAX_LEVEL;
            lore.add("§7Niveau: §f" + level + (maxed ? " §a(max)" : ""));
            if (!maxed) {
                double xpNeeded = job.xpToNextLevel(xpPerLevelBase);
                lore.add("§7Progression: " + progressBar(job.getXp(), xpNeeded));
                lore.add("§7" + Math.round(job.getXp()) + " / " + Math.round(xpNeeded) + " xp");
            }
        } else {
            lore.add("§7Tu n'exerces pas encore ce métier.");
        }
        lore.add("");
        if (type == JobType.HACKER) {
            lore.add("§dCapacités :");
            lore.add("§7- Rapport de marché (insider)");
            lore.add("§7- Piratage de prix (pump/dump)");
            lore.add("§7- Brouillage de trace");
            lore.add("§7- Écoute de ventes (wiretap)");
            lore.add("§7- Piratage de banque d'île");
            lore.add("§7- Terminal (mini-jeu, loot)");
            lore.add("§7Effets plus forts à mesure que le niveau monte.");
        }
        inventory.setItem(13, HackerComputerGUI.namedItem(iconFor(type), type.getDisplayName(), lore.toArray(new String[0])));

        if (!isCurrent) {
            inventory.setItem(22, HackerComputerGUI.namedItem(Material.LIME_DYE, "§aChoisir ce métier", null));
        }
        inventory.setItem(18, HackerComputerGUI.namedItem(Material.ARROW, "§7« Retour", null));

        player.openInventory(inventory);
    }

    private static String progressBar(double xp, double xpNeeded) {
        int total = 20;
        int filled = xpNeeded > 0 ? (int) Math.round(total * Math.min(1.0, xp / xpNeeded)) : total;
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < total; i++) {
            if (i == filled) {
                sb.append("§7");
            }
            sb.append("■");
        }
        return sb.toString();
    }
}
