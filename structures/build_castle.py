"""Genere spawn_castle.nbt : chateau fort moyen-age + village sur une
plateforme de 500x500. Reutilise structure_lib (StructureBuilder eparse :
seuls les blocs explicitement poses sont stockes -> pas de cout pour le
vide) et l'approche "coques creuses" pour rester a une taille raisonnable.

v3 : chateau concentrique veritablement "style moyen-age" (motte avec
donjon a etages + contreforts, enceinte interieure autour de la motte,
enceinte exterieure avec tours d'angle + tours de flanquement + barbacane
a double porte, pont-levis a chaines), basse-cour avec forge/ecurie/puits/
stands, et une passe de decoration (lampadaires le long des rues, buissons,
parterres de fleurs, bancs, statues au portail) sur tout le site.
"""
import math
import random
import sys

sys.path.insert(0, "/home/user/serv/structures")
from structure_lib import StructureBuilder, verify

SIZE = 500
HEIGHT = 100
GROUND_Y = 20
CX = SIZE // 2
CZ = SIZE // 2

rng = random.Random(77)

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


# --------------------------------------------------------------- helpers
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


def buttresses(cx, cz, r, y0, y1, count=8, mat="minecraft:stone_bricks"):
    for i in range(count):
        ang = 360 * i / count
        t = math.radians(ang)
        for y in range(y0, y1 + 1):
            taper = int((y - y0) / max(1, (y1 - y0)) * 2)
            rr = r + 1 - taper
            x = round(cx + rr * math.cos(t))
            z = round(cz + rr * math.sin(t))
            if in_bounds(x, z) and 0 <= y < HEIGHT:
                b.set_block(x, y, z, mat)


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


def tower(cx, cz, r, wall_top, roof_h, roof_mats, floors=(), windows_ys=None,
          window_count=8, base_y=GROUND_Y, spire=False):
    hollow_cylinder(cx, cz, base_y, wall_top, r, wall_mat, thickness=1)
    if windows_ys is None:
        windows_ys = [wall_top - 6, wall_top - 14, wall_top - 22]
    for wy in windows_ys:
        if wy - 1 <= base_y:
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
    if spire:
        tip = wall_top + 2 + roof_h
        for i in range(3):
            if 0 <= tip + i < HEIGHT:
                b.set_block(cx, tip + i, cz, "minecraft:lightning_rod")


def crenelated_wall(x0, z0, x1, z1, top_y, base_y=GROUND_Y):
    length = max(abs(x1 - x0), abs(z1 - z0))
    for i in range(length + 1):
        t = i / max(1, length)
        x = round(x0 + (x1 - x0) * t)
        z = round(z0 + (z1 - z0) * t)
        for y in range(base_y, top_y + 1):
            b.set_block(x, y, z, wall_mat(x, y, z))
        if i % 2 == 0:
            b.set_block(x, top_y + 1, z, "minecraft:stone_brick_wall")
        if i % 6 == 3:
            for wy in (base_y + 5, base_y + 6):
                b.blocks.pop((x, wy, z), None)
    return length


# =========================================================== LE CHATEAU
MOTTE_R = 15
KEEP_BASE = GROUND_Y + 6
INNER_R = 24
OUTER_R = 47

print("Motte (butte du donjon)...")
for i in range(6):
    y = GROUND_Y + 1 + i
    r = MOTTE_R - i
    mat = "minecraft:coarse_dirt" if i < 4 else "minecraft:grass_block" if i == 5 else "minecraft:podzol"
    disk(CX, CZ, y, max(1, r), mat)
print("  blocs:", len(b.blocks))

print("Grand escalier d'acces au donjon (face sud)...")
for step in range(7):
    y = GROUND_Y + step
    outer_z = MOTTE_R + 3 - step * 2
    for x in range(CX - 3, CX + 4):
        b.set_block(x, y, CZ + outer_z, "minecraft:stone_brick_slab")
        b.set_block(x, y - 1, CZ + outer_z, "minecraft:stone_bricks")
    for x in (CX - 4, CX + 4):
        for yy in range(y, y + 2):
            b.set_block(x, yy, CZ + outer_z, "minecraft:stone_brick_wall")
print("  blocs:", len(b.blocks))

print("Donjon (tour maitresse)...")
tower(CX, CZ, 13, KEEP_BASE + 34, 16,
      ["minecraft:dark_prismarine"] * 10 + ["minecraft:light_gray_concrete"] * 4 + ["minecraft:gray_concrete"] * 3,
      floors=(KEEP_BASE + 8, KEEP_BASE + 16, KEEP_BASE + 24), window_count=10,
      base_y=KEEP_BASE, spire=True)
