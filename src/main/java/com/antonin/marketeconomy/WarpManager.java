package com.antonin.marketeconomy;

import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

// Points de teleportation fixes du serveur (spawn/hub, ile pvp), enregistrables
// en jeu via /sethub et /setpvp plutot que devinees a l'aveugle
public class WarpManager {
    private final MarketEconomyPlugin plugin;
    private final File file;
    private Location hub;
    private Location pvp;

    public WarpManager(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        this.load();
    }

    public Location getHub() {
        return this.hub;
    }

    public Location getPvp() {
        return this.pvp;
    }

    public void setHub(Location location) {
        this.hub = location.clone();
        this.save();
    }

    public void setPvp(Location location) {
        this.pvp = location.clone();
        this.save();
    }

    private void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
        this.hub = this.readLocation(data, "hub");
        this.pvp = this.readLocation(data, "pvp");
    }

    private Location readLocation(YamlConfiguration data, String key) {
        if (!data.isConfigurationSection(key)) {
            return null;
        }
        String worldName = data.getString(key + ".world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        if (world == null) {
            this.plugin.getLogger().warning("Monde introuvable pour le warp '" + key + "': " + worldName);
            return null;
        }
        double x = data.getDouble(key + ".x");
        double y = data.getDouble(key + ".y");
        double z = data.getDouble(key + ".z");
        float yaw = (float) data.getDouble(key + ".yaw", 0.0);
        float pitch = (float) data.getDouble(key + ".pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void writeLocation(YamlConfiguration data, String key, Location location) {
        data.set(key + ".world", location.getWorld().getName());
        data.set(key + ".x", location.getX());
        data.set(key + ".y", location.getY());
        data.set(key + ".z", location.getZ());
        data.set(key + ".yaw", location.getYaw());
        data.set(key + ".pitch", location.getPitch());
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        if (this.hub != null) {
            this.writeLocation(data, "hub", this.hub);
        }
        if (this.pvp != null) {
            this.writeLocation(data, "pvp", this.pvp);
        }
        try {
            data.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Impossible de sauvegarder warps.yml: " + e.getMessage());
        }
    }
}
