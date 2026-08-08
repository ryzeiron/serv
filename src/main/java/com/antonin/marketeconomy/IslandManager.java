package com.antonin.marketeconomy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

// Attribue une ile de depart a chaque joueur (posee depuis la ressource embarquee
// structures/starter_island.nbt), sur une grille espacee pour ne jamais se chevaucher
public class IslandManager {
    private static final int SPACING = 400;
    private static final int ISLANDS_PER_ROW = 20;
    private static final int ISLAND_Y = 100;

    private final MarketEconomyPlugin plugin;
    private final File file;
    private final Map<UUID, Location> islands = new HashMap<>();
    private int nextIndex = 0;

    public IslandManager(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "islands.yml");
        this.load();
    }

    public boolean hasIsland(UUID uuid) {
        return this.islands.containsKey(uuid);
    }

    public Location getOrCreateIsland(Player player) {
        Location existing = this.islands.get(player.getUniqueId());
        if (existing != null) {
            return existing;
        }
        World world = Bukkit.getWorlds().get(0);
        int index = this.nextIndex++;
        int gridX = index % ISLANDS_PER_ROW;
        int gridZ = index / ISLANDS_PER_ROW;
        Location origin = new Location(world, gridX * (double) SPACING, ISLAND_Y, gridZ * (double) SPACING);

        this.pasteStarterIsland(origin);

        Location spawnPoint = origin.clone().add(4.5, 3, 4.5);
        this.islands.put(player.getUniqueId(), spawnPoint);
        this.save();
        return spawnPoint;
    }

    private void pasteStarterIsland(Location origin) {
        StructureManager structureManager = Bukkit.getStructureManager();
        try (InputStream in = this.plugin.getResource("structures/starter_island.nbt")) {
            if (in == null) {
                this.plugin.getLogger().warning("Ressource structures/starter_island.nbt introuvable dans le jar.");
                return;
            }
            Structure structure = structureManager.loadStructure(in);
            structure.place(origin, false, StructureRotation.NONE, Mirror.NONE, 0, 1.0f, new Random());
            this.fillStartingChest(origin);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Impossible de charger starter_island.nbt: " + e.getMessage());
        }
    }

    private void fillStartingChest(Location origin) {
        Location chestLocation = origin.clone().add(5, 3, 5);
        BlockState state = chestLocation.getBlock().getState();
        if (!(state instanceof Chest)) {
            return;
        }
        Chest chest = (Chest) state;
        chest.getInventory().addItem(
                new ItemStack(Material.OAK_SAPLING, 1),
                new ItemStack(Material.BREAD, 8),
                new ItemStack(Material.COBBLESTONE, 16),
                new ItemStack(Material.OAK_LOG, 4),
                new ItemStack(Material.LAVA_BUCKET, 1),
                new ItemStack(Material.WATER_BUCKET, 1)
        );
    }

    private void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
        this.nextIndex = data.getInt("next-index", 0);
        ConfigurationSection section = data.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String worldName = section.getString(key + ".world");
                World world = worldName != null ? Bukkit.getWorld(worldName) : null;
                if (world == null) {
                    continue;
                }
                double x = section.getDouble(key + ".x");
                double y = section.getDouble(key + ".y");
                double z = section.getDouble(key + ".z");
                this.islands.put(uuid, new Location(world, x, y, z));
            } catch (IllegalArgumentException ignored) {
                this.plugin.getLogger().warning("Entree invalide dans islands.yml: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("next-index", this.nextIndex);
        for (Map.Entry<UUID, Location> entry : this.islands.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            Location loc = entry.getValue();
            data.set(base + "world", loc.getWorld().getName());
            data.set(base + "x", loc.getX());
            data.set(base + "y", loc.getY());
            data.set(base + "z", loc.getZ());
        }
        try {
            data.save(this.file);
        } catch (IOException e) {
            this.plugin.getLogger().warning("Impossible de sauvegarder islands.yml: " + e.getMessage());
        }
    }
}
