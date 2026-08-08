package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.gui.JobMenuGUI;
import com.antonin.marketeconomy.model.JobType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public JobCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            JobMenuGUI.openMain(this.plugin, player);
            return true;
        }

        JobType requested = JobType.fromString(args[0]);
        if (requested == null) {
            player.sendMessage("§6[Métier] §eMétiers disponibles: §5hacker§e, §6mineur");
            return true;
        }
        if (this.plugin.getJobManager().hasJob(player.getUniqueId(), requested)) {
            player.sendMessage("§6[Métier] §eTu exerces déjà ce métier.");
            return true;
        }
        this.plugin.getJobManager().setJob(player.getUniqueId(), requested);
        player.sendMessage("§6[Métier] §eTu es maintenant " + requested.getDisplayName() + "§e !");
        return true;
    }
}
