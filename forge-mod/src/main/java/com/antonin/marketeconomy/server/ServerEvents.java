package com.antonin.marketeconomy.server;

import com.antonin.marketeconomy.command.FlyCommand;
import com.antonin.marketeconomy.command.FuturesCommands;
import com.antonin.marketeconomy.command.HackCommands;
import com.antonin.marketeconomy.command.HudCommand;
import com.antonin.marketeconomy.command.JobCommands;
import com.antonin.marketeconomy.command.JournalCommand;
import com.antonin.marketeconomy.command.MarketAdminCommand;
import com.antonin.marketeconomy.command.MineCommands;
import com.antonin.marketeconomy.command.ModCommands;
import com.antonin.marketeconomy.command.SpecialItemCommand;
import com.antonin.marketeconomy.command.WarpCommands;
import com.antonin.marketeconomy.island.IslandManager;
import com.antonin.marketeconomy.items.MerchantCompassTracker;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class ServerEvents {
    private static final int PRICE_UPDATE_INTERVAL_TICKS = 20 * 60;
    private static final int MINE_REGEN_INTERVAL_TICKS = 20 * 60 * 25;
    private static final int HUD_REFRESH_INTERVAL_TICKS = 20 * 2;
    private static final int COMPASS_REFRESH_INTERVAL_TICKS = 20 * 2;
    private static int tickCounter = 0;
    private static int mineTickCounter = 0;
    private static int hudTickCounter = 0;
    private static int compassTickCounter = 0;

    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MarketEconomyServer.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MarketEconomyServer.stop();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher(), event.getBuildContext());
        JobCommands.register(event.getDispatcher());
        HackCommands.register(event.getDispatcher());
        MineCommands.register(event.getDispatcher());
        WarpCommands.register(event.getDispatcher());
        FuturesCommands.register(event.getDispatcher(), event.getBuildContext());
        JournalCommand.register(event.getDispatcher());
        HudCommand.register(event.getDispatcher());
        SpecialItemCommand.register(event.getDispatcher());
        MarketAdminCommand.register(event.getDispatcher());
        FlyCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (MarketEconomyServer.get() == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        MarketEconomyServer.get().getReputationManager().refreshTitle(server, player);
        teleportToIsland(server, player);
    }

    // Skyblock : envoie chaque joueur sur son ile de depart a la connexion (la creant si besoin)
    // plutot que de le laisser au spawn vanilla, qui n'a plus de sens dans un monde vide.
    private static void teleportToIsland(MinecraftServer server, ServerPlayer player) {
        ServerLevel overworld = server.overworld();
        IslandManager islands = MarketEconomyServer.get().getIslandManager();
        IslandManager.IslandSpawn island = islands.getOrCreateIsland(player, overworld);
        var pos = island.position();
        player.teleportTo(overworld, pos.x, pos.y, pos.z, Set.of(), player.getYRot(), player.getXRot(), true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (MarketEconomyServer.get() == null) {
            return;
        }
        tickCounter++;
        if (tickCounter >= PRICE_UPDATE_INTERVAL_TICKS) {
            tickCounter = 0;
            MarketEconomyServer.get().getMarketManager().recalculateAll(ServerLifecycleHooks.getCurrentServer());
        }

        mineTickCounter++;
        if (mineTickCounter >= MINE_REGEN_INTERVAL_TICKS) {
            mineTickCounter = 0;
            MarketEconomyServer.get().getMineManager().regenerateAll(ServerLifecycleHooks.getCurrentServer());
        }

        hudTickCounter++;
        if (hudTickCounter >= HUD_REFRESH_INTERVAL_TICKS) {
            hudTickCounter = 0;
            MarketEconomyServer.get().getHudManager().refreshAll(ServerLifecycleHooks.getCurrentServer());
        }

        compassTickCounter++;
        if (compassTickCounter >= COMPASS_REFRESH_INTERVAL_TICKS) {
            compassTickCounter = 0;
            MerchantCompassTracker.tick(ServerLifecycleHooks.getCurrentServer());
        }
    }
}
