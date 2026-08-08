"""Genere mine.nbt : une mine artificielle complete (puits + galeries + filons de
minerai) a poser pres du spawn. Necessaire car le monde est un monde vide
(level-type=flat / biome the_void) : il n'y a naturellement aucune pierre ni
minerai nulle part, donc rien a miner sans une structure dediee.

Meme principe que les autres structures (structure_lib, StructureBuilder eparse,
coques creuses) : les galeries sont des coquilles (sol/plafond/murs poses en
pierre), l'interieur (l'air qu'on peut parcourir) n'est jamais rempli.
"""
import math
import random
import sys

sys.path.insert(0, "/home/user/serv/structures")
from structure_lib import StructureBuilder, verify

SIZE = 55
HEIGHT = 64
CX = SIZE // 2
CZ = SIZE // 2
GROUND_Y = 58

rng = random.Random(4242)

b = StructureBuilder(SIZE, HEIGHT, SIZE)


def in_bounds(x, y, z):
    return 0 <= x < SIZE and 0 <= y < HEIGHT and 0 <= z < SIZE


def set_block(x, y, z, name):
    if in_bounds(x, y, z):
        b.set_block(x, y, z, name)


def clear_air(x, y, z):
    if in_bounds(x, y, z):
        b.blocks.pop((x, y, z), None)


DIRS = {
    "N": (0, -1),
    "S": (0, 1),
    "E": (1, 0),
    "W": (-1, 0),
}


def stone_at(x, y, z):
    # Melange pierre / deepslate selon la profondeur, avec un peu de texture
    h = (x * 92837 + y * 12837 + z * 5483) % 100
    deepslate_bias = max(0, min(100, (GROUND_Y - y - 20) * 3))
    if h < deepslate_bias:
        return "minecraft:deepslate" if h % 3 else "minecraft:cobbled_deepslate"
    return "minecraft:stone" if h % 4 else "minecraft:cobblestone"


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


def tunnel(x0, z0, direction, length, y0, w=3, h=3, ore_table=None, torch_every=5):
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
            side = half
            tx = cx + perp[0] * side
            tz = cz + perp[1] * side
            set_block(tx, y0, tz, "minecraft:torch")
        if ore_table and rng.random() < 0.35:
            ore = rng.choices([o for o, _ in ore_table], weights=[wgt for _, wgt in ore_table])[0]
            ox = cx + perp[0] * rng.randint(-half, half)
            oz = cz + perp[1] * rng.randint(-half, half)
            oy = y0 + rng.randint(0, h - 1)
            ore_pocket(ox, oy, oz, ore, count=rng.randint(3, 6))
    end_x = x0 + dx * length
    end_z = z0 + dz * length
    return end_x, end_z


def room(cx, cz, y0, size=5, h=4, ore_table=None, ore_count=3):
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
    for y in range(y0, y0 + h):
        set_block(cx - half, y, cz, "minecraft:torch")
        set_block(cx + half, y, cz, "minecraft:torch")
    if ore_table:
        for _ in range(ore_count):
            ore = rng.choices([o for o, _ in ore_table], weights=[wgt for _, wgt in ore_table])[0]
            ox = rng.randint(cx - half + 1, cx + half - 1)
            oz = rng.randint(cz - half + 1, cz + half - 1)
            oy = rng.randint(y0, y0 + h - 1)
            ore_pocket(ox, oy, oz, ore, count=rng.randint(3, 7))


# ------------------------------------------------------------- puits central
print("Puits central...")
SHAFT_TOP = GROUND_Y
SHAFT_BOTTOM = GROUND_Y - 54
for y in range(SHAFT_BOTTOM - 1, SHAFT_TOP + 1):
    for x in range(CX - 2, CX + 3):
        for z in range(CZ - 2, CZ + 3):
            edge = x in (CX - 2, CX + 2) or z in (CZ - 2, CZ + 2)
            if edge:
                set_block(x, y, z, stone_at(x, y, z))
            else:
                clear_air(x, y, z)
for y in range(SHAFT_BOTTOM, SHAFT_TOP + 1):
    set_block(CX - 1, y, CZ - 1, "minecraft:ladder")
for y in range(SHAFT_BOTTOM, SHAFT_TOP + 1, 6):
    set_block(CX - 2, y, CZ, "minecraft:torch")
    set_block(CX + 2, y, CZ, "minecraft:torch")
print("  blocs:", len(b.blocks))

# ------------------------------------------------------ galeries par niveau
LEVELS = [
    (12, [("minecraft:coal_ore", 5), ("minecraft:iron_ore", 3), ("minecraft:copper_ore", 3)]),
    (24, [("minecraft:iron_ore", 4), ("minecraft:gold_ore", 3), ("minecraft:redstone_ore", 3)]),
    (36, [("minecraft:gold_ore", 3), ("minecraft:redstone_ore", 3), ("minecraft:lapis_ore", 3)]),
    (48, [("minecraft:diamond_ore", 4), ("minecraft:deepslate_diamond_ore", 3), ("minecraft:emerald_ore", 2),
          ("minecraft:redstone_ore", 2)]),
]

