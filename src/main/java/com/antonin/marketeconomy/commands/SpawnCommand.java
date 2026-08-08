package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public SpawnCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        Location hub = this.plugin.getWarpManager().getHub();
        if (hub == null) {
            player.sendMessage("§cLe spawn n'a pas encore été configuré. Un admin doit utiliser /sethub.");
            return true;
        }
        player.teleport(hub);
        player.sendMessage("§6[Marché] §eTéléporté au spawn.");
        return true;
    }
}
