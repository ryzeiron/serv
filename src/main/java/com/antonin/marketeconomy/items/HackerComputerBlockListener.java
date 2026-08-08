package com.antonin.marketeconomy.items;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.gui.HackerComputerGUI;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public class HackerComputerBlockListener implements Listener {
    private final MarketEconomyPlugin plugin;

    public HackerComputerBlockListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    // Marque le bloc pose (via son bloc-entite) comme etant l'Ordinateur, independamment de l'item
    // d'origine — ainsi la detection au clic ne depend que de l'etat du bloc dans le monde
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!HackerComputerItem.isComputerItem(this.plugin, event.getItemInHand())) {
            return;
        }
        BlockState state = event.getBlock().getState();
        if (!(state instanceof TileState)) {
            return;
        }
        TileState tileState = (TileState) state;
        tileState.getPersistentDataContainer().set(HackerComputerItem.key(this.plugin), PersistentDataType.BYTE, (byte) 1);
        tileState.update();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != HackerComputerItem.BASE_MATERIAL) {
            return;
        }
        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof TileState)) {
            return;
        }
        TileState tileState = (TileState) state;
        Byte tag = tileState.getPersistentDataContainer().get(HackerComputerItem.key(this.plugin), PersistentDataType.BYTE);
        if (tag == null || tag != 1) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!this.plugin.getHackerAbilityService().isHacker(player)) {
            player.sendMessage("§5[Hack] §cIl faut être Hacker pour utiliser cet ordinateur (§f/metier§c).");
            return;
        }
        HackerComputerGUI.openMain(this.plugin, player);
    }
}
