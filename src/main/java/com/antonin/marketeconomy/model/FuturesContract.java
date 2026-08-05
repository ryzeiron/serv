package com.antonin.marketeconomy.model;

import java.util.UUID;
import org.bukkit.Material;

public class FuturesContract {

    public enum Type {
        LONG,
        SHORT
    }

    private final UUID id;
    private final UUID creator;
    private final Material material;
    private final Type type;
    private final double stake;
    private final double priceAtCreation;
    private final long maturityAtMillis;
    private boolean settled = false;
    private double lockedPayout = 0.0;

    public FuturesContract(UUID id, UUID creator, Material material, Type type, double stake, double priceAtCreation, long maturityAtMillis) {
        this.id = id;
        this.creator = creator;
        this.material = material;
        this.type = type;
        this.stake = stake;
        this.priceAtCreation = priceAtCreation;
        this.maturityAtMillis = maturityAtMillis;
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getCreator() {
        return this.creator;
    }

    public Material getMaterial() {
        return this.material;
    }

    public Type getType() {
        return this.type;
    }

    public double getStake() {
        return this.stake;
    }

    public double getPriceAtCreation() {
        return this.priceAtCreation;
    }

    public long getMaturityAtMillis() {
        return this.maturityAtMillis;
    }

    public boolean isMatured(long nowMillis) {
        return nowMillis >= this.maturityAtMillis;
    }

    public boolean isSettled() {
        return this.settled;
    }

    public double getLockedPayout() {
        return this.lockedPayout;
    }

    // Fige le paiement au prix constate a l'echeance ; le contrat reste "au porteur"
    // jusqu'a ce que quelqu'un l'encaisse (clic droit sur l'item physique)
    public void settle(double priceAtMaturity) {
        this.lockedPayout = this.computePayout(priceAtMaturity);
        this.settled = true;
    }

    // LONG gagne quand le prix monte, SHORT gagne quand le prix baisse ; la mise ne peut pas tomber sous 0
    public double computePayout(double priceAtMaturity) {
        if (this.priceAtCreation <= 0.0) {
            return this.stake;
        }
        double changeRatio = (priceAtMaturity - this.priceAtCreation) / this.priceAtCreation;
        double factor = this.type == Type.LONG ? 1.0 + changeRatio : 1.0 - changeRatio;
        return this.stake * Math.max(0.0, factor);
    }
}
