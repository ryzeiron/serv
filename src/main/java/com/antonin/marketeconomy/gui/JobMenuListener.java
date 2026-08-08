package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.JobType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class JobMenuListener implements Listener {
    private final MarketEconomyPlugin plugin;

    public JobMenuListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder rawHolder = topInventory.getHolder();
        if (!(rawHolder instanceof JobMenuHolder)) {
            return;
        }
        JobMenuHolder holder = (JobMenuHolder) rawHolder;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;
        int slot = event.getSlot();

        if (holder.getMode() == JobMenuHolder.Mode.MAIN) {
            JobType clicked = jobTypeAtMainSlot(slot);
            if (clicked != null) {
                JobMenuGUI.openProgression(this.plugin, player, clicked);
            }
            return;
        }

        if (slot == 18) {
            JobMenuGUI.openMain(this.plugin, player);
            return;
        }
        if (slot == 22) {
            JobType type = holder.getJobType();
            if (!this.plugin.getJobManager().hasJob(player.getUniqueId(), type)) {
                this.plugin.getJobManager().setJob(player.getUniqueId(), type);
                player.sendMessage("§6[Métier] §eTu es maintenant " + type.getDisplayName() + "§e !");
            }
            JobMenuGUI.openProgression(this.plugin, player, type);
        }
    }

    private static JobType jobTypeAtMainSlot(int slot) {
        JobType[] types = JobType.values();
        if (slot < 11 || (slot - 11) % 2 != 0) {
            return null;
        }
        int index = (slot - 11) / 2;
        return index < types.length ? types[index] : null;
    }
}
