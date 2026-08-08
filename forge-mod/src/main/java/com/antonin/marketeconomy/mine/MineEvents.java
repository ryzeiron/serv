package com.antonin.marketeconomy.mine;

import com.antonin.marketeconomy.job.JobType;
import com.antonin.marketeconomy.job.PlayerJob;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

// Controle le minage dans les mines par palier (il faut etre Mineur et avoir le niveau minimum
// du palier pour casser un minerai) et le reseau de teleportation entre mines (plaques de
// pression detectees via le tick joueur, faute d'evenement Forge dedie au "pas sur une plaque").
public final class MineEvents {
    private static final double XP_PER_ORE_BASE = 8.0;

    private MineEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        MarketEconomyServer server = MarketEconomyServer.get();
        if (server == null) {
            return;
        }
        Integer tier = server.getMineManager().getTierAt(level, event.getPos());
        if (tier == null) {
            return;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
        if (id == null || !id.getPath().endsWith("_ore")) {
            return;
        }

        PlayerJob job = server.getJobManager().getJob(player.getUUID());
        int minLevel = MineManager.minLevelFor(tier);
        if (job == null || job.getType() != JobType.MINEUR || job.getLevel() < minLevel) {
            event.setResult(Result.DENY);
            player.sendSystemMessage(Component.literal("§6[Mine] §cIl faut être Mineur niveau " + minLevel
                    + "+ pour miner ici (§f/metier§c)."));
            return;
        }

        int gained = server.getJobManager().addXp(player.getUUID(), JobType.MINEUR, XP_PER_ORE_BASE * tier);
        if (gained > 0) {
            player.sendSystemMessage(Component.literal("§5[Métier] §dNiveau supérieur ! " + JobType.MINEUR.getDisplayName()
                    + " §dniveau " + job.getLevel() + "."));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        MarketEconomyServer server = MarketEconomyServer.get();
        if (server == null) {
            return;
        }
        BlockPos pos = player.blockPosition();
        Integer targetTier = server.getMineManager().getTeleportTarget(level, pos);
        if (targetTier == null) {
            return;
        }
        Vec3 destination = server.getMineManager().getEntryPosition(targetTier);
        if (destination == null) {
            player.sendSystemMessage(Component.literal("§6[Mine] §cCette mine n'a pas encore été créée."));
            return;
        }
        player.teleportTo(destination.x, destination.y, destination.z);
    }
}
