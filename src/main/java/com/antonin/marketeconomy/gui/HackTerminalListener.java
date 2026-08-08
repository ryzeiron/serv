package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.JobType;
import com.antonin.marketeconomy.storage.EconomyHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

public class HackTerminalListener implements Listener {
    private final MarketEconomyPlugin plugin;

    public HackTerminalListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder rawHolder = topInventory.getHolder();
        if (!(rawHolder instanceof HackTerminalHolder)) {
            return;
        }
        HackTerminalHolder holder = (HackTerminalHolder) rawHolder;
        event.setCancelled(true);

        if (holder.isResolved() || event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;

        int gridIndex = indexOf(event.getSlot());
        if (gridIndex < 0) {
            return;
        }

        if (gridIndex == holder.getCorrectIndex()) {
            this.resolveSuccess(player, holder, topInventory, event.getSlot());
            return;
        }

        int distance = manhattanDistance(gridIndex, holder.getCorrectIndex());
        topInventory.setItem(event.getSlot(), HackTerminalGUI.namedItem(Material.RED_STAINED_GLASS_PANE,
                "§cRaté", new String[]{"§7Indice: " + hintFor(distance)}));
        holder.decrementAttempts();

        if (holder.getAttemptsLeft() <= 0) {
            this.resolveFailure(player, holder, topInventory);
        } else {
            player.sendMessage("§5[Hack] §7" + hintFor(distance) + " §7— essais restants: §f" + holder.getAttemptsLeft());
        }
    }

    private void resolveSuccess(Player player, HackTerminalHolder holder, Inventory inventory, int slot) {
        holder.setResolved(true);
        inventory.setItem(slot, HackTerminalGUI.namedItem(Material.EMERALD_BLOCK, "§aAccès accordé", null));

        double reward = this.plugin.getConfig().getDouble("jobs.hacker.terminal-reward-base", 50.0)
                + this.plugin.getConfig().getDouble("jobs.hacker.terminal-reward-per-level", 15.0) * holder.getHackerLevel();
        EconomyHook economy = this.plugin.getEconomyHook();
        if (economy.isEnabled()) {
            economy.deposit(player, reward);
            player.sendMessage("§5[Hack] §aIntrusion réussie ! +" + economy.format(reward) + ".");
        } else {
            player.sendMessage("§5[Hack] §aIntrusion réussie !");
        }
        this.plugin.getJobManager().addXp(player, JobType.HACKER,
                this.plugin.getConfig().getDouble("jobs.hacker.xp-terminal-success", 40.0));
        this.closeSoon(player);
    }

    private void resolveFailure(Player player, HackTerminalHolder holder, Inventory inventory) {
        holder.setResolved(true);
        int correctSlot = HackTerminalGUI.GRID_SLOTS[holder.getCorrectIndex()];
        inventory.setItem(correctSlot, HackTerminalGUI.namedItem(Material.EMERALD_BLOCK, "§cÉchec — la clé était ici", null));
        player.sendMessage("§5[Hack] §cIntrusion échouée, le terminal se verrouille.");
        this.plugin.getJobManager().addXp(player, JobType.HACKER,
                this.plugin.getConfig().getDouble("jobs.hacker.xp-terminal-fail", 10.0));
        this.closeSoon(player);
    }

    private void closeSoon(Player player) {
        Bukkit.getScheduler().runTaskLater((Plugin) this.plugin, player::closeInventory, 40L);
    }

    private static int indexOf(int slot) {
        for (int i = 0; i < HackTerminalGUI.GRID_SLOTS.length; i++) {
            if (HackTerminalGUI.GRID_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private static int manhattanDistance(int a, int b) {
        int r1 = a / 3;
        int c1 = a % 3;
        int r2 = b / 3;
        int c2 = b % 3;
        return Math.abs(r1 - r2) + Math.abs(c1 - c2);
    }

    private static String hintFor(int distance) {
        switch (distance) {
            case 1:
                return "§6Brûlant";
            case 2:
                return "§eChaud";
            case 3:
                return "§bTiède";
            default:
                return "§9Froid";
        }
    }
}
