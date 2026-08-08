package com.antonin.marketeconomy.hacker;

import com.antonin.marketeconomy.server.MarketEconomyServer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// Terminal du bloc Ordinateur : ouvert au clic droit, affiche les capacites du metier Hacker
// sous forme de liens cliquables (qui pre-remplissent la commande dans le chat, faute de menu
// graphique pour l'instant -- viendra avec le portage du systeme de GUI).
public final class HackerTerminal {

    private HackerTerminal() {
    }

    public static void open(ServerPlayer player) {
        HackerAbilityService service = MarketEconomyServer.get().getHackerAbilityService();
        if (!service.isHacker(player)) {
            player.sendSystemMessage(Component.literal("§5[Ordinateur] §cIl faut être Hacker pour utiliser ce terminal (§f/metier§c)."));
            return;
        }
        player.sendSystemMessage(Component.literal("§5§l=== Terminal Hacker ==="));
        player.sendSystemMessage(link("§d> Rapport d'initié sur un item", "/hack market "));
        player.sendSystemMessage(link("§d> Pousser un prix (pump/dump)", "/hack price "));
        player.sendSystemMessage(link("§d> Brouiller ma trace", "/hack scramble"));
        player.sendSystemMessage(link("§d> Écouter les ventes d'un joueur", "/hack wiretap "));
        player.sendSystemMessage(link("§d> Pirater une banque d'île", "/hack banque "));
        player.sendSystemMessage(Component.literal("§7(clique une ligne pour préremplir la commande dans ton chat)"));
    }

    private static Component link(String label, String commandPrefix) {
        return Component.literal(label).withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandPrefix))
                .withUnderlined(true));
    }
}
