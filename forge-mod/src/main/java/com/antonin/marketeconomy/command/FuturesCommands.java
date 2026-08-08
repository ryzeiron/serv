package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.market.FuturesContract;
import com.antonin.marketeconomy.market.FuturesItem;
import com.antonin.marketeconomy.market.MarketItem;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// /futures <long|short> <item> <mise> <minutes> : ouvre un contrat a terme et donne un item
// physique et echangeable (FuturesItem) ; /futures list liste les contrats dans l'inventaire.
public final class FuturesCommands {

    private FuturesCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("futures")
                .executes(FuturesCommands::runUsage)
                .then(Commands.literal("list").executes(FuturesCommands::runList))
                .then(Commands.literal("long")
                        .then(openContractArgs(buildContext, FuturesContract.Type.LONG)))
                .then(Commands.literal("short")
                        .then(openContractArgs(buildContext, FuturesContract.Type.SHORT))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> openContractArgs(
            CommandBuildContext buildContext, FuturesContract.Type type) {
        return Commands.argument("item", ItemArgument.item(buildContext))
                .then(Commands.argument("stake", DoubleArgumentType.doubleArg(0.01))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1))
                                .executes(ctx -> runOpen(ctx, type))));
    }

    private static int runOpen(CommandContext<CommandSourceStack> ctx, FuturesContract.Type type) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Item item = ItemArgument.getItem(ctx, "item").getItem();
        double stake = DoubleArgumentType.getDouble(ctx, "stake");
        int minutes = IntegerArgumentType.getInteger(ctx, "minutes");

        MarketManager market = MarketEconomyServer.get().getMarketManager();
        MarketItem marketItem = market.getItem(item);
        if (marketItem == null) {
            player.sendSystemMessage(Component.literal("§cCet item n'est pas échangeable sur le marché."));
            return 0;
        }
        String error = market.validateContractRequest(stake, minutes);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c" + error));
            return 0;
        }
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        if (economy.getBalance(player.getUUID()) < stake) {
            player.sendSystemMessage(Component.literal("§cFonds insuffisants. Mise: " + economy.format(stake)));
            return 0;
        }
        economy.withdraw(player.getUUID(), stake);
        FuturesContract contract = market.openContract(player.getUUID(), marketItem, type, stake, minutes);
        player.getInventory().add(FuturesItem.createContract(contract, marketItem));

        player.sendSystemMessage(Component.literal("§6[Contrat] §eContrat " + (type == FuturesContract.Type.LONG ? "LONG" : "SHORT")
                + " ouvert sur " + marketItem.getDisplayName() + " : mise " + economy.format(stake)
                + ", prix de référence " + marketItem.getCurrentPrice() + ", échéance dans " + minutes + " min. "
                + "Le contrat physique t'a été donné — échangeable avec d'autres joueurs avant échéance."));
        return 1;
    }

    private static int runList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        long now = System.currentTimeMillis();
        int found = 0;
        player.sendSystemMessage(Component.literal("§6=== Contrats dans ton inventaire ==="));
        for (ItemStack stack : player.getInventory().items) {
            UUID contractId = FuturesItem.readContractId(stack);
            if (contractId == null) {
                continue;
            }
            FuturesContract contract = market.getContract(contractId);
            if (contract == null) {
                continue;
            }
            found++;
            String status;
            if (contract.isSettled()) {
                status = "§aprêt à encaisser (" + economy.format(contract.getLockedPayout()) + ")";
            } else {
                long remaining = Math.max(0L, (contract.getMaturityAtMillis() - now) / 1000L);
                status = "§7échéance dans " + remaining + "s";
            }
            player.sendSystemMessage(Component.literal("§7- §e" + (contract.getType() == FuturesContract.Type.LONG ? "LONG" : "SHORT")
                    + " §7" + contract.getItem() + " | mise " + economy.format(contract.getStake()) + " | " + status));
        }
        if (found == 0) {
            player.sendSystemMessage(Component.literal("§7Aucun contrat trouvé dans ton inventaire."));
        }
        return 1;
    }

    private static int runUsage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.sendSystemMessage(Component.literal("§cUtilisation: /futures <long|short> <item> <mise> <minutes>"));
        player.sendSystemMessage(Component.literal("§cOu: /futures list"));
        return 1;
    }
}
