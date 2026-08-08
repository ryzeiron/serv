package com.antonin.marketeconomy.gui;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;

// Menu generique "coffre" reutilisable pour tous les ecrans en lecture seule du mod (marche,
// menu metier, terminal Hacker...) : une grille d'icones cliquables au-dessus de l'inventaire du
// joueur. Sous-classe ChestMenu directement plutot que d'enregistrer un MenuType/Screen custom :
// en passant un MenuType.GENERIC_9xN vanilla au constructeur, le client utilise automatiquement
// l'ecran de coffre vanilla (deja enregistre par le jeu), donc aucun code client n'est necessaire.
public class DisplayMenu extends ChestMenu {

    @FunctionalInterface
    public interface ClickHandler {
        void onClick(int slot, int button, ClickType clickType, ServerPlayer player);
    }

    private final ClickHandler clickHandler;
    private final int topSize;

    private DisplayMenu(int containerId, Inventory playerInventory, SimpleContainer container, int rows, ClickHandler clickHandler) {
        super(menuTypeForRows(rows), containerId, playerInventory, container, rows);
        this.clickHandler = clickHandler;
        this.topSize = rows * 9;
    }

    private static MenuType<?> menuTypeForRows(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < this.topSize) {
            if (this.clickHandler != null && player instanceof ServerPlayer serverPlayer) {
                this.clickHandler.onClick(slotId, dragType, clickType, serverPlayer);
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public static void open(ServerPlayer player, Component title, List<ItemStack> icons, int rows, ClickHandler clickHandler) {
        int size = rows * 9;
        SimpleContainer container = new SimpleContainer(size);
        for (int i = 0; i < icons.size() && i < size; i++) {
            container.setItem(i, icons.get(i));
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new DisplayMenu(containerId, inventory, container, rows, clickHandler),
                title));
    }
}
