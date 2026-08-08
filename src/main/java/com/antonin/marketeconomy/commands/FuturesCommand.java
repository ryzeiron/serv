package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.items.FuturesItem;
import com.antonin.marketeconomy.model.FuturesContract;
import com.antonin.marketeconomy.model.MarketItem;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        FuturesContract contract = manager.openContract(player, item, type, stake, minutes);

        ItemStack contractItem = FuturesItem.createContract(this.plugin, contract, item);
        player.getInventory().addItem(contractItem);

        player.sendMessage("§6[Contrat] §eContrat " + (type == FuturesContract.Type.LONG ? "LONG" : "SHORT")
                + " ouvert sur " + item.getDisplayName() + " : mise " + this.plugin.getEconomyHook().format(stake)
                + ", prix de référence " + item.getCurrentPrice() + ", échéance dans " + minutes + " min. "
                + "Le contrat physique t'a été donné — échangeable avec d'autres joueurs avant échéance.");
        return true;
    }

    private void listContracts(Player player) {
        MarketManager manager = this.plugin.getMarketManager();
        int found = 0;
        player.sendMessage("§6=== Contrats dans ton inventaire ===");
        long now = System.currentTimeMillis();
        for (ItemStack stack : player.getInventory().getContents()) {
            UUID contractId = FuturesItem.readContractId(this.plugin, stack);
            if (contractId == null) {
                continue;
            }
            FuturesContract contract = manager.getContract(contractId);
            if (contract == null) {
                continue;
            }
            found++;
            String status;
            if (contract.isSettled()) {
                status = "§aprêt à encaisser (" + this.plugin.getEconomyHook().format(contract.getLockedPayout()) + ")";
            } else {
                long remaining = Math.max(0L, (contract.getMaturityAtMillis() - now) / 1000L);
                status = "§7échéance dans " + remaining + "s";
            }
            player.sendMessage("§7- §e" + (contract.getType() == FuturesContract.Type.LONG ? "LONG" : "SHORT")
                    + " §7" + contract.getMaterial().name() + " | mise " + this.plugin.getEconomyHook().format(contract.getStake())
                    + " | " + status);
        }
        if (found == 0) {
            player.sendMessage("§7Aucun contrat trouvé dans ton inventaire.");
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage("§cUtilisation: /futures <long|short> <item> <montant> <minutes>");
        player.sendMessage("§cOu: /futures list");
    }
}
