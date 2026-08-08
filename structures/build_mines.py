"""Genere les 4 mines par palier du metier Mineur, directement dans
src/main/resources/structures/ (ressources embarquees dans le plugin,
posees via /marketadmin spawnmine <palier> plutot qu'a la main).

Palier 1 (niveau 1-5)  : charbon, fer, or
Palier 2 (niveau 6-10) : cuivre, lapis, redstone
Palier 3 (niveau 11-20): diamant, quartz, emeraude + minerai de lithium
Palier 4 (niveau 21-45): tous les minerais, probabilites variees + lithium

Le "minerai de lithium" est represente par un amas d'amethyste
(minecraft:amethyst_cluster) : un plugin listener le detecte et donne un
Lingot de Lithium garanti au lieu du drop vanilla.

Meme principe que les autres structures : coques creuses, StructureBuilder
eparse (seuls les blocs explicitement poses sont stockes).
"""
import sys

sys.path.insert(0, "/home/user/serv/structures")
from structure_lib import StructureBuilder, verify

import random

SIZE_X = 33
SIZE_Z = 33
HEIGHT = 20
ENTRANCE_X = SIZE_X // 2
ENTRANCE_Z = 1
FLOOR_Y = 1

DIRS = {
    "N": (0, -1),
    "S": (0, 1),
    "E": (1, 0),
    "W": (-1, 0),
}


def build_mine(seed, ore_table, lithium_count, out_path):
    rng = random.Random(seed)
    b = StructureBuilder(SIZE_X, HEIGHT, SIZE_Z)

    def in_bounds(x, y, z):
        return 0 <= x < SIZE_X and 0 <= y < HEIGHT and 0 <= z < SIZE_Z

    def set_block(x, y, z, name):
        if in_bounds(x, y, z):
            b.set_block(x, y, z, name)

    def clear_air(x, y, z):
        if in_bounds(x, y, z):
            b.blocks.pop((x, y, z), None)

    def stone_at(x, y, z):
        h = (x * 92837 + y * 12841 + z * 5483 + seed) % 100
        if h < 35:
            return "minecraft:deepslate" if h % 2 else "minecraft:cobbled_deepslate"
        return "minecraft:stone" if h % 3 else "minecraft:cobblestone"

    def ore_pocket(x, y, z, ore, count=5):
        placed = 0
        cx, cy, cz = x, y, z
        attempts = 0
        while placed < count and attempts < 40:
            attempts += 1
            set_block(cx, cy, cz, ore)
            placed += 1
            cx += rng.randint(-1, 1)
            cy += rng.randint(-1, 1)
            cz += rng.randint(-1, 1)

    def tunnel(x0, z0, direction, length, y0, w=3, h=3, torch_every=5):
        dx, dz = DIRS[direction]
        perp = (-dz, dx)
        half = w // 2
        for i in range(length):
            cx = x0 + dx * i
            cz = z0 + dz * i
            for p in range(-half, half + 1):
                wx = cx + perp[0] * p
                wz = cz + perp[1] * p
                for level in range(h + 2):
                    y = y0 + level - 1
                    is_floor = level == 0
                    is_ceiling = level == h + 1
                    is_wall = p in (-half, half)
                    if is_floor or is_ceiling or is_wall:
                        set_block(wx, y, wz, stone_at(wx, y, wz))
                    else:
                        clear_air(wx, y, wz)
            if torch_every and i % torch_every == 3:
                tx = cx + perp[0] * half
                tz = cz + perp[1] * half
                set_block(tx, y0, tz, "minecraft:torch")
            if rng.random() < 0.4:
                ore = rng.choices([o for o, _ in ore_table], weights=[wgt for _, wgt in ore_table])[0]
                ox = cx + perp[0] * rng.randint(-half, half)
                oz = cz + perp[1] * rng.randint(-half, half)
                oy = y0 + rng.randint(0, h - 1)
                ore_pocket(ox, oy, oz, ore, count=rng.randint(3, 6))
        return x0 + dx * length, z0 + dz * length

    def room(cx, cz, y0, size=7, h=5):
        half = size // 2
        for x in range(cx - half, cx + half + 1):
            for z in range(cz - half, cz + half + 1):
                edge = x in (cx - half, cx + half) or z in (cz - half, cz + half)
                for level in range(h + 2):
                    y = y0 + level - 1
                    if level == 0 or level == h + 1 or edge:
                        set_block(x, y, z, stone_at(x, y, z))
                    else:
                        clear_air(x, y, z)
        for y in range(y0, y0 + h - 1):
            set_block(cx - half, y, cz, "minecraft:torch")
            set_block(cx + half, y, cz, "minecraft:torch")
            set_block(cx, y, cz - half, "minecraft:torch")
            set_block(cx, y, cz + half, "minecraft:torch")
        return half

    # --------------------------------------------------- entree + galerie principale
    y0 = FLOOR_Y
    for x in range(ENTRANCE_X - 2, ENTRANCE_X + 3):
        for z in range(ENTRANCE_Z - 1, ENTRANCE_Z + 3):
            edge = x in (ENTRANCE_X - 2, ENTRANCE_X + 2) or z == ENTRANCE_Z + 2
            for level in range(6):
                y = y0 + level - 1
                if level == 0 or level == 5 or edge:
                    set_block(x, y, z, stone_at(x, y, z))
                else:
                    clear_air(x, y, z)
    set_block(ENTRANCE_X - 3, y0, ENTRANCE_Z, "minecraft:torch")
    set_block(ENTRANCE_X + 3, y0, ENTRANCE_Z, "minecraft:torch")
    set_block(ENTRANCE_X - 2, y0 + 1, ENTRANCE_Z + 1, "minecraft:oak_sign")

    hub_x, hub_z = ENTRANCE_X, ENTRANCE_Z + 4
    end_x, end_z = tunnel(ENTRANCE_X, ENTRANCE_Z + 3, "S", 4, y0, w=3, h=3, torch_every=0)
    room(hub_x, hub_z, y0, size=7, h=5)

    # 3 galeries partent de la salle centrale
    for direction in ("S", "E", "W"):
        dx, dz = DIRS[direction]
        sx = hub_x + dx * 4
        sz = hub_z + dz * 4
        ex, ez = tunnel(sx, sz, direction, 9, y0, w=3, h=3)
        room(ex + dx * 3, ez + dz * 3, y0, size=6, h=4)

    # filons de lithium (amethyst_cluster) disperses dans la salle centrale et les salles du fond
    lithium_spots = [
        (hub_x - 2, y0 + 1, hub_z - 1),
        (hub_x + 2, y0 + 2, hub_z + 1),
        (hub_x, y0 + 1, hub_z + 3 + 9 + 2),
    ]
    for i in range(lithium_count):
        x, y, z = lithium_spots[i % len(lithium_spots)]
        x += rng.randint(-1, 1)
        z += rng.randint(-1, 1)
        set_block(x, y, z, "minecraft:amethyst_cluster")

    # couverture rocheuse au-dessus des galeries (sauf au-dessus de l'entree, qui reste a ciel
    # ouvert pour qu'on puisse y entrer une fois la structure posee)
    cap_y = y0 + 6
    for x in range(SIZE_X):
        for z in range(SIZE_Z):
            near_entrance = ENTRANCE_X - 3 <= x <= ENTRANCE_X + 3 and z <= 4
            if near_entrance:
                continue
            set_block(x, cap_y, z, stone_at(x, cap_y, z))

    nblocks, npalette = b.save(out_path)
    info = verify(out_path)
    print(out_path, "->", nblocks, "blocs,", npalette, "etats de palette,", info["size"])


