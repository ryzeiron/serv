package com.antonin.marketeconomy.villager;

import com.antonin.marketeconomy.MarketEconomyMod;
import com.antonin.marketeconomy.market.MarketCategory;
import com.antonin.marketeconomy.market.MarketItem;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.reputation.ReputationManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

// PNJ marchands avec memoire : clic droit sur un villageois affiche un accueil selon la
// reputation du joueur, puis les items de sa categorie de metier en liens cliquables (achat/vente
// en un clic, faute de menu graphique — voir VillagerCommands). Un villageois tue penalise la
// reputation du tueur.
@Mod.EventBusSubscriber(modid = MarketEconomyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VillagerEvents {

    private VillagerEvents() {
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getTarget() instanceof Villager villager)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MarketEconomyServer server = MarketEconomyServer.get();
        if (server == null) {
            return;
        }
        event.setCanceled(true);

        ReputationManager reputation = server.getReputationManager();
        player.sendSystemMessage(Component.literal(reputation.buildGreeting(player.getUUID())));
        if (reputation.isHostile(player.getUUID())) {
            return;
        }

        MarketManager market = server.getMarketManager();
        MarketCategory category = VillagerTrade.professionCategory(villager.getVillagerData().getProfession().value());
        boolean hasItems = market.getItems().values().stream().anyMatch(item -> item.getCategory() == category);
        if (!hasItems) {
            player.sendSystemMessage(Component.literal("§7Ce villageois n'a rien à échanger pour l'instant."));
            return;
        }

        openTradeMenu(player, category, VillagerTrade.villagerLabel(villager.getVillagerData().getProfession().value()), market, reputation);
    }

    private static void openTradeMenu(ServerPlayer player, MarketCategory category, String title, MarketManager market, ReputationManager reputation) {
        double buyMultiplier = reputation.getBuyMultiplier(player.getUUID());
        double sellMultiplier = reputation.getSellMultiplier(player.getUUID());

        player.sendSystemMessage(Component.literal("§8§l=== " + title + " ==="));
        for (MarketItem item : market.getItems().values()) {
            if (item.getCategory() != category) {
                continue;
            }
            double buyPrice = round2(item.getBuyPrice() * buyMultiplier);
            double sellPrice = round2(item.getSellPrice() * sellMultiplier);
            player.sendSystemMessage(Component.literal(String.format(Locale.US, "§e%-20s §7Stock: §f%d  ", item.getDisplayName(), item.getStock()))
                    .append(buyLink(buyPrice, item))
                    .append(Component.literal("  "))
                    .append(sellLink(sellPrice, item)));
        }
        player.sendSystemMessage(Component.literal("").append(sellAllLink(category)));
    }

    private static Component buyLink(double price, MarketItem item) {
        String itemId = itemId(item);
        return Component.literal("§a[Acheter " + price + "]").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/villagerbuy " + itemId))
                .withUnderlined(true));
    }

    private static Component sellLink(double price, MarketItem item) {
        String itemId = itemId(item);
        return Component.literal("§c[Vendre " + price + "]").withStyle(style -> style
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/villagersell " + itemId))
                .withUnderlined(true));
    }

    private static Component sellAllLink(MarketCategory category) {
        return Component.literal("§6§l[Tout vendre à ce marchand]").withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/villagersellall " + category.name()))
                .withUnderlined(true));
    }

    private static String itemId(MarketItem item) {
        var id = ForgeRegistries.ITEMS.getKey(item.getItem());
        return id != null ? id.toString() : "";
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

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
