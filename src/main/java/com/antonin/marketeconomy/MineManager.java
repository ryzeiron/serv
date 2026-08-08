package com.antonin.marketeconomy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.Vector;

// Gere les mines du metier Mineur : placement (structures embarquees mine_tier1..4.nbt),
// regeneration periodique (replace la structure a son emplacement d'origine, ce qui restaure
// tout le minerai/les tunnels degrades par le minage) et controle du niveau requis par palier
public class MineManager {
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 4;

    public static class MineInstance {
        final int tier;
        final String world;
        final int x0;
        final int y0;
        final int z0;
        final int sizeX;
        final int sizeY;
        final int sizeZ;

        MineInstance(int tier, String world, int x0, int y0, int z0, int sizeX, int sizeY, int sizeZ) {
            this.tier = tier;
            this.world = world;
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }

        boolean contains(Location loc) {
            if (loc.getWorld() == null || !loc.getWorld().getName().equals(this.world)) {
                return false;
            }
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            return x >= this.x0 && x < this.x0 + this.sizeX
                    && y >= this.y0 && y < this.y0 + this.sizeY
                    && z >= this.z0 && z < this.z0 + this.sizeZ;
        }

        Location corner(World world) {
            return new Location(world, this.x0, this.y0, this.z0);
        }
    }

    private final MarketEconomyPlugin plugin;
    private final File file;
    private final List<MineInstance> mines = new ArrayList<>();
    private final Random random = new Random();

    public MineManager(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mines.yml");
        this.load();
    }

    private String resourceFor(int tier) {
        return "structures/mine_tier" + tier + ".nbt";
    }

    // Place la mine du palier donne au coin "origin" (comme un bloc de structure : le coin
    // s'etend vers +X/+Y/+Z) et enregistre son emprise ; renvoie un message d'erreur ou null
    public String spawnMine(int tier, Location origin) {
        if (tier < MIN_TIER || tier > MAX_TIER) {
            return "Palier invalide (1 à " + MAX_TIER + ").";
        }
        if (origin.getWorld() == null) {
            return "Monde introuvable.";
        }
        StructureManager structureManager = Bukkit.getStructureManager();
        try (InputStream in = this.plugin.getResource(this.resourceFor(tier))) {
            if (in == null) {
                return "Ressource introuvable pour ce palier.";
            }
            Structure structure = structureManager.loadStructure(in);
            structure.place(origin, false, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, this.random);
            Vector size = structure.getSize();
            MineInstance instance = new MineInstance(tier, origin.getWorld().getName(),
                    origin.getBlockX(), origin.getBlockY(), origin.getBlockZ(),
                    size.getBlockX(), size.getBlockY(), size.getBlockZ());
            this.mines.add(instance);
            this.save();
            return null;
        } catch (IOException e) {
            return "Erreur de chargement: " + e.getMessage();
        }
    }

    // Reapplique chaque mine enregistree a son emplacement d'origine : restaure tout le
    // minerai/les tunnels degrades par le minage depuis la derniere regeneration
    public void regenerateAll() {
        if (this.mines.isEmpty()) {
            return;
        }
        StructureManager structureManager = Bukkit.getStructureManager();
        for (MineInstance instance : this.mines) {
            World world = Bukkit.getWorld(instance.world);
            if (world == null) {
                continue;
            }
            try (InputStream in = this.plugin.getResource(this.resourceFor(instance.tier))) {
                if (in == null) {
                    continue;
                }
                Structure structure = structureManager.loadStructure(in);
                structure.place(instance.corner(world), false, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, this.random);
            } catch (IOException e) {
                this.plugin.getLogger().warning("Regeneration mine echouee: " + e.getMessage());
            }
        }
        Bukkit.broadcastMessage("§6[Mines] §eLes mines viennent de se régénérer.");
    }

    public Integer getTierAt(Location location) {
        for (MineInstance instance : this.mines) {
            if (instance.contains(location)) {
                return instance.tier;
            }
        }
        return null;
    }

    public static int minLevelFor(int tier) {
        switch (tier) {
            case 1:
                return 1;
            case 2:
                return 6;
            case 3:
                return 11;
            case 4:
                return 21;
            default:
                return 1;
        }
    }

    private void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection section = data.getConfigurationSection("mines");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String world = section.getString(key + ".world");
            if (world == null || Bukkit.getWorld(world) == null) {
                continue;
            }
            int tier = section.getInt(key + ".tier", 1);
            int x = section.getInt(key + ".x");
            int y = section.getInt(key + ".y");
            int z = section.getInt(key + ".z");
            int sx = section.getInt(key + ".size-x");
            int sy = section.getInt(key + ".size-y");
            int sz = section.getInt(key + ".size-z");
            this.mines.add(new MineInstance(tier, world, x, y, z, sx, sy, sz));
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        int i = 0;
        for (MineInstance instance : this.mines) {
            String base = "mines." + i + ".";
            data.set(base + "tier", instance.tier);
            data.set(base + "world", instance.world);
            data.set(base + "x", instance.x0);
            data.set(base + "y", instance.y0);
            data.set(base + "z", instance.z0);
            data.set(base + "size-x", instance.sizeX);
            data.set(base + "size-y", instance.sizeY);
            data.set(base + "size-z", instance.sizeZ);
            i++;
        }
        try {
            data.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Impossible de sauvegarder mines.yml: " + e.getMessage());
        }
    }
}
