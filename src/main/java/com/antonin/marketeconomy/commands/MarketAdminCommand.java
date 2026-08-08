package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.items.CustomMaterials;
import com.antonin.marketeconomy.items.HackerComputerItem;
import com.antonin.marketeconomy.model.JobType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class MarketAdminCommand implements CommandExecutor {
    private static final int BATCH_PER_TICK = 20000;
    private static final long MAX_VOLUME = 60_000_000L;

    private final MarketEconomyPlugin plugin;

    public MarketAdminCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6[Marché] §eUsage: /marketadmin <clearzone|givemoney|spawnmine|givexp|giveitem> ...");
            return true;
        }
        if (args[0].equalsIgnoreCase("givemoney")) {
            return this.handleGiveMoney(sender, args);
        }
        if (args[0].equalsIgnoreCase("spawnmine")) {
            return this.handleSpawnMine(sender, args);
        }
        if (args[0].equalsIgnoreCase("givexp")) {
            return this.handleGiveXp(sender, args);
        }
        if (args[0].equalsIgnoreCase("giveitem")) {
            return this.handleGiveItem(sender, args);
        }
        if (!args[0].equalsIgnoreCase("clearzone")) {
            sender.sendMessage("§6[Marché] §eUsage: /marketadmin clearzone <tailleX> <tailleY> <tailleZ> [confirm]");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 4) {
            player.sendMessage("§6[Marché] §eUsage: /marketadmin clearzone <tailleX> <tailleY> <tailleZ> [confirm]");
            return true;
        }

        int sizeX, sizeY, sizeZ;
        try {
            sizeX = Integer.parseInt(args[1]);
            sizeY = Integer.parseInt(args[2]);
            sizeZ = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage("§6[Marché] §cTailles invalides.");
            return true;
        }
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            player.sendMessage("§6[Marché] §cLes tailles doivent être positives.");
            return true;
        }

        long volume = (long) sizeX * sizeY * sizeZ;
        if (volume > MAX_VOLUME) {
            player.sendMessage("§6[Marché] §cZone trop grande (" + volume + " blocs, max " + MAX_VOLUME + ").");
            return true;
        }

        boolean confirmed = args.length >= 5 && args[4].equalsIgnoreCase("confirm");
        if (!confirmed) {
            player.sendMessage("§6[Marché] §eCeci va supprimer §c" + volume + " blocs§e (irréversible), en partant de ta"
                    + " position actuelle (coin) et en s'étendant vers +X/+Y/+Z — comme un bloc de structure.");
            player.sendMessage("§6[Marché] §eRelance avec §f/marketadmin clearzone " + sizeX + " " + sizeY + " " + sizeZ
                    + " confirm §epour confirmer.");
            return true;
        }

        Location corner = player.getLocation().getBlock().getLocation();
        World world = player.getWorld();
        int x0 = corner.getBlockX();
        int y0 = corner.getBlockY();
        int z0 = corner.getBlockZ();
        int worldMinY = world.getMinHeight();
        int worldMaxY = world.getMaxHeight();

        player.sendMessage("§6[Marché] §eSuppression de " + volume + " blocs en cours...");

        new BukkitRunnable() {
            int cx = 0;
            int cy = 0;
            int cz = 0;
            long cleared = 0;

            @Override
            public void run() {
                int processed = 0;
                while (processed < BATCH_PER_TICK) {
                    if (cx >= sizeX) {
                        player.sendMessage("§6[Marché] §aZone supprimée (" + cleared + " blocs effacés).");
                        cancel();
                        return;
                    }
                    int wx = x0 + cx;
                    int wy = y0 + cy;
                    int wz = z0 + cz;
                    if (wy >= worldMinY && wy < worldMaxY) {
                        Block block = world.getBlockAt(wx, wy, wz);
                        if (block.getType() != Material.AIR) {
                            block.setType(Material.AIR, false);
                            cleared++;
                        }
                    }
                    processed++;
                    cz++;
                    if (cz >= sizeZ) {
                        cz = 0;
                        cy++;
                        if (cy >= sizeY) {
                            cy = 0;
                            cx++;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleGiveMoney(CommandSender sender, String[] args) {
        if (!this.plugin.getEconomyHook().isEnabled()) {
            sender.sendMessage("§6[Marché] §cLe systeme d'economie (Vault) n'est pas disponible.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§6[Marché] §eUsage: /marketadmin givemoney <joueur> <montant>");
            return true;
        }
        String targetName = args[1];
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§6[Marché] §cMontant invalide.");
            return true;
        }
        if (amount <= 0.0) {
            sender.sendMessage("§6[Marché] §cLe montant doit être positif.");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        this.plugin.getEconomyHook().deposit(target, amount);
        sender.sendMessage("§6[Marché] §aDonné " + this.plugin.getEconomyHook().format(amount) + " à "
                + (target.getName() != null ? target.getName() : targetName) + ".");
        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage("§6[Marché] §eTu as reçu " + this.plugin.getEconomyHook().format(amount) + " d'un admin.");
        }
        return true;
    }

    private boolean handleSpawnMine(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage("§6[Marché] §eUsage: /marketadmin spawnmine <1-4>");
            return true;
        }
        int tier;
        try {
            tier = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§6[Marché] §cPalier invalide.");
            return true;
        }
        String error = this.plugin.getMineManager().spawnMine(tier, player.getLocation().getBlock().getLocation());
        if (error != null) {
            player.sendMessage("§6[Marché] §c" + error);
            return true;
        }
        player.sendMessage("§6[Marché] §aMine du palier " + tier + " posée (à partir de ta position, coin étendu vers +X/+Y/+Z).");
        return true;
    }

    private boolean handleGiveXp(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§6[Marché] §eUsage: /marketadmin givexp <joueur> <hacker|mineur> <montant|max>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§6[Marché] §cJoueur introuvable ou hors ligne.");
            return true;
        }
        JobType type = JobType.fromString(args[2]);
        if (type == null) {
            sender.sendMessage("§6[Marché] §cMétier invalide (hacker|mineur).");
            return true;
        }
        if (!this.plugin.getJobManager().hasJob(target.getUniqueId(), type)) {
            this.plugin.getJobManager().setJob(target.getUniqueId(), type);
        }
        if (args[3].equalsIgnoreCase("max")) {
            this.plugin.getJobManager().setLevel(target.getUniqueId(), type, type.getMaxLevel());
            sender.sendMessage("§6[Marché] §a" + target.getName() + " est maintenant " + type.getDisplayName()
                    + " §aniveau max (" + type.getMaxLevel() + ").");
            target.sendMessage("§6[Métier] §eTon niveau " + type.getDisplayName() + " §eest maintenant au maximum.");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§6[Marché] §cMontant invalide (nombre ou \"max\").");
            return true;
        }
        this.plugin.getJobManager().addXp(target, type, amount);
        sender.sendMessage("§6[Marché] §aDonné " + amount + " xp " + type.getDisplayName() + " §aà " + target.getName() + ".");
        return true;
    }

    private boolean handleGiveItem(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§6[Marché] §eUsage: /marketadmin giveitem <joueur> <lithium|plastique|ordinateur> [quantité]");
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§6[Marché] §cJoueur introuvable ou hors ligne.");
            return true;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException e) {
                sender.sendMessage("§6[Marché] §cQuantité invalide.");
                return true;
            }
        }
        ItemStack item;
        String kind = args[2].toLowerCase();
        switch (kind) {
            case "lithium":
                item = CustomMaterials.createLithiumIngot(this.plugin);
                break;
            case "plastique":
            case "plastic":
                item = CustomMaterials.createPlastic(this.plugin);
                break;
            case "ordinateur":
            case "computer":
                item = HackerComputerItem.create(this.plugin);
                break;
            default:
                sender.sendMessage("§6[Marché] §cItem inconnu (lithium|plastique|ordinateur).");
                return true;
        }
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage("§6[Marché] §aDonné " + amount + "x " + kind + " à " + target.getName() + ".");
        target.sendMessage("§6[Marché] §eTu as reçu " + amount + "x " + kind + " d'un admin.");
        return true;
    }
}
