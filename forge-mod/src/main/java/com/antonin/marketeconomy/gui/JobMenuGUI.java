package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.job.JobManager;
import com.antonin.marketeconomy.job.JobType;
import com.antonin.marketeconomy.job.PlayerJob;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

// Menu graphique de /metier : une icone par metier, avec sa progression en lore si le joueur
// l'exerce deja. Cliquer choisit ce metier (ou affiche juste la progression s'il est deja actif).
public final class JobMenuGUI {
    private static final JobType[] JOBS = {JobType.HACKER, JobType.MINEUR};

    private JobMenuGUI() {
    }

    public static void open(ServerPlayer player) {
        JobManager jobManager = MarketEconomyServer.get().getJobManager();
        PlayerJob current = jobManager.getJob(player.getUUID());

        List<ItemStack> icons = new ArrayList<>();
        for (JobType type : JOBS) {
            icons.add(buildIcon(type, current));
        }

        DisplayMenu.open(player, Component.literal("§6§lChoix de métier"), icons, 1, (slot, button, clickType, clicker) -> {
            if (clickType != ClickType.PICKUP || slot >= JOBS.length) {
                return;
            }
            onSelect(clicker, JOBS[slot]);
        });
    }

    private static ItemStack buildIcon(JobType type, PlayerJob current) {
        ItemStack icon = new ItemStack(type == JobType.HACKER ? Items.SPYGLASS : Items.DIAMOND_PICKAXE);
        icon.set(DataComponents.CUSTOM_NAME, Component.literal(type.getDisplayName()));
        List<Component> lore = new ArrayList<>();
        if (current != null && current.getType() == type) {
            lore.add(Component.literal("§7Niveau §f" + current.getLevel() + "§7/§f" + type.getMaxLevel()));
            if (current.getLevel() < type.getMaxLevel()) {
                lore.add(Component.literal(String.format(Locale.US, "§7Xp: §f%.0f§7/§f%.0f",
                        current.getXp(), current.xpToNextLevel(100.0))));
            } else {
                lore.add(Component.literal("§7Niveau maximum atteint."));
            }
        } else {
            lore.add(Component.literal("§eClic §7pour choisir ce métier."));
        }
        if (type == JobType.HACKER) {
            lore.add(Component.literal("§7Capacités: /hack market, price, scramble, wiretap, banque"));
        } else {
            lore.add(Component.literal("§7Capacités: mines à paliers, teleportation entre mines"));
        }
        icon.set(DataComponents.LORE, new ItemLore(lore));
        return icon;
    }

    private static void onSelect(ServerPlayer player, JobType type) {
        JobManager jobManager = MarketEconomyServer.get().getJobManager();
        if (jobManager.hasJob(player.getUUID(), type)) {
            player.sendSystemMessage(Component.literal("§6[Métier] §eTu exerces déjà ce métier."));
            return;
        }
        jobManager.setJob(player.getUUID(), type);
        player.sendSystemMessage(Component.literal("§6[Métier] §eTu es maintenant " + type.getDisplayName() + "§e !"));
        player.closeContainer();
    }
}
