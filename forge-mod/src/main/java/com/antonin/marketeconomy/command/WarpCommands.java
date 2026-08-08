package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.island.IslandManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.antonin.marketeconomy.warp.WarpManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

// Teleportation fixe (/spawn, /pvp, /sethub, /setpvp) et iles de depart (/ile).
public final class WarpCommands {

    private WarpCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn").executes(ctx -> runWarpTo(ctx, true)));
        dispatcher.register(Commands.literal("pvp").executes(ctx -> runWarpTo(ctx, false)));

        dispatcher.register(Commands.literal("sethub")
                .requires(source -> source.hasPermission(2))
                .executes(WarpCommands::runSetHub));
        dispatcher.register(Commands.literal("setpvp")
                .requires(source -> source.hasPermission(2))
                .executes(WarpCommands::runSetPvp));

        dispatcher.register(Commands.literal("ile").executes(WarpCommands::runIsland));
    }

    private static int runWarpTo(CommandContext<CommandSourceStack> ctx, boolean toHub) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        WarpManager warps = MarketEconomyServer.get().getWarpManager();
        WarpManager.Warp warp = toHub ? warps.getHub() : warps.getPvp();
        if (warp == null) {
            String setter = toHub ? "/sethub" : "/setpvp";
            player.sendSystemMessage(Component.literal("§cCe warp n'a pas encore été configuré. Un admin doit utiliser " + setter + "."));
            return 0;
        }
        if (!WarpManager.teleport(ctx.getSource().getServer(), player, warp)) {
            player.sendSystemMessage(Component.literal("§cImpossible de te téléporter (dimension introuvable)."));
            return 0;
        }
        player.sendSystemMessage(Component.literal("§6[Marché] §eTéléporté au " + (toHub ? "spawn" : "l'île PvP") + "."));
        return 1;
    }

    private static int runSetHub(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MarketEconomyServer.get().getWarpManager().setHub(player);
        player.sendSystemMessage(Component.literal("§6[Marché] §eSpawn enregistré à ta position actuelle."));
        return 1;
    }

    private static int runSetPvp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MarketEconomyServer.get().getWarpManager().setPvp(player);
        player.sendSystemMessage(Component.literal("§6[Marché] §eÎle PvP enregistrée à ta position actuelle."));
        return 1;
    }

    private static int runIsland(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        IslandManager islands = MarketEconomyServer.get().getIslandManager();
        boolean hadIsland = islands.hasIsland(player.getUUID());
        ServerLevel overworld = ctx.getSource().getServer().overworld();
        IslandManager.IslandSpawn island = islands.getOrCreateIsland(player, overworld);
        Vec3 pos = island.position();
        player.teleportTo(overworld, pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
        if (hadIsland) {
            player.sendSystemMessage(Component.literal("§6[Marché] §eTéléporté sur ton île."));
        } else {
            player.sendSystemMessage(Component.literal("§6[Marché] §eTon île vient d'être créée, bienvenue !"));
        }
        return 1;
    }
}
