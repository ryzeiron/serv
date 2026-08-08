package com.antonin.marketeconomy.items;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

// Boussole du Marchand : une vraie boussole (pas une lodestone) dont l'aiguille est reorientee
// periodiquement (MerchantCompassTracker) vers le villageois offrant la meilleure affaire du
// moment, via le composant LODESTONE_TRACKER (tracked=false, cible mise a jour a la main).
public final class MerchantCompassItem {
    private static final String TAG_KEY = "marketeconomy_merchant_compass";

    private MerchantCompassItem() {
    }

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Items.COMPASS);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6§lBoussole du Marchand"));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Pointe vers le marchand le plus proche"),
                Component.literal("§7offrant la meilleure affaire du moment."))));
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(TAG_KEY, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), false));
        return stack;
    }

    public static boolean isMerchantCompass(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.COMPASS)) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(TAG_KEY, false);
    }

    public static void pointTo(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos) {
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(dimension, pos)), false));
    }
}
