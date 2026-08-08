package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.job.JobManager;
import com.antonin.marketeconomy.job.JobType;
import com.antonin.marketeconomy.job.PlayerJob;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

// Mini-jeu "terminal de piratage" du Hacker : une grille 3x3 cache un noeud correct, 3 essais,
// indice de distance a chaque echec. Trouve le bon noeud -> recompense + xp ; les 3 essais
// rates -> juste un peu d'xp de consolation.
public final class HackTerminalMinigame {
    private static final int[] GRID_SLOTS = {3, 4, 5, 12, 13, 14, 21, 22, 23};
    private static final int ROWS = 3;
    private static final double TERMINAL_REWARD_BASE = 50.0;
    private static final double TERMINAL_REWARD_PER_LEVEL = 15.0;
    private static final double XP_TERMINAL_SUCCESS = 40.0;
    private static final double XP_TERMINAL_FAIL = 10.0;
    private static final Random RANDOM = new Random();

    private HackTerminalMinigame() {
    }

    public static void open(ServerPlayer player, PlayerJob job) {
        int correctIndex = RANDOM.nextInt(GRID_SLOTS.length);
        State state = new State(correctIndex, job.getLevel());

        List<ItemStack> icons = new ArrayList<>(ROWS * 9);
        for (int i = 0; i < ROWS * 9; i++) {
            icons.add(filler());
        }
        for (int slot : GRID_SLOTS) {
            icons.set(slot, nodeIcon());
        }

        DisplayMenu.open(player, Component.literal("§5Terminal de piratage"), icons, ROWS,
                (slot, button, clickType, clicker) -> onClick(state, slot, clickType, clicker));
        player.sendSystemMessage(Component.literal("§5[Hack] §dTerminal ouvert — trouve le bon noeud en 3 essais. "
                + "Un indice de distance s'affiche à chaque tentative."));
    }

    private static void onClick(State state, int slot, ClickType clickType, ServerPlayer player) {
        if (state.resolved || clickType != ClickType.PICKUP) {
            return;
        }
        int gridIndex = indexOf(slot);
        if (gridIndex < 0) {
            return;
        }

        if (gridIndex == state.correctIndex) {
            resolveSuccess(state, player);
            return;
        }

        int distance = manhattanDistance(gridIndex, state.correctIndex);
        state.attemptsLeft--;
        if (state.attemptsLeft <= 0) {
            resolveFailure(state, player);
        } else {
            player.sendSystemMessage(Component.literal("§5[Hack] §7" + hintFor(distance) + " §7— essais restants: §f" + state.attemptsLeft));
        }
    }

    private static void resolveSuccess(State state, ServerPlayer player) {
        state.resolved = true;
        double reward = TERMINAL_REWARD_BASE + TERMINAL_REWARD_PER_LEVEL * state.hackerLevel;
        EconomyManager economy = MarketEconomyServer.get().getEconomyManager();
        economy.deposit(player.getUUID(), reward);
        player.sendSystemMessage(Component.literal("§5[Hack] §aIntrusion réussie ! +" + economy.format(reward) + "."));
        addXp(player, XP_TERMINAL_SUCCESS);
        player.closeContainer();
    }

    private static void resolveFailure(State state, ServerPlayer player) {
        state.resolved = true;
        player.sendSystemMessage(Component.literal("§5[Hack] §cIntrusion échouée, le terminal se verrouille."));
        addXp(player, XP_TERMINAL_FAIL);
        player.closeContainer();
    }

    private static void addXp(ServerPlayer player, double amount) {
        JobManager jobManager = MarketEconomyServer.get().getJobManager();
        int gained = jobManager.addXp(player.getUUID(), JobType.HACKER, amount);
        if (gained > 0) {
            PlayerJob job = jobManager.getJob(player.getUUID());
            player.sendSystemMessage(Component.literal("§5[Métier] §dNiveau supérieur ! " + JobType.HACKER.getDisplayName()
                    + " §dniveau " + job.getLevel() + "."));
        }
    }

    private static ItemStack filler() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§7"));
        return stack;
    }

    private static ItemStack nodeIcon() {
        ItemStack stack = new ItemStack(Items.LIGHT_BLUE_STAINED_GLASS_PANE);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§bNoeud ?"));
        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7Clique pour tenter une intrusion"))));
        return stack;
    }

    private static int indexOf(int slot) {
        for (int i = 0; i < GRID_SLOTS.length; i++) {
            if (GRID_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private static int manhattanDistance(int a, int b) {
        int r1 = a / 3;
        int c1 = a % 3;
        int r2 = b / 3;
        int c2 = b % 3;
        return Math.abs(r1 - r2) + Math.abs(c1 - c2);
    }

    private static String hintFor(int distance) {
        return switch (distance) {
            case 1 -> "§6Brûlant";
            case 2 -> "§eChaud";
            case 3 -> "§bTiède";
            default -> "§9Froid";
        };
    }

    private static final class State {
        private final int correctIndex;
        private final int hackerLevel;
        private int attemptsLeft = 3;
        private boolean resolved = false;

        private State(int correctIndex, int hackerLevel) {
            this.correctIndex = correctIndex;
            this.hackerLevel = hackerLevel;
        }
    }
}
