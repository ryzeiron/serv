package com.antonin.marketeconomy.market;

import com.antonin.marketeconomy.economy.EconomyManager;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

// Moteur de prix offre/demande du marche, avec detection de manipulation, primes et ecoutes
// (capacites du metier Hacker). Les contrats a terme et le journal boursier viendront avec
// leurs propres phases.
public class MarketManager {
    private static final int HISTORY_LENGTH = 50;
    private static final double SENSITIVITY = 0.02;
    private static final double MIN_MULTIPLIER = 0.2;
    private static final double MAX_MULTIPLIER = 5.0;
    private static final double CATEGORY_SENSITIVITY = 0.01;

    private static final boolean EVENTS_ENABLED = true;
    private static final double EVENT_CHECK_CHANCE = 0.05;
    private static final int EVENT_DURATION_CYCLES = 3;
    private static final double CRASH_SHOCK = 0.75;
    private static final double BOOM_SHOCK = 1.30;
    private static final double EVENT_VOLATILITY_MULTIPLIER = 2.0;

    private static final boolean MANIPULATION_ENABLED = true;
    private static final long MANIPULATION_MIN_VOLUME = 40L;
    private static final double MANIPULATION_SHARE_THRESHOLD = 0.6;

    private static final long BOUNTY_ELIGIBLE_MILLIS = 300_000L;
    private static final double BOUNTY_CUT_SHARE = 0.10;
    private static final long BOUNTY_DURATION_MILLIS = 1_200_000L;

    private static final int HEADLINE_HISTORY_LENGTH = 30;
    private static final double HEADLINE_THRESHOLD_PERCENT = 3.0;

    private static final double FUTURES_MIN_STAKE = 10.0;
    private static final int FUTURES_MIN_MINUTES = 2;
    private static final int FUTURES_MAX_MINUTES = 120;

    private final Map<Item, MarketItem> items = new LinkedHashMap<>();
    private final Random random = new Random();
    private MarketEvent activeEvent;
    private final EconomyManager economyManager;

    private long totalBought = 0L;
    private long totalSold = 0L;
    private double totalSpent = 0.0;
    private double totalEarned = 0.0;
    private double previousIndexValue = 1.0;
    private double lastIndexValue = 1.0;

    // --- detection de manipulation / primes / ecoutes ---
    private final Map<UUID, Map<Item, long[]>> playerActivity = new HashMap<>();
    private final Map<UUID, Long> bountyEligibleUntil = new HashMap<>();
    private final Map<UUID, Bounty> activeBounties = new HashMap<>();
    private final Map<UUID, Wiretap> activeWiretaps = new HashMap<>(); // cle = cible
    private final Map<UUID, Long> wiretapTargetCooldownUntil = new HashMap<>();
    private final Map<UUID, Long> traceImmuneUntil = new HashMap<>(); // cle = hacker

    // --- journal boursier ---
    private final LinkedList<String> headlines = new LinkedList<>();

    // --- contrats a terme ---
    private final Map<UUID, FuturesContract> contracts = new HashMap<>();

