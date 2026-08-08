package com.antonin.marketeconomy.server;

import com.antonin.marketeconomy.MarketEconomyMod;
import com.antonin.marketeconomy.command.HackCommands;
import com.antonin.marketeconomy.command.JobCommands;
import com.antonin.marketeconomy.command.MineCommands;
import com.antonin.marketeconomy.command.ModCommands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = MarketEconomyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEvents {
    private static final int PRICE_UPDATE_INTERVAL_TICKS = 20 * 60;
    private static final int MINE_REGEN_INTERVAL_TICKS = 20 * 60 * 25;
    private static int tickCounter = 0;
    private static int mineTickCounter = 0;

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
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MarketEconomyServer.get() == null) {
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
    }
}
