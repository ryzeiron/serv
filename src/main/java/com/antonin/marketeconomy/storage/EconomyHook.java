package com.antonin.marketeconomy.storage;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyHook {
    private Economy economy;

    public boolean setup(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = (Economy)rsp.getProvider();
        return true;
    }

    public boolean isEnabled() {
        return this.economy != null;
    }

    public double getBalance(Player player) {
        return this.economy.getBalance((OfflinePlayer)player);
    }

    public boolean withdraw(Player player, double amount) {
        return this.economy.withdrawPlayer((OfflinePlayer)player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        return this.economy.depositPlayer((OfflinePlayer)player, amount).transactionSuccess();
    }

    public String format(double amount) {
        return this.economy.format(amount);
    }
}

