package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BankCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public BankCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        if (!this.plugin.getEconomyHook().isEnabled()) {
            player.sendMessage("§cLe systeme d'economie (Vault) n'est pas disponible.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("solde")) {
            double balance = this.plugin.getBankManager().getBalance(player.getUniqueId());
            player.sendMessage("§6[Banque] §eSolde de ta banque d'île: §f"
                    + this.plugin.getEconomyHook().format(balance));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§6[Banque] §eUsage: /banque <solde|deposer|retirer> [montant]");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§6[Banque] §cMontant invalide.");
            return true;
        }
        if (amount <= 0.0) {
            player.sendMessage("§6[Banque] §cLe montant doit être positif.");
            return true;
        }

        if (args[0].equalsIgnoreCase("deposer")) {
            if (this.plugin.getEconomyHook().getBalance(player) < amount) {
                player.sendMessage("§6[Banque] §cFonds insuffisants dans ta poche.");
                return true;
            }
            this.plugin.getEconomyHook().withdraw(player, amount);
            this.plugin.getBankManager().deposit(player.getUniqueId(), amount);
            player.sendMessage("§6[Banque] §eDéposé " + this.plugin.getEconomyHook().format(amount) + " en banque.");
            return true;
        }

        if (args[0].equalsIgnoreCase("retirer")) {
            if (!this.plugin.getBankManager().withdraw(player.getUniqueId(), amount)) {
                player.sendMessage("§6[Banque] §cSolde bancaire insuffisant.");
                return true;
            }
            this.plugin.getEconomyHook().deposit(player, amount);
            player.sendMessage("§6[Banque] §eRetiré " + this.plugin.getEconomyHook().format(amount) + " de la banque.");
            return true;
        }

        player.sendMessage("§6[Banque] §eUsage: /banque <solde|deposer|retirer> [montant]");
        return true;
    }
}
