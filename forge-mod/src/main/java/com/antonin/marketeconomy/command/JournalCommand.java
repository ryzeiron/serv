package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// /journal : titres generes automatiquement a chaque recalcul de prix. Affiche en chat plutot
// que dans un livre ecrit (le format d'ItemStack des livres a change en 1.20.5+ avec les
// composants de donnees, pour eviter ce risque on garde simple).
public final class JournalCommand {

    private JournalCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("journal").executes(JournalCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        List<String> headlines = MarketEconomyServer.get().getMarketManager().getHeadlines();
        player.sendSystemMessage(Component.literal("§0§l=== Le Journal du Marché ==="));
        if (headlines.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Aucune actualité notable pour l'instant."));
            player.sendSystemMessage(Component.literal("§7Reviens après le prochain recalcul des prix !"));
            return 1;
        }
        for (String headline : headlines) {
            player.sendSystemMessage(Component.literal("§7• §f" + headline));
        }
        return 1;
    }
}
