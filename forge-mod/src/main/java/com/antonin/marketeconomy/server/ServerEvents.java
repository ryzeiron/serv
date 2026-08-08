package com.antonin.marketeconomy.server;

import com.antonin.marketeconomy.MarketEconomyMod;
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
    private static int tickCounter = 0;

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
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || MarketEconomyServer.get() == null) {
            return;
        }
        tickCounter++;
        if (tickCounter < PRICE_UPDATE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        MarketEconomyServer.get().getMarketManager().recalculateAll(ServerLifecycleHooks.getCurrentServer());
    }
}
