"""Genere spawn_castle.nbt : chateau + village sur une plateforme de 500x500,
en remplacement du hub actuel. Reutilise structure_lib (memes conventions que
build_hub.py / build_plaza.py / build_pvp.py).

Principe cle pour rester dans une taille de fichier raisonnable malgre les
500x500 : uniquement des blocs explicitement definis sont stockes (structure
"eparse" - cf. structure_lib), et tous les volumes (tours, murs, maisons)
sont construits en coques creuses (parois) plutot qu'en blocs pleins.

v2 : chateau plus detaille (murs texture, fenetres/meurtrieres, tours
jumelles au porche, drapeaux, puits) + village beaucoup plus dense et
varie (rues concentriques avec maisons des deux cotes, jardins, 8
variantes de batiments, 2 batiments-reperes (taverne, chapelle),
champs cultives).
"""
import math
import random
import sys

sys.path.insert(0, "/home/user/serv/structures")
from structure_lib import StructureBuilder, verify

SIZE = 500
HEIGHT = 90
GROUND_Y = 20
CX = SIZE // 2
CZ = SIZE // 2

rng = random.Random(2024)

b = StructureBuilder(SIZE, HEIGHT, SIZE)


def in_bounds(x, z):
    return 0 <= x < SIZE and 0 <= z < SIZE


def set_ground(x, z, name="minecraft:grass_block"):
    if in_bounds(x, z):
        b.set_block(x, GROUND_Y, z, name)


def set_path(x, z, name="minecraft:polished_andesite"):
    if in_bounds(x, z):
        b.set_block(x, GROUND_Y, z, name)


WALL_VARIANTS = ["minecraft:stone_bricks"] * 6 + [
    "minecraft:mossy_stone_bricks", "minecraft:cracked_stone_bricks", "minecraft:andesite"
]


def wall_mat(x, y, z):
    h = (x * 928371 + y * 128371 + z * 54823) % 100
    return WALL_VARIANTS[h % len(WALL_VARIANTS)]


# ---------------------------------------------------------------- platform
print("Plateforme (herbe pleine 500x500)...")
for x in range(SIZE):
    for z in range(SIZE):
        set_ground(x, z)
print("  blocs:", len(b.blocks))


# ------------------------------------------------------------------- moat
def carve_moat(cx, cz, inner_r, outer_r):
    inner2, outer2 = inner_r * inner_r, outer_r * outer_r
    for x in range(max(0, cx - outer_r - 1), min(SIZE, cx + outer_r + 2)):
        for z in range(max(0, cz - outer_r - 1), min(SIZE, cz + outer_r + 2)):
            dx, dz = x - cx, z - cz
            d2 = dx * dx + dz * dz
            if inner2 <= d2 <= outer2:
                for y in range(GROUND_Y - 4, GROUND_Y + 1):
                    b.set_block(x, y, z, "minecraft:water")
            if (outer2 < d2 <= (outer_r + 1) * (outer_r + 1)) or (
                (inner_r - 1) * (inner_r - 1) <= d2 < inner2
            ):
                b.set_block(x, GROUND_Y, z, "minecraft:stone_brick_slab")


print("Douves...")
carve_moat(CX, CZ, 34, 41)
print("  blocs:", len(b.blocks))


# ---------------------------------------------------------------- helpers
def hollow_cylinder(cx, cz, y0, y1, r, mat_fn, thickness=1):
    for y in range(y0, y1 + 1):
        for ang in range(0, 720):
            t = ang * math.pi / 360.0
            for tr in range(thickness):
                rr = r - tr
                x = round(cx + rr * math.cos(t))
                z = round(cz + rr * math.sin(t))
                if in_bounds(x, z) and 0 <= y < HEIGHT:
                    m = mat_fn(x, y, z) if callable(mat_fn) else mat_fn
                    b.set_block(x, y, z, m)


