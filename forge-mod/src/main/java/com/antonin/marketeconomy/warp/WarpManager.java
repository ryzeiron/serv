package com.antonin.marketeconomy.warp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

// Points de teleportation fixes du serveur (spawn/hub, ile pvp), enregistrables en jeu via
// /sethub et /setpvp plutot que devines a l'aveugle.
public class WarpManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private Warp hub;
    private Warp pvp;

    public WarpManager(Path file) {
        this.file = file;
        this.load();
    }

    public Warp getHub() {
        return this.hub;
    }

    public Warp getPvp() {
        return this.pvp;
    }

    public void setHub(ServerPlayer player) {
        this.hub = Warp.of(player);
        this.save();
    }

    public void setPvp(ServerPlayer player) {
        this.pvp = Warp.of(player);
        this.save();
    }

    public static boolean teleport(MinecraftServer server, ServerPlayer player, Warp warp) {
        ResourceLocation id = ResourceLocation.tryParse(warp.dimension);
        if (id == null) {
            return false;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null) {
            return false;
        }
        player.teleportTo(level, warp.x, warp.y, warp.z, warp.yaw, warp.pitch);
        return true;
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try (var reader = Files.newBufferedReader(this.file)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null) {
                this.hub = data.hub;
                this.pvp = data.pvp;
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de charger " + this.file + ": " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(this.file.getParent());
            try (var writer = Files.newBufferedWriter(this.file)) {
                GSON.toJson(new Data(this.hub, this.pvp), Data.class, writer);
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de sauvegarder " + this.file + ": " + e.getMessage());
        }
    }

    private static final class Data {
        private final Warp hub;
        private final Warp pvp;

        private Data(Warp hub, Warp pvp) {
            this.hub = hub;
            this.pvp = pvp;
        }
    }

    public static final class Warp {
        private final String dimension;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;

        private Warp(String dimension, double x, double y, double z, float yaw, float pitch) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        private static Warp of(ServerPlayer player) {
            ResourceKey<Level> dimension = player.level().dimension();
            return new Warp(dimension.location().toString(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot());
        }
    }
}
