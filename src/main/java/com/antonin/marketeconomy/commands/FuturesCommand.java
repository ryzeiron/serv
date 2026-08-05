package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.model.FuturesContract;
import com.antonin.marketeconomy.model.MarketItem;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FuturesCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public FuturesCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            this.listContracts(player);
            return true;
        }

        if (args.length < 4 || !(args[0].equalsIgnoreCase("long") || args[0].equalsIgnoreCase("short"))) {
            this.sendUsage(player);
            return true;
        }

        if (!this.plugin.getEconomyHook().isEnabled()) {
            player.sendMessage("§cLe systeme d'economie (Vault) n'est pas disponible.");
            return true;
        }
        if (this.plugin.getMarketManager().isSuspended(player.getUniqueId())) {
            player.sendMessage("§cTon acces au marche est suspendu (" + this.plugin.getMarketManager().getSuspensionRemainingSeconds(player.getUniqueId()) + "s restantes).");
            return true;
        }

        Material material = Material.matchMaterial(args[1]);
        if (material == null) {
            player.sendMessage("§cItem inconnu: " + args[1]);
            return true;
        }
        MarketItem item = this.plugin.getMarketManager().getItem(material);
        if (item == null) {
            player.sendMessage("§cCet item n'est pas echangeable sur le marche.");
            return true;
        }

        double stake;
        try {
            stake = Double.parseDouble(args[2]);
        } catch (NumberFormatException ignored) {
            player.sendMessage("§cMise invalide.");
            return true;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(args[3]);
        } catch (NumberFormatException ignored) {
            player.sendMessage("§cDuree invalide.");
            return true;
        }

        MarketManager manager = this.plugin.getMarketManager();
        String error = manager.validateContractRequest(stake, minutes);
        if (error != null) {
            player.sendMessage("§c" + error);
            return true;
        }

        if (this.plugin.getEconomyHook().getBalance(player) < stake) {
            player.sendMessage("§cFonds insuffisants. Mise: " + this.plugin.getEconomyHook().format(stake));
            return true;
        }

        this.plugin.getEconomyHook().withdraw(player, stake);
        FuturesContract.Type type = args[0].equalsIgnoreCase("long") ? FuturesContract.Type.LONG : FuturesContract.Type.SHORT;
        manager.openContract(player, item, type, stake, minutes);

        player.sendMessage("§6[Contrat] §eContrat " + (type == FuturesContract.Type.LONG ? "LONG" : "SHORT")
                + " ouvert sur " + item.getDisplayName() + " : mise " + this.plugin.getEconomyHook().format(stake)
                + ", prix de référence " + item.getCurrentPrice() + ", échéance dans " + minutes + " min.");
        return true;
    }

    private void listContracts(Player player) {
        MarketManager manager = this.plugin.getMarketManager();
        var contracts = manager.getContracts(player.getUniqueId());
        if (contracts.isEmpty()) {
            player.sendMessage("§7Tu n'as aucun contrat à terme actif.");
            return;
        }
        player.sendMessage("§6=== Tes contrats à terme ===");
        long now = System.currentTimeMillis();
        for (FuturesContract contract : contracts) {
            long remaining = Math.max(0L, (contract.getMaturityAtMillis() - now) / 1000L);
            player.sendMessage("§7- §e" + (contract.getType() == FuturesContract.Type.LONG ? "LONG" : "SHORT")
                    + " §7" + contract.getMaterial().name()
                    + " | mise " + this.plugin.getEconomyHook().format(contract.getStake())
                    + " | échéance dans " + remaining + "s");
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage("§cUtilisation: /futures <long|short> <item> <montant> <minutes>");
        player.sendMessage("§cOu: /futures list");
    }
}
