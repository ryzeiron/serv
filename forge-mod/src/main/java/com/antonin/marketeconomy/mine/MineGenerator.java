package com.antonin.marketeconomy.mine;

import com.antonin.marketeconomy.registry.ModBlocks;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

// Genere procéduralement les mines "carrière à ciel ouvert" du métier Mineur : un pavé plein de
// pierre/deepslate 100x100 avec du minerai disperse selon le palier, une plateforme d'entrée
// avec 4 plaques de teleportation, un muret de securite et des torches. Portage direct de
// structures/build_mines.py (memes tables de minerai/densites/graines), mais en generation Java
// directe (Level#setBlock) plutot que via une structure NBT bundlee.
public final class MineGenerator {
    public static final int SIZE = 100;
    public static final int DEPTH = 20;
    public static final int HEIGHT = DEPTH + 4;

    public static final int PLATFORM_X0 = 44;
    public static final int PLATFORM_X1 = 56;
    public static final int PLATFORM_Z0 = 0;
    public static final int PLATFORM_Z1 = 5;
    public static final int[] PLATE_LOCAL_X = {47, 49, 51, 53};
    public static final int PLATE_LOCAL_Z = 2;

    private static final int SET_FLAGS = 2; // met a jour les clients sans recalculer les voisins (generation en masse)

    private MineGenerator() {
    }

