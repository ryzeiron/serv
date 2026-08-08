package com.antonin.marketeconomy.villager;

import com.antonin.marketeconomy.market.MarketCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.registries.ForgeRegistries;

// Association metier de villageois -> categorie du marche, identique au plugin Paper (les
// identifiants vanilla des metiers de villageois n'ont pas change).
public final class VillagerTrade {

    private VillagerTrade() {
    }

    public static MarketCategory professionCategory(VillagerProfession profession) {
        return switch (professionId(profession)) {
            case "farmer", "fisherman", "butcher" -> MarketCategory.CONSUMABLES;
            case "toolsmith", "weaponsmith", "armorer" -> MarketCategory.ORES;
            case "cartographer", "librarian", "cleric" -> MarketCategory.RARE;
            case "leatherworker", "shepherd", "mason" -> MarketCategory.RAW_MATERIALS;
            case "fletcher" -> MarketCategory.MOB_DROPS;
            default -> MarketCategory.OTHER;
        };
    }

    public static String villagerLabel(VillagerProfession profession) {
        return switch (professionId(profession)) {
            case "farmer" -> "Le Fermier";
            case "fisherman" -> "Le Pêcheur";
            case "butcher" -> "Le Boucher";
            case "toolsmith" -> "L'Outilleur";
            case "weaponsmith" -> "L'Armurier";
            case "armorer" -> "Le Forgeron";
            case "cartographer" -> "Le Cartographe";
            case "librarian" -> "Le Bibliothécaire";
            case "cleric" -> "Le Clerc";
            case "leatherworker" -> "Le Tanneur";
            case "shepherd" -> "Le Berger";
            case "mason" -> "Le Maçon";
            case "fletcher" -> "Le Fléchier";
            default -> "Le Villageois";
        };
    }

    private static String professionId(VillagerProfession profession) {
        ResourceLocation id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(profession);
        return id != null ? id.getPath() : "";
    }
}
