package com.antonin.marketeconomy.mine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

// Gere les mines du metier Mineur : placement (generation procedurale via MineGenerator),
// regeneration periodique (restaure tout le minerai degrade par le minage) et controle du
// niveau requis par palier. Toutes les mines vivent dans la meme dimension en pratique (le
// reseau de teleportation entre paliers suppose une seule dimension, comme dans le plugin Paper
// ou toutes les mines etaient dans le monde de minage dedie).
public class MineManager {
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 4;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type INSTANCES_TYPE = new TypeToken<List<MineInstance>>() { }.getType();

    private final Path file;
    private final List<MineInstance> mines = new ArrayList<>();

    public MineManager(Path file) {
        this.file = file;
        this.load();
    }

    public static int minLevelFor(int tier) {
        return switch (tier) {
            case 1 -> 1;
            case 2 -> 6;
            case 3 -> 11;
            case 4 -> 21;
            default -> 1;
        };
    }

    // Genere la mine du palier donne au coin "origin" (s'etend vers +X/+Y/+Z) et l'enregistre ;
    // renvoie un message d'erreur, ou null si tout s'est bien passe
    public String spawnMine(int tier, ServerLevel level, BlockPos origin) {
        if (tier < MIN_TIER || tier > MAX_TIER) {
            return "Palier invalide (1 à " + MAX_TIER + ").";
        }
        MineGenerator.generate(level, origin, tier);
        MineInstance instance = new MineInstance(tier, dimensionId(level),
                origin.getX(), origin.getY(), origin.getZ(),
                MineGenerator.SIZE, MineGenerator.HEIGHT, MineGenerator.SIZE);
        this.mines.add(instance);
        this.save();
        this.spawnTeleporterTags(level, instance);
        return null;
    }

    // Affiche les 4 etiquettes "Mine n°X" au-dessus des plaques de teleportation de la mine
    // qu'on vient de generer
    private void spawnTeleporterTags(ServerLevel level, MineInstance instance) {
        for (int i = 0; i < MineGenerator.PLATE_LOCAL_X.length; i++) {
            int targetTier = i + 1;
            double x = instance.x0 + MineGenerator.PLATE_LOCAL_X[i] + 0.5;
            double y = instance.y0 + MineGenerator.DEPTH + 1.3;
            double z = instance.z0 + MineGenerator.PLATE_LOCAL_Z + 0.5;
            ArmorStand stand = new ArmorStand(level, x, y, z);
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setNoGravity(true);
            stand.setSmall(true);
            stand.setInvulnerable(true);
            stand.setCustomName(Component.literal("§eMine n°" + targetTier));
            stand.setCustomNameVisible(true);
            level.addFreshEntity(stand);
        }
    }

    // Renvoie le palier cible si "clicked" correspond a la plaque de teleportation d'une mine
    // enregistree, sinon null
    public Integer getTeleportTarget(ServerLevel level, BlockPos clicked) {
        String dimension = dimensionId(level);
        for (MineInstance instance : this.mines) {
            if (!instance.dimension.equals(dimension)) {
                continue;
            }
            int py = instance.y0 + MineGenerator.DEPTH;
            int pz = instance.z0 + MineGenerator.PLATE_LOCAL_Z;
            if (clicked.getY() != py || clicked.getZ() != pz) {
                continue;
            }
            for (int i = 0; i < MineGenerator.PLATE_LOCAL_X.length; i++) {
                if (clicked.getX() == instance.x0 + MineGenerator.PLATE_LOCAL_X[i]) {
                    return i + 1;
                }
            }
        }
        return null;
    }

    // Point d'arrivee (sur la plateforme d'entree) de la mine du palier donne, ou null si elle
    // n'a pas encore ete generee
    public Vec3 getEntryPosition(int tier) {
        for (MineInstance instance : this.mines) {
            if (instance.tier != tier) {
                continue;
            }
            double x = instance.x0 + MineGenerator.PLATE_LOCAL_X[MineGenerator.PLATE_LOCAL_X.length / 2] + 0.5;
            double y = instance.y0 + MineGenerator.DEPTH + 1;
            double z = instance.z0 + MineGenerator.PLATE_LOCAL_Z + 0.5;
            return new Vec3(x, y, z);
        }
        return null;
    }

    // Regenere chaque mine enregistree a son emplacement d'origine : restaure tout le minerai
    // degrade par le minage depuis la derniere regeneration
    public void regenerateAll(MinecraftServer server) {
        if (this.mines.isEmpty()) {
            return;
        }
        for (MineInstance instance : this.mines) {
            ServerLevel level = resolveLevel(server, instance.dimension);
            if (level == null) {
                continue;
            }
            MineGenerator.generate(level, new BlockPos(instance.x0, instance.y0, instance.z0), instance.tier);
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal("§6[Mines] §eLes mines viennent de se régénérer."), false);
    }

    public Integer getTierAt(ServerLevel level, BlockPos pos) {
        String dimension = dimensionId(level);
        for (MineInstance instance : this.mines) {
            if (instance.contains(dimension, pos)) {
                return instance.tier;
            }
        }
        return null;
    }

    private static ServerLevel resolveLevel(MinecraftServer server, String dimensionId) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        if (id == null) {
            return null;
        }
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try (var reader = Files.newBufferedReader(this.file)) {
            List<MineInstance> loaded = GSON.fromJson(reader, INSTANCES_TYPE);
            if (loaded != null) {
                this.mines.addAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de charger " + this.file + ": " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(this.file.getParent());
            try (var writer = Files.newBufferedWriter(this.file)) {
                GSON.toJson(this.mines, INSTANCES_TYPE, writer);
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de sauvegarder " + this.file + ": " + e.getMessage());
        }
    }

    private static final class MineInstance {
        private final int tier;
        private final String dimension;
        private final int x0;
        private final int y0;
        private final int z0;
        private final int sizeX;
        private final int sizeY;
        private final int sizeZ;

        private MineInstance(int tier, String dimension, int x0, int y0, int z0, int sizeX, int sizeY, int sizeZ) {
            this.tier = tier;
            this.dimension = dimension;
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }

        private boolean contains(String dimension, BlockPos pos) {
            if (!this.dimension.equals(dimension)) {
                return false;
            }
            return pos.getX() >= this.x0 && pos.getX() < this.x0 + this.sizeX
                    && pos.getY() >= this.y0 && pos.getY() < this.y0 + this.sizeY
                    && pos.getZ() >= this.z0 && pos.getZ() < this.z0 + this.sizeZ;
        }
    }
}
