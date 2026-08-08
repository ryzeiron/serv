package com.antonin.marketeconomy.hacker;

import com.antonin.marketeconomy.economy.BankManager;
import com.antonin.marketeconomy.economy.EconomyManager;
import com.antonin.marketeconomy.gui.HackTerminalMinigame;
import com.antonin.marketeconomy.job.JobManager;
import com.antonin.marketeconomy.job.JobType;
import com.antonin.marketeconomy.job.PlayerJob;
import com.antonin.marketeconomy.market.MarketItem;
import com.antonin.marketeconomy.market.MarketManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// Toutes les capacites du metier Hacker (cooldowns, couts, effets), partagees entre la commande
// /hack et le terminal de l'Ordinateur pour eviter de dupliquer la logique.
public class HackerAbilityService {
    private static final long INSIDER_COOLDOWN_MILLIS = 20_000L;
    private static final double XP_INSIDER = 5.0;

    private static final long PRICE_HACK_COOLDOWN_MILLIS = 300_000L;
    private static final double PRICE_HACK_COST = 200.0;
    private static final double PRICE_HACK_FAKE_VOLUME_BASE = 60.0;
    private static final double PRICE_HACK_FAKE_VOLUME_PER_LEVEL = 10.0;
    private static final double XP_PRICE_HACK = 15.0;

    private static final long SCRAMBLE_COOLDOWN_MILLIS = 600_000L;
    private static final double SCRAMBLE_COST = 150.0;
    private static final long SCRAMBLE_DURATION_MILLIS = 300_000L;
    private static final double XP_SCRAMBLE = 10.0;

    private static final long WIRETAP_COOLDOWN_MILLIS = 900_000L;
    private static final double WIRETAP_COST = 300.0;
    private static final long WIRETAP_DURATION_MILLIS = 180_000L;
    private static final double WIRETAP_CUT_BASE = 0.05;
    private static final double WIRETAP_CUT_PER_LEVEL = 0.01;
    private static final double WIRETAP_CUT_MAX = 0.15;
    private static final double WIRETAP_CATCH_CHANCE_BASE = 0.30;
    private static final double WIRETAP_CATCH_CHANCE_PER_LEVEL = 0.02;
    private static final double WIRETAP_CATCH_CHANCE_MIN = 0.05;
    private static final long WIRETAP_TARGET_COOLDOWN_MILLIS = 1_800_000L;
    private static final double XP_WIRETAP_START = 20.0;

    private static final long BANK_HACK_COOLDOWN_MILLIS = 2_700_000L;
    private static final long BANK_HACK_TARGET_COOLDOWN_MILLIS = 10_800_000L;
    private static final double BANK_HACK_COST = 500.0;
    private static final double BANK_HACK_CHANCE_BASE = 0.40;
    private static final double BANK_HACK_CHANCE_PER_LEVEL = 0.05;
    private static final double BANK_HACK_CHANCE_MAX = 0.85;
    private static final double BANK_HACK_DRAIN_SHARE = 0.30;
    private static final double XP_BANK_HACK_SUCCESS = 60.0;

    private static final long TERMINAL_COOLDOWN_MILLIS = 180_000L;

    private final JobManager jobManager;
    private final MarketManager marketManager;
    private final EconomyManager economyManager;
    private final BankManager bankManager;
    private final Map<UUID, Map<String, Long>> cooldownUntil = new HashMap<>();
    private final Random random = new Random();

    public HackerAbilityService(JobManager jobManager, MarketManager marketManager, EconomyManager economyManager, BankManager bankManager) {
        this.jobManager = jobManager;
        this.marketManager = marketManager;
        this.economyManager = economyManager;
        this.bankManager = bankManager;
    }

    public boolean isHacker(ServerPlayer player) {
        PlayerJob job = this.jobManager.getJob(player.getUUID());
        return job != null && job.getType() == JobType.HACKER;
    }

    private PlayerJob requireJob(ServerPlayer player) {
        return this.jobManager.getJob(player.getUUID());
    }

