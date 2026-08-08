package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.hacker.HackerAbilityService;
import com.antonin.marketeconomy.job.JobType;
import com.antonin.marketeconomy.job.PlayerJob;
import com.antonin.marketeconomy.market.MarketItem;
import com.antonin.marketeconomy.market.MarketManager;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

// Commandes du metier Hacker : /hack <capacite> ... et /prime <joueur>.
public final class HackCommands {

    private HackCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hack")
                .executes(HackCommands::runHelp)
                .then(Commands.literal("market")
                        .then(Commands.argument("item", StringArgumentType.string())
                                .executes(ctx -> runMarket(ctx, StringArgumentType.getString(ctx, "item")))))
                .then(Commands.literal("price")
                        .then(Commands.argument("item", StringArgumentType.string())
                                .then(Commands.literal("up").executes(ctx -> runPrice(ctx, StringArgumentType.getString(ctx, "item"), true)))
                                .then(Commands.literal("down").executes(ctx -> runPrice(ctx, StringArgumentType.getString(ctx, "item"), false)))))
                .then(Commands.literal("scramble").executes(HackCommands::runScramble))
                .then(Commands.literal("wiretap")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(HackCommands::runWiretap)))
                .then(Commands.literal("banque")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(HackCommands::runBankHack)))
                .then(Commands.literal("terminal").executes(HackCommands::runTerminal)));

        dispatcher.register(Commands.literal("prime")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(HackCommands::runPrime)));
    }

    private static ServerPlayer requireHacker(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerJob job = MarketEconomyServer.get().getJobManager().getJob(player.getUUID());
        if (job == null || job.getType() != JobType.HACKER) {
            player.sendSystemMessage(Component.literal("§5[Hack] §cIl faut être Hacker pour utiliser cette commande (§f/metier§c)."));
            return null;
        }
        return player;
    }

    private static int runHelp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = requireHacker(ctx);
        if (player == null) {
            return 0;
        }
        PlayerJob job = MarketEconomyServer.get().getJobManager().getJob(player.getUUID());
        player.sendSystemMessage(Component.literal("§5[Hack] §dNiveau " + job.getLevel() + " — commandes :"));
        player.sendSystemMessage(Component.literal("§7/hack market <item> §f- rapport d'initié sur un item"));
        player.sendSystemMessage(Component.literal("§7/hack price <item> <up|down> §f- piraté le prix (risqué, détectable)"));
        player.sendSystemMessage(Component.literal("§7/hack scramble §f- brouille ta trace face à la détection de manipulation"));
        player.sendSystemMessage(Component.literal("§7/hack wiretap <joueur> §f- intercepte un % de ses ventes pendant un temps"));
        player.sendSystemMessage(Component.literal("§7/hack banque <joueur> §f- tente de pirater sa banque d'île"));
        player.sendSystemMessage(Component.literal("§7/hack terminal §f- mini-jeu de piratage pour du loot"));
        return 1;
    }

    private static MarketItem resolveItem(ServerPlayer player, String arg) {
        MarketManager market = MarketEconomyServer.get().getMarketManager();
        for (MarketItem item : market.getItems().values()) {
            if (item.getDisplayName().equalsIgnoreCase(arg) || ForgeRegistries.ITEMS.getKey(item.getItem()).getPath().equalsIgnoreCase(arg)) {
                return item;
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(arg.contains(":") ? arg : "minecraft:" + arg);
        if (id != null && ForgeRegistries.ITEMS.containsKey(id)) {
            MarketItem item = market.getItem(ForgeRegistries.ITEMS.getValue(id));
            if (item != null) {
                return item;
            }
        }
        player.sendSystemMessage(Component.literal("§5[Hack] §cItem inconnu ou non échangeable sur le marché. "
                + "Utilise son nom affiché (ex: \"Blé\") ou son id (ex: wheat)."));
        return null;
    }

    private static int runMarket(CommandContext<CommandSourceStack> ctx, String itemArg) throws CommandSyntaxException {
        ServerPlayer player = requireHacker(ctx);
        if (player == null) {
            return 0;
        }
        MarketItem item = resolveItem(player, itemArg);
        if (item == null) {
            return 0;
        }
        MarketEconomyServer.get().getHackerAbilityService().marketReport(player, item);
        return 1;
    }

    private static int runPrice(CommandContext<CommandSourceStack> ctx, String itemArg, boolean up) throws CommandSyntaxException {
        ServerPlayer player = requireHacker(ctx);
        if (player == null) {
            return 0;
        }
        MarketItem item = resolveItem(player, itemArg);
        if (item == null) {
            return 0;
        }
        MarketEconomyServer.get().getHackerAbilityService().priceHack(player, item, up);
        return 1;
    }

    private static int runScramble(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = requireHacker(ctx);
        if (player == null) {
            return 0;
        }
        MarketEconomyServer.get().getHackerAbilityService().scramble(player);
        return 1;
    }

    private static int runWiretap(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = requireHacker(ctx);
        if (player == null) {
            return 0;
        }
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        MarketEconomyServer.get().getHackerAbilityService().wiretap(player, target);
        return 1;
    }

    private static int runBankHack(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = requireHacker(ctx);
        if (player == null) {
            return 0;
        }
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        MarketEconomyServer.get().getHackerAbilityService().bankHack(player, target);
        return 1;
    }

    private static int runTerminal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = requireHacker(ctx);
        if (player == null) {
            return 0;
        }
        MarketEconomyServer.get().getHackerAbilityService().openTerminal(player);
        return 1;
    }

    private static int runPrime(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        String error = MarketEconomyServer.get().getMarketManager().placeBounty(player, target);
        if (error != null) {
            player.sendSystemMessage(Component.literal("§c" + error));
        }
        return 1;
    }
}
