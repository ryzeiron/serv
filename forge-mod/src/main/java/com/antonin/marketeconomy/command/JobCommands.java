package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.gui.JobMenuGUI;
import com.antonin.marketeconomy.job.JobManager;
import com.antonin.marketeconomy.job.JobType;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// /metier : menu graphique (icone par metier + progression en lore) et raccourcis en commande
// pour choisir directement un metier.
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
        JobMenuGUI.open(ctx.getSource().getPlayerOrException());
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
}
