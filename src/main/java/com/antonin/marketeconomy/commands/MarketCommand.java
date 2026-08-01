package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.gui.MarketGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MarketCommand
implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public MarketCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("marketeconomy.use")) {
            player.sendMessage("\u00a7cTu n'as pas la permission d'utiliser le marche.");
            return true;
        }
        MarketGUI.openMainMenu(player, this.plugin.getMarketManager(), this.plugin.getEconomyHook());
        return true;
    }
}

