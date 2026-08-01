package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.gui.MarketGUI;
import com.antonin.marketeconomy.model.MarketItem;
import com.antonin.marketeconomy.storage.EconomyHook;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MarketGUIListener
implements Listener {
    private final MarketEconomyPlugin plugin;
    private final MarketManager marketManager;
    private final EconomyHook economyHook;

    public MarketGUIListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.marketManager = plugin.getMarketManager();
        this.economyHook = plugin.getEconomyHook();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!"\u00a78\u00a7lMarch\u00e9".equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        MarketItem item = this.marketManager.getItem(clicked.getType());
        if (item == null) {
            return;
        }
        if (!this.economyHook.isEnabled()) {
            player.sendMessage("\u00a7cLe systeme d'economie (Vault) n'est pas disponible.");
            return;
        }
        if (event.getClick() == ClickType.LEFT) {
            this.handleBuy(player, item);
        } else if (event.getClick() == ClickType.RIGHT) {
            this.handleSell(player, item);
        }
        player.openInventory(MarketGUI.build(this.marketManager));
    }

    private void handleBuy(Player player, MarketItem item) {
        double price = item.getBuyPrice();
        if (item.getStock() <= 0L) {
            player.sendMessage("\u00a7cRupture de stock pour cet item.");
            return;
        }
        if (this.economyHook.getBalance(player) < price) {
            player.sendMessage("\u00a7cFonds insuffisants. Prix: " + this.economyHook.format(price));
            return;
        }
        this.economyHook.withdraw(player, price);
        item.registerBuy(1L);
        player.getInventory().addItem(new ItemStack[]{new ItemStack(item.getMaterial(), 1)});
        player.sendMessage("\u00a7aAchete 1x " + item.getDisplayName() + " pour " + this.economyHook.format(price));
    }

    private void handleSell(Player player, MarketItem item) {
        ItemStack toRemove = new ItemStack(item.getMaterial(), 1);
        if (!player.getInventory().containsAtLeast(toRemove, 1)) {
            player.sendMessage("\u00a7cTu n'as pas cet item dans ton inventaire.");
            return;
        }
        double price = item.getSellPrice();
        player.getInventory().removeItem(new ItemStack[]{toRemove});
        item.registerSell(1L);
        this.economyHook.deposit(player, price);
        player.sendMessage("\u00a7aVendu 1x " + item.getDisplayName() + " pour " + this.economyHook.format(price));
    }
}

