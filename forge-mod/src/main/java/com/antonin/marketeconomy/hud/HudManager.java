package com.antonin.marketeconomy.hud;

import com.antonin.marketeconomy.job.PlayerJob;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// HUD personnel : solde/banque/metier/tendance marche/ile, affiches en barre d'action
// (au-dessus de la barre d'objets), rafraichis periodiquement. Contrairement au plugin Paper
// (barre laterale scoreboard, un Scoreboard personnel par joueur cote Bukkit), le vanilla n'a
// qu'un seul Scoreboard partage par le serveur -- une vraie barre laterale personnalisee par
// joueur demanderait soit des paquets de score bruts par connexion, soit un canal reseau custom
// (component/paquet), deux API plus recentes et incertaines qu'on evite ici par prudence. La
// barre d'action (Player#displayClientMessage) est une methode vanilla stable et simple qui
// donne un resultat equivalent en une ligne.
public class HudManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type UUID_SET_TYPE = new TypeToken<Set<String>>() { }.getType();

    private final Path file;
    private final Set<UUID> disabled = new HashSet<>();

    public HudManager(Path file) {
        this.file = file;
        this.load();
    }

    public boolean isEnabled(UUID uuid) {
        return !this.disabled.contains(uuid);
    }

    public void setEnabled(UUID uuid, boolean enabled) {
        if (enabled) {
            this.disabled.remove(uuid);
        } else {
            this.disabled.add(uuid);
        }
        this.save();
    }

    public void refreshAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (this.isEnabled(player.getUUID())) {
                this.refresh(player);
            }
        }
    }

    private void refresh(ServerPlayer player) {
        MarketEconomyServer economy = MarketEconomyServer.get();
        if (economy == null) {
            return;
        }
        StringBuilder line = new StringBuilder("§6⚡ ");
        line.append("§eSolde: §f").append(economy.getEconomyManager().format(economy.getEconomyManager().getBalance(player.getUUID())));
        line.append(" §7| §eBanque: §f").append(economy.getEconomyManager().format(economy.getBankManager().getBalance(player.getUUID())));

        PlayerJob job = economy.getJobManager().getJob(player.getUUID());
        if (job != null) {
            line.append(" §7| §e").append(job.getType().getDisplayName()).append(" §7niv.").append(job.getLevel());
        }

        line.append(" §7| §eMarché: §f").append(economy.getMarketManager().getMarketTrendArrow())
                .append(' ').append(String.format(Locale.US, "%.1f", economy.getMarketManager().getMarketIndexChangePercent())).append('%');

        var island = economy.getIslandManager().getIslandLocation(player.getUUID());
        if (island != null) {
            var pos = island.position();
            line.append(" §7| §eÎle: §f").append((int) pos.x).append(',').append((int) pos.z);
        }

        player.displayClientMessage(Component.literal(line.toString()), true);
    }

    private void load() {
        if (!Files.exists(this.file)) {
            return;
        }
        try (var reader = Files.newBufferedReader(this.file)) {
            Set<String> raw = GSON.fromJson(reader, UUID_SET_TYPE);
            if (raw == null) {
                return;
            }
            for (String value : raw) {
                try {
                    this.disabled.add(UUID.fromString(value));
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
            Set<String> raw = new HashSet<>();
            for (UUID uuid : this.disabled) {
                raw.add(uuid.toString());
            }
            try (var writer = Files.newBufferedWriter(this.file)) {
                GSON.toJson(raw, UUID_SET_TYPE, writer);
            }
        } catch (IOException e) {
            System.err.println("[MarketEconomy] Impossible de sauvegarder " + this.file + ": " + e.getMessage());
        }
    }
}
