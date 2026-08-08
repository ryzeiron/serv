package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HudCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public HudCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        boolean currentlyEnabled = this.plugin.getHudManager().isEnabled(player.getUniqueId());
        boolean newState;
        if (args.length == 0) {
            newState = !currentlyEnabled;
        } else if (args[0].equalsIgnoreCase("on")) {
            newState = true;
        } else if (args[0].equalsIgnoreCase("off")) {
            newState = false;
        } else {
            player.sendMessage("§6[HUD] §eUsage: /hud [on|off]");
            return true;
        }
        this.plugin.getHudManager().setEnabled(player, newState);
        player.sendMessage(newState ? "§6[HUD] §eAffichage activé." : "§6[HUD] §eAffichage désactivé.");
        return true;
    }
}