    public static void generate(ServerLevel level, BlockPos origin, int tier) {
        long seed = seedFor(tier);
        List<OreWeight> ores = oresFor(tier);
        double density = densityFor(tier);
        int lithiumVeins = lithiumVeinsFor(tier);
        Random rng = new Random(seed);

        Set<Long> lithiumSpots = buildLithiumVeins(rng, lithiumVeins);
        BlockState lithiumOre = ModBlocks.LITHIUM_ORE.get().defaultBlockState();

        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                boolean platform = onPlatform(x, z);
                for (int y = 0; y < DEPTH; y++) {
                    BlockState state;
                    if (platform) {
                        state = stoneAt(x, y, z, seed);
                    } else if (lithiumSpots.contains(packPos(x, y, z))) {
                        state = lithiumOre;
                    } else if (rng.nextDouble() < density) {
                        state = pickOre(ores, rng);
                    } else {
                        state = stoneAt(x, y, z, seed);
                    }
                    level.setBlock(origin.offset(x, y, z), state, SET_FLAGS);
                }
            }
        }

        for (int x = 4; x < SIZE - 4; x += 8) {
            for (int z = 4; z < SIZE - 4; z += 8) {
                if (onPlatform(x, z)) {
                    continue;
                }
                level.setBlock(origin.offset(x, DEPTH, z), Blocks.TORCH.defaultBlockState(), SET_FLAGS);
            }
        }

        for (int x = 0; x < SIZE; x++) {
            level.setBlock(origin.offset(x, DEPTH, 0), Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState(), SET_FLAGS);
            level.setBlock(origin.offset(x, DEPTH, SIZE - 1), Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState(), SET_FLAGS);
        }
        for (int z = 0; z < SIZE; z++) {
            level.setBlock(origin.offset(0, DEPTH, z), Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState(), SET_FLAGS);
            level.setBlock(origin.offset(SIZE - 1, DEPTH, z), Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState(), SET_FLAGS);
        }

        for (int x = PLATFORM_X0; x <= PLATFORM_X1; x++) {
            for (int z = PLATFORM_Z0; z <= PLATFORM_Z1; z++) {
                level.setBlock(origin.offset(x, DEPTH - 1, z), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), SET_FLAGS);
            }
        }
        for (int px : PLATE_LOCAL_X) {
            level.setBlock(origin.offset(px, DEPTH - 1, PLATE_LOCAL_Z), Blocks.SEA_LANTERN.defaultBlockState(), SET_FLAGS);
            level.setBlock(origin.offset(px, DEPTH, PLATE_LOCAL_Z), Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.defaultBlockState(), SET_FLAGS);
        }
        level.setBlock(origin.offset(PLATFORM_X0 + 1, DEPTH, PLATFORM_Z1 - 1), Blocks.OAK_SIGN.defaultBlockState(), SET_FLAGS);
        for (int x : new int[] {PLATFORM_X0, PLATFORM_X1}) {
            level.setBlock(origin.offset(x, DEPTH, PLATFORM_Z0), Blocks.IRON_CHAIN.defaultBlockState(), SET_FLAGS);
            level.setBlock(origin.offset(x, DEPTH + 1, PLATFORM_Z0), Blocks.IRON_CHAIN.defaultBlockState(), SET_FLAGS);
        }
    }

    private static Set<Long> buildLithiumVeins(Random rng, int lithiumVeins) {
        Set<Long> spots = new HashSet<>();
        int veinsPlaced = 0;
        int attempts = 0;
        while (veinsPlaced < lithiumVeins && attempts < lithiumVeins * 30) {
            attempts++;
            int lx = 5 + rng.nextInt(Math.max(1, SIZE - 11));
            int lz = 6 + rng.nextInt(Math.max(1, SIZE - 12));
            int lowY = DEPTH / 2;
            int highY = DEPTH - 2;
            int ly = lowY + rng.nextInt(Math.max(1, highY - lowY + 1));
            if (onPlatform(lx, lz)) {
                continue;
            }
            int veinSize = 3 + rng.nextInt(3);
            int cx = lx;
            int cy = ly;
            int cz = lz;
            for (int i = 0; i < veinSize; i++) {
                if (cx >= 0 && cx < SIZE && cy >= 0 && cy < DEPTH && cz >= 0 && cz < SIZE && !onPlatform(cx, cz)) {
                    spots.add(packPos(cx, cy, cz));
                }
                cx += rng.nextInt(3) - 1;
                cy += rng.nextInt(3) - 1;
                cz += rng.nextInt(3) - 1;
            }
            veinsPlaced++;
        }
        return spots;
    }

    private static boolean onPlatform(int x, int z) {
        return x >= PLATFORM_X0 && x <= PLATFORM_X1 && z >= PLATFORM_Z0 && z <= PLATFORM_Z1;
    }

    private static long packPos(int x, int y, int z) {
        return ((long) x & 0xFFFFFL) << 40 | ((long) y & 0xFFFFFL) << 20 | (long) z & 0xFFFFFL;
    }

    private static BlockState stoneAt(int x, int y, int z, long seed) {
        long h = Math.floorMod(x * 92837L + y * 12841L + z * 5483L + seed, 100L);
        if (h < 40) {
            return h % 2 == 0 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
        }
        return h % 3 == 0 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }

    private static BlockState pickOre(List<OreWeight> ores, Random rng) {
        int total = 0;
        for (OreWeight ore : ores) {
            total += ore.weight;
        }
        int roll = rng.nextInt(total);
        int acc = 0;
        for (OreWeight ore : ores) {
            acc += ore.weight;
            if (roll < acc) {
                return ore.state;
            }
        }
        return ores.get(ores.size() - 1).state;
    }

    private static long seedFor(int tier) {
        return switch (tier) {
            case 1 -> 101L;
            case 2 -> 202L;
            case 3 -> 303L;
            case 4 -> 404L;
            default -> 1L;
        };
    }

    private static double densityFor(int tier) {
        return switch (tier) {
            case 1 -> 0.06;
            case 2 -> 0.05;
            case 3 -> 0.035;
            case 4 -> 0.045;
            default -> 0.05;
        };
    }

    private static int lithiumVeinsFor(int tier) {
        return switch (tier) {
            case 3 -> 18;
            case 4 -> 24;
            default -> 0;
        };
    }

    private static List<OreWeight> oresFor(int tier) {
        return switch (tier) {
            case 1 -> List.of(
                    ore("coal_ore", 5), ore("iron_ore", 4), ore("gold_ore", 2),
                    ore("deepslate_coal_ore", 3), ore("deepslate_iron_ore", 2));
            case 2 -> List.of(
                    ore("copper_ore", 5), ore("lapis_ore", 3), ore("redstone_ore", 4),
                    ore("deepslate_copper_ore", 3), ore("deepslate_redstone_ore", 2));
            case 3 -> List.of(
                    ore("diamond_ore", 3), ore("deepslate_diamond_ore", 2),
                    ore("nether_quartz_ore", 4), ore("emerald_ore", 2), ore("deepslate_emerald_ore", 1));
            default -> List.of(
                    ore("coal_ore", 7), ore("iron_ore", 6), ore("gold_ore", 3),
                    ore("copper_ore", 6), ore("lapis_ore", 3), ore("redstone_ore", 4),
                    ore("diamond_ore", 2), ore("nether_quartz_ore", 3), ore("emerald_ore", 1),
                    ore("deepslate_diamond_ore", 1), ore("deepslate_emerald_ore", 1));
        };
    }

    private static OreWeight ore(String id, int weight) {
        var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.withDefaultNamespace(id));
        return new OreWeight(block.defaultBlockState(), weight);
    }

    private static final class OreWeight {
        private final BlockState state;
        private final int weight;

        private OreWeight(BlockState state, int weight) {
            this.state = state;
            this.weight = weight;
        }
    }
}
