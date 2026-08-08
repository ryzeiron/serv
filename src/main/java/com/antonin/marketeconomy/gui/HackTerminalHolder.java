package com.antonin.marketeconomy.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

// Etat du mini-jeu "terminal de piratage" : trouver le bon noeud sur une grille 3x3 en 3 essais
public class HackTerminalHolder implements InventoryHolder {
    private Inventory inventory;
    private final int correctIndex;
    private final int hackerLevel;
    private int attemptsLeft;
    private boolean resolved = false;

    public HackTerminalHolder(int correctIndex, int hackerLevel, int attempts) {
        this.correctIndex = correctIndex;
        this.hackerLevel = hackerLevel;
        this.attemptsLeft = attempts;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public int getCorrectIndex() {
        return this.correctIndex;
    }

    public int getHackerLevel() {
        return this.hackerLevel;
    }

    public int getAttemptsLeft() {
        return this.attemptsLeft;
    }

    public void decrementAttempts() {
        this.attemptsLeft = Math.max(0, this.attemptsLeft - 1);
    }

    public boolean isResolved() {
        return this.resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
