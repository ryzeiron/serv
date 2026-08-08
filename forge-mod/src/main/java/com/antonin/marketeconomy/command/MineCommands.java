package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// Commande admin (niveau de permission 2, comme /gamemode) pour generer une mine du metier
// Mineur a la position du joueur.
public final class MineCommands {

    private MineCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mine")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 4))
                                .executes(MineCommands::runSpawn))));
    }

    private static int runSpawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int tier = IntegerArgumentType.getInteger(ctx, "tier");
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos origin = player.blockPosition();
        String error = MarketEconomyServer.get().getMineManager().spawnMine(tier, level, origin);
        if (error != null) {
            ctx.getSource().sendFailure(Component.literal("§c" + error));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§6[Mine] §eMine du palier " + tier + " générée."), false);
        return 1;
    }
}
