package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.job.JobManager;
import com.antonin.marketeconomy.job.JobType;
import com.antonin.marketeconomy.job.PlayerJob;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// /metier : choix de metier et affichage de la progression (en attendant un vrai menu GUI,
// on utilise des liens cliquables dans le chat).
public final class JobCommands {

    private JobCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("metier")
                .executes(JobCommands::runShow)
                .then(Commands.literal("hacker").executes(ctx -> runChoose(ctx, JobType.HACKER)))
                .then(Commands.literal("mineur").executes(ctx -> runChoose(ctx, JobType.MINEUR))));
    }

    private static int runShow(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        JobManager jobManager = MarketEconomyServer.get().getJobManager();
        PlayerJob job = jobManager.getJob(player.getUUID());

        if (job == null) {
            player.sendSystemMessage(Component.literal("§6[Métier] §eTu n'exerces aucun métier. Choisis-en un :"));
            player.sendSystemMessage(clickableChoice("§5[Devenir Hacker]", "/metier hacker"));
            player.sendSystemMessage(clickableChoice("§6[Devenir Mineur]", "/metier mineur"));
            return 1;
        }

        int level = job.getLevel();
        int maxLevel = job.getType().getMaxLevel();
        player.sendSystemMessage(Component.literal("§6[Métier] " + job.getType().getDisplayName()
                + " §6— niveau " + level + "/" + maxLevel));
        if (level < maxLevel) {
            double needed = job.xpToNextLevel(100.0);
            player.sendSystemMessage(Component.literal(String.format(java.util.Locale.US,
                    "§7Progression: §f%.0f§7/§f%.0f §7xp", job.getXp(), needed)));
        } else {
            player.sendSystemMessage(Component.literal("§7Niveau maximum atteint."));
        }
        if (job.getType() == JobType.HACKER) {
            player.sendSystemMessage(Component.literal("§7Capacités: §f/hack market, price, scramble, wiretap, banque"));
        } else {
            player.sendSystemMessage(Component.literal("§7Capacités: mine, extraction de minerai (§c pas encore porté§7)"));
        }
        return 1;
    }

    private static int runChoose(CommandContext<CommandSourceStack> ctx, JobType requested) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        JobManager jobManager = MarketEconomyServer.get().getJobManager();
        if (jobManager.hasJob(player.getUUID(), requested)) {
            player.sendSystemMessage(Component.literal("§6[Métier] §eTu exerces déjà ce métier."));
            return 0;
        }
        jobManager.setJob(player.getUUID(), requested);
        player.sendSystemMessage(Component.literal("§6[Métier] §eTu es maintenant " + requested.getDisplayName() + "§e !"));
        return 1;
    }

    private static Component clickableChoice(String label, String command) {
        return Component.literal(label).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withUnderlined(true));
    }
}