buttresses(CX, CZ, 13, KEEP_BASE, KEEP_BASE + 28, count=8)
flag(CX, KEEP_BASE + 34 + 20, CZ, "minecraft:blue_wool")
print("  blocs:", len(b.blocks))

print("Enceinte interieure (autour de la motte)...")
inner_top = GROUND_Y + 9
INNER_GATE_HALF = 3
inner_pts = [
    (CX - INNER_R, CZ - INNER_R), (CX + INNER_R, CZ - INNER_R),
    (CX + INNER_R, CZ + INNER_R), (CX - INNER_R, CZ + INNER_R),
    (CX - INNER_R, CZ - INNER_R),
]
for i in range(len(inner_pts) - 1):
    x0, z0 = inner_pts[i]
    x1, z1 = inner_pts[i + 1]
    if z0 == z1 and z0 == CZ + INNER_R:
        crenelated_wall(x0, z0, CX - INNER_GATE_HALF, z1, inner_top)
        crenelated_wall(CX + INNER_GATE_HALF, z0, x1, z1, inner_top)
    else:
        crenelated_wall(x0, z0, x1, z1, inner_top)
for dx, dz in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
    tower(CX + dx * INNER_R, CZ + dz * INNER_R, 4, GROUND_Y + 15, 6,
          ["minecraft:stone_bricks"] * 4 + ["minecraft:andesite"] * 2, window_count=4)
print("  blocs:", len(b.blocks))

print("Enceinte exterieure : tours d'angle...")
corner_towers = []
for dx, dz in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
    tx = CX + dx * OUTER_R
    tz = CZ + dz * OUTER_R
    corner_towers.append((tx, tz))
    tower(tx, tz, 8, GROUND_Y + 30, 11,
          ["minecraft:dark_prismarine"] * 6 + ["minecraft:light_gray_concrete"] * 3,
          floors=(GROUND_Y + 13, GROUND_Y + 22), window_count=6, spire=True)
    buttresses(tx, tz, 8, GROUND_Y, GROUND_Y + 20, count=6)
    flag(tx, GROUND_Y + 30 + 12, tz, "minecraft:blue_wool")
print("  blocs:", len(b.blocks))

print("Enceinte exterieure : tours de flanquement (milieu des courtines)...")
watchtowers = []
for ang_deg, skip in [(0, False), (90, False), (180, False), (270, True)]:
    t = math.radians(ang_deg)
    wx = CX + round(OUTER_R * math.cos(t))
    wz = CZ + round(OUTER_R * math.sin(t))
    if skip:
        continue
    watchtowers.append((wx, wz))
    tower(wx, wz, 5, GROUND_Y + 22, 8,
          ["minecraft:stone_bricks"] * 5 + ["minecraft:andesite"] * 2, window_count=5)
    flag(wx, GROUND_Y + 22 + 9, wz, "minecraft:red_wool")
print("  blocs:", len(b.blocks))

print("Enceinte exterieure : courtines...")
outer_top = GROUND_Y + 14
outer_pts = [
    (CX - OUTER_R, CZ - OUTER_R), (CX + OUTER_R, CZ - OUTER_R),
    (CX + OUTER_R, CZ + OUTER_R), (CX - OUTER_R, CZ + OUTER_R),
    (CX - OUTER_R, CZ - OUTER_R),
]
OUTER_GATE_HALF = 4
for i in range(len(outer_pts) - 1):
    x0, z0 = outer_pts[i]
    x1, z1 = outer_pts[i + 1]
    if z0 == z1 and z0 == CZ + OUTER_R:
        crenelated_wall(x0, z0, CX - OUTER_GATE_HALF - 4, z1, outer_top)
        crenelated_wall(CX + OUTER_GATE_HALF + 4, z0, x1, z1, outer_top)
    else:
        crenelated_wall(x0, z0, x1, z1, outer_top)
print("  blocs:", len(b.blocks))

print("Barbacane (double porte + tours jumelles + herse)...")
gate_z = CZ + OUTER_R