TUNNEL_LEN = 16
for depth, ore_table in LEVELS:
    y0 = GROUND_Y - depth
    print(f"Niveau -{depth}...")
    for direction in ("N", "S", "E", "W"):
        dx, dz = DIRS[direction]
        start_x = CX + dx * 3
        start_z = CZ + dz * 3
        end_x, end_z = tunnel(start_x, start_z, direction, TUNNEL_LEN, y0, w=3, h=3, ore_table=ore_table)
        room(end_x + dx * 3, end_z + dz * 3, y0, size=5, h=4, ore_table=ore_table, ore_count=4)
    print("  blocs:", len(b.blocks))

# petite mare souterraine au niveau -36 (cote nord, avant la salle)
print("Mare souterraine...")
lake_y = GROUND_Y - 36
lake_x = CX
lake_z = CZ - 3 - TUNNEL_LEN - 6
for x in range(lake_x - 3, lake_x + 4):
    for z in range(lake_z - 3, lake_z + 4):
        d2 = (x - lake_x) ** 2 + (z - lake_z) ** 2
        if d2 <= 9:
            set_block(x, lake_y - 1, z, "minecraft:water")
            set_block(x, lake_y - 2, z, stone_at(x, lake_y - 2, z))
        if d2 <= 16:
            clear_air(x, lake_y, z)
            clear_air(x, lake_y + 1, z)


# ---------------------------------------------------------------- entree
def sign_lit_torch_ring(cx, cz, y, r):
    for ang in range(0, 360, 45):
        t = math.radians(ang)
        set_block(round(cx + r * math.cos(t)), y, round(cz + r * math.sin(t)), "minecraft:torch")


print("Couverture rocheuse (vue du dessus = colline, pas des galeries a nu)...")
for x in range(SIZE):
    for z in range(SIZE):
        if (x - CX) ** 2 + (z - CZ) ** 2 <= 6 ** 2:
            continue
        set_block(x, GROUND_Y, z, stone_at(x, GROUND_Y, z))
print("  blocs:", len(b.blocks))

print("Entree en surface...")
for x in range(CX - 6, CX + 7):
    for z in range(CZ - 6, CZ + 7):
        set_block(x, GROUND_Y + 1, z, "minecraft:coarse_dirt" if (x + z) % 5 else "minecraft:gravel")
# cabane d'entree en bois
HUT_Y = GROUND_Y + 1
for x in range(CX - 5, CX - 1):
    for z in range(CZ - 5, CZ - 1):
        edge = x in (CX - 5, CX - 2) or z in (CZ - 5, CZ - 2)
        for level in range(4):
            y = HUT_Y + level
            if level == 0 or edge:
                set_block(x, y, z, "minecraft:spruce_log" if (x in (CX - 5, CX - 2) and z in (CZ - 5, CZ - 2)) else "minecraft:spruce_planks")
            else:
                clear_air(x, y, z)
for i, x in enumerate(range(CX - 6, CX - 1)):
    ry = HUT_Y + 4 + min(i, 4 - i)
    for z in range(CZ - 6, CZ - 1):
        set_block(x, ry, z, "minecraft:dark_oak_planks")
set_block(CX - 3, HUT_Y, CZ - 2, "minecraft:air")
set_block(CX - 3, HUT_Y + 1, CZ - 2, "minecraft:air")
set_block(CX - 4, HUT_Y + 1, CZ - 4, "minecraft:oak_sign")
for y in (HUT_Y, HUT_Y + 1):
    set_block(CX - 5, y, CZ - 3, "minecraft:torch")

# chevalet de mine (portique) au-dessus du puits
for dx, dz in [(-2, -2), (2, -2), (-2, 2), (2, 2)]:
    for y in range(GROUND_Y + 1, GROUND_Y + 6):
        set_block(CX + dx, y, CZ + dz, "minecraft:spruce_log")
for dx in (-2, 2):
    for dz in (-2, 2):
        set_block(CX + dx, GROUND_Y + 6, CZ + dz, "minecraft:spruce_fence")
for x in range(CX - 2, CX + 3):
    set_block(x, GROUND_Y + 6, CZ - 2, "minecraft:spruce_slab")
    set_block(x, GROUND_Y + 6, CZ + 2, "minecraft:spruce_slab")
for z in range(CZ - 2, CZ + 3):
    set_block(CX - 2, GROUND_Y + 6, z, "minecraft:spruce_slab")
    set_block(CX + 2, GROUND_Y + 6, z, "minecraft:spruce_slab")
set_block(CX, GROUND_Y + 7, CZ, "minecraft:hopper")
# rambarde de securite autour du trou du puits (sauf cote echelle/entree)
for x in range(CX - 2, CX + 3):
    for z in (CZ - 3, CZ + 3):
        set_block(x, GROUND_Y + 1, z, "minecraft:spruce_fence")
for z in range(CZ - 2, CZ + 3):
    for x in (CX - 3, CX + 3):
        set_block(x, GROUND_Y + 1, z, "minecraft:spruce_fence")
sign_lit_torch_ring(CX, CZ, GROUND_Y + 1, 4)

print("  blocs:", len(b.blocks))

print("Total final:", len(b.blocks), "blocs,", len(b.palette), "etats de palette")

out_path = "/home/user/serv/structures/mine.nbt"
nblocks, npalette = b.save(out_path)
print("Sauvegarde:", out_path, nblocks, "blocs")

info = verify(out_path)
print("Verification:", info)
