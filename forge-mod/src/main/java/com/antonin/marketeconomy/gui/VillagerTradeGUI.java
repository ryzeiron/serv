package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.market.MarketCategory;
import com.antonin.marketeconomy.market.MarketItem;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.reputation.ReputationManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraftforge.server.ServerLifecycleHooks;

// Menu graphique du marchand PNJ : une icone par item de la categorie du villageois, prix
// ajustes par la reputation du joueur, clic gauche achete 1, clic droit vend 1, et une icone
// "tout vendre" en derniere position. Meme DisplayMenu que /market et le terminal Hacker.
public final class VillagerTradeGUI {

    private VillagerTradeGUI() {
    }

    public static void open(ServerPlayer player, MarketCategory category, String title) {
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        ReputationManager reputation = MarketEconomyServer.get().getReputationManager();
        double buyMultiplier = reputation.getBuyMultiplier(player.getUUID());
        double sellMultiplier = reputation.getSellMultiplier(player.getUUID());

        List<Item> order = new ArrayList<>();
        List<ItemStack> icons = new ArrayList<>();
        for (MarketItem item : market.getItems().values()) {
            if (item.getCategory() != category) {
                continue;
            }
            order.add(item.getItem());
            icons.add(buildIcon(item, buyMultiplier, sellMultiplier));
        }

        int rows = Math.max(1, Math.min(6, (icons.size() + 9) / 9));
        int sellAllSlot = rows * 9 - 1;
        while (icons.size() < sellAllSlot) {
            icons.add(ItemStack.EMPTY);
        }
        icons.add(sellAllIcon());

        DisplayMenu.open(player, Component.literal("§8§l" + title), icons, rows, (slot, button, clickType, clicker) -> {
            if (clickType != ClickType.PICKUP) {
                return;
            }
            if (slot == sellAllSlot) {
                sellAll(clicker, category);
                open(clicker, category, title);
                return;
            }
            if (slot >= order.size()) {
                return;
            }
            onTrade(clicker, order.get(slot), button);
            open(clicker, category, title);
        });
    }

    private static ItemStack buildIcon(MarketItem item, double buyMultiplier, double sellMultiplier) {
        ItemStack icon = new ItemStack(item.getItem());
        icon.set(DataComponents.CUSTOM_NAME, Component.literal("§e" + item.getDisplayName()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Prix d'achat: §a" + round2(item.getBuyPrice() * buyMultiplier)));
        lore.add(Component.literal("§7Prix de vente: §c" + round2(item.getSellPrice() * sellMultiplier)));
        lore.add(Component.literal("§7Stock: §f" + item.getStock()));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§eClic gauche §7pour acheter 1"));
        lore.add(Component.literal("§eClic droit §7pour vendre 1"));
        icon.set(DataComponents.LORE, new ItemLore(lore));
        return icon;
    }

    private static ItemStack sellAllIcon() {
        ItemStack icon = new ItemStack(Items.GOLD_BLOCK);
        icon.set(DataComponents.CUSTOM_NAME, Component.literal("§6§lTout vendre"));
        icon.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Vend tout ce que tu portes qui se"),
                Component.literal("§7négocie avec ce marchand."))));
        return icon;
    }

    private static void onTrade(ServerPlayer player, Item vanillaItem, int button) {
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        ReputationManager reputation = MarketEconomyServer.get().getReputationManager();
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        MarketItem marketItem = market.getItem(vanillaItem);
        if (marketItem == null) {
            return;
        }

        if (button == 0) {
            double price = round2(marketItem.getBuyPrice() * reputation.getBuyMultiplier(player.getUUID()));
            if (marketItem.getStock() <= 0L) {
                player.sendSystemMessage(Component.literal("§cRupture de stock pour cet item."));
                return;
            }
            if (!economy.withdraw(player.getUUID(), price)) {
                player.sendSystemMessage(Component.literal("§cFonds insuffisants. Prix: " + economy.format(price)));
                return;
            }
            market.recordPurchase(player.getUUID(), marketItem, 1L, price);
            reputation.registerTrade(ServerLifecycleHooks.getCurrentServer(), player, price);
            player.getInventory().add(new ItemStack(vanillaItem, 1));
            player.sendSystemMessage(Component.literal("§aAcheté 1x " + marketItem.getDisplayName() + " pour " + economy.format(price)));
        } else if (button == 1) {
            ItemStack toRemove = null;
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (stack.is(vanillaItem) && !stack.isEmpty()) {
                    toRemove = stack;
                    break;
                }
            }
            if (toRemove == null) {
                player.sendSystemMessage(Component.literal("§cTu n'as pas cet item dans ton inventaire."));
                return;
            }
            double price = round2(marketItem.getSellPrice() * reputation.getSellMultiplier(player.getUUID()));
            toRemove.shrink(1);
            market.recordSale(player.getUUID(), marketItem, 1L, price);
            reputation.registerTrade(ServerLifecycleHooks.getCurrentServer(), player, price);
            double net = market.applyBountyCut(player, price);
            economy.deposit(player.getUUID(), net);
            player.sendSystemMessage(Component.literal("§aVendu 1x " + marketItem.getDisplayName() + " pour " + economy.format(net)));
        }
    }

    private static void sellAll(ServerPlayer player, MarketCategory category) {
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        ReputationManager reputation = MarketEconomyServer.get().getReputationManager();
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        double sellMultiplier = reputation.getSellMultiplier(player.getUUID());

        double grandTotal = 0.0;
        int itemTypesSold = 0;
        long unitsSold = 0L;
        for (MarketItem marketItem : market.getItems().values()) {
            if (marketItem.getCategory() != category) {
                continue;
            }
            int count = 0;
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (stack.is(marketItem.getItem())) {
                    count += stack.getCount();
                    stack.setCount(0);
                }
            }
            if (count <= 0) {
                continue;
            }
            double total = round2(marketItem.getSellPrice() * sellMultiplier * count);
            market.recordSale(player.getUUID(), marketItem, count, total);
            reputation.registerTrade(ServerLifecycleHooks.getCurrentServer(), player, total);
            grandTotal += total;
            itemTypesSold++;
            unitsSold += count;
        }

        if (itemTypesSold == 0) {
            player.sendSystemMessage(Component.literal("§7Tu n'as rien à vendre à ce marchand."));
            return;
        }
        double net = market.applyBountyCut(player, grandTotal);
        economy.deposit(player.getUUID(), net);
        player.sendSystemMessage(Component.literal("§aVendu " + unitsSold + " item(s) (" + itemTypesSold + " type(s)) pour " + economy.format(net)));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
