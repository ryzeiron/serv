package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.JobType;
import com.antonin.marketeconomy.model.MarketItem;
import com.antonin.marketeconomy.model.PlayerJob;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HackCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;

    public HackCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        PlayerJob job = this.plugin.getJobManager().getJob(player.getUniqueId());
        if (job == null || job.getType() != JobType.HACKER) {
            player.sendMessage("§5[Hack] §cIl faut être Hacker pour utiliser cette commande (§f/metier§c).");
            return true;
        }
        if (!this.plugin.getEconomyHook().isEnabled()) {
            player.sendMessage("§cLe systeme d'economie (Vault) n'est pas disponible.");
            return true;
        }

        if (args.length == 0) {
            this.sendHelp(player, job);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "market": {
                if (args.length < 2) {
                    player.sendMessage("§5[Hack] §eUsage: /hack market <item>");
                    return true;
                }
                MarketItem item = this.resolveItem(player, args[1]);
                if (item != null) {
                    this.plugin.getHackerAbilityService().marketReport(player, item);
                }
                return true;
            }
            case "price": {
                if (args.length < 3 || (!args[2].equalsIgnoreCase("up") && !args[2].equalsIgnoreCase("down"))) {
                    player.sendMessage("§5[Hack] §eUsage: /hack price <item> <up|down>");
                    return true;
                }
                MarketItem item = this.resolveItem(player, args[1]);
                if (item != null) {
                    this.plugin.getHackerAbilityService().priceHack(player, item, args[2].equalsIgnoreCase("up"));
                }
                return true;
            }
            case "scramble":
                this.plugin.getHackerAbilityService().scramble(player);
                return true;
            case "wiretap": {
                if (args.length < 2) {
                    player.sendMessage("§5[Hack] §eUsage: /hack wiretap <joueur>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("§5[Hack] §cJoueur introuvable ou hors ligne.");
                    return true;
                }
                this.plugin.getHackerAbilityService().wiretap(player, target);
                return true;
            }
            case "banque": {
                if (args.length < 2) {
                    player.sendMessage("§5[Hack] §eUsage: /hack banque <joueur>");
                    return true;
                }
                @SuppressWarnings("deprecation")
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                this.plugin.getHackerAbilityService().bankHack(player, target);
                return true;
            }
            case "terminal":
                this.plugin.getHackerAbilityService().openTerminal(player);
                return true;
            default:
                this.sendHelp(player, job);
                return true;
        }
    }

    private void sendHelp(Player player, PlayerJob job) {
        player.sendMessage("§5[Hack] §dNiveau " + job.getLevel() + " — commandes (ou pose un §fOrdinateur§d et clique droit dessus) :");
        player.sendMessage("§7/hack market <item> §f- rapport d'initié sur un item");
        player.sendMessage("§7/hack price <item> <up|down> §f- piraté le prix (risqué, détectable)");
        player.sendMessage("§7/hack scramble §f- brouille ta trace face à la détection de manipulation");
        player.sendMessage("§7/hack wiretap <joueur> §f- intercepte un % de ses ventes pendant un temps");
        player.sendMessage("§7/hack banque <joueur> §f- tente de pirater sa banque d'île (30% volés si réussi)");
        player.sendMessage("§7/hack terminal §f- mini-jeu de piratage pour du loot");
    }

    private MarketItem resolveItem(Player player, String arg) {
        MarketItem item = this.plugin.getMarketManager().getItems().values().stream()
                .filter(i -> i.getDisplayName().equalsIgnoreCase(arg) || i.getMaterial().name().equalsIgnoreCase(arg))
                .findFirst()
                .orElse(null);
        if (item == null) {
            Material material = Material.matchMaterial(arg);
            item = material != null ? this.plugin.getMarketManager().getItem(material) : null;
        }
        if (item == null) {
            player.sendMessage("§5[Hack] §cItem inconnu ou non échangeable sur le marché. Tu peux utiliser son nom affiché (ex: \"Blé\") ou son id (ex: minecraft:wheat).");
        }
        return item;
    }
}
