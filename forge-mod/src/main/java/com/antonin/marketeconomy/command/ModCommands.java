package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.economy.BankManager;
import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.market.MarketItem;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Commandes economiques de base : /market (liste des prix), /buy, /sell, /banque.
// Le menu graphique du marche (GUI) viendra avec le portage du reste des interfaces.
public final class ModCommands {

    private ModCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("market")
                .executes(ModCommands::runMarket));

        dispatcher.register(Commands.literal("buy")
                .then(Commands.argument("item", ItemArgument.item(buildContext))
                        .executes(ctx -> runBuy(ctx, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> runBuy(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))));

        dispatcher.register(Commands.literal("sell")
                .executes(ctx -> runSell(ctx, 1))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> runSell(ctx, IntegerArgumentType.getInteger(ctx, "amount")))));

        dispatcher.register(Commands.literal("banque")
                .executes(ModCommands::runBankBalance)
                .then(Commands.literal("solde").executes(ModCommands::runBankBalance))
                .then(Commands.literal("deposer")
                        .then(Commands.argument("montant", DoubleArgumentType.doubleArg(0.01))
                                .executes(ctx -> runBankDeposit(ctx, DoubleArgumentType.getDouble(ctx, "montant")))))
                .then(Commands.literal("retirer")
                        .then(Commands.argument("montant", DoubleArgumentType.doubleArg(0.01))
                                .executes(ctx -> runBankWithdraw(ctx, DoubleArgumentType.getDouble(ctx, "montant"))))));
    }

    private static int runMarket(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        player.sendSystemMessage(Component.literal("§6=== Marché (tendance " + market.getMarketTrendArrow() + ") ==="));
        for (MarketItem item : market.getItems().values()) {
            player.sendSystemMessage(Component.literal(String.format(Locale.US,
                    "§7%-22s §fAchat: §a%.2f §7| Vente: §c%.2f §7| Stock: §f%d §7%s",
                    item.getDisplayName(), item.getBuyPrice(), item.getSellPrice(), item.getStock(), item.getTrendArrow())));
        }
        return 1;
    }

    private static int runBuy(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Item item = ItemArgument.getItem(ctx, "item").getItem();
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        MarketItem marketItem = market.getItem(item);
        if (marketItem == null) {
            ctx.getSource().sendFailure(Component.literal("§cCet item n'est pas échangeable sur le marché."));
            return 0;
        }
        if (marketItem.getStock() < amount) {
            ctx.getSource().sendFailure(Component.literal("§cStock insuffisant. Disponible: " + marketItem.getStock()));
            return 0;
        }
        double total = round2(marketItem.getBuyPrice() * amount);
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        if (!economy.withdraw(player.getUUID(), total)) {
            ctx.getSource().sendFailure(Component.literal("§cFonds insuffisants. Prix total: " + economy.format(total)));
            return 0;
        }
        market.recordPurchase(player.getUUID(), marketItem, amount, total);
        MarketEconomyServer.get().getReputationManager().registerTrade(ctx.getSource().getServer(), player, total);
        player.getInventory().add(new ItemStack(item, amount));
        player.sendSystemMessage(Component.literal("§aAcheté " + amount + "x " + marketItem.getDisplayName()
                + " pour " + economy.format(total)));
        return 1;
    }

    private static int runSell(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§cTiens l'item que tu veux vendre en main."));
            return 0;
        }
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        MarketItem marketItem = market.getItem(hand.getItem());
        if (marketItem == null) {
            ctx.getSource().sendFailure(Component.literal("§cCet item n'est pas échangeable sur le marché."));
            return 0;
        }
        int sellAmount = Math.min(amount, hand.getCount());
        double total = round2(marketItem.getSellPrice() * sellAmount);
        hand.shrink(sellAmount);
        market.recordSale(player.getUUID(), marketItem, sellAmount, total);
        MarketEconomyServer.get().getReputationManager().registerTrade(ctx.getSource().getServer(), player, total);
        double net = market.applyBountyCut(player, total);
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        economy.deposit(player.getUUID(), net);
        player.sendSystemMessage(Component.literal("§aVendu " + sellAmount + "x " + marketItem.getDisplayName()
                + " pour " + economy.format(net)));
        return 1;
    }

    private static int runBankBalance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BankManager bank = MarketEconomyServer.get().getBankManager();
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        player.sendSystemMessage(Component.literal("§6[Banque] §eSolde de ta banque: §f"
                + economy.format(bank.getBalance(player.getUUID()))));
        return 1;
    }

    private static int runBankDeposit(CommandContext<CommandSourceStack> ctx, double amount) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        if (!economy.withdraw(player.getUUID(), amount)) {
            ctx.getSource().sendFailure(Component.literal("§6[Banque] §cFonds insuffisants dans ta poche."));
            return 0;
        }
        MarketEconomyServer.get().getBankManager().deposit(player.getUUID(), amount);
        player.sendSystemMessage(Component.literal("§6[Banque] §eDéposé " + economy.format(amount) + " en banque."));
        return 1;
    }

    private static int runBankWithdraw(CommandContext<CommandSourceStack> ctx, double amount) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (!MarketEconomyServer.get().getBankManager().withdraw(player.getUUID(), amount)) {
            ctx.getSource().sendFailure(Component.literal("§6[Banque] §cSolde bancaire insuffisant."));
            return 0;
        }
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        economy.deposit(player.getUUID(), amount);
        player.sendSystemMessage(Component.literal("§6[Banque] §eRetiré " + economy.format(amount) + " de la banque."));
        return 1;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