def carve_slit(cx, cz, r, angle_deg, y0, y1, width_deg=5, thickness=2):
    center_ang = int(angle_deg * 2)
    span = int(width_deg * 2)
    for ang in range(center_ang - span, center_ang + span + 1):
        t = (ang % 720) * math.pi / 360.0
        for tr in range(thickness + 1):
            rr = r - tr
            x = round(cx + rr * math.cos(t))
            z = round(cz + rr * math.sin(t))
            for y in range(y0, y1 + 1):
                b.blocks.pop((x, y, z), None)


def disk(cx, cz, y, r, name):
    r2 = r * r
    for x in range(max(0, cx - r), min(SIZE, cx + r + 1)):
        for z in range(max(0, cz - r), min(SIZE, cz + r + 1)):
            dx, dz = x - cx, z - cz
            if dx * dx + dz * dz <= r2 and 0 <= y < HEIGHT:
                b.set_block(x, y, z, name)


def cone_roof(cx, cz, y0, r, height, mats):
    for i in range(height):
        y = y0 + i
        rr = max(0, r - i)
        mat = mats[min(i, len(mats) - 1)]
        if rr == 0:
            if 0 <= y < HEIGHT:
                b.set_block(cx, y, cz, mat)
            continue
        for ang in range(0, 720):
            t = ang * math.pi / 360.0
            x = round(cx + rr * math.cos(t))
            z = round(cz + rr * math.sin(t))
            if in_bounds(x, z) and 0 <= y < HEIGHT:
                b.set_block(x, y, z, mat)


def flag(cx, y0, cz, wool="minecraft:red_wool"):
    for i in range(4):
        if 0 <= y0 + i < HEIGHT:
            b.set_block(cx, y0 + i, cz, "minecraft:oak_fence")
    for dx in range(0, 3):
        for dy in range(0, 2):
            y = y0 + 3 - dy
            if 0 <= y < HEIGHT:
                b.set_block(cx + dx, y, cz, wool)


def box_shell(x0, y0, z0, x1, y1, z1, name, skip_top=False, skip_bottom=True):
    xs = range(min(x0, x1), max(x0, x1) + 1)
    ys = range(min(y0, y1), max(y0, y1) + 1)
    zs = range(min(z0, z1), max(z0, z1) + 1)
    ylo, yhi = min(ys), max(ys)
    xlo, xhi = min(xs), max(xs)
    zlo, zhi = min(zs), max(zs)
    for y in ys:
        for x in xs:
            for z in zs:
                on_wall = x in (xlo, xhi) or z in (zlo, zhi)
                on_top = y == yhi and not skip_top
                on_bottom = y == ylo and not skip_bottom
                if on_wall or on_top or on_bottom:
                    if in_bounds(x, z) and 0 <= y < HEIGHT:
                        b.set_block(x, y, z, name)


def well(cx, cz):
    disk(cx, cz, GROUND_Y - 1, 2, "minecraft:water")
    hollow_cylinder(cx, cz, GROUND_Y, GROUND_Y + 2, 2, "minecraft:cobblestone", thickness=1)
    for dx, dz in [(-2, -2), (2, -2), (-2, 2), (2, 2)]:
        for y in range(GROUND_Y + 2, GROUND_Y + 5):
            b.set_block(cx + dx, y, cz + dz, "minecraft:spruce_fence")
    for dx in (-2, 2):
        for dz in (-2, 2):
            b.set_block(cx + dx, GROUND_Y + 5, cz + dz, "minecraft:spruce_planks")
    for dx in range(-2, 3):
        b.set_block(cx + dx, GROUND_Y + 5, cz, "minecraft:spruce_slab")
    for dz in range(-2, 3):
        b.set_block(cx, GROUND_Y + 5, cz + dz, "minecraft:spruce_slab")


