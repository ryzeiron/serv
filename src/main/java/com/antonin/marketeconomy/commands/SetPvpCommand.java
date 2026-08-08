package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetPvpCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public SetPvpCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        this.plugin.getWarpManager().setPvp(player.getLocation());
        player.sendMessage("§6[Marché] §eÎle PvP enregistrée à ta position actuelle.");
        return true;
    }
}
