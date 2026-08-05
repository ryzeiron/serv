package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.MarketItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SellCommand
implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public SellCommand(MarketEconomyPlugin plugin) {
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
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            player.sendMessage("\u00a7cTiens l'item que tu veux vendre en main.");
            return true;
        }
        MarketItem item = this.plugin.getMarketManager().getItem(hand.getType());
        if (item == null) {
            player.sendMessage("\u00a7cCet item n'est pas echangeable sur le marche.");
            return true;
        }
        int amount = 1;
        if (args.length >= 1) {
            try {
                amount = Math.max(1, Integer.parseInt(args[0]));
            }
            catch (NumberFormatException ignored) {
                player.sendMessage("\u00a7cQuantite invalide, utilisation: /sell <quantite>");
                return true;
            }
        }
        amount = Math.min(amount, hand.getAmount());
        double unitPrice = item.getSellPrice();
        double total = (double)Math.round(unitPrice * (double)amount * 100.0) / 100.0;
        hand.setAmount(hand.getAmount() - amount);
        this.plugin.getMarketManager().recordSale(player, item, amount, total);
        this.plugin.getEconomyHook().deposit(player, total);
        player.sendMessage("\u00a7aVendu " + amount + "x " + item.getDisplayName() + " pour " + this.plugin.getEconomyHook().format(total));
        return true;
    }
}