def tower(cx, cz, r, wall_top, roof_h, roof_mats, floors=(), windows_ys=None, window_count=8):
    hollow_cylinder(cx, cz, GROUND_Y, wall_top, r, wall_mat, thickness=1)
    if windows_ys is None:
        windows_ys = [wall_top - 6, wall_top - 14]
    for wy in windows_ys:
        if wy - 1 <= GROUND_Y:
            continue
        for i in range(window_count):
            carve_slit(cx, cz, r, i * (360 / window_count), wy, wy + 1)
    for ang in range(0, 720, 45):
        t = ang * math.pi / 360.0
        x = round(cx + r * math.cos(t))
        z = round(cz + r * math.sin(t))
        if in_bounds(x, z):
            b.set_block(x, wall_top + 1, z, "minecraft:stone_brick_wall")
    for fy in floors:
        disk(cx, cz, fy, r - 1, "minecraft:spruce_planks")
    cone_roof(cx, cz, wall_top + 2, r, roof_h, roof_mats)


# --------------------------------------------------------------- chateau
print("Chateau : donjon central...")
tower(CX, CZ, 13, GROUND_Y + 44, 16,
      ["minecraft:dark_prismarine"] * 10 + ["minecraft:light_gray_concrete"] * 4 + ["minecraft:gray_concrete"] * 3,
      floors=(GROUND_Y + 10, GROUND_Y + 20, GROUND_Y + 30, GROUND_Y + 40), window_count=10)
flag(CX, GROUND_Y + 44 + 17, CZ, "minecraft:blue_wool")
print("  blocs:", len(b.blocks))

print("Chateau : tours d'angle...")
corner_r = 27
corner_towers = []
for dx, dz in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
    tx = CX + dx * corner_r
    tz = CZ + dz * corner_r
    corner_towers.append((tx, tz))
    tower(tx, tz, 7, GROUND_Y + 28, 10,
          ["minecraft:dark_prismarine"] * 6 + ["minecraft:light_gray_concrete"] * 3,
          floors=(GROUND_Y + 13,), window_count=6)
    flag(tx, GROUND_Y + 28 + 11, tz, "minecraft:blue_wool")
print("  blocs:", len(b.blocks))

print("Chateau : courtines...")
wall_top = GROUND_Y + 13


def crenelated_wall(x0, z0, x1, z1, top_y):
    length = max(abs(x1 - x0), abs(z1 - z0))
    for i in range(length + 1):
        t = i / max(1, length)
        x = round(x0 + (x1 - x0) * t)
        z = round(z0 + (z1 - z0) * t)
        for y in range(GROUND_Y, top_y + 1):
            b.set_block(x, y, z, wall_mat(x, y, z))
        if i % 2 == 0:
            b.set_block(x, top_y + 1, z, "minecraft:stone_brick_wall")
        if i % 6 == 3:
            for wy in (GROUND_Y + 5, GROUND_Y + 6):
                b.blocks.pop((x, wy, z), None)


pts = [
    (CX - corner_r, CZ - corner_r), (CX + corner_r, CZ - corner_r),
    (CX + corner_r, CZ + corner_r), (CX - corner_r, CZ + corner_r),
    (CX - corner_r, CZ - corner_r),
]
GATE_HALF = 4
for i in range(len(pts) - 1):
    x0, z0 = pts[i]
    x1, z1 = pts[i + 1]
    if z0 == z1 and z0 == CZ + corner_r:
        crenelated_wall(x0, z0, CX - GATE_HALF - 3, z1, wall_top)
        crenelated_wall(CX + GATE_HALF + 3, z0, x1, z1, wall_top)
    else:
        crenelated_wall(x0, z0, x1, z1, wall_top)
print("  blocs:", len(b.blocks))

# porte principale : barbacane avec 2 tours jumelles
print("Chateau : porche + tours jumelles...")
gate_z = CZ + corner_r
for gx in (CX - GATE_HALF - 3, CX + GATE_HALF + 3):
    tower(gx, gate_z, 4, GROUND_Y + 20, 7,
          ["minecraft:dark_prismarine"] * 4 + ["minecraft:light_gray_concrete"] * 2,
          floors=(), window_count=4)
box_shell(CX - GATE_HALF - 1, GROUND_Y, gate_z - 3, CX + GATE_HALF + 1, GROUND_Y + 15, gate_z + 3,
          "minecraft:stone_bricks", skip_top=False)
