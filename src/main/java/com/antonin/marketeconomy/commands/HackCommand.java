package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.JobType;
import com.antonin.marketeconomy.model.MarketItem;
import com.antonin.marketeconomy.model.PlayerJob;
import com.antonin.marketeconomy.storage.EconomyHook;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HackCommand implements CommandExecutor {
    private final MarketEconomyPlugin plugin;
    private final Map<UUID, Map<String, Long>> cooldownUntil = new HashMap<>();
    private final java.util.Random random = new java.util.Random();

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
            player.sendMessage("§5[Hack] §cIl faut être Hacker pour utiliser cette commande (§f/metier hacker§c).");
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
            case "market":
                this.handleMarket(player, args);
                return true;
            case "price":
                this.handlePrice(player, job, args);
                return true;
            case "scramble":
                this.handleScramble(player, job);
                return true;
            case "wiretap":
                this.handleWiretap(player, job, args);
                return true;
            case "banque":
                this.handleBank(player, job, args);
                return true;
            case "terminal":
                this.handleTerminal(player, job);
                return true;
            default:
                this.sendHelp(player, job);
                return true;
        }
    }

    private void sendHelp(Player player, PlayerJob job) {
        player.sendMessage("§5[Hack] §dNiveau " + job.getLevel() + " — commandes:");
        player.sendMessage("§7/hack market <item> §f- rapport d'initié sur un item");
        player.sendMessage("§7/hack price <item> <up|down> §f- piraté le prix (risqué, détectable)");
        player.sendMessage("§7/hack scramble §f- brouille ta trace face à la détection de manipulation");
        player.sendMessage("§7/hack wiretap <joueur> §f- intercepte un % de ses ventes pendant un temps");
        player.sendMessage("§7/hack banque <joueur> §f- tente de pirater sa banque d'île (30% volés si réussi)");
        player.sendMessage("§7/hack terminal §f- mini-jeu de piratage pour du loot");
    }

    private double cfg(String key, double def) {
        return this.plugin.getConfig().getDouble("jobs.hacker." + key, def);
    }

    private long cfgSeconds(String key, long def) {
        return this.plugin.getConfig().getLong("jobs.hacker." + key, def) * 1000L;
    }

    private long remainingCooldownSeconds(Player player, String key) {
        Map<String, Long> map = this.cooldownUntil.get(player.getUniqueId());
        if (map == null) {
            return 0L;
        }
        Long until = map.get(key);
        if (until == null) {
            return 0L;
        }
        long remaining = (until - System.currentTimeMillis()) / 1000L;
        return Math.max(0L, remaining);
    }

    private void setCooldown(Player player, String key, long durationMillis) {
        this.cooldownUntil.computeIfAbsent(player.getUniqueId(), u -> new HashMap<>())
                .put(key, System.currentTimeMillis() + durationMillis);
    }

    private boolean checkCooldown(Player player, String key, long durationMillis) {
        long remaining = this.remainingCooldownSeconds(player, key);
        if (remaining > 0L) {
            player.sendMessage("§5[Hack] §cEncore en recharge (" + remaining + "s).");
            return false;
        }
        return true;
    }

    private MarketItem resolveItem(Player player, String arg) {
        Material material = Material.matchMaterial(arg);
        MarketItem item = material != null ? this.plugin.getMarketManager().getItem(material) : null;
        if (item == null) {
            player.sendMessage("§5[Hack] §cItem inconnu ou non échangeable sur le marché.");
        }
        return item;
    }

    // --- market ---

    private void handleMarket(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§5[Hack] §eUsage: /hack market <item>");
            return;
        }
        long cooldownMillis = this.cfgSeconds("insider-cooldown-seconds", 20L);
        if (!this.checkCooldown(player, "market", cooldownMillis)) {
            return;
        }
        MarketItem item = this.resolveItem(player, args[1]);
        if (item == null) {
            return;
        }
        this.setCooldown(player, "market", cooldownMillis);
        player.sendMessage(this.plugin.getMarketManager().getInsiderReport(item));
        this.plugin.getJobManager().addXp(player, JobType.HACKER, this.cfg("xp-insider", 5.0));
    }

    // --- price ---

    private void handlePrice(Player player, PlayerJob job, String[] args) {
        if (args.length < 3 || (!args[2].equalsIgnoreCase("up") && !args[2].equalsIgnoreCase("down"))) {
            player.sendMessage("§5[Hack] §eUsage: /hack price <item> <up|down>");
            return;
        }
        long cooldownMillis = this.cfgSeconds("price-hack-cooldown-seconds", 300L);
        if (!this.checkCooldown(player, "price", cooldownMillis)) {
            return;
        }
        MarketItem item = this.resolveItem(player, args[1]);
        if (item == null) {
            return;
        }
        double cost = this.cfg("price-hack-cost", 200.0);
        EconomyHook economy = this.plugin.getEconomyHook();
        if (economy.getBalance(player) < cost) {
            player.sendMessage("§5[Hack] §cIl te faut " + economy.format(cost) + " pour lancer cet exploit.");
            return;
        }
        economy.withdraw(player, cost);
        this.setCooldown(player, "price", cooldownMillis);

        boolean up = args[2].equalsIgnoreCase("up");
        long fakeVolume = Math.round(this.cfg("price-hack-fake-volume-base", 60.0)
                + this.cfg("price-hack-fake-volume-per-level", 10.0) * job.getLevel());
        this.plugin.getMarketManager().hackPrice(player, item, up, fakeVolume);
        player.sendMessage("§5[Hack] §dInjection de fausse activité sur " + item.getDisplayName()
                + " (" + (up ? "pump" : "dump") + "). Effet visible au prochain cycle de prix — risque d'être détecté.");
        this.plugin.getJobManager().addXp(player, JobType.HACKER, this.cfg("xp-price-hack", 15.0));
    }

    // --- scramble ---

    private void handleScramble(Player player, PlayerJob job) {
        long cooldownMillis = this.cfgSeconds("scramble-cooldown-seconds", 600L);
        if (!this.checkCooldown(player, "scramble", cooldownMillis)) {
            return;
        }
        double cost = this.cfg("scramble-cost", 150.0);
        EconomyHook economy = this.plugin.getEconomyHook();
        if (economy.getBalance(player) < cost) {
            player.sendMessage("§5[Hack] §cIl te faut " + economy.format(cost) + " pour brouiller ta trace.");
            return;
        }
        economy.withdraw(player, cost);
        this.setCooldown(player, "scramble", cooldownMillis);
        long durationMillis = this.cfgSeconds("scramble-duration-seconds", 300L);
        this.plugin.getMarketManager().scrambleTrace(player, durationMillis);
        player.sendMessage("§5[Hack] §dTrace brouillée pendant " + (durationMillis / 1000L) + "s : la détection de manipulation ne peut pas te repérer.");
        this.plugin.getJobManager().addXp(player, JobType.HACKER, this.cfg("xp-scramble", 10.0));
    }

    // --- wiretap ---

    private void handleWiretap(Player player, PlayerJob job, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§5[Hack] §eUsage: /hack wiretap <joueur>");
            return;
        }
        long cooldownMillis = this.cfgSeconds("wiretap-cooldown-seconds", 900L);
        if (!this.checkCooldown(player, "wiretap", cooldownMillis)) {
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§5[Hack] §cJoueur introuvable ou hors ligne.");
            return;
        }
        double cutShare = Math.min(this.cfg("wiretap-cut-max", 0.15),
                this.cfg("wiretap-cut-base", 0.05) + this.cfg("wiretap-cut-per-level", 0.01) * job.getLevel());
        double catchChance = Math.max(this.cfg("wiretap-catch-chance-min", 0.05),
                this.cfg("wiretap-catch-chance-base", 0.30) - this.cfg("wiretap-catch-chance-per-level", 0.02) * job.getLevel());
        long durationMillis = this.cfgSeconds("wiretap-duration-seconds", 180L);
        long targetCooldownMillis = this.cfgSeconds("wiretap-target-cooldown-seconds", 1800L);

        String error = this.plugin.getMarketManager().startWiretap(player, target, cutShare, catchChance, durationMillis, targetCooldownMillis);
        if (error != null) {
            player.sendMessage("§5[Hack] §c" + error);
            return;
        }

        double cost = this.cfg("wiretap-cost", 300.0);
        EconomyHook economy = this.plugin.getEconomyHook();
        if (economy.getBalance(player) >= cost) {
            economy.withdraw(player, cost);
        }
        this.setCooldown(player, "wiretap", cooldownMillis);
        player.sendMessage("§5[Hack] §dÉcoute posée sur " + target.getName() + " pendant " + (durationMillis / 1000L)
                + "s — tu toucheras " + Math.round(cutShare * 100) + "% de ses ventes.");
        this.plugin.getJobManager().addXp(player, JobType.HACKER, this.cfg("xp-wiretap-start", 20.0));
    }

    // --- banque ---

    @SuppressWarnings("deprecation")
    private void handleBank(Player player, PlayerJob job, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§5[Hack] §eUsage: /hack banque <joueur>");
            return;
        }
        long cooldownMillis = this.cfgSeconds("bank-hack-cooldown-seconds", 2700L);
        if (!this.checkCooldown(player, "banque", cooldownMillis)) {
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§5[Hack] §cTu ne peux pas te pirater toi-même.");
            return;
        }
        if (this.plugin.getBankManager().isRecentlyHacked(target.getUniqueId())) {
            player.sendMessage("§5[Hack] §cCette banque a été piratée récemment, elle est sous surveillance renforcée.");
            return;
        }

        double cost = this.cfg("bank-hack-cost", 500.0);
        EconomyHook economy = this.plugin.getEconomyHook();
        if (economy.getBalance(player) < cost) {
            player.sendMessage("§5[Hack] §cIl te faut " + economy.format(cost) + " pour monter cette intrusion.");
            return;
        }
        economy.withdraw(player, cost);
        this.setCooldown(player, "banque", cooldownMillis);

        double chance = Math.min(this.cfg("bank-hack-chance-max", 0.85),
                this.cfg("bank-hack-chance-base", 0.40) + this.cfg("bank-hack-chance-per-level", 0.05) * job.getLevel());
        if (this.random.nextDouble() >= chance) {
            player.sendMessage("§5[Hack] §cIntrusion échouée, le pare-feu a tenu bon.");
            return;
        }

        double drainShare = this.cfg("bank-hack-drain-share", 0.30);
        long targetCooldownMillis = this.cfgSeconds("bank-hack-target-cooldown-seconds", 10800L);
        double stolen = this.plugin.getBankManager().hack(target.getUniqueId(), drainShare, targetCooldownMillis);
        if (stolen <= 0.0) {
            player.sendMessage("§5[Hack] §eIntrusion réussie mais le compte était vide.");
            return;
        }
        economy.deposit(player, stolen);
        player.sendMessage("§5[Hack] §dBanque de " + (target.getName() != null ? target.getName() : "la cible")
                + " piratée ! Tu voles " + economy.format(stolen) + ".");
        Player targetOnline = target.getPlayer();
        if (targetOnline != null) {
            targetOnline.sendMessage("§4[!] §cTa banque d'île a été piratée ! Tu perds " + Math.round(drainShare * 100)
                    + "% de ton solde (" + economy.format(stolen) + ").");
        }
        this.plugin.getJobManager().addXp(player, JobType.HACKER, this.cfg("xp-bank-hack-success", 60.0));
    }

    // --- terminal ---

    private void handleTerminal(Player player, PlayerJob job) {
        long cooldownMillis = this.cfgSeconds("terminal-cooldown-seconds", 180L);
        if (!this.checkCooldown(player, "terminal", cooldownMillis)) {
            return;
        }
        this.setCooldown(player, "terminal", cooldownMillis);
        com.antonin.marketeconomy.gui.HackTerminalGUI.open(this.plugin, player, job);
    }
}