def gatehouse(gz, gate_half, wall_top, twin_r, twin_top):
    for gx in (CX - gate_half - 4, CX + gate_half + 4):
        tower(gx, gz, twin_r, twin_top, 7,
              ["minecraft:dark_prismarine"] * 4 + ["minecraft:light_gray_concrete"] * 2, window_count=4)
    box_shell(CX - gate_half - 2, GROUND_Y, gz - 3, CX + gate_half + 2, wall_top + 2, gz + 3,
              "minecraft:stone_bricks", skip_top=False)
    for x in range(CX - gate_half, CX + gate_half + 1):
        for y in range(GROUND_Y, GROUND_Y + 6):
            b.set_block(x, y, gz - 3, "minecraft:air")
            b.set_block(x, y, gz, "minecraft:air")
            b.set_block(x, y, gz + 3, "minecraft:air")
    for x in range(CX - gate_half + 1, CX + gate_half):
        b.set_block(x, GROUND_Y + 5, gz, "minecraft:iron_bars")
        b.set_block(x, GROUND_Y + 4, gz, "minecraft:iron_bars")
        b.blocks.pop((x, GROUND_Y + 6, gz - 1), None)
        b.blocks.pop((x, GROUND_Y + 6, gz + 1), None)
    for y in (GROUND_Y + 1, GROUND_Y + 2):
        b.set_block(CX - gate_half, y, gz - 3, "minecraft:torch")
        b.set_block(CX + gate_half, y, gz - 3, "minecraft:torch")
        b.set_block(CX - gate_half, y, gz + 3, "minecraft:torch")
        b.set_block(CX + gate_half, y, gz + 3, "minecraft:torch")


gatehouse(gate_z, OUTER_GATE_HALF, outer_top, 5, GROUND_Y + 21)
for x in (CX - OUTER_GATE_HALF - 4, CX + OUTER_GATE_HALF + 4):
    for y in range(GROUND_Y + 3, GROUND_Y + 7):
        b.set_block(x, y, gate_z - 2, "minecraft:chain")
    flag(x, GROUND_Y + 21 + 8, gate_z, "minecraft:red_wool")
# statues (piliers) flanquant l'entree, au bord du pont
for sx in (CX - OUTER_GATE_HALF - 1, CX + OUTER_GATE_HALF + 1):
    sz = gate_z - 9
    for y in range(GROUND_Y, GROUND_Y + 4):
        b.set_block(sx, y, sz, "minecraft:chiseled_stone_bricks")
    b.set_block(sx, GROUND_Y + 4, sz, "minecraft:lantern")
print("  blocs:", len(b.blocks))

print("Ponts...")
for z in range(gate_z - 9, gate_z + 4):
    for x in range(CX - 3, CX + 4):
        b.set_block(x, GROUND_Y, z, "minecraft:oak_planks")
    for x in (CX - 3, CX + 3):
        b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_fence")
for (axis, sign) in [("z", -1), ("x", -1), ("x", 1)]:
    if axis == "z":
        x0, z0 = CX, CZ + sign * OUTER_R
        for z in range(min(z0, z0 + sign * 8), max(z0, z0 + sign * 8) + 1):
            for x in range(CX - 2, CX + 3):
                b.set_block(x, GROUND_Y, z, "minecraft:oak_planks")
            for x in (CX - 2, CX + 2):
                b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_fence")
    else:
        z0 = CZ
        x0 = CX + sign * OUTER_R
        for x in range(min(x0, x0 + sign * 8), max(x0, x0 + sign * 8) + 1):
            for z in range(CZ - 2, CZ + 3):
                b.set_block(x, GROUND_Y, z, "minecraft:oak_planks")
            for z in (CZ - 2, CZ + 2):
                b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_fence")
print("  blocs:", len(b.blocks))

print("Douves...")


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


carve_moat(CX, CZ, 76, 85)
print("  blocs:", len(b.blocks))


# ------------------------------------------------------- basse-cour (bailey)
def rotate(lx, lz, rot):
    if rot == 0:
        return lx, lz
    if rot == 90:
        return -lz, lx
    if rot == 180:
        return -lx, -lz
    return lz, -lx


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


