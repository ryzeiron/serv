package com.antonin.marketeconomy.registry;

import com.antonin.marketeconomy.MarketEconomyMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MarketEconomyMod.MOD_ID);

    public static final RegistryObject<Item> LITHIUM_INGOT = ITEMS.register("lithium_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PLASTIC = ITEMS.register("plastic",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ORDINATEUR = ITEMS.register("ordinateur",
            () -> new BlockItem(ModBlocks.ORDINATEUR.get(), new Item.Properties()));

    private ModItems() {
    }
}
