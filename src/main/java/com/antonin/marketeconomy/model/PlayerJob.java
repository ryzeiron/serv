package com.antonin.marketeconomy.model;

public class PlayerJob {
    public static final int MAX_LEVEL = 10;

    private final JobType type;
    private int level;
    private double xp;

    public PlayerJob(JobType type, int level, double xp) {
        this.type = type;
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
        this.xp = xp;
    }

    public PlayerJob(JobType type) {
        this(type, 1, 0.0);
    }

    public JobType getType() {
        return this.type;
    }

    public int getLevel() {
        return this.level;
    }

    public double getXp() {
        return this.xp;
    }

    public double xpToNextLevel(double xpPerLevelBase) {
        return xpPerLevelBase * this.level;
    }

    // Ajoute de l'xp et monte de niveau si besoin ; renvoie le nombre de niveaux gagnes
    public int addXp(double amount, double xpPerLevelBase) {
        if (this.level >= MAX_LEVEL) {
            return 0;
        }
        this.xp += amount;
        int gained = 0;
        while (this.level < MAX_LEVEL) {
            double needed = this.xpToNextLevel(xpPerLevelBase);
            if (this.xp < needed) {
                break;
            }
            this.xp -= needed;
            this.level++;
            gained++;
        }
        if (this.level >= MAX_LEVEL) {
            this.xp = 0.0;
        }
        return gained;
    }
}
