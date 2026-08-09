package com.antonin.marketeconomy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// /fly : bascule le vol libre pour le joueur qui l'execute (reserve aux admins, niveau 2).
public final class FlyCommand {

    private FlyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fly")
                .requires(source -> source.hasPermission(2))
                .executes(FlyCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var abilities = player.getAbilities();
        boolean newState = !abilities.mayfly;
        abilities.mayfly = newState;
        if (!newState) {
            abilities.flying = false;
        }
        player.onUpdateAbilities();
        player.sendSystemMessage(Component.literal(newState
                ? "§6[Marché] §eVol libre activé."
                : "§6[Marché] §eVol libre désactivé."));
        return 1;
    }
}
