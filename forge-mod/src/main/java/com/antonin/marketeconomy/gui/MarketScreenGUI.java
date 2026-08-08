package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.market.MarketItem;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

// Menu graphique du marche : une icone par item tradable, clic gauche pour acheter 1, clic droit
// pour en vendre 1 (depuis l'inventaire du joueur). Remplace la liste en chat de la phase 2.
public final class MarketScreenGUI {

    private MarketScreenGUI() {
    }

    public static void open(ServerPlayer player) {
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        List<Item> order = new ArrayList<>(market.getItems().keySet());
        List<ItemStack> icons = new ArrayList<>();
        for (Item vanillaItem : order) {
            icons.add(buildIcon(market.getItem(vanillaItem)));
        }

        int rows = Math.max(1, Math.min(6, (icons.size() + 8) / 9));
        DisplayMenu.open(player, Component.literal("§8§lMarché (" + market.getMarketTrendArrow() + ")"), icons, rows,
                (slot, button, clickType, clicker) -> onClick(order, slot, button, clickType, clicker));
    }

    private static ItemStack buildIcon(MarketItem item) {
        ItemStack icon = new ItemStack(item.getItem());
        icon.set(DataComponents.CUSTOM_NAME, Component.literal("§e" + item.getDisplayName()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Prix d'achat: §a" + item.getBuyPrice()));
        lore.add(Component.literal("§7Prix de vente: §c" + item.getSellPrice()));
        lore.add(Component.literal("§7Stock: §f" + item.getStock() + " §7" + item.getTrendArrow()));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§eClic gauche §7pour acheter 1"));
        lore.add(Component.literal("§eClic droit §7pour vendre 1"));
        icon.set(DataComponents.LORE, new ItemLore(lore));
        return icon;
    }

    private static void onClick(List<Item> order, int slot, int button, ClickType clickType, ServerPlayer player) {
        if (clickType != ClickType.PICKUP || slot >= order.size()) {
            return;
        }
        Item vanillaItem = order.get(slot);
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        MarketItem marketItem = market.getItem(vanillaItem);
        if (marketItem == null) {
            return;
        }
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();

        if (button == 0) {
            if (marketItem.getStock() <= 0L) {
                player.sendSystemMessage(Component.literal("§cRupture de stock."));
                return;
            }
            double price = marketItem.getBuyPrice();
            if (!economy.withdraw(player.getUUID(), price)) {
                player.sendSystemMessage(Component.literal("§cFonds insuffisants. Prix: " + economy.format(price)));
                return;
            }
            market.recordPurchase(player.getUUID(), marketItem, 1L, price);
            MarketEconomyServer.get().getReputationManager().registerTrade(player.getServer(), player, price);
            player.getInventory().add(new ItemStack(vanillaItem, 1));
            player.sendSystemMessage(Component.literal("§aAcheté 1x " + marketItem.getDisplayName() + " pour " + economy.format(price)));
            open(player);
        } else if (button == 1) {
            ItemStack toRemove = null;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(vanillaItem) && !stack.isEmpty()) {
                    toRemove = stack;
                    break;
                }
            }
            if (toRemove == null) {
                player.sendSystemMessage(Component.literal("§cTu n'as pas cet item dans ton inventaire."));
                return;
            }
            double price = marketItem.getSellPrice();
            toRemove.shrink(1);
            market.recordSale(player.getUUID(), marketItem, 1L, price);
            MarketEconomyServer.get().getReputationManager().registerTrade(player.getServer(), player, price);
            double net = market.applyBountyCut(player, price);
            economy.deposit(player.getUUID(), net);
            player.sendSystemMessage(Component.literal("§aVendu 1x " + marketItem.getDisplayName() + " pour " + economy.format(net)));
            open(player);
        }
    }
}
