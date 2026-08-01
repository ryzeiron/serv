package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.model.MarketCategory;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MarketMenuHolder implements InventoryHolder {
    private final MarketCategory category;
    private final Map<Integer, MarketCategory> categoryBySlot;
    private final Map<Integer, Material> materialBySlot;
    private final int backButtonSlot;
    private Inventory inventory;

    public static MarketMenuHolder mainMenu(Map<Integer, MarketCategory> categoryBySlot) {
        return new MarketMenuHolder(null, categoryBySlot, null, -1);
    }

    public static MarketMenuHolder categoryMenu(MarketCategory category, Map<Integer, Material> materialBySlot, int backButtonSlot) {
        return new MarketMenuHolder(category, null, materialBySlot, backButtonSlot);
    }

    private MarketMenuHolder(MarketCategory category, Map<Integer, MarketCategory> categoryBySlot, Map<Integer, Material> materialBySlot, int backButtonSlot) {
        this.category = category;
        this.categoryBySlot = categoryBySlot;
        this.materialBySlot = materialBySlot;
        this.backButtonSlot = backButtonSlot;
    }

    public boolean isMainMenu() {
        return this.category == null;
    }

    // categorie affichee par ce sous-menu, null si c'est le menu principal
    public MarketCategory getCategory() {
        return this.category;
    }

    public MarketCategory getCategoryAt(int slot) {
        return this.categoryBySlot == null ? null : this.categoryBySlot.get(slot);
    }

    public Material getMaterialAt(int slot) {
        return this.materialBySlot == null ? null : this.materialBySlot.get(slot);
    }

    public boolean isBackButton(int slot) {
        return slot == this.backButtonSlot;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