for x in range(CX - GATE_HALF, CX + GATE_HALF + 1):
    for y in range(GROUND_Y, GROUND_Y + 6):
        b.set_block(x, y, gate_z - 3, "minecraft:air")
        b.set_block(x, y, gate_z, "minecraft:air")
        b.set_block(x, y, gate_z + 3, "minecraft:air")
for x in range(CX - GATE_HALF + 1, CX + GATE_HALF):
    b.set_block(x, GROUND_Y + 5, gate_z, "minecraft:iron_bars")
    b.set_block(x, GROUND_Y + 4, gate_z, "minecraft:iron_bars")
for y in (GROUND_Y + 1, GROUND_Y + 2):
    b.set_block(CX - GATE_HALF, y, gate_z - 3, "minecraft:torch")
    b.set_block(CX + GATE_HALF, y, gate_z - 3, "minecraft:torch")
for x in (CX - GATE_HALF - 3, CX + GATE_HALF + 3):
    for y in range(GROUND_Y + 3, GROUND_Y + 6):
        b.set_block(x, y, gate_z - 2, "minecraft:chain")
flag(CX - GATE_HALF - 3, GROUND_Y + 20 + 8, gate_z, "minecraft:red_wool")
flag(CX + GATE_HALF + 3, GROUND_Y + 20 + 8, gate_z, "minecraft:red_wool")
print("  blocs:", len(b.blocks))

print("Ponts...")
for z in range(gate_z - 8, gate_z + 4):
    for x in range(CX - 3, CX + 4):
        b.set_block(x, GROUND_Y, z, "minecraft:oak_planks")
    for x in (CX - 3, CX + 3):
        b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_fence")
for (axis, sign) in [("z", -1), ("x", -1), ("x", 1)]:
    if axis == "z":
        x0, z0 = CX, CZ + sign * corner_r
        for z in range(min(z0, z0 + sign * 8), max(z0, z0 + sign * 8) + 1):
            for x in range(CX - 2, CX + 3):
                b.set_block(x, GROUND_Y, z, "minecraft:oak_planks")
            for x in (CX - 2, CX + 2):
                b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_fence")
    else:
        z0 = CZ
        x0 = CX + sign * corner_r
        for x in range(min(x0, x0 + sign * 8), max(x0, x0 + sign * 8) + 1):
            for z in range(CZ - 2, CZ + 3):
                b.set_block(x, GROUND_Y, z, "minecraft:oak_planks")
            for z in (CZ - 2, CZ + 2):
                b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_fence")
print("  blocs:", len(b.blocks))


# ------------------------------------------------------- stands (cour)
def stall(cx, cz, facing_dx, facing_dz, wood="oak", roof="minecraft:red_terracotta"):
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            b.set_block(x, GROUND_Y + 1, z, f"minecraft:{wood}_fence")
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            b.set_block(x, GROUND_Y + 3, z, roof)
    b.set_block(cx, GROUND_Y + 1, cz, f"minecraft:{wood}_planks")
    sign_x = cx + facing_dx * 2
    sign_z = cz + facing_dz * 2
    if in_bounds(sign_x, sign_z):
        b.set_block(sign_x, GROUND_Y + 2, sign_z, f"minecraft:{wood}_sign")


print("Stands + puits dans la cour...")
stall_ring_r = 19
stall_defs = [
    (0, "oak"), (30, "spruce"), (60, "oak"), (90, "birch"),
    (120, "oak"), (150, "spruce"), (180, "oak"), (210, "birch"),
    (240, "oak"), (270, "spruce"), (300, "oak"), (330, "birch"),
]
for ang, wood in stall_defs:
    t = math.radians(ang)
    sx = round(CX + stall_ring_r * math.cos(t))
    sz = round(CZ + stall_ring_r * math.sin(t))
    stall(sx, sz, round(math.cos(t)), round(math.sin(t)), wood=wood)
well(CX, CZ - 6)
print("  blocs:", len(b.blocks))


