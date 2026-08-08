package com.antonin.marketeconomy.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Banque d'ile : un joueur peut y deposer de l'argent pour le mettre a l'abri de sa poche
// courante (EconomyManager). Cible future du piratage bancaire du metier Hacker.
public class BankManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type BALANCES_TYPE = new TypeToken<Map<String, Double>>() { }.getType();

    private final Path file;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Long> hackedCooldownUntil = new HashMap<>();

    public BankManager(Path file) {
        this.file = file;
        this.load();
    }

    public double getBalance(UUID uuid) {
        return this.balances.getOrDefault(uuid, 0.0);
    }

    public void deposit(UUID uuid, double amount) {
        this.balances.merge(uuid, amount, Double::sum);
        this.save();
    }

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

    // Draine "share" du solde de la cible et renvoie le montant vole (-1 si recemment piratee,
    // 0 si le compte est vide)
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

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try (var reader = Files.newBufferedReader(this.file)) {
            Map<String, Double> raw = GSON.fromJson(reader, BALANCES_TYPE);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, Double> entry : raw.entrySet()) {
                try {
                    this.balances.put(UUID.fromString(entry.getKey()), entry.getValue());
                } catch (IllegalArgumentException ignored) {
                    // entree invalide, on l'ignore
                }
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de charger " + this.file + ": " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(this.file.getParent());
            Map<String, Double> raw = new ConcurrentHashMap<>();
            for (Map.Entry<UUID, Double> entry : this.balances.entrySet()) {
                raw.put(entry.getKey().toString(), entry.getValue());
            }
            try (var writer = Files.newBufferedWriter(this.file)) {
                GSON.toJson(raw, BALANCES_TYPE, writer);
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de sauvegarder " + this.file + ": " + e.getMessage());
        }
    }
}
