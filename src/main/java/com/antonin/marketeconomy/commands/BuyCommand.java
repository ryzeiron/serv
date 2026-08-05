package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.MarketItem;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BuyCommand
implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public BuyCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player)sender;
        if (!this.plugin.getEconomyHook().isEnabled()) {
            player.sendMessage("\u00a7cLe systeme d'economie (Vault) n'est pas disponible.");
            return true;
        }
        if (this.plugin.getMarketManager().isSuspended(player.getUniqueId())) {
            player.sendMessage("\u00a7cTon acces au marche est suspendu (" + this.plugin.getMarketManager().getSuspensionRemainingSeconds(player.getUniqueId()) + "s restantes).");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage("\u00a7cUtilisation: /buy <item> <quantite>");
            return true;
        }
        Material material = Material.matchMaterial((String)args[0]);
        if (material == null) {
            player.sendMessage("\u00a7cItem inconnu: " + args[0]);
            return true;
        }
        MarketItem item = this.plugin.getMarketManager().getItem(material);
        if (item == null) {
            player.sendMessage("\u00a7cCet item n'est pas echangeable sur le marche.");
            return true;
        }
        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Math.max(1, Integer.parseInt(args[1]));
            }
            catch (NumberFormatException ignored) {
                player.sendMessage("\u00a7cQuantite invalide, utilisation: /buy <item> <quantite>");
                return true;
            }
        }
        if (item.getStock() < (long)amount) {
            player.sendMessage("\u00a7cStock insuffisant. Disponible: " + item.getStock());
            return true;
        }
        double unitPrice = item.getBuyPrice();
        double total = (double)Math.round(unitPrice * (double)amount * 100.0) / 100.0;
        if (this.plugin.getEconomyHook().getBalance(player) < total) {
            player.sendMessage("\u00a7cFonds insuffisants. Prix total: " + this.plugin.getEconomyHook().format(total));
            return true;
        }
        this.plugin.getEconomyHook().withdraw(player, total);
        this.plugin.getMarketManager().recordPurchase(player, item, amount, total);
        this.plugin.getReputationManager().registerTrade(player, total);
        player.getInventory().addItem(new ItemStack[]{new ItemStack(material, amount)});
        player.sendMessage("\u00a7aAchete " + amount + "x " + item.getDisplayName() + " pour " + this.plugin.getEconomyHook().format(total));
        return true;
    }
}

