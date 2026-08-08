package com.antonin.marketeconomy;

import com.antonin.marketeconomy.registry.ModBlocks;
import com.antonin.marketeconomy.registry.ModCreativeTabs;
import com.antonin.marketeconomy.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MarketEconomyMod.MOD_ID)
public class MarketEconomyMod {
    public static final String MOD_ID = "marketeconomy";

    public MarketEconomyMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }
}
