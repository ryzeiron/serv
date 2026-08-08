package com.antonin.marketeconomy.items;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import java.util.Random;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

// Trace de lithium trouvee en minant du minerai de diamant (comme la netherite : rare, tres
// contextuel, pas de bloc dedie)
public class LithiumMiningListener implements Listener {
    private final MarketEconomyPlugin plugin;
    private final Random random = new Random();

    public LithiumMiningListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        if (type != Material.DIAMOND_ORE && type != Material.DEEPSLATE_DIAMOND_ORE) {
            return;
        }
        double chance = this.plugin.getConfig().getDouble("custom-items.lithium-chance", 0.015);
        if (this.random.nextDouble() >= chance) {
            return;
        }
        Player player = event.getPlayer();
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
                CustomMaterials.createLithiumIngot(this.plugin));
        player.sendMessage("§dTu remarques un éclat métallique argenté dans le minerai... §fLingot de Lithium obtenu !");
    }
}
