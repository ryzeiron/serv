package com.antonin.marketeconomy;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final ReputationManager reputationManager;

    public PlayerJoinListener(MarketEconomyPlugin plugin) {
        this.reputationManager = plugin.getReputationManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.reputationManager.refreshTitle(event.getPlayer());
    }
}
