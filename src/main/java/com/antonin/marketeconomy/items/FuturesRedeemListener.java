package com.antonin.marketeconomy.items;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.model.FuturesContract;
import com.antonin.marketeconomy.storage.EconomyHook;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class FuturesRedeemListener implements Listener {
    private final MarketEconomyPlugin plugin;
    private final MarketManager marketManager;
    private final EconomyHook economyHook;

    public FuturesRedeemListener(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.marketManager = plugin.getMarketManager();
        this.economyHook = plugin.getEconomyHook();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        UUID contractId = FuturesItem.readContractId(this.plugin, hand);
        if (contractId == null) {
            return;
        }
        event.setCancelled(true);

        FuturesContract contract = this.marketManager.getContract(contractId);
        if (contract == null) {
            player.sendMessage("§cCe contrat n'existe plus ou a déjà été encaissé.");
            return;
        }
        if (!contract.isSettled()) {
            long remaining = Math.max(0L, (contract.getMaturityAtMillis() - System.currentTimeMillis()) / 1000L);
            player.sendMessage("§cCe contrat n'est pas encore arrivé à échéance (" + remaining + "s restantes).");
            return;
        }

        double payout = this.marketManager.redeemContract(contractId);
        if (this.economyHook.isEnabled()) {
            this.economyHook.deposit(player, payout);
        }
        hand.setAmount(hand.getAmount() - 1);

        double profit = payout - contract.getStake();
        String profitColor = profit >= 0 ? "§a+" : "§c";
        String amount = this.economyHook.isEnabled() ? this.economyHook.format(payout) : String.format("%.2f", payout);
        player.sendMessage("§6[Contrat] §eEncaissé : " + amount + " (" + profitColor + String.format("%.2f", profit) + "§e)");
    }
}
