package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.model.MarketCategory;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class VillagerTradeHolder implements InventoryHolder {
    private final Map<Integer, Material> materialBySlot;
    private final MarketCategory category;
    private final int sellAllButtonSlot;
    private Inventory inventory;

    public VillagerTradeHolder(Map<Integer, Material> materialBySlot, MarketCategory category, int sellAllButtonSlot) {
        this.materialBySlot = materialBySlot;
        this.category = category;
        this.sellAllButtonSlot = sellAllButtonSlot;
    }

    public Material getMaterialAt(int slot) {
        return this.materialBySlot.get(slot);
    }

    public MarketCategory getCategory() {
        return this.category;
    }

    public boolean isSellAllButton(int slot) {
        return slot == this.sellAllButtonSlot;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
