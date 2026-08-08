package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.market.MarketCategory;
import com.antonin.marketeconomy.market.MarketItem;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.reputation.ReputationManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Actions declenchees par les liens cliquables du menu marchand PNJ (VillagerEvents) : achat/vente
// unitaire ajustes par la reputation, et "tout vendre" par categorie. Pas destinees a etre tapees
// a la main (equivalent des clics sur le menu graphique du plugin Paper).
public final class VillagerCommands {

    private VillagerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("villagerbuy")
                .then(Commands.argument("item", ItemArgument.item(buildContext)).executes(VillagerCommands::runBuy)));

        dispatcher.register(Commands.literal("villagersell")
                .then(Commands.argument("item", ItemArgument.item(buildContext)).executes(VillagerCommands::runSell)));

        var sellAll = Commands.literal("villagersellall");
        for (MarketCategory category : MarketCategory.values()) {
            sellAll.then(Commands.literal(category.name()).executes(ctx -> runSellAll(ctx, category)));
        }
        dispatcher.register(sellAll);
    }

    private static int runBuy(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Item item = ItemArgument.getItem(ctx, "item").getItem();
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        ReputationManager reputation = MarketEconomyServer.get().getReputationManager();
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();

        MarketItem marketItem = market.getItem(item);
        if (marketItem == null) {
            return 0;
        }
        double price = round2(marketItem.getBuyPrice() * reputation.getBuyMultiplier(player.getUUID()));
        if (marketItem.getStock() <= 0L) {
            player.sendSystemMessage(Component.literal("§cRupture de stock pour cet item."));
            return 0;
        }
        if (!economy.withdraw(player.getUUID(), price)) {
            player.sendSystemMessage(Component.literal("§cFonds insuffisants. Prix: " + economy.format(price)));
            return 0;
        }
        market.recordPurchase(player.getUUID(), marketItem, 1L, price);
        reputation.registerTrade(ctx.getSource().getServer(), player, price);
        player.getInventory().add(new ItemStack(item, 1));
        player.sendSystemMessage(Component.literal("§aAcheté 1x " + marketItem.getDisplayName() + " pour " + economy.format(price)));
        return 1;
    }

    private static int runSell(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Item item = ItemArgument.getItem(ctx, "item").getItem();
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        ReputationManager reputation = MarketEconomyServer.get().getReputationManager();
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();

        MarketItem marketItem = market.getItem(item);
        if (marketItem == null) {
            return 0;
        }
        ItemStack toRemove = findOne(player, item);
        if (toRemove == null) {
            player.sendSystemMessage(Component.literal("§cTu n'as pas cet item dans ton inventaire."));
            return 0;
        }
        double price = round2(marketItem.getSellPrice() * reputation.getSellMultiplier(player.getUUID()));
        toRemove.shrink(1);
        market.recordSale(player.getUUID(), marketItem, 1L, price);
        reputation.registerTrade(ctx.getSource().getServer(), player, price);
        double net = market.applyBountyCut(player, price);
        economy.deposit(player.getUUID(), net);
        player.sendSystemMessage(Component.literal("§aVendu 1x " + marketItem.getDisplayName() + " pour " + economy.format(net)));
        return 1;
    }

    private static int runSellAll(CommandContext<CommandSourceStack> ctx, MarketCategory category) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
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
            for (ItemStack stack : player.getInventory().items) {
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
            reputation.registerTrade(ctx.getSource().getServer(), player, total);
            grandTotal += total;
            itemTypesSold++;
            unitsSold += count;
        }

        if (itemTypesSold == 0) {
            player.sendSystemMessage(Component.literal("§7Tu n'as rien à vendre à ce marchand."));
            return 0;
        }
        double net = market.applyBountyCut(player, grandTotal);
        economy.deposit(player.getUUID(), net);
        player.sendSystemMessage(Component.literal("§aVendu " + unitsSold + " item(s) (" + itemTypesSold
                + " type(s)) pour " + economy.format(net)));
        return 1;
    }

    private static ItemStack findOne(ServerPlayer player, Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && !stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
