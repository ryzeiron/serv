package com.antonin.marketeconomy.villager;

import com.antonin.marketeconomy.gui.VillagerTradeGUI;
import com.antonin.marketeconomy.market.MarketCategory;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.reputation.ReputationManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

// PNJ marchands avec memoire : clic droit sur un villageois affiche un accueil selon la
// reputation du joueur, puis ouvre un menu graphique (VillagerTradeGUI) avec les items de sa
// categorie de metier. Un villageois tue penalise la reputation du tueur.
public final class VillagerEvents {

    private VillagerEvents() {
    }

    @SubscribeEvent
    public static boolean onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getTarget() instanceof Villager villager)) {
            return false;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        MarketEconomyServer server = MarketEconomyServer.get();
        if (server == null) {
            return false;
        }

        ReputationManager reputation = server.getReputationManager();
        player.sendSystemMessage(Component.literal(reputation.buildGreeting(player.getUUID())));
        if (reputation.isHostile(player.getUUID())) {
            return true;
        }

        MarketManager market = server.getMarketManager();
        MarketCategory category = VillagerTrade.professionCategory(villager.getVillagerData().profession().value());
        boolean hasItems = market.getItems().values().stream().anyMatch(item -> item.getCategory() == category);
        if (!hasItems) {
            player.sendSystemMessage(Component.literal("§7Ce villageois n'a rien à échanger pour l'instant."));
            return true;
        }

        VillagerTradeGUI.open(player, category, VillagerTrade.villagerLabel(villager.getVillagerData().profession().value()));
        return true;
    }

    @SubscribeEvent
    public static void onVillagerDeath(LivingDeathEvent event) {
        MarketEconomyServer server = MarketEconomyServer.get();
        if (server == null || !(event.getEntity() instanceof Villager)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        server.getReputationManager().penalizeVillagerKill(killer.getUUID());
        killer.sendSystemMessage(Component.literal("§cLes villageois se souviendront de ça..."));
    }
}
