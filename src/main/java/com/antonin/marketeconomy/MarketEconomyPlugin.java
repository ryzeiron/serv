package com.antonin.marketeconomy;

import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.commands.BuyCommand;
import com.antonin.marketeconomy.commands.FuturesCommand;
import com.antonin.marketeconomy.commands.JournalCommand;
import com.antonin.marketeconomy.commands.MarketCommand;
import com.antonin.marketeconomy.commands.SellCommand;
import com.antonin.marketeconomy.gui.MarketGUIListener;
import com.antonin.marketeconomy.storage.EconomyHook;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MarketEconomyPlugin
extends JavaPlugin {
    private MarketManager marketManager;
    private EconomyHook economyHook;

    public void onEnable() {
        this.saveDefaultConfig();
        this.economyHook = new EconomyHook();
        if (!this.economyHook.setup(this)) {
            this.getLogger().warning("Vault ou un plugin d'economie n'a pas ete trouve. Les achats/ventes seront desactives tant que Vault + un plugin d'economie ne sont pas installes.");
        }
        this.marketManager = new MarketManager(this, this.economyHook);
        this.getCommand("market").setExecutor((CommandExecutor)new MarketCommand(this));
        this.getCommand("buy").setExecutor((CommandExecutor)new BuyCommand(this));
        this.getCommand("sell").setExecutor((CommandExecutor)new SellCommand(this));
        this.getCommand("journal").setExecutor((CommandExecutor)new JournalCommand(this));
        this.getCommand("futures").setExecutor((CommandExecutor)new FuturesCommand(this));
        Bukkit.getPluginManager().registerEvents((Listener)new MarketGUIListener(this), (Plugin)this);
        long intervalTicks = this.getConfig().getLong("price-update-interval", 60L) * 20L;
        Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> this.marketManager.recalculateAll(), intervalTicks, intervalTicks);
        this.getLogger().info("MarketEconomy active avec " + this.marketManager.getItems().size() + " items echangeables.");
    }

    public void onDisable() {
        this.getLogger().info("MarketEconomy desactive.");
    }

    public MarketManager getMarketManager() {
        return this.marketManager;
    }

    public EconomyHook getEconomyHook() {
        return this.economyHook;
    }
}

