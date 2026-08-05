package com.antonin.marketeconomy.commands;

import com.antonin.marketeconomy.MarketEconomyPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class JournalCommand implements CommandExecutor {
    private static final int HEADLINES_PER_PAGE = 6;

    private final MarketEconomyPlugin plugin;

    public JournalCommand(MarketEconomyPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Cette commande est reservee aux joueurs.");
            return true;
        }
        Player player = (Player) sender;

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle("Le Journal du Marché");
            meta.setAuthor("La Bourse");
            meta.setPages(this.buildPages());
            book.setItemMeta(meta);
        }

        player.getInventory().addItem(book);
        player.sendMessage("§6[Marché] §eTu reçois la dernière édition du Journal du Marché.");
        return true;
    }

    private List<String> buildPages() {
        List<String> headlines = this.plugin.getMarketManager().getHeadlines();
        List<String> pages = new ArrayList<>();
        StringBuilder page = new StringBuilder();
        page.append("§l§0Édition du jour\n\n");

        if (headlines.isEmpty()) {
            page.append("§0Aucune actualité notable pour l'instant.\n\nReviens après le prochain recalcul des prix !");
            pages.add(page.toString());
            return pages;
        }

        int count = 0;
        for (String headline : headlines) {
            page.append("§0• ").append(headline).append("\n\n");
            count++;
            if (count % HEADLINES_PER_PAGE == 0) {
                pages.add(page.toString());
                page = new StringBuilder();
            }
        }
        if (page.length() > 0) {
            pages.add(page.toString());
        }
        return pages;
    }
}
