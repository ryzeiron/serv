package com.antonin.marketeconomy.items;

import com.antonin.marketeconomy.market.MarketCategory;
import com.antonin.marketeconomy.market.MarketItem;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.antonin.marketeconomy.villager.VillagerTrade;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

// Met a jour periodiquement l'aiguille de chaque Boussole du Marchand tenue par un joueur, pour
// pointer vers le villageois le plus proche de la categorie actuellement la plus "en solde".
public final class MerchantCompassTracker {
    private static final double SEARCH_RADIUS = 256.0;

    private MerchantCompassTracker() {
    }

    public static void tick(MinecraftServer server) {
        MarketEconomyServer economy = MarketEconomyServer.get();
        if (economy == null) {
            return;
        }
        MarketCategory bestCategory = findBestDiscountCategory(economy.getMarketManager());
        if (bestCategory == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updateIfHolding(player, player.getMainHandItem(), bestCategory);
            updateIfHolding(player, player.getOffhandItem(), bestCategory);
        }
    }

    private static void updateIfHolding(ServerPlayer player, ItemStack stack, MarketCategory category) {
        if (!MerchantCompassItem.isMerchantCompass(stack)) {
            return;
        }
        Villager target = findNearestVillager(player, category);
        if (target == null) {
            return;
        }
        MerchantCompassItem.pointTo(stack, player.level().dimension(), target.blockPosition());
    }

    private static Villager findNearestVillager(ServerPlayer player, MarketCategory category) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        AABB area = player.getBoundingBox().inflate(SEARCH_RADIUS);
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, area,
                v -> VillagerTrade.professionCategory(v.getVillagerData().profession().value()) == category);

        Villager nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Villager villager : villagers) {
            double distance = villager.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = villager;
            }
        }
        return nearest;
    }

    // La categorie la plus "en solde" : ratio moyen prix actuel/prix de base le plus bas
    private static MarketCategory findBestDiscountCategory(MarketManager market) {
        Map<MarketCategory, double[]> sums = new EnumMap<>(MarketCategory.class);
        for (MarketItem item : market.getItems().values()) {
            double[] totals = sums.computeIfAbsent(item.getCategory(), c -> new double[2]);
            totals[0] += item.getCurrentPrice() / item.getBasePrice();
            totals[1] += 1.0;
        }
        MarketCategory best = null;
        double bestRatio = Double.MAX_VALUE;
        for (Map.Entry<MarketCategory, double[]> entry : sums.entrySet()) {
            double ratio = entry.getValue()[0] / entry.getValue()[1];
            if (ratio < bestRatio) {
                bestRatio = ratio;
                best = entry.getKey();
            }
        }
        return best;
    }
}
