package com.antonin.marketeconomy;

import com.antonin.marketeconomy.model.PlayerJob;
import com.antonin.marketeconomy.storage.EconomyHook;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

// HUD personnel (barre laterale scoreboard) : solde, banque, metier, tendance du marche, ile.
// Chaque joueur avec le HUD actif recoit son propre Scoreboard (le contenu differe par joueur),
// donc les titres de prestige (ReputationManager) doivent etre recopies dessus pour rester visibles
public class HudManager {
    private static final String OBJECTIVE_NAME = "meco_hud";

    private final MarketEconomyPlugin plugin;
    private final File file;
    private final Set<UUID> disabled = new HashSet<>();

    public HudManager(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "hud.yml");
        this.load();
    }

    public boolean isEnabled(UUID uuid) {
        return !this.disabled.contains(uuid);
    }

    public void setEnabled(Player player, boolean enabled) {
        if (enabled) {
            this.disabled.remove(player.getUniqueId());
            this.assignPersonalScoreboard(player);
            this.refresh(player);
        } else {
            this.disabled.add(player.getUniqueId());
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        this.save();
    }

    // A appeler a la connexion : donne un scoreboard personnel si le joueur n'a pas desactive le HUD
    public void handleJoin(Player player) {
        if (this.isEnabled(player.getUniqueId())) {
            this.assignPersonalScoreboard(player);
        }
    }

    @SuppressWarnings("deprecation")
    private void assignPersonalScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team sourceTeam : main.getTeams()) {
            Team copy = board.registerNewTeam(sourceTeam.getName());
            copy.setPrefix(sourceTeam.getPrefix());
            for (String entry : sourceTeam.getEntries()) {
                copy.addEntry(entry);
            }
        }
        player.setScoreboard(board);
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.isEnabled(player.getUniqueId())) {
                this.refresh(player);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void refresh(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == null || board.equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            return;
        }
        Objective old = board.getObjective(OBJECTIVE_NAME);
        if (old != null) {
            old.unregister();
        }
        Objective objective = board.registerNewObjective(OBJECTIVE_NAME, "dummy", "§6§l⚡ MarketEconomy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = this.buildLines(player);
        int score = lines.size();
        for (String line : lines) {
            objective.getScore(line).setScore(score--);
        }
    }

    private List<String> buildLines(Player player) {
        List<String> lines = new ArrayList<>();
        EconomyHook economy = this.plugin.getEconomyHook();
        if (economy.isEnabled()) {
            lines.add("§eSolde: §f" + economy.format(economy.getBalance(player)));
            lines.add("§eBanque: §f" + economy.format(this.plugin.getBankManager().getBalance(player.getUniqueId())));
        }
        PlayerJob job = this.plugin.getJobManager().getJob(player.getUniqueId());
        if (job != null) {
            lines.add("§eMétier: §f" + job.getType().getDisplayName() + " §7(niv. " + job.getLevel() + ")");
        }
        lines.add("§eMarché: §f" + this.plugin.getMarketManager().getMarketTrendArrow() + " "
                + String.format("%.1f", this.plugin.getMarketManager().getMarketIndexChangePercent()) + "%");
        Location islandLoc = this.plugin.getIslandManager().getIslandLocation(player.getUniqueId());
        if (islandLoc != null) {
            lines.add("§eÎle: §f" + islandLoc.getBlockX() + ", " + islandLoc.getBlockZ());
        } else {
            lines.add("§7Île: §f/ile pour en créer une");
        }
        return lines;
    }

    private void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
        for (String key : data.getStringList("disabled")) {
            try {
                this.disabled.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                this.plugin.getLogger().warning("Entree invalide dans hud.yml: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        List<String> values = new ArrayList<>();
        for (UUID uuid : this.disabled) {
            values.add(uuid.toString());
        }
        data.set("disabled", values);
        try {
            data.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Impossible de sauvegarder hud.yml: " + e.getMessage());
        }
    }
}
