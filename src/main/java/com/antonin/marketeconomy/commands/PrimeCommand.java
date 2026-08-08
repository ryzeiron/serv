package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrimeCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public PrimeCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage("§cUtilisation: /prime <joueur>");
            player.sendMessage("§7Ne fonctionne que sur un joueur actuellement repéré pour manipulation de marché.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§cJoueur introuvable ou hors ligne: " + args[0]);
            return true;
        }

        String error = this.plugin.getMarketManager().placeBounty(player, target);
        if (error != null) {
            player.sendMessage("§c" + error);
        }
        return true;
    }
}
