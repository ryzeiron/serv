"""Genere les 4 mines par palier du metier Mineur, directement dans
src/main/resources/structures/ (ressources embarquees dans le plugin,
posees via /marketadmin spawnmine <palier>).

v2 : mines "carriere a ciel ouvert" de 100x100, un pave plein de
pierre/deepslate avec du minerai disperse selon le palier (pas des
galeries creuses) + une petite plateforme d'entree avec 4 plaques de
teleportation (une par palier) et un panneau d'infos. Les positions
locales des plaques (PLATE_LOCAL_X/Z, MINE_DEPTH) sont dupliquees cote
Java (MineManager) : si ces constantes changent ici, il faut les
reporter la-bas.

Palier 1 (niveau 1-5)  : charbon, fer, or
Palier 2 (niveau 6-10) : cuivre, lapis, redstone
Palier 3 (niveau 11-20): diamant, quartz, emeraude + minerai de lithium
Palier 4 (niveau 21-45): tous les minerais, probabilites variees + lithium

Le "minerai de lithium" est un bloc de calcite (minecraft:calcite) reskinne par le
resource pack (cube simple, sans risque de deformation du modele) ;
un listener cote plugin le detecte et donne un Lingot de Lithium garanti.
"""
import random
import sys

sys.path.insert(0, "/home/user/serv/structures")
from structure_lib import StructureBuilder, verify

SIZE = 100
DEPTH = 20  # nombre de couches pleines (y locaux 0..DEPTH-1) ; doit matcher MineManager.MINE_DEPTH
HEIGHT = DEPTH + 4

# Plateforme d'entree : zone reservee (jamais de minerai), plaques de teleport au sommet
PLATFORM_X0, PLATFORM_X1 = 44, 56
PLATFORM_Z0, PLATFORM_Z1 = 0, 5
PLATE_LOCAL_X = [47, 49, 51, 53]  # doit matcher MineManager.PLATE_LOCAL_X
PLATE_LOCAL_Z = 2  # doit matcher MineManager.PLATE_LOCAL_Z

OUT_DIR = "/home/user/serv/src/main/resources/structures"


