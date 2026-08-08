package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// /hud [on|off] : bascule (ou force) l'affichage du HUD en barre d'action.
public final class HudCommand {

    private HudCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hud")
                .executes(ctx -> run(ctx, null))
                .then(Commands.literal("on").executes(ctx -> run(ctx, true)))
                .then(Commands.literal("off").executes(ctx -> run(ctx, false))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, Boolean forced) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var hud = MarketEconomyServer.get().getHudManager();
        boolean newState = forced != null ? forced : !hud.isEnabled(player.getUUID());
        hud.setEnabled(player.getUUID(), newState);
        player.sendSystemMessage(Component.literal(newState ? "§6[HUD] §eAffichage activé." : "§6[HUD] §eAffichage désactivé."));
        return 1;
    }
}
