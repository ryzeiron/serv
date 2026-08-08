package com.antonin.marketeconomy;

import com.antonin.marketeconomy.model.JobType;
import com.antonin.marketeconomy.model.PlayerJob;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class JobManager {
    private final MarketEconomyPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerJob> jobs = new HashMap<>();
    private final double xpPerLevelBase;

    public JobManager(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "jobs.yml");
        FileConfiguration config = plugin.getConfig();
        this.xpPerLevelBase = config.getDouble("jobs.xp-per-level-base", 100.0);
        this.load();
    }

    public PlayerJob getJob(UUID uuid) {
        return this.jobs.get(uuid);
    }

    public boolean hasJob(UUID uuid, JobType type) {
        PlayerJob job = this.jobs.get(uuid);
        return job != null && job.getType() == type;
    }

    public void setJob(UUID uuid, JobType type) {
        this.jobs.put(uuid, new PlayerJob(type));
        this.save();
    }

    // Ajoute de l'xp au metier "type" du joueur (ne fait rien s'il n'exerce pas ce metier) ;
    // annonce les montees de niveau
    public void addXp(Player player, JobType type, double amount) {
        PlayerJob job = this.jobs.get(player.getUniqueId());
        if (job == null || job.getType() != type) {
            return;
        }
        int gained = job.addXp(amount, this.xpPerLevelBase);
        if (gained > 0) {
            player.sendMessage("§5[Métier] §dNiveau supérieur ! " + type.getDisplayName() + " §dniveau " + job.getLevel() + ".");
        }
        this.save();
    }

    public void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                JobType type = JobType.fromString(section.getString(key + ".type"));
                if (type == null) {
                    continue;
                }
                int level = section.getInt(key + ".level", 1);
                double xp = section.getDouble(key + ".xp", 0.0);
                this.jobs.put(uuid, new PlayerJob(type, level, xp));
            } catch (IllegalArgumentException ignored) {
                this.plugin.getLogger().warning("Entree invalide dans jobs.yml: " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerJob> entry : this.jobs.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            PlayerJob job = entry.getValue();
            data.set(base + "type", job.getType().name());
            data.set(base + "level", job.getLevel());
            data.set(base + "xp", job.getXp());
        }
        try {
            data.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Impossible de sauvegarder jobs.yml: " + e.getMessage());
        }
    }
}
