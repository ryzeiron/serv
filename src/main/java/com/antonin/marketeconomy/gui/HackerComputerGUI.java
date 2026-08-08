package com.antonin.marketeconomy.gui;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import com.antonin.marketeconomy.model.MarketItem;
import com.antonin.marketeconomy.model.PlayerJob;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

// Interface complete du metier Hacker, accessible en clic droit sur le bloc Ordinateur : evite
// d'avoir a taper des noms de materiau (minecraft:...) en ligne de commande, tout se choisit
// en cliquant (items du marche affiches avec leur vrai nom, joueurs en ligne via leur tete)
public final class HackerComputerGUI {
    private HackerComputerGUI() {
    }

    public static void openMain(MarketEconomyPlugin plugin, Player player) {
        HackerComputerHolder holder = new HackerComputerHolder(HackerComputerHolder.Mode.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 27, "§b§lOrdinateur — Hacker");
        holder.setInventory(inventory);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        PlayerJob job = plugin.getJobManager().getJob(player.getUniqueId());
        int level = job != null ? job.getLevel() : 1;
        inventory.setItem(4, namedItem(Material.NETHER_STAR, "§b§lOrdinateur",
                new String[]{"§7Métier: §5Hacker", "§7Niveau: §f" + level}));

        inventory.setItem(11, namedItem(Material.PAPER, "§eRapport de marché",
                new String[]{"§7Voir la pression du cycle en cours", "§7sur un item, avant qu'elle soit publique."}));
        inventory.setItem(12, namedItem(Material.REDSTONE, "§cPirater un prix",
                new String[]{"§7Pousse le prix d'un item (pump/dump).", "§7Risqué : détectable comme une vraie manipulation."}));
        inventory.setItem(13, namedItem(Material.ENDER_EYE, "§5Brouiller ma trace",
                new String[]{"§7T'immunise temporairement contre", "§7la détection de manipulation."}));
        inventory.setItem(14, namedItem(Material.PLAYER_HEAD, "§dÉcoute (wiretap)",
                new String[]{"§7Intercepte un % des ventes d'un", "§7joueur en ligne pendant un temps."}));
        inventory.setItem(15, namedItem(Material.GOLD_BLOCK, "§6Pirater une banque",
                new String[]{"§7Tente de voler 30% du solde bancaire", "§7d'un joueur en ligne."}));
        inventory.setItem(16, namedItem(Material.COMPARATOR, "§bTerminal",
                new String[]{"§7Mini-jeu de piratage contre du loot."}));

        player.openInventory(inventory);
    }

    public static void openItemPicker(MarketEconomyPlugin plugin, Player player, HackerComputerHolder.Mode mode) {
        HackerComputerHolder holder = new HackerComputerHolder(mode);
        Inventory inventory = Bukkit.createInventory(holder, 36,
                mode == HackerComputerHolder.Mode.ITEM_PRICE ? "§cPirater un prix — choisis un item" : "§eRapport — choisis un item");
        holder.setInventory(inventory);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        List<MarketItem> items = new ArrayList<>(plugin.getMarketManager().getItems().values());
        int slot = 9;
        for (MarketItem item : items) {
            if (slot >= 27) {
                break;
            }
            List<String> lore = new ArrayList<>();
            lore.add("§7Prix actuel: §f" + round2(item.getCurrentPrice()));
            lore.add("§7Tendance: §f" + item.getTrendArrow());
            if (mode == HackerComputerHolder.Mode.ITEM_PRICE) {
                lore.add("");
                lore.add("§aClic gauche §7: pump (hausse)");
                lore.add("§cClic droit §7: dump (baisse)");
            } else {
                lore.add("");
                lore.add("§eClique §7pour voir le rapport");
            }
            inventory.setItem(slot, namedItem(item.getMaterial(), "§f" + item.getDisplayName(),
                    lore.toArray(new String[0])));
            holder.putItem(slot, item.getMaterial());
            slot++;
        }

        inventory.setItem(31, namedItem(Material.ARROW, "§7« Retour", null));
        player.openInventory(inventory);
    }

    public static void openPlayerPicker(MarketEconomyPlugin plugin, Player player, HackerComputerHolder.Mode mode) {
        HackerComputerHolder holder = new HackerComputerHolder(mode);
        Inventory inventory = Bukkit.createInventory(holder, 36,
                mode == HackerComputerHolder.Mode.PLAYER_BANK ? "§6Pirater une banque — choisis une cible" : "§dÉcoute — choisis une cible");
        holder.setInventory(inventory);

        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, "§7", null);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        int slot = 9;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            if (slot >= 27) {
                break;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta) {
                ((SkullMeta) meta).setOwningPlayer(online);
                meta.setDisplayName("§f" + online.getName());
                meta.setLore(java.util.Collections.singletonList("§eClique §7pour cibler"));
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
            holder.putPlayer(slot, online.getUniqueId());
            slot++;
        }

        if (slot == 9) {
            inventory.setItem(13, namedItem(Material.BARRIER, "§cAucun joueur en ligne", null));
        }

        inventory.setItem(31, namedItem(Material.ARROW, "§7« Retour", null));
        player.openInventory(inventory);
    }

    static ItemStack namedItem(Material material, String name, String[] lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(java.util.Arrays.asList(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
