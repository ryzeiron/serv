package com.antonin.marketeconomy.registry;

import com.antonin.marketeconomy.MarketEconomyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MarketEconomyMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MARKET_ECONOMY_TAB = TABS.register("market_economy",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.marketeconomy"))
                    .icon(() -> new ItemStack(ModItems.ORDINATEUR.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.LITHIUM_INGOT.get());
                        output.accept(ModItems.PLASTIC.get());
                        output.accept(ModItems.ORDINATEUR.get());
                        output.accept(ModItems.LITHIUM_ORE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
