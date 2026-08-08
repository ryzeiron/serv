package com.antonin.marketeconomy.market;

import java.util.UUID;

// Ecoute posee par un Hacker sur les ventes d'un joueur cible
public class Wiretap {
    private final UUID target;
    private final UUID hacker;
    private final double cutShare;
    private final double catchChance;
    private final long expiresAtMillis;
    private double totalSkimmed = 0.0;
    private boolean caught = false;

    public Wiretap(UUID target, UUID hacker, double cutShare, double catchChance, long expiresAtMillis) {
        this.target = target;
        this.hacker = hacker;
        this.cutShare = cutShare;
        this.catchChance = catchChance;
        this.expiresAtMillis = expiresAtMillis;
    }

    public UUID getTarget() {
        return this.target;
    }

    public UUID getHacker() {
        return this.hacker;
    }

    public double getCutShare() {
        return this.cutShare;
    }

    public double getCatchChance() {
        return this.catchChance;
    }

    public boolean isExpired(long now) {
        return now >= this.expiresAtMillis;
    }

    public double getTotalSkimmed() {
        return this.totalSkimmed;
    }

    public void addSkimmed(double amount) {
        this.totalSkimmed += amount;
    }

    public boolean isCaught() {
        return this.caught;
    }

    public void setCaught(boolean caught) {
        this.caught = caught;
    }
}
