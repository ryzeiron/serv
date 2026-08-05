package com.antonin.marketeconomy.gui;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class VillagerTradeHolder implements InventoryHolder {
    private final Map<Integer, Material> materialBySlot;
    private Inventory inventory;

    public VillagerTradeHolder(Map<Integer, Material> materialBySlot) {
        this.materialBySlot = materialBySlot;
    }

    public Material getMaterialAt(int slot) {
        return this.materialBySlot.get(slot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
