package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.model.MarketCategory;
import com.antonin.marketeconomy.model.MarketItem;
import com.antonin.marketeconomy.storage.EconomyHook;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class MarketGUIListener implements Listener {
    private final MarketManager marketManager;
    private final EconomyHook economyHook;

    public MarketGUIListener(MarketEconomyPlugin plugin) {
        this.marketManager = plugin.getMarketManager();
        this.economyHook = plugin.getEconomyHook();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder rawHolder = topInventory.getHolder();
        if (!(rawHolder instanceof MarketMenuHolder)) {
            return;
        }
        MarketMenuHolder holder = (MarketMenuHolder) rawHolder;
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

        if (holder.isMainMenu()) {
            MarketCategory category = holder.getCategoryAt(slot);
            if (category != null) {
                MarketGUI.openCategoryMenu(player, this.marketManager, category);
            }
            return;
        }

        if (holder.isBackButton(slot)) {
            MarketGUI.openMainMenu(player, this.marketManager, this.economyHook);
            return;
        }

        Material material = holder.getMaterialAt(slot);
        if (material == null) {
            return;
        }
        MarketItem item = this.marketManager.getItem(material);
        if (item == null) {
            return;
        }
        if (!this.economyHook.isEnabled()) {
            player.sendMessage("§cLe systeme d'economie (Vault) n'est pas disponible.");
            return;
        }
        if (event.getClick() == ClickType.LEFT) {
            this.handleBuy(player, item);
        } else if (event.getClick() == ClickType.RIGHT) {
            this.handleSell(player, item);
        }
        MarketGUI.openCategoryMenu(player, this.marketManager, holder.getCategory());
    }

    private void handleBuy(Player player, MarketItem item) {
        double price = item.getBuyPrice();
        if (item.getStock() <= 0L) {
            player.sendMessage("§cRupture de stock pour cet item.");
            return;
        }
        if (this.economyHook.getBalance(player) < price) {
            player.sendMessage("§cFonds insuffisants. Prix: " + this.economyHook.format(price));
            return;
        }
        this.economyHook.withdraw(player, price);
        this.marketManager.recordPurchase(item, 1L, price);
        player.getInventory().addItem(new ItemStack(item.getMaterial(), 1));
        player.sendMessage("§aAcheté 1x " + item.getDisplayName() + " pour " + this.economyHook.format(price));
    }

    private void handleSell(Player player, MarketItem item) {
        ItemStack toRemove = new ItemStack(item.getMaterial(), 1);
        if (!player.getInventory().containsAtLeast(toRemove, 1)) {
            player.sendMessage("§cTu n'as pas cet item dans ton inventaire.");
            return;
        }
        double price = item.getSellPrice();
        player.getInventory().removeItem(toRemove);
        this.marketManager.recordSale(item, 1L, price);
        this.economyHook.deposit(player, price);
        player.sendMessage("§aVendu 1x " + item.getDisplayName() + " pour " + this.economyHook.format(price));
    }
}
