package com.antonin.marketeconomy;

import com.antonin.marketeconomy.MarketManager;
import com.antonin.marketeconomy.commands.BankCommand;
import com.antonin.marketeconomy.commands.BuyCommand;
import com.antonin.marketeconomy.commands.FuturesCommand;
import com.antonin.marketeconomy.commands.HackCommand;
import com.antonin.marketeconomy.commands.HudCommand;
import com.antonin.marketeconomy.commands.IslandCommand;
import com.antonin.marketeconomy.commands.JobCommand;
import com.antonin.marketeconomy.commands.JournalCommand;
import com.antonin.marketeconomy.commands.MarketAdminCommand;
import com.antonin.marketeconomy.commands.MarketCommand;
import com.antonin.marketeconomy.commands.PrimeCommand;
import com.antonin.marketeconomy.commands.PvpCommand;
import com.antonin.marketeconomy.commands.SellCommand;
import com.antonin.marketeconomy.commands.SetHubCommand;
import com.antonin.marketeconomy.commands.SetPvpCommand;
import com.antonin.marketeconomy.commands.SpawnCommand;
import com.antonin.marketeconomy.commands.SpecialItemCommand;
import com.antonin.marketeconomy.gui.HackTerminalListener;
import com.antonin.marketeconomy.gui.HackerComputerGUIListener;
import com.antonin.marketeconomy.gui.JobMenuListener;
import com.antonin.marketeconomy.gui.MarketGUIListener;
import com.antonin.marketeconomy.gui.VillagerInteractionListener;
import com.antonin.marketeconomy.items.CustomMaterials;
import com.antonin.marketeconomy.items.FuturesRedeemListener;
import com.antonin.marketeconomy.items.HackerComputerBlockListener;
import com.antonin.marketeconomy.items.HackerComputerItem;
import com.antonin.marketeconomy.items.LithiumMiningListener;
import com.antonin.marketeconomy.items.MerchantCompassTracker;
import com.antonin.marketeconomy.items.MineBlockListener;
import com.antonin.marketeconomy.items.MineTeleportListener;
import com.antonin.marketeconomy.items.PlasticFishingListener;
import com.antonin.marketeconomy.storage.EconomyHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class MarketEconomyPlugin
extends JavaPlugin {
    private MarketManager marketManager;
    private EconomyHook economyHook;
    private ReputationManager reputationManager;
    private WarpManager warpManager;
    private IslandManager islandManager;
    private JobManager jobManager;
    private BankManager bankManager;
    private HudManager hudManager;
    private HackerAbilityService hackerAbilityService;
    private MineManager mineManager;

    public void onEnable() {
        this.saveDefaultConfig();
        this.economyHook = new EconomyHook();
        if (!this.economyHook.setup(this)) {
            this.getLogger().warning("Vault ou un plugin d'economie n'a pas ete trouve. Les achats/ventes seront desactives tant que Vault + un plugin d'economie ne sont pas installes.");
        }
        this.marketManager = new MarketManager(this, this.economyHook);
        this.reputationManager = new ReputationManager(this);
        this.warpManager = new WarpManager(this);
        this.islandManager = new IslandManager(this);
        this.jobManager = new JobManager(this);
        this.bankManager = new BankManager(this);
        this.hudManager = new HudManager(this);
        this.hackerAbilityService = new HackerAbilityService(this);
        this.mineManager = new MineManager(this);

        this.getCommand("market").setExecutor((CommandExecutor)new MarketCommand(this));
        this.getCommand("buy").setExecutor((CommandExecutor)new BuyCommand(this));
        this.getCommand("sell").setExecutor((CommandExecutor)new SellCommand(this));
        this.getCommand("journal").setExecutor((CommandExecutor)new JournalCommand(this));
        this.getCommand("futures").setExecutor((CommandExecutor)new FuturesCommand(this));
        this.getCommand("marketitem").setExecutor((CommandExecutor)new SpecialItemCommand(this));
        this.getCommand("prime").setExecutor((CommandExecutor)new PrimeCommand(this));
        this.getCommand("spawn").setExecutor((CommandExecutor)new SpawnCommand(this));
        this.getCommand("pvp").setExecutor((CommandExecutor)new PvpCommand(this));
        this.getCommand("ile").setExecutor((CommandExecutor)new IslandCommand(this));
        this.getCommand("sethub").setExecutor((CommandExecutor)new SetHubCommand(this));
        this.getCommand("setpvp").setExecutor((CommandExecutor)new SetPvpCommand(this));
        this.getCommand("marketadmin").setExecutor((CommandExecutor)new MarketAdminCommand(this));
        this.getCommand("metier").setExecutor((CommandExecutor)new JobCommand(this));
        this.getCommand("banque").setExecutor((CommandExecutor)new BankCommand(this));
        this.getCommand("hack").setExecutor((CommandExecutor)new HackCommand(this));
        this.getCommand("hud").setExecutor((CommandExecutor)new HudCommand(this));

        Bukkit.getPluginManager().registerEvents((Listener)new MarketGUIListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new VillagerInteractionListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new FuturesRedeemListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new PlayerJoinListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new HackTerminalListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new HackerComputerGUIListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new HackerComputerBlockListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new JobMenuListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new LithiumMiningListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new PlasticFishingListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new MineBlockListener(this), (Plugin)this);
        Bukkit.getPluginManager().registerEvents((Listener)new MineTeleportListener(this), (Plugin)this);

        this.registerHackerComputerRecipe();

        long intervalTicks = this.getConfig().getLong("price-update-interval", 60L) * 20L;
        Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> this.marketManager.recalculateAll(), intervalTicks, intervalTicks);
        Bukkit.getScheduler().runTaskTimer((Plugin)this, new MerchantCompassTracker(this), 40L, 40L);
        Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> this.hudManager.refreshAll(), 40L, 40L);
        long mineRegenTicks = this.getConfig().getLong("mines.regen-minutes", 25L) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> this.mineManager.regenerateAll(), mineRegenTicks, mineRegenTicks);

        this.getLogger().info("MarketEconomy active avec " + this.marketManager.getItems().size() + " items echangeables.");
    }

    // Craft de l'Ordinateur du Hacker : 3 blocs de fer en bas, un Lingot de Lithium au centre,
    // du Plastique en haut
    private void registerHackerComputerRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(this, "hacker_computer");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, HackerComputerItem.create(this));
        recipe.shape("PPP", " L ", "III");
        recipe.setIngredient('P', new RecipeChoice.ExactChoice(CustomMaterials.createPlastic(this)));
        recipe.setIngredient('L', new RecipeChoice.ExactChoice(CustomMaterials.createLithiumIngot(this)));
        recipe.setIngredient('I', Material.IRON_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    public void onDisable() {
        if (this.reputationManager != null) {
            this.reputationManager.save();
        }
        if (this.jobManager != null) {
            this.jobManager.save();
        }
        if (this.bankManager != null) {
            this.bankManager.save();
        }
        if (this.hudManager != null) {
            this.hudManager.save();
        }
        this.getLogger().info("MarketEconomy desactive.");
    }

    public MarketManager getMarketManager() {
        return this.marketManager;
    }

    public EconomyHook getEconomyHook() {
        return this.economyHook;
    }

    public ReputationManager getReputationManager() {
        return this.reputationManager;
    }

    public WarpManager getWarpManager() {
        return this.warpManager;
    }

    public IslandManager getIslandManager() {
        return this.islandManager;
    }

    public JobManager getJobManager() {
        return this.jobManager;
    }

    public BankManager getBankManager() {
        return this.bankManager;
    }

    public HudManager getHudManager() {
        return this.hudManager;
    }

    public HackerAbilityService getHackerAbilityService() {
        return this.hackerAbilityService;
    }

    public MineManager getMineManager() {
        return this.mineManager;
    }
}
