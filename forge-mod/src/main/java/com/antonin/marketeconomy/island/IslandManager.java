package com.antonin.marketeconomy.island;

import com.antonin.marketeconomy.MarketEconomyMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;

// Attribue une ile de depart a chaque joueur (posee depuis la structure embarquee
// data/marketeconomy/structure/starter_island.nbt), sur une grille espacee pour ne jamais
// se chevaucher.
public class IslandManager {
    private static final int SPACING = 400;
    private static final int ISLANDS_PER_ROW = 20;
    private static final int ISLAND_Y = 100;
    private static final ResourceLocation STARTER_ISLAND_ID = ResourceLocation.fromNamespaceAndPath(MarketEconomyMod.MOD_ID, "starter_island");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Data>() { }.getType();

    private final Path file;
    private final Map<UUID, IslandSpawn> islands = new HashMap<>();
    private int nextIndex = 0;

    public IslandManager(Path file) {
        this.file = file;
        this.load();
    }

    public boolean hasIsland(UUID uuid) {
        return this.islands.containsKey(uuid);
    }

    public IslandSpawn getIslandLocation(UUID uuid) {
        return this.islands.get(uuid);
    }

    // Renvoie l'ile existante du joueur, ou en genere une nouvelle sur la grille
    public IslandSpawn getOrCreateIsland(ServerPlayer player, ServerLevel overworld) {
        IslandSpawn existing = this.islands.get(player.getUUID());
        if (existing != null) {
            return existing;
        }
        int index = this.nextIndex++;
        int gridX = index % ISLANDS_PER_ROW;
        int gridZ = index / ISLANDS_PER_ROW;
        BlockPos origin = new BlockPos(gridX * SPACING, ISLAND_Y, gridZ * SPACING);

        this.pasteStarterIsland(overworld, origin);

        IslandSpawn spawn = new IslandSpawn(overworld.dimension().location().toString(),
                origin.getX() + 4.5, origin.getY() + 3, origin.getZ() + 4.5);
        this.islands.put(player.getUUID(), spawn);
        this.save();
        return spawn;
    }

    private void pasteStarterIsland(ServerLevel level, BlockPos origin) {
        StructureTemplateManager templateManager = level.getServer().getStructureManager();
        Optional<StructureTemplate> template = templateManager.get(STARTER_ISLAND_ID);
        if (template.isEmpty()) {
            System.err.println("[MarketEconomy] Structure introuvable: " + STARTER_ISLAND_ID);
            return;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings();
        template.get().placeInWorld(level, origin, origin, settings, level.getRandom(), 2);
        this.fillStartingChest(level, origin);
    }

    private void fillStartingChest(ServerLevel level, BlockPos origin) {
        BlockPos chestPos = origin.offset(5, 3, 5);
        BlockEntity entity = level.getBlockEntity(chestPos);
        if (!(entity instanceof ChestBlockEntity chest)) {
            return;
        }
        chest.setItem(0, new ItemStack(Items.OAK_SAPLING, 1));
        chest.setItem(1, new ItemStack(Items.BREAD, 8));
        chest.setItem(2, new ItemStack(Items.COBBLESTONE, 16));
        chest.setItem(3, new ItemStack(Items.OAK_LOG, 4));
        chest.setItem(4, new ItemStack(Items.LAVA_BUCKET, 1));
        chest.setItem(5, new ItemStack(Items.WATER_BUCKET, 1));
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try (var reader = Files.newBufferedReader(this.file)) {
            Data data = GSON.fromJson(reader, DATA_TYPE);
            if (data == null) {
                return;
            }
            this.nextIndex = data.nextIndex;
            if (data.islands != null) {
                for (Map.Entry<String, IslandSpawn> entry : data.islands.entrySet()) {
                    try {
                        this.islands.put(UUID.fromString(entry.getKey()), entry.getValue());
                    } catch (IllegalArgumentException ignored) {
                        // entree invalide, on l'ignore
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de charger " + this.file + ": " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(this.file.getParent());
            Map<String, IslandSpawn> raw = new HashMap<>();
            for (Map.Entry<UUID, IslandSpawn> entry : this.islands.entrySet()) {
                raw.put(entry.getKey().toString(), entry.getValue());
            }
            try (var writer = Files.newBufferedWriter(this.file)) {
                GSON.toJson(new Data(this.nextIndex, raw), DATA_TYPE, writer);
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de sauvegarder " + this.file + ": " + e.getMessage());
        }
    }

    private static final class Data {
        private final int nextIndex;
        private final Map<String, IslandSpawn> islands;

        private Data(int nextIndex, Map<String, IslandSpawn> islands) {
            this.nextIndex = nextIndex;
            this.islands = islands;
        }
    }

    public static final class IslandSpawn {
        private final String dimension;
        private final double x;
        private final double y;
        private final double z;

        public IslandSpawn(String dimension, double x, double y, double z) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String dimension() {
            return this.dimension;
        }

        public Vec3 position() {
            return new Vec3(this.x, this.y, this.z);
        }
    }
}
