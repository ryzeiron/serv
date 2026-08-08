package com.antonin.marketeconomy.market;

import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

// Contrat a terme physique et echangeable (item PAPIER taggue via le composant CustomData) :
// la mise appartient a qui detient l'item a l'echeance, encaissable en clic droit.
public final class FuturesItem {
    private static final String CONTRACT_ID_KEY = "marketeconomy_futures_contract_id";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault());

    private FuturesItem() {
    }

    public static ItemStack createContract(FuturesContract contract, MarketItem item) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§6§lContrat Scellé"));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Type: §e" + (contract.getType() == FuturesContract.Type.LONG ? "LONG (hausse)" : "SHORT (baisse)")));
        lore.add(Component.literal("§7Sous-jacent: §f" + item.getDisplayName()));
        lore.add(Component.literal("§7Mise: §f" + round2(contract.getStake())));
        lore.add(Component.literal("§7Prix de référence: §f" + round2(contract.getPriceAtCreation())));
        lore.add(Component.literal("§7Échéance: §f" + DATE_FORMAT.format(Instant.ofEpochMilli(contract.getMaturityAtMillis()))));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§7Cet objet est échangeable : la mise"));
        lore.add(Component.literal("§7appartient à qui le détient à l'échéance."));
        lore.add(Component.literal("§eClic droit après échéance §7pour encaisser"));
        stack.set(DataComponents.LORE, new ItemLore(lore));

        CompoundTag tag = new CompoundTag();
        tag.putString(CONTRACT_ID_KEY, contract.getId().toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static UUID readContractId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(CONTRACT_ID_KEY)) {
            return null;
        }
        try {
            return UUID.fromString(tag.getStringOr(CONTRACT_ID_KEY, ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
