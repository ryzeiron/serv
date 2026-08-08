package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IslandCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public IslandCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        boolean hadIsland = this.plugin.getIslandManager().hasIsland(player.getUniqueId());
        Location island = this.plugin.getIslandManager().getOrCreateIsland(player);
        player.teleport(island);
        if (hadIsland) {
            player.sendMessage("§6[Marché] §eTéléporté sur ton île.");
        } else {
            player.sendMessage("§6[Marché] §eTon île vient d'être créée, bienvenue !");
        }
        return true;
    }
}