# --------------------------------------------------------------- village
HOUSE_VARIANTS = [
    dict(w=6, d=6, h=4, wall="minecraft:oak_planks", corner="minecraft:oak_log",
         roofblock="minecraft:oak_planks", two_story=False),
    dict(w=7, d=7, h=5, wall="minecraft:cobblestone", corner="minecraft:cobblestone_wall",
         roofblock="minecraft:stone_bricks", two_story=False),
    dict(w=6, d=7, h=4, wall="minecraft:spruce_planks", corner="minecraft:spruce_log",
         roofblock="minecraft:spruce_planks", two_story=False),
    dict(w=9, d=9, h=4, wall="minecraft:white_concrete", corner="minecraft:dark_oak_log",
         roofblock="minecraft:dark_oak_planks", two_story=True),
    dict(w=7, d=6, h=4, wall="minecraft:terracotta", corner="minecraft:stripped_oak_log",
         roofblock="minecraft:orange_terracotta", two_story=False),
    dict(w=6, d=6, h=4, wall="minecraft:birch_planks", corner="minecraft:birch_log",
         roofblock="minecraft:birch_planks", two_story=False),
    dict(w=8, d=8, h=4, wall="minecraft:smooth_stone", corner="minecraft:polished_andesite",
         roofblock="minecraft:cobblestone", two_story=True),
]


def rotate(lx, lz, rot):
    if rot == 0:
        return lx, lz
    if rot == 90:
        return -lz, lx
    if rot == 180:
        return -lx, -lz
    return lz, -lx


