package com.antonin.marketeconomy.model;

import java.util.UUID;
import org.bukkit.Material;

public class FuturesContract {

    public enum Type {
        LONG,
        SHORT
    }

    private final UUID player;
    private final Material material;
    private final Type type;
    private final double stake;
    private final double priceAtCreation;
    private final long maturityAtMillis;

    public FuturesContract(UUID player, Material material, Type type, double stake, double priceAtCreation, long maturityAtMillis) {
        this.player = player;
        this.material = material;
        this.type = type;
        this.stake = stake;
        this.priceAtCreation = priceAtCreation;
        this.maturityAtMillis = maturityAtMillis;
    }

    public UUID getPlayer() {
        return this.player;
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
