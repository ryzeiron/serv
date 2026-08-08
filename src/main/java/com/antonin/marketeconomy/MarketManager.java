package com.antonin.marketeconomy;

import com.antonin.marketeconomy.model.Bounty;
import com.antonin.marketeconomy.model.FuturesContract;
import com.antonin.marketeconomy.model.MarketCategory;
import com.antonin.marketeconomy.model.MarketEvent;
import com.antonin.marketeconomy.model.MarketItem;
import com.antonin.marketeconomy.model.Wiretap;
import com.antonin.marketeconomy.storage.EconomyHook;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MarketManager {
    private final Map<Material, MarketItem> items = new LinkedHashMap<>();
    private final double sensitivity;
    private final double minMultiplier;
    private final double maxMultiplier;
    private final double categorySensitivity;

    private final boolean eventsEnabled;
    private final double eventCheckChance;
    private final int eventDurationCycles;
    private final double crashShock;
    private final double boomShock;
    private final double eventVolatilityMultiplier;

    private final Random random = new Random();
    private MarketEvent activeEvent;

    private long totalBought = 0L;
    private long totalSold = 0L;
    private double totalSpent = 0.0;
    private double totalEarned = 0.0;
    private double previousIndexValue = 1.0;
    private double lastIndexValue = 1.0;

    private final EconomyHook economyHook;

    // --- Detection de manipulation ---
    private final boolean manipulationEnabled;
    private final long manipulationMinVolume;
    private final double manipulationShareThreshold;
    private final Map<UUID, Map<Material, long[]>> playerActivity = new HashMap<>();

    // --- Primes (contrats places sur un joueur repere pour manipulation) ---
    private final long bountyEligibleMillis;
    private final double bountyCutShare;
    private final long bountyDurationMillis;
    private final Map<UUID, Long> bountyEligibleUntil = new HashMap<>();
    private final Map<UUID, Bounty> activeBounties = new HashMap<>();

    // --- Journal boursier ---
    private final int headlineHistoryLength;
    private final double headlineThresholdPercent;
    private final LinkedList<String> headlines = new LinkedList<>();

    // --- Contrats a terme ---
    private final double futuresMinStake;
    private final double futuresMaxStake;
    private final int futuresMinMinutes;
    private final int futuresMaxMinutes;
    private final Map<UUID, FuturesContract> contracts = new HashMap<>();

    // --- Metier Hacker : ecoutes de ventes (wiretap) et brouillage de trace ---
    private final Map<UUID, Wiretap> activeWiretaps = new HashMap<>(); // cle = cible
    private final Map<UUID, Long> wiretapTargetCooldownUntil = new HashMap<>();
    private final Map<UUID, Long> traceImmuneUntil = new HashMap<>(); // cle = hacker

    public MarketManager(MarketEconomyPlugin plugin, EconomyHook economyHook) {
        this.economyHook = economyHook;
        FileConfiguration config = plugin.getConfig();
        this.sensitivity = config.getDouble("market-sensitivity", 0.02);
        this.minMultiplier = config.getDouble("min-price-multiplier", 0.2);
        this.maxMultiplier = config.getDouble("max-price-multiplier", 5.0);
        this.categorySensitivity = config.getDouble("category-sensitivity", 0.01);
        int historyLength = config.getInt("price-history-length", 50);

        this.eventsEnabled = config.getBoolean("events.enabled", true);
        this.eventCheckChance = config.getDouble("events.check-chance", 0.05);
        this.eventDurationCycles = Math.max(1, config.getInt("events.duration-cycles", 3));
        this.crashShock = config.getDouble("events.crash-shock", 0.75);
        this.boomShock = config.getDouble("events.boom-shock", 1.30);
        this.eventVolatilityMultiplier = config.getDouble("events.volatility-multiplier", 2.0);

        this.manipulationEnabled = config.getBoolean("manipulation.enabled", true);
        this.manipulationMinVolume = config.getLong("manipulation.min-volume", 40L);
        this.manipulationShareThreshold = config.getDouble("manipulation.share-threshold", 0.6);

        this.bountyEligibleMillis = config.getLong("manipulation.bounty-eligible-seconds", 300L) * 1000L;
        this.bountyCutShare = config.getDouble("manipulation.bounty-cut-share", 0.10);
        this.bountyDurationMillis = config.getLong("manipulation.bounty-duration-seconds", 1200L) * 1000L;

        this.headlineHistoryLength = Math.max(1, config.getInt("journal.history-length", 30));
        this.headlineThresholdPercent = config.getDouble("journal.headline-threshold-percent", 3.0);

        this.futuresMinStake = config.getDouble("futures.min-stake", 10.0);
        this.futuresMaxStake = config.getDouble("futures.max-stake", 0.0);
        this.futuresMinMinutes = Math.max(1, config.getInt("futures.min-minutes", 2));
        this.futuresMaxMinutes = Math.max(this.futuresMinMinutes, config.getInt("futures.max-minutes", 120));

        if (config.isConfigurationSection("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material == null) {
                    plugin.getLogger().warning("Materiau inconnu dans config.yml: " + key);
                    continue;
                }
                double basePrice = config.getDouble("items." + key + ".base-price", 10.0);
                long initialStock = config.getLong("items." + key + ".initial-stock", 100L);
                String displayName = config.getString("items." + key + ".display-name", MarketManager.defaultDisplayName(material));
                MarketCategory category = MarketCategory.fromConfig(config.getString("items." + key + ".category"));
                this.items.put(material, new MarketItem(material, displayName, category, basePrice, initialStock, historyLength));
            }
        }
        this.lastIndexValue = this.computeIndex();
        this.previousIndexValue = this.lastIndexValue;
    }

    private String formatAmount(double amount) {
        if (this.economyHook != null && this.economyHook.isEnabled()) {
            return this.economyHook.format(amount);
        }
        return String.valueOf(Math.round(amount * 100.0) / 100.0);
    }

    private static String defaultDisplayName(Material material) {
        String name = material.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public Map<Material, MarketItem> getItems() {
        return this.items;
    }

    public MarketItem getItem(Material material) {
        return this.items.get(material);
    }

    public boolean isTradable(Material material) {
        return this.items.containsKey(material);
    }

    public MarketEvent getActiveEvent() {
        return this.activeEvent;
    }

    public void recordPurchase(Player player, MarketItem item, long amount, double totalPrice) {
        item.registerBuy(amount);
        this.totalBought += amount;
        this.totalSpent += totalPrice;
        this.trackPlayerActivity(player, item.getMaterial(), amount, 0L);
    }

    public void recordSale(Player player, MarketItem item, long amount, double totalPrice) {
        item.registerSell(amount);
        this.totalSold += amount;
        this.totalEarned += totalPrice;
        this.trackPlayerActivity(player, item.getMaterial(), 0L, amount);
    }

    private void trackPlayerActivity(Player player, Material material, long bought, long sold) {
        Map<Material, long[]> perItem = this.playerActivity.computeIfAbsent(player.getUniqueId(), u -> new HashMap<>());
        long[] totals = perItem.computeIfAbsent(material, m -> new long[2]);
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

    // --- Manipulation de marche ---

    private void checkManipulation(MarketItem item) {
        if (!this.manipulationEnabled) {
            return;
        }
        long totalActivity = item.getRecentBought() + item.getRecentSold();
        if (totalActivity < this.manipulationMinVolume) {
            return;
        }
        Material material = item.getMaterial();
        for (Map.Entry<UUID, Map<Material, long[]>> entry : this.playerActivity.entrySet()) {
            long[] playerTotals = entry.getValue().get(material);
            if (playerTotals == null) {
                continue;
            }
            long playerAmount = playerTotals[0] + playerTotals[1];
            double share = (double) playerAmount / (double) totalActivity;
            if (share >= this.manipulationShareThreshold) {
                this.flagManipulation(entry.getKey(), item, share);
            }
        }
    }

    // Ne bloque plus le trading : rend simplement le joueur "primable" pendant une fenetre de temps
    private void flagManipulation(UUID uuid, MarketItem item, double share) {
        Long immuneUntil = this.traceImmuneUntil.get(uuid);
        if (immuneUntil != null && immuneUntil > System.currentTimeMillis()) {
            return;
        }
        this.bountyEligibleUntil.put(uuid, System.currentTimeMillis() + this.bountyEligibleMillis);
        Player player = Bukkit.getPlayer(uuid);
        String name = player != null ? player.getName() : "Un joueur";
        Bukkit.broadcastMessage("§4[Marché] §cActivité suspecte détectée sur " + item.getDisplayName()
                + " (" + Math.round(share * 100) + "% du volume) — " + name
                + " peut être ciblé par une prime (§7/prime " + name + "§c) pendant "
                + (this.bountyEligibleMillis / 1000L) + "s.");
        if (player != null) {
            player.sendMessage("§cTon activité sur le marché ressemble à de la manipulation de prix. "
                    + "Les autres joueurs peuvent placer une prime sur toi pendant "
                    + (this.bountyEligibleMillis / 1000L) + "s.");
        }
    }

    // --- Primes ---

    public boolean isBountyEligible(UUID uuid) {
        Long until = this.bountyEligibleUntil.get(uuid);
        return until != null && until > System.currentTimeMillis();
    }

    public Bounty getBounty(UUID targetUuid) {
        return this.activeBounties.get(targetUuid);
    }

    // Place une prime sur "target" si elle est actuellement primable et pas deja ciblee ; renvoie
    // un message d'erreur, ou null si la prime a bien ete posee
    public String placeBounty(Player placer, Player target) {
        if (placer.getUniqueId().equals(target.getUniqueId())) {
            return "Tu ne peux pas placer une prime sur toi-même.";
        }
        if (!this.isBountyEligible(target.getUniqueId())) {
            return target.getName() + " n'est pas actuellement recherché pour manipulation de marché.";
        }
        if (this.activeBounties.containsKey(target.getUniqueId())) {
            return "Un contrat est déjà actif sur " + target.getName() + ".";
        }
        long expiresAt = System.currentTimeMillis() + this.bountyDurationMillis;
        this.activeBounties.put(target.getUniqueId(), new Bounty(target.getUniqueId(), placer.getUniqueId(), this.bountyCutShare, expiresAt));
        this.bountyEligibleUntil.remove(target.getUniqueId());
        Bukkit.broadcastMessage("§4[Marché] §c" + placer.getName() + " place un contrat sur la tête de " + target.getName()
                + " ! " + Math.round(this.bountyCutShare * 100) + "% de ses ventes lui reviendront pendant "
                + (this.bountyDurationMillis / 60_000L) + " min.");
        return null;
    }

    // A appeler avant de crediter une vente : redirige la part de la prime puis celle d'une
    // eventuelle ecoute (wiretap) de Hacker, et renvoie ce qu'il reste a verser au vendeur
    public double applyBountyCut(Player seller, double saleAmount) {
        double remaining = saleAmount;

        Bounty bounty = this.activeBounties.get(seller.getUniqueId());
        if (bounty != null) {
            double cut = remaining * bounty.getCutShare();
            if (this.economyHook != null && this.economyHook.isEnabled() && cut > 0.0) {
                OfflinePlayer placer = Bukkit.getOfflinePlayer(bounty.getPlacer());
                this.economyHook.deposit(placer, cut);
                Player onlinePlacer = Bukkit.getPlayer(bounty.getPlacer());
                if (onlinePlacer != null) {
                    onlinePlacer.sendMessage("§6[Prime] §eTa cible " + seller.getName() + " a vendu — tu touches "
                            + this.economyHook.format(cut) + ".");
                }
            }
            remaining -= cut;
        }

        remaining = this.applyWiretapCut(seller, remaining);
        return remaining;
    }

    private double applyWiretapCut(Player seller, double remaining) {
        Wiretap wiretap = this.activeWiretaps.get(seller.getUniqueId());
        if (wiretap == null || this.economyHook == null || !this.economyHook.isEnabled()) {
            return remaining;
        }
        double cut = Math.round(remaining * wiretap.getCutShare() * 100.0) / 100.0;
        if (cut <= 0.0) {
            return remaining;
        }
        OfflinePlayer hackerOff = Bukkit.getOfflinePlayer(wiretap.getHacker());
        this.economyHook.deposit(hackerOff, cut);
        wiretap.addSkimmed(cut);
        Player hackerOnline = Bukkit.getPlayer(wiretap.getHacker());
        if (hackerOnline != null) {
            hackerOnline.sendMessage("§5[Hack] §dInterception sur " + seller.getName() + " : +"
                    + this.economyHook.format(cut) + ".");
        }
        if (!wiretap.isCaught() && this.random.nextDouble() < wiretap.getCatchChance()) {
            wiretap.setCaught(true);
            String hackerName = hackerOff.getName() != null ? hackerOff.getName() : "un joueur";
            seller.sendMessage("§c[!] Intrusion détectée sur tes ventes : " + hackerName
                    + " t'espionnait ! Tu peux le signaler avec §7/prime " + hackerName);
            this.bountyEligibleUntil.put(wiretap.getHacker(), System.currentTimeMillis() + this.bountyEligibleMillis);
            Bukkit.broadcastMessage("§4[Marché] §c" + hackerName + " a été repéré en train de pirater les ventes de "
                    + seller.getName() + " ! Une prime peut être placée (§7/prime " + hackerName + "§c).");
        }
        return remaining - cut;
    }

    // --- Metier Hacker ---

    // Pose une ecoute sur les ventes de "target" ; renvoie un message d'erreur, ou null si l'ecoute
    // a bien ete posee
    public String startWiretap(Player hacker, Player target, double cutShare, double catchChance,
            long durationMillis, long targetCooldownMillis) {
        if (hacker.getUniqueId().equals(target.getUniqueId())) {
            return "Tu ne peux pas te pirater toi-même.";
        }
        Long targetCd = this.wiretapTargetCooldownUntil.get(target.getUniqueId());
        if (targetCd != null && targetCd > System.currentTimeMillis()) {
            return target.getName() + " a été ciblé récemment, réessaie plus tard.";
        }
        if (this.activeWiretaps.containsKey(target.getUniqueId())) {
            return target.getName() + " est déjà sous écoute.";
        }
        long expiresAt = System.currentTimeMillis() + durationMillis;
        this.activeWiretaps.put(target.getUniqueId(),
                new Wiretap(target.getUniqueId(), hacker.getUniqueId(), cutShare, catchChance, expiresAt));
        this.wiretapTargetCooldownUntil.put(target.getUniqueId(), expiresAt + targetCooldownMillis);
        return null;
    }

    // Rend "hacker" temporairement invisible a la detection de manipulation de marche
    public void scrambleTrace(Player hacker, long durationMillis) {
        this.traceImmuneUntil.put(hacker.getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    // Injecte une fausse activite massive sur "item" pour en pousser le prix (pump si up=true,
    // sinon dump). Comme une vraie manipulation, ca peut declencher la detection au prochain tick
    // sauf si la trace a ete brouillee juste avant
    public void hackPrice(Player hacker, MarketItem item, boolean up, long fakeVolume) {
        if (up) {
            item.registerBuy(fakeVolume);
            this.totalBought += fakeVolume;
        } else {
            item.registerSell(fakeVolume);
            this.totalSold += fakeVolume;
        }
        this.trackPlayerActivity(hacker, item.getMaterial(), up ? fakeVolume : 0L, up ? 0L : fakeVolume);
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
        sb.append("§7Prix actuel: §f").append(this.formatAmount(item.getCurrentPrice()))
                .append(" §7| Tendance affichée: ").append(item.getTrendArrow()).append("\n");
        sb.append("§7Pression du cycle en cours (pas encore visible publiquement): §f").append(bought)
                .append(" achats / ").append(sold).append(" ventes → ").append(direction);
        if (eventBrewing) {
            sb.append("\n§c⚠ Un évènement de marché est actif sur cette catégorie.");
        }
        return sb.toString();
    }

    private void tickWiretaps() {
        if (this.activeWiretaps.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        this.activeWiretaps.entrySet().removeIf(entry -> {
            Wiretap wiretap = entry.getValue();
            if (!wiretap.isExpired(now)) {
                return false;
            }
            Player target = Bukkit.getPlayer(wiretap.getTarget());
            if (target != null && !wiretap.isCaught() && wiretap.getTotalSkimmed() > 0.0) {
                target.sendMessage("§7Tu remarques après coup une activité suspecte sur tes dernières ventes... (environ "
                        + this.formatAmount(wiretap.getTotalSkimmed())
                        + " manquants, origine inconnue)");
            }
            return true;
        });
    }

    private void tickBounties() {
        if (this.activeBounties.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        this.activeBounties.entrySet().removeIf(entry -> {
            if (!entry.getValue().isExpired(now)) {
                return false;
            }
            Player target = Bukkit.getPlayer(entry.getKey());
            String name = target != null ? target.getName() : "un joueur recherché";
            Bukkit.broadcastMessage("§6[Marché] §eLe contrat sur " + name + " a expiré.");
            return true;
        });
    }

    // --- Journal boursier ---

    public List<String> getHeadlines() {
        return this.headlines;
    }

    private void pushHeadline(String headline) {
        this.headlines.addFirst(headline);
        while (this.headlines.size() > this.headlineHistoryLength) {
            this.headlines.removeLast();
        }
    }

    private void generateMoveHeadline(MarketItem item, double changePercent) {
        if (item == null || Math.abs(changePercent) < this.headlineThresholdPercent) {
            return;
        }
        String verb = changePercent >= 0 ? "grimpe" : "chute";
        String sign = changePercent >= 0 ? "+" : "";
        this.pushHeadline(item.getDisplayName() + " " + verb + " de " + sign + String.format("%.1f", changePercent) + "%");
    }

    // --- Contrats a terme ---

    public String validateContractRequest(double stake, int minutes) {
        if (minutes < this.futuresMinMinutes || minutes > this.futuresMaxMinutes) {
            return "Duree invalide (entre " + this.futuresMinMinutes + " et " + this.futuresMaxMinutes + " minutes).";
        }
        if (stake < this.futuresMinStake) {
            return "Mise minimale : " + this.futuresMinStake;
        }
        if (this.futuresMaxStake > 0.0 && stake > this.futuresMaxStake) {
            return "Mise maximale : " + this.futuresMaxStake;
        }
        return null;
    }

    public FuturesContract openContract(Player player, MarketItem item, FuturesContract.Type type, double stake, int minutes) {
        long maturity = System.currentTimeMillis() + minutes * 60_000L;
        UUID id = UUID.randomUUID();
        FuturesContract contract = new FuturesContract(id, player.getUniqueId(), item.getMaterial(), type, stake, item.getCurrentPrice(), maturity);
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

    // Fige le paiement au prix du marche au moment de l'echeance, mais ne paie personne :
    // le contrat est un instrument au porteur, seul l'encaissement (clic droit sur l'item) paie
    private void tickContracts() {
        if (this.contracts.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (FuturesContract contract : this.contracts.values()) {
            if (contract.isSettled() || !contract.isMatured(now)) {
                continue;
            }
            MarketItem item = this.items.get(contract.getMaterial());
            double priceAtMaturity = item != null ? item.getCurrentPrice() : contract.getPriceAtCreation();
            contract.settle(priceAtMaturity);
        }
    }

    public void recalculateAll() {
        Map<MarketCategory, long[]> categoryTotals = new EnumMap<>(MarketCategory.class);
        for (MarketItem item : this.items.values()) {
            long[] totals = categoryTotals.computeIfAbsent(item.getCategory(), c -> new long[2]);
            totals[0] += item.getRecentBought();
            totals[1] += item.getRecentSold();
            this.checkManipulation(item);
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
            double itemSensitivity = underEvent ? this.sensitivity * this.eventVolatilityMultiplier : this.sensitivity;

            double priceBefore = item.getCurrentPrice();
            item.recalculatePrice(itemSensitivity, this.minMultiplier, this.maxMultiplier, categoryPressure, this.categorySensitivity);
            double priceAfter = item.getCurrentPrice();
            double changePercent = priceBefore > 0.0 ? (priceAfter - priceBefore) / priceBefore * 100.0 : 0.0;
            if (Math.abs(changePercent) > Math.abs(biggestMoveChangePercent)) {
                biggestMoveChangePercent = changePercent;
                biggestMoveItem = item;
            }
        }

        this.tickEvent();
        this.generateMoveHeadline(biggestMoveItem, biggestMoveChangePercent);
        this.tickContracts();
        this.tickBounties();
        this.tickWiretaps();
        this.playerActivity.clear();

        this.previousIndexValue = this.lastIndexValue;
        this.lastIndexValue = this.computeIndex();
    }

    private void tickEvent() {
        if (this.activeEvent != null) {
            if (this.activeEvent.tick()) {
                String cause = this.activeEvent.getType() == MarketEvent.Type.CRASH ? "le krach" : "la ruée";
                Bukkit.broadcastMessage("§6[Marché] §eLe marché se stabilise après " + cause + ".");
                this.pushHeadline("Le marché se stabilise après " + cause);
                this.activeEvent = null;
            }
            return;
        }
        if (!this.eventsEnabled || this.random.nextDouble() >= this.eventCheckChance) {
            return;
        }
        this.startRandomEvent();
    }

    private void startRandomEvent() {
        List<MarketCategory> categories = this.items.values().stream()
                .map(MarketItem::getCategory)
                .distinct()
                .collect(Collectors.toList());
        if (categories.isEmpty()) {
            return;
        }

        MarketEvent.Type type = this.random.nextBoolean() ? MarketEvent.Type.BOOM : MarketEvent.Type.CRASH;
        MarketCategory targetCategory = this.random.nextDouble() < 0.2 ? null : categories.get(this.random.nextInt(categories.size()));
        double shock = type == MarketEvent.Type.BOOM ? this.boomShock : this.crashShock;

        for (MarketItem item : this.items.values()) {
            if (targetCategory == null || item.getCategory() == targetCategory) {
                item.applyShock(shock, this.minMultiplier, this.maxMultiplier);
            }
        }

        this.activeEvent = new MarketEvent(type, targetCategory, this.eventDurationCycles);
        String scope = targetCategory == null ? "tout le marché" : targetCategory.getDisplayName().toLowerCase();
        Bukkit.broadcastMessage(this.buildEventMessage(type, scope));
        this.pushHeadline((type == MarketEvent.Type.CRASH ? "Krach sur " : "Ruée sur ") + scope + " !");
    }

    private String buildEventMessage(MarketEvent.Type type, String scope) {
        if (type == MarketEvent.Type.CRASH) {
            return "§c[Marché] §eKrach boursier ! Les prix de " + scope + " s'effondrent.";
        }
        return "§6[Marché] §eRuée sur le marché ! Les prix de " + scope + " s'envolent.";
    }
}
