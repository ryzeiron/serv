package com.antonin.marketeconomy.market;

import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

// Encaissement d'un contrat a terme physique (FuturesItem) : clic droit avec le contrat en
// main principale, apres son echeance.
public final class FuturesEvents {

    private FuturesEvents() {
    }

    @SubscribeEvent
    public static boolean onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        return handle(event);
    }

    @SubscribeEvent
    public static boolean onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        return handle(event);
    }

    private static boolean handle(PlayerInteractEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        ItemStack hand = event.getItemStack();
        UUID contractId = FuturesItem.readContractId(hand);
        if (contractId == null) {
            return false;
        }

        MarketManager market = MarketEconomyServer.get().getMarketManager();
        FuturesContract contract = market.getContract(contractId);
        if (contract == null) {
            player.sendSystemMessage(Component.literal("§cCe contrat n'existe plus ou a déjà été encaissé."));
            return true;
        }
        if (!contract.isSettled()) {
            long remaining = Math.max(0L, (contract.getMaturityAtMillis() - System.currentTimeMillis()) / 1000L);
            player.sendSystemMessage(Component.literal("§cCe contrat n'est pas encore arrivé à échéance (" + remaining + "s restantes)."));
            return true;
        }

        double payout = market.redeemContract(contractId);
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        economy.deposit(player.getUUID(), payout);
        hand.shrink(1);

        double profit = payout - contract.getStake();
        String profitColor = profit >= 0 ? "§a+" : "§c";
        player.sendSystemMessage(Component.literal("§6[Contrat] §eEncaissé : " + economy.format(payout)
                + " (" + profitColor + String.format("%.2f", profit) + "§e)"));
        return true;
    }
}
