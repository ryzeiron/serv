package com.antonin.marketeconomy.items;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import java.util.Random;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

// Dechet de plastique tres rare remonte a la peche (remplace la prise normale)
public class PlasticFishingListener implements Listener {
    private final MarketEconomyPlugin plugin;
    private final Random random = new Random();

    public PlasticFishingListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (!(event.getCaught() instanceof Item)) {
            return;
        }
        double chance = this.plugin.getConfig().getDouble("custom-items.plastic-chance", 0.002);
        if (this.random.nextDouble() >= chance) {
            return;
        }
        Item caughtItem = (Item) event.getCaught();
        caughtItem.setItemStack(CustomMaterials.createPlastic(this.plugin));
        Player player = event.getPlayer();
        player.sendMessage("§aTu remontes un bout de plastique flottant au milieu des algues...");
    }
}