def utility_building(cx, cz, rot, kind):
    w, d, h = 7, 6, 4
    for lx in range(-w // 2, w // 2 + 1):
        for lz in range(-d // 2, d // 2 + 1):
            x, z = rotate(lx, lz, rot)
            b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:spruce_log" if abs(lx) == w // 2 and abs(lz) == d // 2 else "minecraft:oak_planks")
    if kind == "forge":
        for wy in range(GROUND_Y + 1, GROUND_Y + 1 + h):
            for lx in range(-w // 2, w // 2 + 1):
                for lz in (-d // 2, d // 2):
                    x, z = rotate(lx, lz, rot)
                    b.set_block(x + cx, wy, z + cz, "minecraft:cobblestone")
            for lz in range(-d // 2, d // 2 + 1):
                for lx in (-w // 2, w // 2):
                    x, z = rotate(lx, lz, rot)
                    b.set_block(x + cx, wy, z + cz, "minecraft:cobblestone")
        x, z = rotate(0, 0, rot)
        b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:furnace")
        x, z = rotate(1, 0, rot)
        b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:anvil")
        x, z = rotate(0, 1, rot)
        b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:campfire")
        for i, lx in enumerate(range(-w // 2 - 1, w // 2 + 2)):
            ry = GROUND_Y + 1 + h + min(i, w + 1 - i)
            for lz in range(-d // 2 - 1, d // 2 + 2):
                x, z = rotate(lx, lz, rot)
                b.set_block(x + cx, ry, z + cz, "minecraft:cobblestone")
    else:  # stable
        for lx in (-w // 2, w // 2):
            for lz in range(-d // 2, d // 2 + 1):
                for wy in range(GROUND_Y + 1, GROUND_Y + 1 + h):
                    x, z = rotate(lx, lz, rot)
                    b.set_block(x + cx, wy, z + cz, "minecraft:spruce_fence" if wy < GROUND_Y + 3 else "minecraft:oak_planks")
        for i, lx in enumerate(range(-w // 2 - 1, w // 2 + 2)):
            ry = GROUND_Y + 1 + h + min(i, w + 1 - i)
            for lz in range(-d // 2 - 1, d // 2 + 2):
                x, z = rotate(lx, lz, rot)
                b.set_block(x + cx, ry, z + cz, "minecraft:hay_block")
        for hx_ in (-2, 0, 2):
            x, z = rotate(hx_, 0, rot)
            b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:hay_block")


print("Basse-cour : stands, puits, forge, ecurie...")
stall_ring_r = 33
stall_defs = [
    (10, "oak"), (35, "spruce"), (60, "birch"), (100, "oak"),
    (125, "spruce"), (150, "birch"), (190, "oak"), (215, "spruce"),
    (240, "birch"), (280, "oak"), (305, "spruce"), (330, "birch"),
]
for ang, wood in stall_defs:
    t = math.radians(ang)
    sx = round(CX + stall_ring_r * math.cos(t))
    sz = round(CZ + stall_ring_r * math.sin(t))
    stall(sx, sz, round(math.cos(t)), round(math.sin(t)), wood=wood)
well(CX + 32, CZ - 10)
utility_building(CX - 32, CZ - 15, 90, "forge")
utility_building(CX - 32, CZ + 12, 90, "stable")
print("  blocs:", len(b.blocks))


# ================================================================ village


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


def house(cx, cz, rot, variant):
    v = HOUSE_VARIANTS[variant]
    w, d, h = v["w"], v["d"], v["h"]
    wall, corner, roofblock = v["wall"], v["corner"], v["roofblock"]
    stories = 2 if v["two_story"] else 1

    for lx in range(-w // 2, w // 2 + 1):
        for lz in range(-d // 2, d // 2 + 1):
            is_corner = abs(lx) == w // 2 and abs(lz) == d // 2
            x, z = rotate(lx, lz, rot)
            b.set_block(x + cx, GROUND_Y + 1, z + cz, corner if is_corner else "minecraft:oak_planks")

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

    for lx in range(-w // 2 - 1, w // 2 + 2):
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
    b.set_block(x + cx, GROUND_Y + 2, z + cz, "minecraft:oak_sign")
    if kind == "chapel":
        cxp, czp = rotate(0, -d // 2, rot)
        for i in range(4):
            b.set_block(cxp + cx, roof_y + h + i, czp + cz, "minecraft:andesite")
        b.set_block(cxp + cx, roof_y + h + 4, czp + cz, "minecraft:stone_brick_wall")


print("Village : rues concentriques + maisons + jardins...")
occupied = list(corner_towers) + list(watchtowers) + [(CX, CZ)]
houses_built = 0
ring_radii = [100, 128, 158, 190, 218, 235]
for ridx, ring_r in enumerate(ring_radii):
    circumference = 2 * math.pi * ring_r
    spacing = 17 if ring_r < 190 else 15
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
    x0 = CX + round((OUTER_R + 10) * math.cos(t) * 1.3)
    z0 = CZ + round((OUTER_R + 10) * math.sin(t) * 1.3)
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
landmark(CX, CZ + 100, 0, "tavern")
occupied.append((CX, CZ + 100))
landmark(CX - 100, CZ, 90, "chapel")
occupied.append((CX - 100, CZ))
print("  blocs:", len(b.blocks))


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
    (70, 70, "minecraft:wheat"),
    (SIZE - 70, 70, "minecraft:carrots"),
    (70, SIZE - 70, "minecraft:potatoes"),
    (SIZE - 70, SIZE - 70, "minecraft:wheat"),
]
for fx, fz, crop in field_spots:
    if any((fx - ox) ** 2 + (fz - oz) ** 2 < 15 ** 2 for ox, oz in occupied):
        continue
    field(fx, fz, 10, 8, crop)
print("  blocs:", len(b.blocks))


# ------------------------------------------------------------------ lac
LAKE_X, LAKE_Z = CX - 165, CZ + 165
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
path_line(hill_x, hill_z - LAKE_R - 2, CX - 100, CZ + 130, width=3)


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
    r = rng.uniform(90, 245)
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


# =============================================================== DECOR
def lamp_post(x, z, h=4):
    if not in_bounds(x, z):
        return
    for i in range(h):
        b.set_block(x, GROUND_Y + 1 + i, z, "minecraft:cobblestone_wall" if i == 0 else "minecraft:oak_fence")
    b.set_block(x, GROUND_Y + 1 + h, z, "minecraft:lantern")


def bush(x, z):
    if not in_bounds(x, z):
        return
    b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_leaves")


def flower_patch(x, z):
    flowers = ["minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower",
               "minecraft:blue_orchid", "minecraft:azure_bluet"]
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            if rng.random() < 0.55 and in_bounds(x + dx, z + dz):
                b.set_block(x + dx, GROUND_Y + 1, z + dz, rng.choice(flowers))


def bench(x, z, rot):
    for lx, name in [(-1, "minecraft:oak_stairs"), (1, "minecraft:oak_stairs")]:
        lx2, lz2 = rotate(lx, 0, rot)
        b.set_block(x + lx2, GROUND_Y + 1, z + lz2, name)
    b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_slab")


print("Decor : lampadaires le long des avenues...")
lamp_count = 0
for deg in range(0, 360, 45):
    t = math.radians(deg)
    perp = math.radians(deg + 90)
    for r in range(OUTER_R + 25, 235, 22):
        side = 1 if (r // 22) % 2 == 0 else -1
        x = round(CX + r * math.cos(t) + side * 4 * math.cos(perp))
        z = round(CZ + r * math.sin(t) + side * 4 * math.sin(perp))
        lamp_post(x, z)
        lamp_count += 1
print("  blocs:", len(b.blocks))

print("Decor : lampadaires le long des anneaux...")
for ring_r in ring_radii:
    n = max(10, int(2 * math.pi * ring_r / 26))
    for i in range(n):
        t = 2 * math.pi * i / n
        x = round(CX + (ring_r + 4) * math.cos(t))
        z = round(CZ + (ring_r + 4) * math.sin(t))
        if any((x - ox) ** 2 + (z - oz) ** 2 < 6 ** 2 for ox, oz in occupied):
            continue
        lamp_post(x, z)
        lamp_count += 1
print(f"  {lamp_count} lampadaires, blocs:", len(b.blocks))

print("Decor : buissons, fleurs, bancs...")
deco_count = 0
attempts = 0
placed_deco = []
while deco_count < 260 and attempts < 9000:
    attempts += 1
    ang = rng.uniform(0, 2 * math.pi)
    r = rng.uniform(60, 245)
    dx_ = round(CX + r * math.cos(ang))
    dz_ = round(CZ + r * math.sin(ang))
    if not (4 <= dx_ < SIZE - 4 and 4 <= dz_ < SIZE - 4):
        continue
    if any((dx_ - ox) ** 2 + (dz_ - oz) ** 2 < 9 ** 2 for ox, oz in occupied):
        continue
    if any((dx_ - ox) ** 2 + (dz_ - oz) ** 2 < 6 ** 2 for ox, oz in placed_trees):
        continue
    if any((dx_ - ox) ** 2 + (dz_ - oz) ** 2 < 4 ** 2 for ox, oz in placed_deco):
        continue
    if (dx_ - LAKE_X) ** 2 + (dz_ - LAKE_Z) ** 2 < (LAKE_R + 5) ** 2:
        continue
    kind = rng.random()
    if kind < 0.45:
        flower_patch(dx_, dz_)
    elif kind < 0.8:
        bush(dx_, dz_)
    else:
        bench(dx_, dz_, rng.choice([0, 90, 180, 270]))
    placed_deco.append((dx_, dz_))
    deco_count += 1
print(f"  {deco_count} elements de decor, blocs:", len(b.blocks))

print("Total final:", len(b.blocks), "blocs,", len(b.palette), "etats de palette")

out_path = "/home/user/serv/structures/spawn_castle.nbt"
nblocks, npalette = b.save(out_path)
print("Sauvegarde:", out_path, nblocks, "blocs")

info = verify(out_path)
print("Verification:", info)
