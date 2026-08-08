package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.hacker.HackerAbilityService;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

// Terminal du bloc Ordinateur : menu graphique listant les capacites du metier Hacker. Les
// capacites sans argument (brouiller la trace) s'executent directement au clic ; celles qui ont
// besoin d'un argument (item, joueur) ferment le menu et suggerent la commande dans le chat.
public final class HackerTerminalGUI {
    private static final List<Entry> ENTRIES = List.of(
            new Entry(Items.SPYGLASS, "§dRapport d'initié", "Prix + pression du cycle en cours sur un item.", "/hack market "),
            new Entry(Items.TNT, "§dPousser un prix", "Injecte une fausse activité (pump/dump), risqué.", "/hack price "),
            new Entry(Items.ENDER_EYE, "§dBrouiller ma trace", "Immunise temporairement contre la détection.", null),
            new Entry(Items.OBSERVER, "§dÉcouter un joueur", "Intercepte un % de ses ventes.", "/hack wiretap "),
            new Entry(Items.REDSTONE, "§dPirater une banque", "Tente de vider une banque d'île.", "/hack banque ")
    );

    private HackerTerminalGUI() {
    }

    public static void open(ServerPlayer player) {
        HackerAbilityService service = MarketEconomyServer.get().getHackerAbilityService();
        if (!service.isHacker(player)) {
            player.sendSystemMessage(Component.literal("§5[Ordinateur] §cIl faut être Hacker pour utiliser ce terminal (§f/metier§c)."));
            return;
        }

        List<ItemStack> icons = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            ItemStack icon = new ItemStack(entry.icon);
            icon.set(DataComponents.CUSTOM_NAME, Component.literal(entry.label));
            icon.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("§7" + entry.description))));
            icons.add(icon);
        }

        DisplayMenu.open(player, Component.literal("§5§lTerminal Hacker"), icons, 1, (slot, button, clickType, clicker) -> {
            if (clickType != ClickType.PICKUP || slot >= ENTRIES.size()) {
                return;
            }
            Entry entry = ENTRIES.get(slot);
            clicker.closeContainer();
            if (entry.commandPrefix == null) {
                MarketEconomyServer.get().getHackerAbilityService().scramble(clicker);
                return;
            }
            clicker.sendSystemMessage(Component.literal("§d" + entry.label + " §7— clique pour compléter la commande :"));
            clicker.sendSystemMessage(Component.literal("§7» " + entry.commandPrefix).withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, entry.commandPrefix))
                    .withUnderlined(true)));
        });
    }

    private record Entry(net.minecraft.world.item.Item icon, String label, String description, String commandPrefix) {
    }
}
