package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.items.MerchantCompassItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// /marketitem boussole : donne la Boussole du Marchand. La variante "Graine Spéculative" du
// plugin Paper (item fantaisie pour un contrat a terme identique) n'est pas portee, voir /futures.
public final class SpecialItemCommand {

    private SpecialItemCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("marketitem")
                .executes(SpecialItemCommand::runUsage)
                .then(Commands.literal("boussole").executes(SpecialItemCommand::runCompass)));
    }

    private static int runCompass(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.getInventory().add(MerchantCompassItem.create());
        player.sendSystemMessage(Component.literal("§6[Marché] §eTu reçois une Boussole du Marchand."));
        return 1;
    }

    private static int runUsage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.sendSystemMessage(Component.literal("§cUtilisation: /marketitem boussole"));
        return 1;
    }
}
