package com.antonin.marketeconomy.server;

import com.antonin.marketeconomy.economy.BankManager;
import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.market.MarketManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

// Contexte central du mod pour une partie de serveur en cours : equivalent de l'ancienne classe
// MarketEconomyPlugin du plugin Paper, mais sous forme de contexte recree a chaque demarrage
// de serveur (pas de "plugin" persistant en modding Forge).
public class MarketEconomyServer {
    private static MarketEconomyServer instance;

    private final EconomyManager economyManager;
    private final BankManager bankManager;
    private final MarketManager marketManager;

    private MarketEconomyServer(MinecraftServer server) {
        var dataDir = server.getWorldPath(LevelResource.ROOT).resolve("marketeconomy");
        this.economyManager = new EconomyManager(dataDir.resolve("economy.json"));
        this.bankManager = new BankManager(dataDir.resolve("banks.json"));
        this.marketManager = new MarketManager();
    }

    public static void start(MinecraftServer server) {
        instance = new MarketEconomyServer(server);
    }

    public static void stop() {
        if (instance == null) {
            return;
        }
        instance.economyManager.save();
        instance.bankManager.save();
        instance = null;
    }

    public static MarketEconomyServer get() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return this.economyManager;
    }

    public BankManager getBankManager() {
        return this.bankManager;
    }

    public MarketManager getMarketManager() {
        return this.marketManager;
    }
}
