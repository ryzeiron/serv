package com.antonin.marketeconomy.job;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobManager {
    private static final double XP_PER_LEVEL_BASE = 100.0;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type RECORDS_TYPE = new TypeToken<Map<String, Record>>() { }.getType();

    private final Path file;
    private final Map<UUID, PlayerJob> jobs = new ConcurrentHashMap<>();

    public JobManager(Path file) {
        this.file = file;
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

    // Fixe directement le niveau du metier "type" (utilitaire admin/test, contourne la
    // progression normale par xp)
    public void setLevel(UUID uuid, JobType type, int level) {
        this.jobs.put(uuid, new PlayerJob(type, level, 0.0));
        this.save();
    }

    // Ajoute de l'xp au metier "type" du joueur (ne fait rien s'il n'exerce pas ce metier) ;
    // renvoie le nombre de niveaux gagnes, a l'appelant d'annoncer la montee de niveau
    public int addXp(UUID uuid, JobType type, double amount) {
        PlayerJob job = this.jobs.get(uuid);
        if (job == null || job.getType() != type) {
            return 0;
        }
        int gained = job.addXp(amount, XP_PER_LEVEL_BASE);
        if (gained > 0) {
            this.save();
        }
        return gained;
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try (var reader = Files.newBufferedReader(this.file)) {
            Map<String, Record> raw = GSON.fromJson(reader, RECORDS_TYPE);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, Record> entry : raw.entrySet()) {
                JobType type = JobType.fromString(entry.getValue().type);
                if (type == null) {
                    continue;
                }
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    this.jobs.put(uuid, new PlayerJob(type, entry.getValue().level, entry.getValue().xp));
                } catch (IllegalArgumentException ignored) {
                    // entree invalide, on l'ignore
                }
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de charger " + this.file + ": " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(this.file.getParent());
            Map<String, Record> raw = new ConcurrentHashMap<>();
            for (Map.Entry<UUID, PlayerJob> entry : this.jobs.entrySet()) {
                PlayerJob job = entry.getValue();
                raw.put(entry.getKey().toString(), new Record(job.getType().name(), job.getLevel(), job.getXp()));
            }
            try (var writer = Files.newBufferedWriter(this.file)) {
                GSON.toJson(raw, RECORDS_TYPE, writer);
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de sauvegarder " + this.file + ": " + e.getMessage());
        }
    }

    private static final class Record {
        private final String type;
        private final int level;
        private final double xp;

        private Record(String type, int level, double xp) {
            this.type = type;
            this.level = level;
            this.xp = xp;
        }
    }
}
