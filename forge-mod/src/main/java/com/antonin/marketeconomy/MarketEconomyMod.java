package com.antonin.marketeconomy;

import com.antonin.marketeconomy.mine.MineEvents;
import com.antonin.marketeconomy.market.FuturesEvents;
import com.antonin.marketeconomy.registry.ModBlocks;
import com.antonin.marketeconomy.registry.ModCreativeTabs;
import com.antonin.marketeconomy.registry.ModItems;
import com.antonin.marketeconomy.server.ServerEvents;
import com.antonin.marketeconomy.villager.VillagerEvents;
import java.lang.invoke.MethodHandles;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MarketEconomyMod.MOD_ID)
public class MarketEconomyMod {
    public static final String MOD_ID = "marketeconomy";

    public MarketEconomyMod(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();

        ModItems.ITEMS.register(modBusGroup);
        ModBlocks.BLOCKS.register(modBusGroup);
        ModCreativeTabs.TABS.register(modBusGroup);

        BusGroup.DEFAULT.register(MethodHandles.lookup(), ServerEvents.class);
        BusGroup.DEFAULT.register(MethodHandles.lookup(), MineEvents.class);
        BusGroup.DEFAULT.register(MethodHandles.lookup(), VillagerEvents.class);
        BusGroup.DEFAULT.register(MethodHandles.lookup(), FuturesEvents.class);
    }
}
