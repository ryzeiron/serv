package com.antonin.marketeconomy.items;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.MineManager;
import com.antonin.marketeconomy.model.JobType;
import com.antonin.marketeconomy.model.PlayerJob;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

// Controle le minage dans les mines par palier : il faut etre Mineur et avoir le niveau minimum
// du palier pour casser un bloc de minerai ; le "minerai de lithium" (calcite, reskinne par le
// resource pack) donne un Lingot de Lithium garanti au lieu de son drop vanilla
public class MineBlockListener implements Listener {
    private final MarketEconomyPlugin plugin;

    public MineBlockListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        Integer tier = this.plugin.getMineManager().getTierAt(location);
        if (tier == null) {
            return;
        }

        Material type = event.getBlock().getType();
        boolean isLithiumMarker = type == Material.CALCITE;
        boolean isOre = type.name().endsWith("_ORE");
        if (!isOre && !isLithiumMarker) {
            return;
        }

        Player player = event.getPlayer();
        PlayerJob job = this.plugin.getJobManager().getJob(player.getUniqueId());
        int minLevel = MineManager.minLevelFor(tier);
        if (job == null || job.getType() != JobType.MINEUR || job.getLevel() < minLevel) {
            event.setCancelled(true);
            player.sendMessage("§6[Mine] §cIl faut être Mineur niveau " + minLevel + "+ pour miner ici (§f/metier§c).");
            return;
        }

        if (isLithiumMarker) {
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            event.getBlock().getWorld().dropItemNaturally(location, CustomMaterials.createLithiumIngot(this.plugin));
            player.sendMessage("§dTu extrais un Lingot de Lithium du filon !");
        }

        double xpPerOre = this.plugin.getConfig().getDouble("jobs.mineur.xp-per-ore-base", 8.0);
        this.plugin.getJobManager().addXp(player, JobType.MINEUR, xpPerOre * tier);
    }
}