    public MarketManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.registerDefaultItems();
        this.lastIndexValue = this.computeIndex();
        this.previousIndexValue = this.lastIndexValue;
    }

    private void registerDefaultItems() {
        this.register("oak_log", "Bois de Chêne", MarketCategory.RAW_MATERIALS, 3.0, 4000L);
        this.register("cobblestone", "Pierre", MarketCategory.RAW_MATERIALS, 1.0, 8000L);
        this.register("iron_ingot", "Lingot de Fer", MarketCategory.ORES, 8.0, 2000L);
        this.register("gold_ingot", "Lingot d'Or", MarketCategory.ORES, 15.0, 1200L);
        this.register("diamond", "Diamant", MarketCategory.RARE, 100.0, 500L);
        this.register("emerald", "Émeraude", MarketCategory.RARE, 40.0, 800L);
        this.register("netherite_ingot", "Lingot de Netherite", MarketCategory.RARE, 500.0, 50L);
        this.register("wheat", "Blé", MarketCategory.CONSUMABLES, 2.0, 5000L);
        this.register("cooked_beef", "Steak Cuit", MarketCategory.CONSUMABLES, 5.0, 2000L);
        this.register("rotten_flesh", "Chair Pourrie", MarketCategory.MOB_DROPS, 0.5, 5000L);
        this.register("ender_pearl", "Perle d'Ender", MarketCategory.MOB_DROPS, 25.0, 300L);
    }

    private void register(String vanillaItemId, String displayName, MarketCategory category, double basePrice, long initialStock) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.withDefaultNamespace(vanillaItemId));
        if (item == null) {
            return;
        }
        this.items.put(item, new MarketItem(item, displayName, category, basePrice, initialStock, HISTORY_LENGTH));
    }

    public Map<Item, MarketItem> getItems() {
        return this.items;
    }

    public MarketItem getItem(Item item) {
        return this.items.get(item);
    }

    public boolean isTradable(Item item) {
        return this.items.containsKey(item);
    }

    public MarketEvent getActiveEvent() {
        return this.activeEvent;
    }

    public void recordPurchase(UUID buyer, MarketItem item, long amount, double totalPrice) {
        item.registerBuy(amount);
        this.totalBought += amount;
        this.totalSpent += totalPrice;
        this.trackPlayerActivity(buyer, item.getItem(), amount, 0L);
    }

    public void recordSale(UUID seller, MarketItem item, long amount, double totalPrice) {
        item.registerSell(amount);
        this.totalSold += amount;
        this.totalEarned += totalPrice;
        this.trackPlayerActivity(seller, item.getItem(), 0L, amount);
    }

    private void trackPlayerActivity(UUID uuid, Item item, long bought, long sold) {
        Map<Item, long[]> perItem = this.playerActivity.computeIfAbsent(uuid, u -> new HashMap<>());
        long[] totals = perItem.computeIfAbsent(item, i -> new long[2]);
        totals[0] += bought;
        totals[1] += sold;
    }

    public long getTotalBought() {
        return this.totalBought;
    }

    public long getTotalSold() {
        return this.totalSold;
    }

    public double getTotalSpent() {
        return this.totalSpent;
    }

    public double getTotalEarned() {
        return this.totalEarned;
    }

    public double getMarketIndexChangePercent() {
        if (this.previousIndexValue == 0.0) {
            return 0.0;
        }
        return (this.lastIndexValue - this.previousIndexValue) / this.previousIndexValue * 100.0;
    }

    public String getMarketTrendArrow() {
        double change = this.getMarketIndexChangePercent();
        if (change > 0.01) {
            return "↑";
        }
        if (change < -0.01) {
            return "↓";
        }
        return "→";
    }

    private double computeIndex() {
        if (this.items.isEmpty()) {
            return 1.0;
        }
        double sum = 0.0;
        for (MarketItem item : this.items.values()) {
            sum += item.getCurrentPrice() / item.getBasePrice();
        }
        return sum / this.items.size();
    }

    // --- detection de manipulation ---

    private void checkManipulation(MarketItem item, MinecraftServer server) {
        if (!MANIPULATION_ENABLED) {
            return;
        }
        long totalActivity = item.getRecentBought() + item.getRecentSold();
        if (totalActivity < MANIPULATION_MIN_VOLUME) {
            return;
        }
        Item vanillaItem = item.getItem();
        for (Map.Entry<UUID, Map<Item, long[]>> entry : this.playerActivity.entrySet()) {
            long[] playerTotals = entry.getValue().get(vanillaItem);
            if (playerTotals == null) {
                continue;
            }
            long playerAmount = playerTotals[0] + playerTotals[1];
            double share = (double) playerAmount / (double) totalActivity;
            if (share >= MANIPULATION_SHARE_THRESHOLD) {
                this.flagManipulation(entry.getKey(), item, share, server);
            }
        }
    }

    // Ne bloque pas le trading : rend simplement le joueur "primable" pendant une fenetre de temps
    private void flagManipulation(UUID uuid, MarketItem item, double share, MinecraftServer server) {
        Long immuneUntil = this.traceImmuneUntil.get(uuid);
        if (immuneUntil != null && immuneUntil > System.currentTimeMillis()) {
            return;
        }
        this.bountyEligibleUntil.put(uuid, System.currentTimeMillis() + BOUNTY_ELIGIBLE_MILLIS);
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        String name = player != null ? player.getGameProfile().name() : "Un joueur";
        broadcast(server, "§4[Marché] §cActivité suspecte détectée sur " + item.getDisplayName()
                + " (" + Math.round(share * 100) + "% du volume) — " + name
                + " peut être ciblé par une prime (§7/prime " + name + "§c) pendant "
                + (BOUNTY_ELIGIBLE_MILLIS / 1000L) + "s.");
        if (player != null) {
            player.sendSystemMessage(Component.literal("§cTon activité sur le marché ressemble à de la manipulation de prix. "
                    + "Les autres joueurs peuvent placer une prime sur toi pendant "
                    + (BOUNTY_ELIGIBLE_MILLIS / 1000L) + "s."));
        }
    }

    // --- primes ---

    public boolean isBountyEligible(UUID uuid) {
        Long until = this.bountyEligibleUntil.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public Bounty getBounty(UUID targetUuid) {
        return this.activeBounties.get(targetUuid);
    }

    // Place une prime sur "target" si elle est actuellement primable et pas deja ciblee ; renvoie
    // un message d'erreur, ou null si la prime a bien ete posee
    public String placeBounty(ServerPlayer placer, ServerPlayer target) {
        if (placer.getUUID().equals(target.getUUID())) {
            return "Tu ne peux pas placer une prime sur toi-même.";
        }
        if (!this.isBountyEligible(target.getUUID())) {
            return target.getGameProfile().name() + " n'est pas actuellement recherché pour manipulation de marché.";
        }
        if (this.activeBounties.containsKey(target.getUUID())) {
            return "Un contrat est déjà actif sur " + target.getGameProfile().name() + ".";
        }
        long expiresAt = System.currentTimeMillis() + BOUNTY_DURATION_MILLIS;
        this.activeBounties.put(target.getUUID(), new Bounty(target.getUUID(), placer.getUUID(), BOUNTY_CUT_SHARE, expiresAt));
        this.bountyEligibleUntil.remove(target.getUUID());
        broadcast(ServerLifecycleHooks.getCurrentServer(), "§4[Marché] §c" + placer.getGameProfile().name()
                + " place un contrat sur la tête de " + target.getGameProfile().name()
                + " ! " + Math.round(BOUNTY_CUT_SHARE * 100) + "% de ses ventes lui reviendront pendant "
                + (BOUNTY_DURATION_MILLIS / 60_000L) + " min.");
        return null;
    }

    // A appeler avant de crediter une vente : redirige la part de la prime puis celle d'une
    // eventuelle ecoute (wiretap) de Hacker, et renvoie ce qu'il reste a verser au vendeur
    public double applyBountyCut(ServerPlayer seller, double saleAmount) {
        double remaining = saleAmount;

        Bounty bounty = this.activeBounties.get(seller.getUUID());
        if (bounty != null) {
            double cut = remaining * bounty.getCutShare();
            if (cut > 0.0) {
                this.economyManager.deposit(bounty.getPlacer(), cut);
                ServerPlayer onlinePlacer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(bounty.getPlacer());
                if (onlinePlacer != null) {
                    onlinePlacer.sendSystemMessage(Component.literal("§6[Prime] §eTa cible " + seller.getGameProfile().name()
                            + " a vendu — tu touches " + this.economyManager.format(cut) + "."));
                }
            }
            remaining -= cut;
        }

        remaining = this.applyWiretapCut(seller, remaining);
        return remaining;
    }

    private double applyWiretapCut(ServerPlayer seller, double remaining) {
        Wiretap wiretap = this.activeWiretaps.get(seller.getUUID());
        if (wiretap == null) {
            return remaining;
        }
        double cut = Math.round(remaining * wiretap.getCutShare() * 100.0) / 100.0;
        if (cut <= 0.0) {
            return remaining;
        }
        this.economyManager.deposit(wiretap.getHacker(), cut);
        wiretap.addSkimmed(cut);
        ServerPlayer hackerOnline = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(wiretap.getHacker());
        if (hackerOnline != null) {
            hackerOnline.sendSystemMessage(Component.literal("§5[Hack] §dInterception sur " + seller.getGameProfile().name()
                    + " : +" + this.economyManager.format(cut) + "."));
        }
        if (!wiretap.isCaught() && this.random.nextDouble() < wiretap.getCatchChance()) {
            wiretap.setCaught(true);
            String hackerName = hackerOnline != null ? hackerOnline.getGameProfile().name() : "un joueur";
            seller.sendSystemMessage(Component.literal("§c[!] Intrusion détectée sur tes ventes : " + hackerName
                    + " t'espionnait ! Tu peux le signaler avec §7/prime " + hackerName));
            this.bountyEligibleUntil.put(wiretap.getHacker(), System.currentTimeMillis() + BOUNTY_ELIGIBLE_MILLIS);
            broadcast(ServerLifecycleHooks.getCurrentServer(), "§4[Marché] §c" + hackerName + " a été repéré en train de pirater les ventes de "
                    + seller.getGameProfile().name() + " ! Une prime peut être placée (§7/prime " + hackerName + "§c).");
        }
        return remaining - cut;
    }

    // --- metier Hacker ---

    // Pose une ecoute sur les ventes de "target" ; renvoie un message d'erreur, ou null si l'ecoute
    // a bien ete posee
    public String startWiretap(ServerPlayer hacker, ServerPlayer target, double cutShare, double catchChance,
            long durationMillis, long targetCooldownMillis) {
        if (hacker.getUUID().equals(target.getUUID())) {
            return "Tu ne peux pas te pirater toi-même.";
        }
        Long targetCd = this.wiretapTargetCooldownUntil.get(target.getUUID());
        if (targetCd != null && targetCd > System.currentTimeMillis()) {
            return target.getGameProfile().name() + " a été ciblé récemment, réessaie plus tard.";
        }
        if (this.activeWiretaps.containsKey(target.getUUID())) {
            return target.getGameProfile().name() + " est déjà sous écoute.";
        }
        long expiresAt = System.currentTimeMillis() + durationMillis;
        this.activeWiretaps.put(target.getUUID(),
                new Wiretap(target.getUUID(), hacker.getUUID(), cutShare, catchChance, expiresAt));
        this.wiretapTargetCooldownUntil.put(target.getUUID(), expiresAt + targetCooldownMillis);
        return null;
    }

    // Rend "hacker" temporairement invisible a la detection de manipulation de marche
    public void scrambleTrace(UUID hacker, long durationMillis) {
        this.traceImmuneUntil.put(hacker, System.currentTimeMillis() + durationMillis);
    }

    // Injecte une fausse activite massive sur "item" pour en pousser le prix (pump si up=true,
    // sinon dump). Comme une vraie manipulation, ca peut declencher la detection au prochain tick
    // sauf si la trace a ete brouillee juste avant
    public void hackPrice(UUID hacker, MarketItem item, boolean up, long fakeVolume) {
        if (up) {
            item.registerBuy(fakeVolume);
            this.totalBought += fakeVolume;
        } else {
            item.registerSell(fakeVolume);
            this.totalSold += fakeVolume;
        }
        this.trackPlayerActivity(hacker, item.getItem(), up ? fakeVolume : 0L, up ? 0L : fakeVolume);
    }

    // "Vole" les infos d'initie sur un item : pression du cycle en cours (pas encore appliquee au
    // prix) + evenement de marche en cours sur sa categorie
    public String getInsiderReport(MarketItem item) {
        long bought = item.getRecentBought();
        long sold = item.getRecentSold();
        long net = bought - sold;
        String direction = net > 0 ? "§ahausse probable" : net < 0 ? "§cbaisse probable" : "§7stable";
        boolean eventBrewing = this.activeEvent != null
                && (this.activeEvent.getCategory() == null || this.activeEvent.getCategory() == item.getCategory());
        StringBuilder sb = new StringBuilder();
        sb.append("§5[Hack] §dRapport d'initié — §f").append(item.getDisplayName()).append("\n");
        sb.append("§7Prix actuel: §f").append(this.economyManager.format(item.getCurrentPrice()))
                .append(" §7| Tendance affichée: ").append(item.getTrendArrow()).append("\n");
        sb.append("§7Pression du cycle en cours (pas encore visible publiquement): §f").append(bought)
                .append(" achats / ").append(sold).append(" ventes → ").append(direction);
        if (eventBrewing) {
            sb.append("\n§c⚠ Un évènement de marché est actif sur cette catégorie.");
        }
        return sb.toString();
    }

    private void tickWiretaps(MinecraftServer server) {
        if (this.activeWiretaps.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        this.activeWiretaps.entrySet().removeIf(entry -> {
            Wiretap wiretap = entry.getValue();
            if (!wiretap.isExpired(now)) {
                return false;
            }
            ServerPlayer target = server.getPlayerList().getPlayer(wiretap.getTarget());
            if (target != null && !wiretap.isCaught() && wiretap.getTotalSkimmed() > 0.0) {
                target.sendSystemMessage(Component.literal("§7Tu remarques après coup une activité suspecte sur tes dernières ventes... (environ "
                        + this.economyManager.format(wiretap.getTotalSkimmed())
                        + " manquants, origine inconnue)"));
            }
            return true;
        });
    }

    private void tickBounties(MinecraftServer server) {
        if (this.activeBounties.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        this.activeBounties.entrySet().removeIf(entry -> {
            if (!entry.getValue().isExpired(now)) {
                return false;
            }
            ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
            String name = target != null ? target.getGameProfile().name() : "un joueur recherché";
            broadcast(server, "§6[Marché] §eLe contrat sur " + name + " a expiré.");
            return true;
        });
    }

    public void recalculateAll(MinecraftServer server) {
        Map<MarketCategory, long[]> categoryTotals = new EnumMap<>(MarketCategory.class);
        for (MarketItem item : this.items.values()) {
            long[] totals = categoryTotals.computeIfAbsent(item.getCategory(), c -> new long[2]);
            totals[0] += item.getRecentBought();
            totals[1] += item.getRecentSold();
            this.checkManipulation(item, server);
        }

        MarketItem biggestMoveItem = null;
        double biggestMoveChangePercent = 0.0;

        for (MarketItem item : this.items.values()) {
            long[] totals = categoryTotals.get(item.getCategory());
            long categoryActivity = totals[0] + totals[1];
            double categoryPressure = categoryActivity > 0L
                    ? (double) (totals[0] - totals[1]) / (double) categoryActivity
                    : 0.0;
            boolean underEvent = this.activeEvent != null
                    && (this.activeEvent.getCategory() == null || this.activeEvent.getCategory() == item.getCategory());
            double itemSensitivity = underEvent ? SENSITIVITY * EVENT_VOLATILITY_MULTIPLIER : SENSITIVITY;

            double priceBefore = item.getCurrentPrice();
            item.recalculatePrice(itemSensitivity, MIN_MULTIPLIER, MAX_MULTIPLIER, categoryPressure, CATEGORY_SENSITIVITY);
            double priceAfter = item.getCurrentPrice();
            double changePercent = priceBefore > 0.0 ? (priceAfter - priceBefore) / priceBefore * 100.0 : 0.0;
            if (Math.abs(changePercent) > Math.abs(biggestMoveChangePercent)) {
                biggestMoveChangePercent = changePercent;
                biggestMoveItem = item;
            }
        }

        this.tickEvent(server);
        this.generateMoveHeadline(biggestMoveItem, biggestMoveChangePercent);
        this.tickContracts();
        this.tickBounties(server);
        this.tickWiretaps(server);
        this.playerActivity.clear();

        this.previousIndexValue = this.lastIndexValue;
        this.lastIndexValue = this.computeIndex();
    }

    // --- journal boursier ---

    public List<String> getHeadlines() {
        return this.headlines;
    }

    private void pushHeadline(String headline) {
        this.headlines.addFirst(headline);
        while (this.headlines.size() > HEADLINE_HISTORY_LENGTH) {
            this.headlines.removeLast();
        }
    }

    private void generateMoveHeadline(MarketItem item, double changePercent) {
        if (item == null || Math.abs(changePercent) < HEADLINE_THRESHOLD_PERCENT) {
            return;
        }
        String verb = changePercent >= 0 ? "grimpe" : "chute";
        String sign = changePercent >= 0 ? "+" : "";
        this.pushHeadline(item.getDisplayName() + " " + verb + " de " + sign + String.format("%.1f", changePercent) + "%");
    }

    // --- contrats a terme ---

    public String validateContractRequest(double stake, int minutes) {
        if (minutes < FUTURES_MIN_MINUTES || minutes > FUTURES_MAX_MINUTES) {
            return "Durée invalide (entre " + FUTURES_MIN_MINUTES + " et " + FUTURES_MAX_MINUTES + " minutes).";
        }
        if (stake < FUTURES_MIN_STAKE) {
            return "Mise minimale : " + FUTURES_MIN_STAKE;
        }
        return null;
    }

    public FuturesContract openContract(UUID creator, MarketItem item, FuturesContract.Type type, double stake, int minutes) {
        long maturity = System.currentTimeMillis() + minutes * 60_000L;
        UUID id = UUID.randomUUID();
        FuturesContract contract = new FuturesContract(id, creator, item.getItem(), type, stake, item.getCurrentPrice(), maturity);
        this.contracts.put(id, contract);
        return contract;
    }

    public FuturesContract getContract(UUID contractId) {
        return this.contracts.get(contractId);
    }

    // Retire le contrat et rend le paiement fige (0 si le contrat n'existe pas/plus)
    public double redeemContract(UUID contractId) {
        FuturesContract contract = this.contracts.remove(contractId);
        return contract != null ? contract.getLockedPayout() : 0.0;
    }

    // Fige le paiement au prix du marche au moment de l'echeance, mais ne paie personne : le
    // contrat est un instrument au porteur, seul l'encaissement (clic droit sur l'item) paie
    private void tickContracts() {
        if (this.contracts.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (FuturesContract contract : this.contracts.values()) {
            if (contract.isSettled() || !contract.isMatured(now)) {
                continue;
            }
            MarketItem item = this.items.get(contract.getItem());
            double priceAtMaturity = item != null ? item.getCurrentPrice() : contract.getPriceAtCreation();
            contract.settle(priceAtMaturity);
        }
    }

    private void tickEvent(MinecraftServer server) {
        if (this.activeEvent != null) {
            if (this.activeEvent.tick()) {
                String cause = this.activeEvent.getType() == MarketEvent.Type.CRASH ? "le krach" : "la ruée";
                broadcast(server, "§6[Marché] §eLe marché se stabilise après " + cause + ".");
                this.activeEvent = null;
            }
            return;
        }
        if (!EVENTS_ENABLED || this.random.nextDouble() >= EVENT_CHECK_CHANCE) {
            return;
        }
        this.startRandomEvent(server);
    }

    private void startRandomEvent(MinecraftServer server) {
        List<MarketCategory> categories = this.items.values().stream()
                .map(MarketItem::getCategory)
                .distinct()
                .collect(Collectors.toList());
        if (categories.isEmpty()) {
            return;
        }

        MarketEvent.Type type = this.random.nextBoolean() ? MarketEvent.Type.BOOM : MarketEvent.Type.CRASH;
        MarketCategory targetCategory = this.random.nextDouble() < 0.2 ? null : categories.get(this.random.nextInt(categories.size()));
        double shock = type == MarketEvent.Type.BOOM ? BOOM_SHOCK : CRASH_SHOCK;

        for (MarketItem item : this.items.values()) {
            if (targetCategory == null || item.getCategory() == targetCategory) {
                item.applyShock(shock, MIN_MULTIPLIER, MAX_MULTIPLIER);
            }
        }

        this.activeEvent = new MarketEvent(type, targetCategory, EVENT_DURATION_CYCLES);
        String scope = targetCategory == null ? "tout le marché" : targetCategory.getDisplayName().toLowerCase();
        broadcast(server, buildEventMessage(type, scope));
    }

    private static String buildEventMessage(MarketEvent.Type type, String scope) {
        if (type == MarketEvent.Type.CRASH) {
            return "§c[Marché] §eKrach boursier ! Les prix de " + scope + " s'effondrent.";
        }
        return "§6[Marché] §eRuée sur le marché ! Les prix de " + scope + " s'envolent.";
    }

    private static void broadcast(MinecraftServer server, String message) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }
}
