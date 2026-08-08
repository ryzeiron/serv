package com.antonin.marketeconomy.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class HackerComputerHolder implements InventoryHolder {
    public enum Mode { MAIN, ITEM_REPORT, ITEM_PRICE, PLAYER_WIRETAP, PLAYER_BANK }

    private Inventory inventory;
    private final Mode mode;
    private final Map<Integer, Material> itemsBySlot = new HashMap<>();
    private final Map<Integer, UUID> playersBySlot = new HashMap<>();

    public HackerComputerHolder(Mode mode) {
        this.mode = mode;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public Mode getMode() {
        return this.mode;
    }

    public void putItem(int slot, Material material) {
        this.itemsBySlot.put(slot, material);
    }

    public Material getMaterialAt(int slot) {
        return this.itemsBySlot.get(slot);
    }

    public void putPlayer(int slot, UUID uuid) {
        this.playersBySlot.put(slot, uuid);
    }

    public UUID getPlayerAt(int slot) {
        return this.playersBySlot.get(slot);
    }
}
