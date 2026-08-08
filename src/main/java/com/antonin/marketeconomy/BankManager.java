package com.antonin.marketeconomy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

// Banque d'ile : un joueur peut y deposer de l'argent pour le mettre "a l'abri" de sa poche
// courante. C'est aussi la cible du piratage bancaire du metier Hacker (/hack banque).
public class BankManager {
    private final MarketEconomyPlugin plugin;
    private final File file;
    private final Map<UUID, Double> balances = new HashMap<>();
    private final Map<UUID, Long> hackedCooldownUntil = new HashMap<>();

    public BankManager(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "banks.yml");
        this.load();
    }

    public double getBalance(UUID uuid) {
        return this.balances.getOrDefault(uuid, 0.0);
    }

    public void deposit(UUID uuid, double amount) {
        this.balances.merge(uuid, amount, Double::sum);
        this.save();
    }

    // Renvoie false si le solde est insuffisant (rien n'est retire dans ce cas)
    public boolean withdraw(UUID uuid, double amount) {
        double balance = this.getBalance(uuid);
        if (balance < amount) {
            return false;
        }
        this.balances.put(uuid, balance - amount);
        this.save();
        return true;
    }

    public boolean isRecentlyHacked(UUID uuid) {
        Long until = this.hackedCooldownUntil.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    // Draine "share" du solde de la cible (ex: 0.30 = 30%) et renvoie le montant vole (0 si le
    // compte est vide ou vient deja d'etre pirate)
    public double hack(UUID target, double share, long targetCooldownMillis) {
        if (this.isRecentlyHacked(target)) {
            return -1.0;
        }
        double balance = this.getBalance(target);
        if (balance <= 0.0) {
            return 0.0;
        }
        double stolen = Math.round(balance * share * 100.0) / 100.0;
        this.balances.put(target, balance - stolen);
        this.hackedCooldownUntil.put(target, System.currentTimeMillis() + targetCooldownMillis);
        this.save();
        return stolen;
    }

    public void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    this.balances.put(uuid, section.getDouble(key + ".balance", 0.0));
                } catch (IllegalArgumentException ignored) {
                    this.plugin.getLogger().warning("Entree invalide dans banks.yml: " + key);
                }
            }
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : this.balances.entrySet()) {
            data.set("players." + entry.getKey() + ".balance", entry.getValue());
        }
        try {
            data.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Impossible de sauvegarder banks.yml: " + e.getMessage());
        }
    }
}
