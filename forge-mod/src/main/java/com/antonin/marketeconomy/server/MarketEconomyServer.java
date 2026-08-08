package com.antonin.marketeconomy.server;

import com.antonin.marketeconomy.economy.BankManager;
import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.hacker.HackerAbilityService;
import com.antonin.marketeconomy.job.JobManager;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.mine.MineManager;
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
    private final JobManager jobManager;
    private final HackerAbilityService hackerAbilityService;
    private final MineManager mineManager;

    private MarketEconomyServer(MinecraftServer server) {
        var dataDir = server.getWorldPath(LevelResource.ROOT).resolve("marketeconomy");
        this.economyManager = new EconomyManager(dataDir.resolve("economy.json"));
        this.bankManager = new BankManager(dataDir.resolve("banks.json"));
        this.marketManager = new MarketManager(this.economyManager);
        this.jobManager = new JobManager(dataDir.resolve("jobs.json"));
        this.hackerAbilityService = new HackerAbilityService(this.jobManager, this.marketManager, this.economyManager, this.bankManager);
        this.mineManager = new MineManager(dataDir.resolve("mines.json"));
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
        instance.jobManager.save();
        instance.mineManager.save();
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

    public JobManager getJobManager() {
        return this.jobManager;
    }

    public HackerAbilityService getHackerAbilityService() {
        return this.hackerAbilityService;
    }

    public MineManager getMineManager() {
        return this.mineManager;
    }
}
