package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.MarketItem;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class HackerComputerGUIListener implements Listener {
    private final MarketEconomyPlugin plugin;

    public HackerComputerGUIListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder rawHolder = topInventory.getHolder();
        if (!(rawHolder instanceof HackerComputerHolder)) {
            return;
        }
        HackerComputerHolder holder = (HackerComputerHolder) rawHolder;
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

        switch (holder.getMode()) {
            case MAIN:
                this.handleMain(player, slot);
                return;
            case ITEM_REPORT:
            case ITEM_PRICE:
                this.handleItemPicker(player, holder, slot, event.getClick());
                return;
            case PLAYER_WIRETAP:
            case PLAYER_BANK:
                this.handlePlayerPicker(player, holder, slot);
                return;
            default:
        }
    }

    private void handleMain(Player player, int slot) {
        switch (slot) {
            case 11:
                HackerComputerGUI.openItemPicker(this.plugin, player, HackerComputerHolder.Mode.ITEM_REPORT);
                return;
            case 12:
                HackerComputerGUI.openItemPicker(this.plugin, player, HackerComputerHolder.Mode.ITEM_PRICE);
                return;
            case 13:
                this.plugin.getHackerAbilityService().scramble(player);
                player.closeInventory();
                return;
            case 14:
                HackerComputerGUI.openPlayerPicker(this.plugin, player, HackerComputerHolder.Mode.PLAYER_WIRETAP);
                return;
            case 15:
                HackerComputerGUI.openPlayerPicker(this.plugin, player, HackerComputerHolder.Mode.PLAYER_BANK);
                return;
            case 16:
                this.plugin.getHackerAbilityService().openTerminal(player);
                return;
            default:
        }
    }

    private void handleItemPicker(Player player, HackerComputerHolder holder, int slot, ClickType click) {
        if (slot == 31) {
            HackerComputerGUI.openMain(this.plugin, player);
            return;
        }
        Material material = holder.getMaterialAt(slot);
        if (material == null) {
            return;
        }
        MarketItem item = this.plugin.getMarketManager().getItem(material);
        if (item == null) {
            return;
        }
        if (holder.getMode() == HackerComputerHolder.Mode.ITEM_REPORT) {
            this.plugin.getHackerAbilityService().marketReport(player, item);
            player.closeInventory();
        } else {
            boolean up = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
            this.plugin.getHackerAbilityService().priceHack(player, item, up);
            player.closeInventory();
        }
    }

    @SuppressWarnings("deprecation")
    private void handlePlayerPicker(Player player, HackerComputerHolder holder, int slot) {
        if (slot == 31) {
            HackerComputerGUI.openMain(this.plugin, player);
            return;
        }
        UUID targetUuid = holder.getPlayerAt(slot);
        if (targetUuid == null) {
            return;
        }
        if (holder.getMode() == HackerComputerHolder.Mode.PLAYER_WIRETAP) {
            Player target = Bukkit.getPlayer(targetUuid);
            if (target == null) {
                player.sendMessage("§5[Hack] §cCe joueur s'est déconnecté.");
                player.closeInventory();
                return;
            }
            this.plugin.getHackerAbilityService().wiretap(player, target);
        } else {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
            this.plugin.getHackerAbilityService().bankHack(player, target);
        }
        player.closeInventory();
    }
}
