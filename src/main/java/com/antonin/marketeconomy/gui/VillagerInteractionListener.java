package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.ReputationManager;
import com.antonin.marketeconomy.model.MarketItem;
import com.antonin.marketeconomy.storage.EconomyHook;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class VillagerInteractionListener implements Listener {
    private final MarketManager marketManager;
    private final ReputationManager reputationManager;
    private final EconomyHook economyHook;

    public VillagerInteractionListener(MarketEconomyPlugin plugin) {
        this.marketManager = plugin.getMarketManager();
        this.reputationManager = plugin.getReputationManager();
        this.economyHook = plugin.getEconomyHook();
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) {
            return;
        }
        Villager villager = (Villager) event.getRightClicked();
        Player player = event.getPlayer();
        event.setCancelled(true);

        if (!this.economyHook.isEnabled()) {
            player.sendMessage("§cLe systeme d'economie (Vault) n'est pas disponible.");
            return;
        }
        UUID uuid = player.getUniqueId();
        if (this.marketManager.isSuspended(uuid)) {
            player.sendMessage("§cTon acces au marche est suspendu (" + this.marketManager.getSuspensionRemainingSeconds(uuid) + "s restantes).");
            return;
        }

        player.sendMessage(this.reputationManager.buildGreeting(uuid));
        if (this.reputationManager.isHostile(uuid)) {
            return;
        }
        if (!VillagerTradeGUI.hasTradableItems(villager, this.marketManager)) {
            player.sendMessage("§7Ce villageois n'a rien à échanger pour l'instant.");
            return;
        }

        this.reputationManager.get(uuid).touchInteraction();
        VillagerTradeGUI.open(player, villager, this.marketManager, this.reputationManager);
    }

    @EventHandler
    public void onVillagerDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager)) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        this.reputationManager.penalizeVillagerKill(killer.getUniqueId());
        killer.sendMessage("§cLes villageois se souviendront de ça...");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder rawHolder = topInventory.getHolder();
        if (!(rawHolder instanceof VillagerTradeHolder)) {
            return;
        }
        VillagerTradeHolder holder = (VillagerTradeHolder) rawHolder;
        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player) humanEntity;

        Material material = holder.getMaterialAt(event.getSlot());
        if (material == null) {
            return;
        }
        MarketItem item = this.marketManager.getItem(material);
        if (item == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (event.getClick() == ClickType.LEFT) {
            this.handleBuy(player, item, uuid);
        } else if (event.getClick() == ClickType.RIGHT) {
            this.handleSell(player, item, uuid);
        }
    }

    private void handleBuy(Player player, MarketItem item, UUID uuid) {
        double price = round2(item.getBuyPrice() * this.reputationManager.getBuyMultiplier(uuid));
        if (item.getStock() <= 0L) {
            player.sendMessage("§cRupture de stock pour cet item.");
            return;
        }
        if (this.economyHook.getBalance(player) < price) {
            player.sendMessage("§cFonds insuffisants. Prix: " + this.economyHook.format(price));
            return;
        }
        this.economyHook.withdraw(player, price);
        this.marketManager.recordPurchase(player, item, 1L, price);
        this.reputationManager.registerTrade(player, price);
        player.getInventory().addItem(new ItemStack(item.getMaterial(), 1));
        player.sendMessage("§aAcheté 1x " + item.getDisplayName() + " pour " + this.economyHook.format(price));
    }

    private void handleSell(Player player, MarketItem item, UUID uuid) {
        ItemStack toRemove = new ItemStack(item.getMaterial(), 1);
        if (!player.getInventory().containsAtLeast(toRemove, 1)) {
            player.sendMessage("§cTu n'as pas cet item dans ton inventaire.");
            return;
        }
        double price = round2(item.getSellPrice() * this.reputationManager.getSellMultiplier(uuid));
        player.getInventory().removeItem(toRemove);
        this.marketManager.recordSale(player, item, 1L, price);
        this.reputationManager.registerTrade(player, price);
        this.economyHook.deposit(player, price);
        player.sendMessage("§aVendu 1x " + item.getDisplayName() + " pour " + this.economyHook.format(price));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
