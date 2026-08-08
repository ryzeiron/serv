package com.antonin.marketeconomy.command;

import com.antonin.marketeconomy.job.JobType;
import com.antonin.marketeconomy.registry.ModItems;
import com.antonin.marketeconomy.server.MarketEconomyServer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

// /marketadmin : utilitaires de test reserves aux admins (permission niveau 2), pour donner de
// l'argent/xp de metier/items custom sans avoir a farmer. Contrairement au plugin Paper,
// "clearzone" n'est pas porte : la commande vanilla /fill couvre deja ce besoin sous Forge.
public final class MarketAdminCommand {

    private MarketAdminCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("marketadmin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("givemoney")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                        .executes(MarketAdminCommand::runGiveMoney))))
                .then(Commands.literal("givexp")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("hacker")
                                        .then(Commands.argument("amount", StringArgumentType.word())
                                                .executes(ctx -> runGiveXp(ctx, JobType.HACKER))))
                                .then(Commands.literal("mineur")
                                        .then(Commands.argument("amount", StringArgumentType.word())
                                                .executes(ctx -> runGiveXp(ctx, JobType.MINEUR))))))
                .then(Commands.literal("giveitem")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("lithium")
                                        .executes(ctx -> runGiveItem(ctx, Kind.LITHIUM, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> runGiveItem(ctx, Kind.LITHIUM, IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("plastique")
                                        .executes(ctx -> runGiveItem(ctx, Kind.PLASTIC, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> runGiveItem(ctx, Kind.PLASTIC, IntegerArgumentType.getInteger(ctx, "amount")))))
                                .then(Commands.literal("ordinateur")
                                        .executes(ctx -> runGiveItem(ctx, Kind.ORDINATEUR, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> runGiveItem(ctx, Kind.ORDINATEUR, IntegerArgumentType.getInteger(ctx, "amount"))))))));
    }

    private static int runGiveMoney(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        var economy = MarketEconomyServer.get().getEconomyManager();
        economy.deposit(target.getUUID(), amount);
        ctx.getSource().sendSuccess(() -> Component.literal("§6[Marché] §aDonné " + economy.format(amount) + " à " + target.getGameProfile().getName() + "."), true);
        target.sendSystemMessage(Component.literal("§6[Marché] §eTu as reçu " + economy.format(amount) + " d'un admin."));
        return 1;
    }

    private static int runGiveXp(CommandContext<CommandSourceStack> ctx, JobType type) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        String raw = StringArgumentType.getString(ctx, "amount");
        var jobManager = MarketEconomyServer.get().getJobManager();
        if (!jobManager.hasJob(target.getUUID(), type)) {
            jobManager.setJob(target.getUUID(), type);
        }
        if (raw.equalsIgnoreCase("max")) {
            jobManager.setLevel(target.getUUID(), type, type.getMaxLevel());
            ctx.getSource().sendSuccess(() -> Component.literal("§6[Marché] §a" + target.getGameProfile().getName()
                    + " est maintenant " + type.getDisplayName() + " §aniveau max (" + type.getMaxLevel() + ")."), true);
            target.sendSystemMessage(Component.literal("§6[Métier] §eTon niveau " + type.getDisplayName() + " §eest maintenant au maximum."));
            return 1;
        }
        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            ctx.getSource().sendFailure(Component.literal("§6[Marché] §cMontant invalide (nombre ou \"max\")."));
            return 0;
        }
        jobManager.addXp(target.getUUID(), type, amount);
        ctx.getSource().sendSuccess(() -> Component.literal("§6[Marché] §aDonné " + amount + " xp " + type.getDisplayName()
                + " §aà " + target.getGameProfile().getName() + "."), true);
        return 1;
    }

    private enum Kind { LITHIUM, PLASTIC, ORDINATEUR }

    private static int runGiveItem(CommandContext<CommandSourceStack> ctx, Kind kind, int amount) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        ItemStack item = switch (kind) {
            case LITHIUM -> new ItemStack(ModItems.LITHIUM_INGOT.get(), amount);
            case PLASTIC -> new ItemStack(ModItems.PLASTIC.get(), amount);
            case ORDINATEUR -> new ItemStack(ModItems.ORDINATEUR.get(), amount);
        };
        target.getInventory().add(item);
        String label = kind.name().toLowerCase();
        ctx.getSource().sendSuccess(() -> Component.literal("§6[Marché] §aDonné " + amount + "x " + label + " à " + target.getGameProfile().getName() + "."), true);
        target.sendSystemMessage(Component.literal("§6[Marché] §eTu as reçu " + amount + "x " + label + " d'un admin."));
        return 1;
    }
}