def build_mine(seed, ore_table, ore_density, lithium_count, out_path):
    rng = random.Random(seed)
    b = StructureBuilder(SIZE, HEIGHT, SIZE)

    def in_bounds(x, y, z):
        return 0 <= x < SIZE and 0 <= y < HEIGHT and 0 <= z < SIZE

    def set_block(x, y, z, name):
        if in_bounds(x, y, z):
            b.set_block(x, y, z, name)

    def on_platform(x, z):
        return PLATFORM_X0 <= x <= PLATFORM_X1 and PLATFORM_Z0 <= z <= PLATFORM_Z1

    def stone_at(x, y, z):
        h = (x * 92837 + y * 12841 + z * 5483 + seed) % 100
        if h < 40:
            return "minecraft:deepslate" if h % 2 else "minecraft:cobbled_deepslate"
        return "minecraft:stone" if h % 3 else "minecraft:cobblestone"

    print(f"[{out_path}] remplissage {SIZE}x{DEPTH}x{SIZE}...")
    # Le lithium est place en petits filons (pas des blocs isoles) et plutot dans la moitie
    # haute du gisement, pour rester trouvable en creusant normalement plutot que perdu au
    # hasard dans 200 000 blocs
    lithium_spots = set()
    veins_placed = 0
    attempts = 0
    while veins_placed < lithium_count and attempts < lithium_count * 30:
        attempts += 1
        lx = rng.randint(5, SIZE - 6)
        lz = rng.randint(6, SIZE - 6)
        ly = rng.randint(max(2, DEPTH // 2), DEPTH - 2)
        if on_platform(lx, lz):
            continue
        vein_size = rng.randint(3, 5)
        cx, cy, cz = lx, ly, lz
        for _ in range(vein_size):
            if 0 <= cx < SIZE and 0 <= cy < DEPTH and 0 <= cz < SIZE and not on_platform(cx, cz):
                lithium_spots.add((cx, cy, cz))
            cx += rng.randint(-1, 1)
            cy += rng.randint(-1, 1)
            cz += rng.randint(-1, 1)
        veins_placed += 1

    for x in range(SIZE):
        for z in range(SIZE):
            platform = on_platform(x, z)
            for y in range(DEPTH):
                if platform:
                    set_block(x, y, z, stone_at(x, y, z))
                    continue
                if (x, y, z) in lithium_spots:
                    set_block(x, y, z, "minecraft:calcite")
                elif rng.random() < ore_density:
                    ore = rng.choices([o for o, _ in ore_table], weights=[wgt for _, wgt in ore_table])[0]
                    set_block(x, y, z, ore)
                else:
                    set_block(x, y, z, stone_at(x, y, z))
        if x % 20 == 0:
            print(f"  ... x={x}, blocs={len(b.blocks)}")

    # torches sur la surface pour eviter que ce soit totalement plonge dans le noir
    for x in range(4, SIZE - 4, 8):
        for z in range(4, SIZE - 4, 8):
            if on_platform(x, z):
                continue
            set_block(x, DEPTH, z, "minecraft:torch")

    # muret de securite tout autour du sommet de la carriere
    for x in range(SIZE):
        set_block(x, DEPTH, 0, "minecraft:cobbled_deepslate_wall")
        set_block(x, DEPTH, SIZE - 1, "minecraft:cobbled_deepslate_wall")
    for z in range(SIZE):
        set_block(0, DEPTH, z, "minecraft:cobbled_deepslate_wall")
        set_block(SIZE - 1, DEPTH, z, "minecraft:cobbled_deepslate_wall")

    # plateforme d'entree : dalle propre + 4 plaques de teleportation eclairees + panneau
    for x in range(PLATFORM_X0, PLATFORM_X1 + 1):
        for z in range(PLATFORM_Z0, PLATFORM_Z1 + 1):
            set_block(x, DEPTH - 1, z, "minecraft:polished_blackstone")
    for i, px in enumerate(PLATE_LOCAL_X):
        set_block(px, DEPTH - 1, PLATE_LOCAL_Z, "minecraft:sea_lantern")
        set_block(px, DEPTH, PLATE_LOCAL_Z, "minecraft:polished_blackstone_pressure_plate")
    set_block(PLATFORM_X0 + 1, DEPTH, PLATFORM_Z1 - 1, "minecraft:oak_sign")
    for x in (PLATFORM_X0, PLATFORM_X1):
        for y in (DEPTH, DEPTH + 1):
            set_block(x, y, PLATFORM_Z0, "minecraft:chain")

    nblocks, npalette = b.save(out_path)
    info = verify(out_path)
    print(out_path, "->", nblocks, "blocs,", npalette, "etats de palette,", info["size"])


TIER1_ORES = [
    ("minecraft:coal_ore", 5), ("minecraft:iron_ore", 4), ("minecraft:gold_ore", 2),
    ("minecraft:deepslate_coal_ore", 3), ("minecraft:deepslate_iron_ore", 2),
]
TIER2_ORES = [
    ("minecraft:copper_ore", 5), ("minecraft:lapis_ore", 3), ("minecraft:redstone_ore", 4),
    ("minecraft:deepslate_copper_ore", 3), ("minecraft:deepslate_redstone_ore", 2),
]
TIER3_ORES = [
    ("minecraft:diamond_ore", 3), ("minecraft:deepslate_diamond_ore", 2),
    ("minecraft:nether_quartz_ore", 4), ("minecraft:emerald_ore", 2), ("minecraft:deepslate_emerald_ore", 1),
]
TIER4_ORES = [
    ("minecraft:coal_ore", 7), ("minecraft:iron_ore", 6), ("minecraft:gold_ore", 3),
    ("minecraft:copper_ore", 6), ("minecraft:lapis_ore", 3), ("minecraft:redstone_ore", 4),
    ("minecraft:diamond_ore", 2), ("minecraft:nether_quartz_ore", 3), ("minecraft:emerald_ore", 1),
    ("minecraft:deepslate_diamond_ore", 1), ("minecraft:deepslate_emerald_ore", 1),
]

build_mine(101, TIER1_ORES, 0.06, 0, f"{OUT_DIR}/mine_tier1.nbt")
build_mine(202, TIER2_ORES, 0.05, 0, f"{OUT_DIR}/mine_tier2.nbt")
build_mine(303, TIER3_ORES, 0.035, 18, f"{OUT_DIR}/mine_tier3.nbt")
build_mine(404, TIER4_ORES, 0.045, 24, f"{OUT_DIR}/mine_tier4.nbt")