def house(cx, cz, rot, variant, garden_side=1):
    v = HOUSE_VARIANTS[variant]
    w, d, h = v["w"], v["d"], v["h"]
    wall, corner, roofblock = v["wall"], v["corner"], v["roofblock"]
    stories = 2 if v["two_story"] else 1

    for lx in range(-w // 2, w // 2 + 1):
        for lz in range(-d // 2, d // 2 + 1):
            is_corner = abs(lx) == w // 2 and abs(lz) == d // 2
            x, z = rotate(lx, lz, rot)
            b.set_block(x + cx, GROUND_Y + 1, z + cz, corner if is_corner else "minecraft:oak_planks")

    floor_h = h if stories == 1 else h // 2 + 1
    total_h = h if stories == 1 else h + 3
    for wy in range(GROUND_Y + 1, GROUND_Y + 1 + total_h):
        band = wy - (GROUND_Y + 1)
        for lx in range(-w // 2, w // 2 + 1):
            for lz in (-d // 2, d // 2):
                door = lx == 0 and lz == d // 2 and band <= 2
                window = (lx in (-w // 2 + 2, w // 2 - 2)) and band in (2, 3) and abs(lz) == d // 2
                x, z = rotate(lx, lz, rot)
                if door:
                    continue
                mat = "minecraft:glass_pane" if window else wall
                b.set_block(x + cx, wy, z + cz, mat)
        for lz in range(-d // 2, d // 2 + 1):
            for lx in (-w // 2, w // 2):
                window = (lz in (-d // 2 + 2, d // 2 - 2)) and band in (2, 3)
                x, z = rotate(lx, lz, rot)
                mat = "minecraft:glass_pane" if window else wall
                b.set_block(x + cx, wy, z + cz, mat)
        if stories == 2 and band == h // 2:
            for lx in range(-w // 2, w // 2 + 1):
                for lz in range(-d // 2, d // 2 + 1):
                    x, z = rotate(lx, lz, rot)
                    b.set_block(x + cx, wy, z + cz, "minecraft:spruce_planks")

    roof_y = GROUND_Y + 1 + total_h
    for i, lx in enumerate(range(-w // 2 - 1, w // 2 + 2)):
        ry = roof_y + min(i, w + 1 - i)
        for lz in range(-d // 2 - 1, d // 2 + 2):
            x, z = rotate(lx, lz, rot)
            b.set_block(x + cx, ry, z + cz, roofblock)
    x, z = rotate(0, d // 2 + 1, rot)
    b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:oak_door")
    x, z = rotate(0, d // 2 + 2, rot)
    b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:oak_planks")
    cx_, cz_ = rotate(w // 2, -d // 2, rot)
    b.set_block(cx_ + cx, GROUND_Y + total_h, cz_ + cz, "minecraft:cobblestone")
    b.set_block(cx_ + cx, GROUND_Y + total_h + 1, cz_ + cz, "minecraft:campfire")

    # petit jardin cloture a l'arriere
    gx0, gz0 = rotate(-w // 2 - 1, -d // 2 - 1, rot)
    gx1, gz1 = rotate(w // 2 + 1, -d // 2 - 3, rot)
    for lx in range(min(-w // 2 - 1, -w // 2 - 1), w // 2 + 2):
        for lz in range(-d // 2 - 3, -d // 2 - 1 + 1):
            x, z = rotate(lx, lz, rot)
            edge = lz == -d // 2 - 3 or lx in (-w // 2 - 1, w // 2 + 1)
            if edge:
                b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:oak_fence")
            else:
                b.set_block(x + cx, GROUND_Y, z + cz, "minecraft:farmland")
                crop = ["minecraft:wheat", "minecraft:carrots", "minecraft:potatoes"][rng.randint(0, 2)]
                b.set_block(x + cx, GROUND_Y + 1, z + cz, crop)


def landmark(cx, cz, rot, kind):
    if kind == "tavern":
        w, d, h = 11, 9, 6
        wall, roofblock, corner = "minecraft:spruce_planks", "minecraft:dark_oak_planks", "minecraft:spruce_log"
    else:
        w, d, h = 7, 9, 7
        wall, roofblock, corner = "minecraft:smooth_stone", "minecraft:andesite", "minecraft:polished_andesite"
    for lx in range(-w // 2, w // 2 + 1):
        for lz in range(-d // 2, d // 2 + 1):
            is_corner = abs(lx) == w // 2 and abs(lz) == d // 2
            x, z = rotate(lx, lz, rot)
            b.set_block(x + cx, GROUND_Y + 1, z + cz, corner if is_corner else "minecraft:stone_bricks")
    for wy in range(GROUND_Y + 1, GROUND_Y + 1 + h):
        band = wy - (GROUND_Y + 1)
        for lx in range(-w // 2, w // 2 + 1):
            for lz in (-d // 2, d // 2):
                door = lx == 0 and lz == d // 2 and band <= 2
                window = band in (2, 3) and lx % 3 == 0
                x, z = rotate(lx, lz, rot)
                if door:
                    continue
                b.set_block(x + cx, wy, z + cz, "minecraft:glass_pane" if window else wall)
        for lz in range(-d // 2, d // 2 + 1):
            for lx in (-w // 2, w // 2):
                window = band in (2, 3) and lz % 3 == 0
                x, z = rotate(lx, lz, rot)
                b.set_block(x + cx, wy, z + cz, "minecraft:glass_pane" if window else wall)
    roof_y = GROUND_Y + 1 + h
    for i, lx in enumerate(range(-w // 2 - 1, w // 2 + 2)):
        ry = roof_y + min(i, w + 1 - i)
        for lz in range(-d // 2 - 1, d // 2 + 2):
            x, z = rotate(lx, lz, rot)
            b.set_block(x + cx, ry, z + cz, roofblock)
    x, z = rotate(0, d // 2 + 1, rot)
    b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:oak_door")
    x, z = rotate(0, d // 2 + 3, rot)
    b.set_block(x + cx, GROUND_Y + 2, z + cz, f"minecraft:oak_sign")
    if kind == "chapel":
        cxp, czp = rotate(0, -d // 2, rot)
        for i in range(4):
            b.set_block(cxp + cx, roof_y + h + i, czp + cz, "minecraft:andesite")
        b.set_block(cxp + cx, roof_y + h + 4, czp + cz, "minecraft:stone_brick_wall")


print("Village : rues concentriques + maisons + jardins...")
occupied = list(corner_towers) + [(CX, CZ)]
houses_built = 0
ring_radii = [58, 90, 125, 162, 200, 235]
for ridx, ring_r in enumerate(ring_radii):
    circumference = 2 * math.pi * ring_r
    spacing = 17 if ring_r < 150 else 15
    n = max(6, int(circumference / spacing))
    offset = rng.uniform(0, 2 * math.pi / n)
    for i in range(n):
        ang = offset + 2 * math.pi * i / n
        side = 1 if i % 2 == 0 else -1
        set_r = ring_r + side * 6
        hx = round(CX + set_r * math.cos(ang))
        hz = round(CZ + set_r * math.sin(ang))
        if not (12 <= hx < SIZE - 12 and 12 <= hz < SIZE - 12):
            continue
        if any((hx - ox) ** 2 + (hz - oz) ** 2 < 13 ** 2 for ox, oz in occupied):
            continue
        facing_ang = ang + math.pi if side == 1 else ang
        deg = math.degrees(facing_ang) % 360
        rot = min([0, 90, 180, 270], key=lambda r_: min(abs(r_ - deg), 360 - abs(r_ - deg)))
        variant = rng.randint(0, len(HOUSE_VARIANTS) - 1)
        house(hx, hz, rot, variant)
        occupied.append((hx, hz))
        houses_built += 1
    print(f"  anneau r={ring_r}: {n} emplacements, total maisons={houses_built}, blocs={len(b.blocks)}")

# rues radiales (8 directions) reliant les anneaux
print("Rues radiales...")


def path_line(x0, z0, x1, z1, width=3, name="minecraft:polished_andesite"):
    length = max(abs(x1 - x0), abs(z1 - z0))
    if length == 0:
        return
    for i in range(length + 1):
        t = i / length
        cx_ = x0 + (x1 - x0) * t
        cz_ = z0 + (z1 - z0) * t
        for wx in range(-width // 2, width // 2 + 1):
            for wz in range(-width // 2, width // 2 + 1):
                set_path(round(cx_) + wx, round(cz_) + wz, name)


for deg in range(0, 360, 45):
    t = math.radians(deg)
    x0 = CX + round((corner_r + 8) * math.cos(t) * 1.3)
    z0 = CZ + round((corner_r + 8) * math.sin(t) * 1.3)
    x1 = CX + round(240 * math.cos(t))
    z1 = CZ + round(240 * math.sin(t))
    path_line(x0, z0, x1, z1, width=5 if deg % 90 == 0 else 4)

for ring_r in ring_radii:
    steps = max(40, int(ring_r * 1.6))
    prev = None
    for i in range(steps + 1):
        t = 2 * math.pi * i / steps
        x = round(CX + ring_r * math.cos(t))
        z = round(CZ + ring_r * math.sin(t))
        if prev:
            path_line(prev[0], prev[1], x, z, width=3)
        prev = (x, z)
print("  blocs:", len(b.blocks))

print("Batiments-reperes...")
landmark(CX, CZ + 55, 0, "tavern")
occupied.append((CX, CZ + 55))
landmark(CX - 55, CZ, 90, "chapel")
occupied.append((CX - 55, CZ))
print("  blocs:", len(b.blocks))


# ------------------------------------------------------------------ champs
def field(cx, cz, w, d, crop):
    for lx in range(-w // 2, w // 2 + 1):
        for lz in range(-d // 2, d // 2 + 1):
            edge = lx in (-w // 2, w // 2) or lz in (-d // 2, d // 2)
            x, z = cx + lx, cz + lz
            if edge:
                b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_fence")
            else:
                b.set_block(x, GROUND_Y, z, "minecraft:farmland")
                b.set_block(x, GROUND_Y + 1, z, crop)


print("Champs cultives...")
field_spots = [
    (CX + 210, CZ - 60, "minecraft:wheat"),
    (CX + 195, CZ + 40, "minecraft:carrots"),
    (CX - 40, CZ - 215, "minecraft:potatoes"),
    (CX + 40, CZ - 200, "minecraft:wheat"),
]
for fx, fz, crop in field_spots:
    if any((fx - ox) ** 2 + (fz - oz) ** 2 < 15 ** 2 for ox, oz in occupied):
        continue
    field(fx, fz, 10, 8, crop)
print("  blocs:", len(b.blocks))


# ------------------------------------------------------------------ lac
LAKE_X, LAKE_Z = CX - 155, CZ + 155
LAKE_R = 27
print("Lac + cascade...")
for x in range(LAKE_X - LAKE_R - 2, LAKE_X + LAKE_R + 3):
    for z in range(LAKE_Z - LAKE_R - 2, LAKE_Z + LAKE_R + 3):
        dx, dz = x - LAKE_X, z - LAKE_Z
        d2 = dx * dx + dz * dz
        if d2 <= LAKE_R * LAKE_R:
            for y in range(GROUND_Y - 3, GROUND_Y + 1):
                b.set_block(x, y, z, "minecraft:water")
        elif d2 <= (LAKE_R + 2) * (LAKE_R + 2):
            b.set_block(x, GROUND_Y, z, "minecraft:sand")
hill_x, hill_z = LAKE_X, LAKE_Z - LAKE_R - 4
for i in range(6):
    y = GROUND_Y + i
    r = 6 - i
    disk(hill_x, hill_z, y, max(1, r), "minecraft:stone")
for y in range(GROUND_Y, GROUND_Y + 6):
    b.set_block(hill_x, y, hill_z + 2, "minecraft:water")
print("  blocs:", len(b.blocks))

for z in range(hill_z + 5, hill_z + 5 + 10):
    for x in range(hill_x - 2, hill_x + 3):
        b.set_block(x, GROUND_Y, z, "minecraft:spruce_planks")
    for x in (hill_x - 2, hill_x + 2):
        b.set_block(x, GROUND_Y + 1, z, "minecraft:spruce_fence")
path_line(hill_x, hill_z - LAKE_R - 2, CX - 90, CZ + 120, width=3)


# ------------------------------------------------------------------ arbres
def tree(x, z, y=GROUND_Y + 1):
    for i in range(5):
        b.set_block(x, y + i, z, "minecraft:oak_log")
    for ly in range(3, 6):
        r = 2 if ly < 5 else 1
        for lx in range(-r, r + 1):
            for lz in range(-r, r + 1):
                if lx * lx + lz * lz <= r * r + 1 and (lx, lz) != (0, 0):
                    b.set_block(x + lx, y + ly, z + lz, "minecraft:oak_leaves")


print("Arbres...")
tcount = 0
attempts = 0
placed_trees = []
while tcount < 90 and attempts < 5000:
    attempts += 1
    ang = rng.uniform(0, 2 * math.pi)
    r = rng.uniform(55, 245)
    tx = round(CX + r * math.cos(ang))
    tz = round(CZ + r * math.sin(ang))
    if not (5 <= tx < SIZE - 5 and 5 <= tz < SIZE - 5):
        continue
    if any((tx - ox) ** 2 + (tz - oz) ** 2 < 11 ** 2 for ox, oz in occupied):
        continue
    if any((tx - ox) ** 2 + (tz - oz) ** 2 < 8 ** 2 for ox, oz in placed_trees):
        continue
    if (tx - LAKE_X) ** 2 + (tz - LAKE_Z) ** 2 < (LAKE_R + 6) ** 2:
        continue
    tree(tx, tz)
    placed_trees.append((tx, tz))
    tcount += 1
print(f"  {tcount} arbres, blocs:", len(b.blocks))

print("Total final:", len(b.blocks), "blocs,", len(b.palette), "etats de palette")

out_path = "/home/user/serv/structures/spawn_castle.nbt"
nblocks, npalette = b.save(out_path)
print("Sauvegarde:", out_path, nblocks, "blocs")

info = verify(out_path)
print("Verification:", info)
