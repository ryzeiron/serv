package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.items.FuturesItem;
import com.antonin.marketeconomy.items.MerchantCompassItem;
import com.antonin.marketeconomy.model.FuturesContract;
import com.antonin.marketeconomy.model.MarketItem;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SpecialItemCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public SpecialItemCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
            this.sendUsage(player);
            return true;
        }

        String type = args[0].toLowerCase();
        if (type.equals("boussole") || type.equals("compass")) {
            player.getInventory().addItem(MerchantCompassItem.create(this.plugin));
            player.sendMessage("§6[Marché] §eTu reçois une Boussole du Marchand.");
            return true;
        }

        if (type.equals("graine") || type.equals("seed")) {
            this.giveSeed(player, args);
            return true;
        }

        this.sendUsage(player);
        return true;
    }

    private void giveSeed(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUtilisation: /marketitem graine <montant> <minutes>");
            return;
        }
        if (!this.plugin.getEconomyHook().isEnabled()) {
            player.sendMessage("§cLe systeme d'economie (Vault) n'est pas disponible.");
            return;
        }

        MarketManager manager = this.plugin.getMarketManager();
        MarketItem wheat = manager.getItem(Material.WHEAT);
        if (wheat == null) {
            player.sendMessage("§cLe blé n'est pas configuré comme item echangeable sur ce serveur.");
            return;
        }

        double stake;
        try {
            stake = Double.parseDouble(args[1]);
        } catch (NumberFormatException ignored) {
            player.sendMessage("§cMise invalide.");
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(args[2]);
        } catch (NumberFormatException ignored) {
            player.sendMessage("§cDuree invalide.");
            return;
        }

        String error = manager.validateContractRequest(stake, minutes);
        if (error != null) {
            player.sendMessage("§c" + error);
            return;
        }
        if (this.plugin.getEconomyHook().getBalance(player) < stake) {
            player.sendMessage("§cFonds insuffisants. Mise: " + this.plugin.getEconomyHook().format(stake));
            return;
        }

        this.plugin.getEconomyHook().withdraw(player, stake);
        FuturesContract contract = manager.openContract(player, wheat, FuturesContract.Type.LONG, stake, minutes);
        ItemStack seed = FuturesItem.createSeed(this.plugin, contract, wheat);
        player.getInventory().addItem(seed);

        player.sendMessage("§a[Marché] §eTu sèmes ta graine spéculative (mise " + this.plugin.getEconomyHook().format(stake)
                + ") — elle mûrira dans " + minutes + " min.");
    }

    private void sendUsage(Player player) {
        player.sendMessage("§cUtilisation: /marketitem <boussole|graine> [montant] [minutes]");
    }
}
