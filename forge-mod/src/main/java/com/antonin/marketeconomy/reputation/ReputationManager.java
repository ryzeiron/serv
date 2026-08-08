package com.antonin.marketeconomy.reputation;

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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

// Reputation ("confiance") d'un joueur, qui influence les prix pratiques par les PNJ marchands
// et accorde des titres de prestige affiches en prefixe d'equipe (visibles au-dessus de la tete
// et dans le tab) selon le nombre total d'echanges cumules.
public class ReputationManager {
    private static final double KILL_PENALTY = 25.0;
    private static final double TRADE_GAIN = 1.5;
    private static final double TRUSTED_THRESHOLD = 80.0;
    private static final double LIKED_THRESHOLD = 60.0;
    private static final double WEARY_THRESHOLD = 20.0;
    private static final double HOSTILE_THRESHOLD = 10.0;
    private static final double TRUSTED_DISCOUNT = 0.10;
    private static final double LIKED_DISCOUNT = 0.05;
    private static final double WEARY_MARKUP = 0.10;

    // Paliers de prestige (nombre d'echanges cumules) et titres associes, du plus bas au plus haut
    private static final long[] TITLE_THRESHOLDS = {10L, 50L, 200L, 1000L};
    private static final String[] TITLES = {"§7Négociant", "§aMarchand", "§e§lGrand Marchand", "§6§lMagnat du Marché"};

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Map<String, Entry>>() { }.getType();

    private final Path file;
    private final Map<UUID, PlayerReputation> reputations = new ConcurrentHashMap<>();

    public ReputationManager(Path file) {
        this.file = file;
        this.load();
    }

    public PlayerReputation get(UUID uuid) {
        return this.reputations.computeIfAbsent(uuid, u -> new PlayerReputation());
    }

    public void registerTrade(MinecraftServer server, ServerPlayer player, double amountSpent) {
        PlayerReputation reputation = this.get(player.getUUID());
        long before = reputation.getTotalTrades();
        reputation.recordSpend(amountSpent);
        reputation.addTrust(TRADE_GAIN);
        long after = reputation.getTotalTrades();
        this.save();

        String titleBefore = this.getMerchantTitle(before);
        String titleAfter = this.getMerchantTitle(after);
        if (titleAfter != null) {
            this.applyTitle(server, player, titleAfter);
        }
        if (titleAfter != null && !titleAfter.equals(titleBefore)) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("§6[Marché] §e" + player.getGameProfile().name()
                    + " devient " + titleAfter + " §e!"), false);
        }
    }

    // Renvoie le titre le plus haut atteint, ou null si aucun palier n'est franchi
    public String getMerchantTitle(long totalTrades) {
        String title = null;
        for (int i = 0; i < TITLE_THRESHOLDS.length; i++) {
            if (totalTrades >= TITLE_THRESHOLDS[i]) {
                title = TITLES[i];
            }
        }
        return title;
    }

    // Applique le titre en prefixe d'equipe scoreboard (contrairement a Bukkit, une partie
    // vanilla n'a qu'un seul Scoreboard partage par tous les joueurs, pas de scoreboard "prive")
    public void applyTitle(MinecraftServer server, ServerPlayer player, String title) {
        if (title == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        String teamName = ("mt" + player.getUUID().toString().replace("-", "")).substring(0, 16);
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        scoreboard.addPlayerToTeam(player.getGameProfile().name(), team);
        team.setPlayerPrefix(Component.literal(title + " "));
    }

    // A rappeler a la connexion pour reappliquer le titre deja acquis
    public void refreshTitle(MinecraftServer server, ServerPlayer player) {
        this.applyTitle(server, player, this.getMerchantTitle(this.get(player.getUUID()).getTotalTrades()));
    }

    public void penalizeVillagerKill(UUID uuid) {
        this.get(uuid).addTrust(-KILL_PENALTY);
    }

    public boolean isHostile(UUID uuid) {
        return this.get(uuid).getTrustScore() < HOSTILE_THRESHOLD;
    }

    public double getBuyMultiplier(UUID uuid) {
        double trust = this.get(uuid).getTrustScore();
        if (trust >= TRUSTED_THRESHOLD) {
            return 1.0 - TRUSTED_DISCOUNT;
        }
        if (trust >= LIKED_THRESHOLD) {
            return 1.0 - LIKED_DISCOUNT;
        }
        if (trust < WEARY_THRESHOLD) {
            return 1.0 + WEARY_MARKUP;
        }
        return 1.0;
    }

    public double getSellMultiplier(UUID uuid) {
        double trust = this.get(uuid).getTrustScore();
        if (trust >= TRUSTED_THRESHOLD) {
            return 1.0 + TRUSTED_DISCOUNT / 2.0;
        }
        if (trust >= LIKED_THRESHOLD) {
            return 1.0 + LIKED_DISCOUNT / 2.0;
        }
        if (trust < WEARY_THRESHOLD) {
            return 1.0 - WEARY_MARKUP / 2.0;
        }
        return 1.0;
    }

    public String buildGreeting(UUID uuid) {
        PlayerReputation reputation = this.get(uuid);
        double trust = reputation.getTrustScore();
        if (reputation.getTotalTrades() == 0L) {
            return "§eBonjour l'ami, je ne t'ai jamais vu ici. Bienvenue !";
        }
        if (trust < HOSTILE_THRESHOLD) {
            return "§cPars. Je ne commerce pas avec les gens comme toi.";
        }
        if (trust < WEARY_THRESHOLD) {
            return "§7Toi... je ne suis pas sûr de vouloir te vendre quoi que ce soit.";
        }
        if (trust >= TRUSTED_THRESHOLD) {
            return "§eAh, te revoilà ! Toujours un plaisir de commercer avec toi.";
        }
        return "§eContent de te revoir.";
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try (var reader = Files.newBufferedReader(this.file)) {
            Map<String, Entry> raw = GSON.fromJson(reader, DATA_TYPE);
            if (raw == null) {
                return;
            }
            for (Map.Entry<String, Entry> entry : raw.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    PlayerReputation reputation = new PlayerReputation();
                    reputation.setTrustScore(entry.getValue().trust);
                    reputation.setTotalTrades(entry.getValue().trades);
                    reputation.setTotalSpent(entry.getValue().spent);
                    this.reputations.put(uuid, reputation);
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
            Map<String, Entry> raw = new ConcurrentHashMap<>();
            for (Map.Entry<UUID, PlayerReputation> entry : this.reputations.entrySet()) {
                PlayerReputation reputation = entry.getValue();
                raw.put(entry.getKey().toString(), new Entry(reputation.getTrustScore(), reputation.getTotalTrades(), reputation.getTotalSpent()));
            }
            try (var writer = Files.newBufferedWriter(this.file)) {
                GSON.toJson(raw, DATA_TYPE, writer);
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de sauvegarder " + this.file + ": " + e.getMessage());
        }
    }

    private static final class Entry {
        private final double trust;
        private final long trades;
        private final double spent;

        private Entry(double trust, long trades, double spent) {
            this.trust = trust;
            this.trades = trades;
            this.spent = spent;
        }
    }
}
