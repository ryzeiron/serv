package com.antonin.marketeconomy;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final ReputationManager reputationManager;
    private final HudManager hudManager;

    public PlayerJoinListener(MarketEconomyPlugin plugin) {
        this.reputationManager = plugin.getReputationManager();
        this.hudManager = plugin.getHudManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.hudManager.handleJoin(event.getPlayer());
        this.reputationManager.refreshTitle(event.getPlayer());
        this.hudManager.refresh(event.getPlayer());
    }
}
