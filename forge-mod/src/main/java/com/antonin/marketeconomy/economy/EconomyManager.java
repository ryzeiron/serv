package com.antonin.marketeconomy.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

// Porte-monnaie des joueurs. Remplace Vault (absent sous Forge) par une persistence JSON
// simple propre au mod, sauvegardee dans le dossier de la partie.
public class EconomyManager {
    private static final double STARTING_BALANCE = 500.0;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type BALANCES_TYPE = new TypeToken<Map<String, Double>>() { }.getType();

    private final Path file;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();

    public EconomyManager(Path file) {
        this.file = file;
        this.load();
    }

    public double getBalance(UUID uuid) {
        return this.balances.computeIfAbsent(uuid, u -> STARTING_BALANCE);
    }

    public double getBalance(ServerPlayer player) {
        return this.getBalance(player.getUUID());
    }

    public boolean withdraw(UUID uuid, double amount) {
        double balance = this.getBalance(uuid);
        if (balance < amount) {
            return false;
        }
        this.balances.put(uuid, round2(balance - amount));
        this.save();
        return true;
    }

    public void deposit(UUID uuid, double amount) {
        this.balances.merge(uuid, round2(amount), Double::sum);
        this.save();
    }

    public String format(double amount) {
        return round2(amount) + " pièces";
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

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
