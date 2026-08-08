package com.antonin.marketeconomy.market;

import java.util.UUID;

// Contrat place sur la tete d'un joueur repere pour manipulation de marche :
// tant qu'il est actif, une part de chaque vente de la cible est reversee au placeur
public class Bounty {
    private final UUID target;
    private final UUID placer;
    private final double cutShare;
    private final long expiresAtMillis;

    public Bounty(UUID target, UUID placer, double cutShare, long expiresAtMillis) {
        this.target = target;
        this.placer = placer;
        this.cutShare = cutShare;
        this.expiresAtMillis = expiresAtMillis;
    }

    public UUID getTarget() {
        return this.target;
    }

    public UUID getPlacer() {
        return this.placer;
    }

    public double getCutShare() {
        return this.cutShare;
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis >= this.expiresAtMillis;
    }
}
