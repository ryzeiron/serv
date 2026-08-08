package com.antonin.marketeconomy.items;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

// Plaques de teleportation des mines : marcher sur l'une des 4 plaques d'une mine teleporte
// vers l'entree de la mine du palier correspondant (si elle a deja ete posee)
public class MineTeleportListener implements Listener {
    private final MarketEconomyPlugin plugin;

    public MineTeleportListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPhysical(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL || event.getClickedBlock() == null) {
            return;
        }
        Integer targetTier = this.plugin.getMineManager().getTeleportTarget(event.getClickedBlock().getLocation());
        if (targetTier == null) {
            return;
        }
        Player player = event.getPlayer();
        Location destination = this.plugin.getMineManager().getEntryLocation(targetTier);
        if (destination == null) {
            player.sendMessage("§6[Mine] §cLa mine n°" + targetTier + " n'a pas encore été créée.");
            return;
        }
        player.teleport(destination);
        player.sendMessage("§6[Mine] §eTéléporté vers la mine n°" + targetTier + ".");
    }
}