    private long remainingCooldownSeconds(ServerPlayer player, String key) {
        Map<String, Long> map = this.cooldownUntil.get(player.getUUID());
        if (map == null) {
            return 0L;
        }
        Long until = map.get(key);
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, (until - System.currentTimeMillis()) / 1000L);
    }

    private void setCooldown(ServerPlayer player, String key, long durationMillis) {
        this.cooldownUntil.computeIfAbsent(player.getUUID(), u -> new HashMap<>())
                .put(key, System.currentTimeMillis() + durationMillis);
    }

    private boolean checkCooldown(ServerPlayer player, String key, long durationMillis) {
        long remaining = this.remainingCooldownSeconds(player, key);
        if (remaining > 0L) {
            player.sendSystemMessage(Component.literal("§5[Hack] §cEncore en recharge (" + remaining + "s)."));
            return false;
        }
        return true;
    }

    private void addXp(ServerPlayer player, double amount) {
        int gained = this.jobManager.addXp(player.getUUID(), JobType.HACKER, amount);
        if (gained > 0) {
            PlayerJob job = this.jobManager.getJob(player.getUUID());
            player.sendSystemMessage(Component.literal("§5[Métier] §dNiveau supérieur ! " + JobType.HACKER.getDisplayName()
                    + " §dniveau " + job.getLevel() + "."));
        }
    }

    // --- market ---

    public void marketReport(ServerPlayer player, MarketItem item) {
        if (!this.checkCooldown(player, "market", INSIDER_COOLDOWN_MILLIS)) {
            return;
        }
        this.setCooldown(player, "market", INSIDER_COOLDOWN_MILLIS);
        player.sendSystemMessage(Component.literal(this.marketManager.getInsiderReport(item)));
        this.addXp(player, XP_INSIDER);
    }

    // --- price ---

    public void priceHack(ServerPlayer player, MarketItem item, boolean up) {
        PlayerJob job = this.requireJob(player);
        if (job == null) {
            return;
        }
        if (!this.checkCooldown(player, "price", PRICE_HACK_COOLDOWN_MILLIS)) {
            return;
        }
        if (this.economyManager.getBalance(player.getUUID()) < PRICE_HACK_COST) {
            player.sendSystemMessage(Component.literal("§5[Hack] §cIl te faut " + this.economyManager.format(PRICE_HACK_COST) + " pour lancer cet exploit."));
            return;
        }
        this.economyManager.withdraw(player.getUUID(), PRICE_HACK_COST);
        this.setCooldown(player, "price", PRICE_HACK_COOLDOWN_MILLIS);

        long fakeVolume = Math.round(PRICE_HACK_FAKE_VOLUME_BASE + PRICE_HACK_FAKE_VOLUME_PER_LEVEL * job.getLevel());
        this.marketManager.hackPrice(player.getUUID(), item, up, fakeVolume);
        player.sendSystemMessage(Component.literal("§5[Hack] §dInjection de fausse activité sur " + item.getDisplayName()
                + " (" + (up ? "pump" : "dump") + "). Effet visible au prochain cycle de prix — risque d'être détecté."));
        this.addXp(player, XP_PRICE_HACK);
    }

    // --- scramble ---

    public void scramble(ServerPlayer player) {
        if (!this.checkCooldown(player, "scramble", SCRAMBLE_COOLDOWN_MILLIS)) {
            return;
        }
        if (this.economyManager.getBalance(player.getUUID()) < SCRAMBLE_COST) {
            player.sendSystemMessage(Component.literal("§5[Hack] §cIl te faut " + this.economyManager.format(SCRAMBLE_COST) + " pour brouiller ta trace."));
            return;
        }
        this.economyManager.withdraw(player.getUUID(), SCRAMBLE_COST);
        this.setCooldown(player, "scramble", SCRAMBLE_COOLDOWN_MILLIS);
        this.marketManager.scrambleTrace(player.getUUID(), SCRAMBLE_DURATION_MILLIS);
        player.sendSystemMessage(Component.literal("§5[Hack] §dTrace brouillée pendant " + (SCRAMBLE_DURATION_MILLIS / 1000L)
                + "s : la détection de manipulation ne peut pas te repérer."));
        this.addXp(player, XP_SCRAMBLE);
    }

    // --- wiretap ---

    public void wiretap(ServerPlayer player, ServerPlayer target) {
        PlayerJob job = this.requireJob(player);
        if (job == null) {
            return;
        }
        if (!this.checkCooldown(player, "wiretap", WIRETAP_COOLDOWN_MILLIS)) {
            return;
        }
        double cutShare = Math.min(WIRETAP_CUT_MAX, WIRETAP_CUT_BASE + WIRETAP_CUT_PER_LEVEL * job.getLevel());
        double catchChance = Math.max(WIRETAP_CATCH_CHANCE_MIN, WIRETAP_CATCH_CHANCE_BASE - WIRETAP_CATCH_CHANCE_PER_LEVEL * job.getLevel());

        String error = this.marketManager.startWiretap(player, target, cutShare, catchChance, WIRETAP_DURATION_MILLIS, WIRETAP_TARGET_COOLDOWN_MILLIS);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§5[Hack] §c" + error));
            return;
        }

        if (this.economyManager.getBalance(player.getUUID()) >= WIRETAP_COST) {
            this.economyManager.withdraw(player.getUUID(), WIRETAP_COST);
        }
        this.setCooldown(player, "wiretap", WIRETAP_COOLDOWN_MILLIS);
        player.sendSystemMessage(Component.literal("§5[Hack] §dÉcoute posée sur " + target.getGameProfile().name()
                + " pendant " + (WIRETAP_DURATION_MILLIS / 1000L) + "s — tu toucheras " + Math.round(cutShare * 100) + "% de ses ventes."));
        this.addXp(player, XP_WIRETAP_START);
    }

    // --- banque ---

    public void bankHack(ServerPlayer player, ServerPlayer target) {
        PlayerJob job = this.requireJob(player);
        if (job == null) {
            return;
        }
        if (!this.checkCooldown(player, "banque", BANK_HACK_COOLDOWN_MILLIS)) {
            return;
        }
        if (target.getUUID().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§5[Hack] §cTu ne peux pas te pirater toi-même."));
            return;
        }
        if (this.bankManager.isRecentlyHacked(target.getUUID())) {
            player.sendSystemMessage(Component.literal("§5[Hack] §cCette banque a été piratée récemment, elle est sous surveillance renforcée."));
            return;
        }

        if (this.economyManager.getBalance(player.getUUID()) < BANK_HACK_COST) {
            player.sendSystemMessage(Component.literal("§5[Hack] §cIl te faut " + this.economyManager.format(BANK_HACK_COST) + " pour monter cette intrusion."));
            return;
        }
        this.economyManager.withdraw(player.getUUID(), BANK_HACK_COST);
        this.setCooldown(player, "banque", BANK_HACK_COOLDOWN_MILLIS);

        double chance = Math.min(BANK_HACK_CHANCE_MAX, BANK_HACK_CHANCE_BASE + BANK_HACK_CHANCE_PER_LEVEL * job.getLevel());
        if (this.random.nextDouble() >= chance) {
            player.sendSystemMessage(Component.literal("§5[Hack] §cIntrusion échouée, le pare-feu a tenu bon."));
            return;
        }

        double stolen = this.bankManager.hack(target.getUUID(), BANK_HACK_DRAIN_SHARE, BANK_HACK_TARGET_COOLDOWN_MILLIS);
        if (stolen <= 0.0) {
            player.sendSystemMessage(Component.literal("§5[Hack] §eIntrusion réussie mais le compte était vide."));
            return;
        }
        this.economyManager.deposit(player.getUUID(), stolen);
        player.sendSystemMessage(Component.literal("§5[Hack] §dBanque de " + target.getGameProfile().name()
                + " piratée ! Tu voles " + this.economyManager.format(stolen) + "."));
        target.sendSystemMessage(Component.literal("§4[!] §cTa banque d'île a été piratée ! Tu perds "
                + Math.round(BANK_HACK_DRAIN_SHARE * 100) + "% de ton solde (" + this.economyManager.format(stolen) + ")."));
        this.addXp(player, XP_BANK_HACK_SUCCESS);
    }

    // --- terminal ---

    public void openTerminal(ServerPlayer player) {
        PlayerJob job = this.requireJob(player);
        if (job == null) {
            return;
        }
        if (!this.checkCooldown(player, "terminal", TERMINAL_COOLDOWN_MILLIS)) {
            return;
        }
        this.setCooldown(player, "terminal", TERMINAL_COOLDOWN_MILLIS);
        HackTerminalMinigame.open(player, job);
    }
}
