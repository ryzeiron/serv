package com.antonin.marketeconomy;

import com.antonin.marketeconomy.model.PlayerReputation;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ReputationManager {
    private final MarketEconomyPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerReputation> reputations = new HashMap<>();

    private final double killPenalty;
    private final double tradeGain;
    private final double trustedThreshold;
    private final double likedThreshold;
    private final double wearyThreshold;
    private final double hostileThreshold;
    private final double trustedDiscount;
    private final double likedDiscount;
    private final double wearyMarkup;

    public ReputationManager(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "reputation.yml");

        FileConfiguration config = plugin.getConfig();
        this.killPenalty = config.getDouble("villagers.kill-penalty", 25.0);
        this.tradeGain = config.getDouble("villagers.trade-gain", 1.5);
        this.trustedThreshold = config.getDouble("villagers.trusted-threshold", 80.0);
        this.likedThreshold = config.getDouble("villagers.liked-threshold", 60.0);
        this.wearyThreshold = config.getDouble("villagers.weary-threshold", 20.0);
        this.hostileThreshold = config.getDouble("villagers.hostile-threshold", 10.0);
        this.trustedDiscount = config.getDouble("villagers.trusted-discount", 0.10);
        this.likedDiscount = config.getDouble("villagers.liked-discount", 0.05);
        this.wearyMarkup = config.getDouble("villagers.weary-markup", 0.10);

        this.load();
    }

    public PlayerReputation get(UUID uuid) {
        return this.reputations.computeIfAbsent(uuid, u -> new PlayerReputation());
    }

    public void registerTrade(UUID uuid, double amountSpent) {
        PlayerReputation reputation = this.get(uuid);
        reputation.recordSpend(amountSpent);
        reputation.addTrust(this.tradeGain);
        reputation.touchInteraction();
    }

    public void penalizeVillagerKill(UUID uuid) {
        this.get(uuid).addTrust(-this.killPenalty);
    }

    public boolean isHostile(UUID uuid) {
        return this.get(uuid).getTrustScore() < this.hostileThreshold;
    }

    public double getBuyMultiplier(UUID uuid) {
        double trust = this.get(uuid).getTrustScore();
        if (trust >= this.trustedThreshold) {
            return 1.0 - this.trustedDiscount;
        }
        if (trust >= this.likedThreshold) {
            return 1.0 - this.likedDiscount;
        }
        if (trust < this.wearyThreshold) {
            return 1.0 + this.wearyMarkup;
        }
        return 1.0;
    }

    public double getSellMultiplier(UUID uuid) {
        double trust = this.get(uuid).getTrustScore();
        if (trust >= this.trustedThreshold) {
            return 1.0 + this.trustedDiscount / 2.0;
        }
        if (trust >= this.likedThreshold) {
            return 1.0 + this.likedDiscount / 2.0;
        }
        if (trust < this.wearyThreshold) {
            return 1.0 - this.wearyMarkup / 2.0;
        }
        return 1.0;
    }

    public String getTrustLabel(UUID uuid) {
        double trust = this.get(uuid).getTrustScore();
        if (trust >= this.trustedThreshold) {
            return "Ami de confiance";
        }
        if (trust >= this.likedThreshold) {
            return "Apprécié";
        }
        if (trust < this.hostileThreshold) {
            return "Hostile";
        }
        if (trust < this.wearyThreshold) {
            return "Méfiant";
        }
        return "Neutre";
    }

    public String buildGreeting(UUID uuid) {
        PlayerReputation reputation = this.get(uuid);
        double trust = reputation.getTrustScore();
        if (reputation.getTotalTrades() == 0L) {
            return "§eBonjour l'ami, je ne t'ai jamais vu ici. Bienvenue !";
        }
        if (trust < this.hostileThreshold) {
            return "§cPars. Je ne commerce pas avec les gens comme toi.";
        }
        if (trust < this.wearyThreshold) {
            return "§7Toi... je ne suis pas sûr de vouloir te vendre quoi que ce soit.";
        }
        if (trust >= this.trustedThreshold) {
            return "§eAh, te revoilà ! Toujours un plaisir de commercer avec toi.";
        }
        return "§eContent de te revoir.";
    }

    public void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                PlayerReputation reputation = new PlayerReputation();
                reputation.setTrustScore(section.getDouble(key + ".trust", 50.0));
                reputation.setTotalTrades(section.getLong(key + ".trades", 0L));
                reputation.setTotalSpent(section.getDouble(key + ".spent", 0.0));
                reputation.setLastInteractionMillis(section.getLong(key + ".last-interaction", 0L));
                this.reputations.put(uuid, reputation);
            } catch (IllegalArgumentException ignored) {
                this.plugin.getLogger().warning("Entree invalide dans reputation.yml: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerReputation> entry : this.reputations.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            PlayerReputation reputation = entry.getValue();
            data.set(base + "trust", reputation.getTrustScore());
            data.set(base + "trades", reputation.getTotalTrades());
            data.set(base + "spent", reputation.getTotalSpent());
            data.set(base + "last-interaction", reputation.getLastInteractionMillis());
        }
        try {
            data.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Impossible de sauvegarder reputation.yml: " + e.getMessage());
        }
    }
}