TIER1_ORES = [
    ("minecraft:coal_ore", 4), ("minecraft:iron_ore", 3), ("minecraft:gold_ore", 2),
    ("minecraft:deepslate_coal_ore", 2), ("minecraft:deepslate_iron_ore", 2),
]
TIER2_ORES = [
    ("minecraft:copper_ore", 4), ("minecraft:lapis_ore", 3), ("minecraft:redstone_ore", 3),
    ("minecraft:deepslate_copper_ore", 2), ("minecraft:deepslate_redstone_ore", 2),
]
TIER3_ORES = [
    ("minecraft:diamond_ore", 3), ("minecraft:deepslate_diamond_ore", 2),
    ("minecraft:nether_quartz_ore", 3), ("minecraft:emerald_ore", 2), ("minecraft:deepslate_emerald_ore", 1),
]
TIER4_ORES = [
    ("minecraft:coal_ore", 6), ("minecraft:iron_ore", 5), ("minecraft:gold_ore", 3),
    ("minecraft:copper_ore", 5), ("minecraft:lapis_ore", 3), ("minecraft:redstone_ore", 4),
    ("minecraft:diamond_ore", 2), ("minecraft:nether_quartz_ore", 3), ("minecraft:emerald_ore", 1),
    ("minecraft:deepslate_diamond_ore", 1), ("minecraft:deepslate_emerald_ore", 1),
]

OUT_DIR = "/home/user/serv/src/main/resources/structures"

build_mine(101, TIER1_ORES, 0, f"{OUT_DIR}/mine_tier1.nbt")
build_mine(202, TIER2_ORES, 0, f"{OUT_DIR}/mine_tier2.nbt")
build_mine(303, TIER3_ORES, 3, f"{OUT_DIR}/mine_tier3.nbt")
build_mine(404, TIER4_ORES, 4, f"{OUT_DIR}/mine_tier4.nbt")
