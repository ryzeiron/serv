package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PvpCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public PvpCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        Location pvp = this.plugin.getWarpManager().getPvp();
        if (pvp == null) {
            player.sendMessage("§cL'île PvP n'a pas encore été configurée. Un admin doit utiliser /setpvp.");
            return true;
        }
        player.teleport(pvp);
        player.sendMessage("§6[Marché] §eTéléporté sur l'île PvP.");
        return true;
    }
}
