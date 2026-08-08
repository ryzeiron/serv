package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.JobType;
import com.antonin.marketeconomy.model.PlayerJob;
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
            PlayerJob job = this.plugin.getJobManager().getJob(player.getUniqueId());
            if (job == null) {
                player.sendMessage("§6[Métier] §eTu n'as pas encore de métier. Disponible: §5hacker§e. Utilise §f/metier hacker§e pour le choisir.");
                return true;
            }
            double xpNeeded = job.xpToNextLevel(this.plugin.getConfig().getDouble("jobs.xp-per-level-base", 100.0));
            String progress = job.getLevel() >= PlayerJob.MAX_LEVEL
                    ? "niveau maximum"
                    : Math.round(job.getXp()) + "/" + Math.round(xpNeeded) + " xp";
            player.sendMessage("§6[Métier] §eTon métier: " + job.getType().getDisplayName()
                    + " §e— niveau " + job.getLevel() + " (" + progress + ")");
            return true;
        }

        JobType requested = JobType.fromString(args[0]);
        if (requested == null) {
            player.sendMessage("§6[Métier] §eMétiers disponibles: §5hacker");
            return true;
        }
        if (this.plugin.getJobManager().hasJob(player.getUniqueId(), requested)) {
            player.sendMessage("§6[Métier] §eTu exerces déjà ce métier.");
            return true;
        }
        this.plugin.getJobManager().setJob(player.getUniqueId(), requested);
        player.sendMessage("§6[Métier] §eTu es maintenant " + requested.getDisplayName() + "§e ! Tape §f/hack§e pour voir tes capacités.");
        return true;
    }
}
