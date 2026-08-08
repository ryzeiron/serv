"""Genere spawn_castle.nbt : chateau + village sur une plateforme de 500x500,
en remplacement du hub actuel. Reutilise structure_lib (memes conventions que
build_hub.py / build_plaza.py / build_pvp.py).

Principe cle pour rester dans une taille de fichier raisonnable malgre les
500x500 : uniquement des blocs explicitement definis sont stockes (structure
"eparse" - cf. structure_lib), et tous les volumes (tours, murs, maisons)
sont construits en coques creuses (parois) plutot qu'en blocs pleins.
"""
import math
import random
import sys

sys.path.insert(0, "/home/user/serv/structures")
from structure_lib import StructureBuilder, verify, ascii_top

SIZE = 500
HEIGHT = 80
GROUND_Y = 20
CX = SIZE // 2
CZ = SIZE // 2

rng = random.Random(1337)

b = StructureBuilder(SIZE, HEIGHT, SIZE)


def in_bounds(x, z):
    return 0 <= x < SIZE and 0 <= z < SIZE


def set_ground(x, z, name="minecraft:grass_block"):
    if in_bounds(x, z):
        b.set_block(x, GROUND_Y, z, name)


def set_path(x, z, name="minecraft:polished_andesite"):
    if in_bounds(x, z):
        b.set_block(x, GROUND_Y, z, name)


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


print("Douves...")
carve_moat(CX, CZ, 34, 40)
print("  blocs:", len(b.blocks))


# ---------------------------------------------------------------- helpers
def hollow_cylinder(cx, cz, y0, y1, r, name, thickness=1):
    for y in range(y0, y1 + 1):
        for ang in range(0, 720):
            t = ang * math.pi / 360.0
            for tr in range(thickness):
                rr = r - tr
                x = round(cx + rr * math.cos(t))
                z = round(cz + rr * math.sin(t))
                if in_bounds(x, z) and 0 <= y < HEIGHT:
                    b.set_block(x, y, z, name)


def disk(cx, cz, y, r, name):
    r2 = r * r
    for x in range(max(0, cx - r), min(SIZE, cx + r + 1)):
        for z in range(max(0, cz - r), min(SIZE, cz + r + 1)):
            dx, dz = x - cx, z - cz
            if dx * dx + dz * dz <= r2 and 0 <= y < HEIGHT:
                b.set_block(x, y, z, name)


def cone_roof(cx, cz, y0, r, height, name):
    for i in range(height):
        y = y0 + i
        rr = max(0, r - i)
        if rr == 0:
            if 0 <= y < HEIGHT:
                b.set_block(cx, y, cz, name)
            continue
        for ang in range(0, 720):
            t = ang * math.pi / 360.0
            x = round(cx + rr * math.cos(t))
            z = round(cz + rr * math.sin(t))
            if in_bounds(x, z) and 0 <= y < HEIGHT:
                b.set_block(x, y, z, name)


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


def tower(cx, cz, r, wall_top, roof_h, wall_name="minecraft:stone_bricks",
          roof_name="minecraft:dark_prismarine", floors=()):
    hollow_cylinder(cx, cz, GROUND_Y, wall_top, r, wall_name, thickness=1)
    # merlons (crenelage) en haut
    for ang in range(0, 720, 60):
        t = ang * math.pi / 360.0
        x = round(cx + r * math.cos(t))
        z = round(cz + r * math.sin(t))
        if in_bounds(x, z):
            b.set_block(x, wall_top + 1, z, wall_name)
    for fy in floors:
        disk(cx, cz, fy, r - 1, "minecraft:spruce_planks")
    cone_roof(cx, cz, wall_top + 2, r, roof_h, roof_name)
    # fenetres (juste des trous, rien a placer)


def crenelated_wall(x0, z0, x1, z1, top_y, name="minecraft:stone_bricks"):
    length = max(abs(x1 - x0), abs(z1 - z0))
    for i in range(length + 1):
        t = i / max(1, length)
        x = round(x0 + (x1 - x0) * t)
        z = round(z0 + (z1 - z0) * t)
        for y in range(GROUND_Y, top_y + 1):
            b.set_block(x, y, z, name)
        if i % 2 == 0:
            b.set_block(x, top_y + 1, z, name)


# --------------------------------------------------------------- chateau
print("Chateau : donjon central...")
tower(CX, CZ, 12, GROUND_Y + 40, 14, floors=(GROUND_Y + 10, GROUND_Y + 20, GROUND_Y + 30))
print("  blocs:", len(b.blocks))

print("Chateau : tours d'angle...")
corner_r = 26
for dx, dz in [(-1, -1), (1, -1), (-1, 1), (1, 1)]:
    tx = CX + dx * corner_r
    tz = CZ + dz * corner_r
    tower(tx, tz, 6, GROUND_Y + 26, 8, floors=(GROUND_Y + 12,))
print("  blocs:", len(b.blocks))

print("Chateau : courtines...")
wall_top = GROUND_Y + 12
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
        # cote sud : on laisse une ouverture pour la porte principale
        crenelated_wall(x0, z0, CX - GATE_HALF, z1, wall_top)
        crenelated_wall(CX + GATE_HALF, z0, x1, z1, wall_top)
    else:
        crenelated_wall(x0, z0, x1, z1, wall_top)
print("  blocs:", len(b.blocks))

# porte principale (tour-porche)
print("Chateau : porche d'entree...")
gate_z = CZ + corner_r
box_shell(CX - GATE_HALF - 2, GROUND_Y, gate_z - 3, CX + GATE_HALF + 2, GROUND_Y + 16, gate_z + 3,
           "minecraft:stone_bricks", skip_top=False)
for x in range(CX - GATE_HALF, CX + GATE_HALF + 1):
    for y in range(GROUND_Y, GROUND_Y + 6):
        b.set_block(x, y, gate_z - 3, "minecraft:air")
        b.set_block(x, y, gate_z, "minecraft:air")
        b.set_block(x, y, gate_z + 3, "minecraft:air")
for x in range(CX - GATE_HALF, CX + GATE_HALF + 1):
    b.set_block(x, GROUND_Y + 6, gate_z - 3, "minecraft:stone_brick_wall")
print("  blocs:", len(b.blocks))

# pont-levis en dur (planches) au-dessus des douves, porte principale (sud)
print("Pont principal...")
for z in range(gate_z - 8, gate_z + 4):
    for x in range(CX - 3, CX + 4):
        b.set_block(x, GROUND_Y, z, "minecraft:oak_planks")
    for x in (CX - 3, CX + 3):
        b.set_block(x, GROUND_Y + 1, z, "minecraft:oak_fence")
print("  blocs:", len(b.blocks))

# 3 autres ponts (nord, est, ouest) plus modestes
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
def stall(cx, cz, facing_dx, facing_dz, wood="minecraft:oak", roof="minecraft:red_terracotta"):
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


print("Stands dans la cour...")
stall_ring_r = 18
stall_defs = [
    (0, "oak"), (45, "spruce"), (90, "oak"), (135, "birch"),
    (180, "oak"), (225, "spruce"), (270, "oak"), (315, "birch"),
]
for ang, wood in stall_defs:
    t = math.radians(ang)
    sx = round(CX + stall_ring_r * math.cos(t))
    sz = round(CZ + stall_ring_r * math.sin(t))
    stall(sx, sz, round(math.cos(t)), round(math.sin(t)), wood=wood)
print("  blocs:", len(b.blocks))


# --------------------------------------------------------------- village
def house(cx, cz, rot, variant):
    w, d, h = 7, 7, 5
    wall = {"a": "minecraft:oak_planks", "b": "minecraft:cobblestone", "c": "minecraft:spruce_planks"}[variant]
    roof = {"a": "minecraft:dark_oak_stairs", "b": "minecraft:stone_brick_stairs", "c": "minecraft:spruce_stairs"}[variant]
    log = {"a": "minecraft:oak_log", "b": "minecraft:cobblestone_wall", "c": "minecraft:spruce_log"}[variant]

    def rotate(lx, lz):
        if rot == 0:
            return lx, lz
        if rot == 90:
            return -lz, lx
        if rot == 180:
            return -lx, -lz
        return lz, -lx

    for lx in range(-w // 2, w // 2 + 1):
        for lz in range(-d // 2, d // 2 + 1):
            corner = abs(lx) == w // 2 and abs(lz) == d // 2
            b.set_block(*(lambda p: (p[0] + cx, GROUND_Y + 1, p[1] + cz))(rotate(lx, lz)),
                        log if corner else "minecraft:oak_planks")
    for wy in range(GROUND_Y + 1, GROUND_Y + 1 + h):
        for lx in range(-w // 2, w // 2 + 1):
            for lz in (-d // 2, d // 2):
                door = lx == 0 and lz == d // 2 and wy <= GROUND_Y + 2
                x, z = rotate(lx, lz)
                if not door:
                    b.set_block(x + cx, wy, z + cz, wall)
        for lz in range(-d // 2, d // 2 + 1):
            for lx in (-w // 2, w // 2):
                x, z = rotate(lx, lz)
                b.set_block(x + cx, wy, z + cz, wall)
    roof_y = GROUND_Y + 1 + h
    for i, lx in enumerate(range(-w // 2 - 1, w // 2 + 2)):
        ry = roof_y + min(i, w + 1 - i)
        for lz in range(-d // 2 - 1, d // 2 + 2):
            x, z = rotate(lx, lz)
            b.set_block(x + cx, ry, z + cz, "minecraft:dark_oak_planks" if variant != "b" else "minecraft:stone_bricks")
    x, z = rotate(0, d // 2 + 1)
    b.set_block(x + cx, GROUND_Y + 1, z + cz, "minecraft:oak_door")
    torch_x, torch_z = rotate(w // 2, -d // 2)
    b.set_block(torch_x + cx, GROUND_Y + 3, torch_z + cz, "minecraft:torch")


print("Village : maisons...")
houses = []
attempts = 0
while len(houses) < 26 and attempts < 4000:
    attempts += 1
    ang = rng.uniform(0, 2 * math.pi)
    r = rng.uniform(60, 210)
    hx = round(CX + r * math.cos(ang))
    hz = round(CZ + r * math.sin(ang))
    if any((hx - ox) ** 2 + (hz - oz) ** 2 < 22 ** 2 for ox, oz, _, _ in houses):
        continue
    if not (10 <= hx < SIZE - 10 and 10 <= hz < SIZE - 10):
        continue
    rot = rng.choice([0, 90, 180, 270])
    variant = rng.choice(["a", "b", "c"])
    houses.append((hx, hz, rot, variant))
    house(hx, hz, rot, variant)
print(f"  {len(houses)} maisons, blocs:", len(b.blocks))


# ------------------------------------------------------------------ lac
LAKE_X, LAKE_Z = CX - 150, CZ + 150
LAKE_R = 26
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
# cascade : une petite colline rocheuse au bord nord du lac qui deverse de l'eau
hill_x, hill_z = LAKE_X, LAKE_Z - LAKE_R - 4
for i in range(6):
    y = GROUND_Y + i
    r = 6 - i
    disk(hill_x, hill_z, y, max(1, r), "minecraft:stone")
for y in range(GROUND_Y, GROUND_Y + 6):
    b.set_block(hill_x, y, hill_z + 2, "minecraft:water")
print("  blocs:", len(b.blocks))

# pont vers le lac
for z in range(hill_z + 5, hill_z + 5 + 10):
    for x in range(hill_x - 2, hill_x + 3):
        b.set_block(x, GROUND_Y, z, "minecraft:spruce_planks")
    for x in (hill_x - 2, hill_x + 2):
        b.set_block(x, GROUND_Y + 1, z, "minecraft:spruce_fence")


# ---------------------------------------------------------------- chemins
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


print("Chemins...")
path_line(CX, gate_z + 4, CX, gate_z + 210, width=5)
path_line(CX - corner_r - 8, CZ, CX - corner_r - 210, CZ, width=5)
path_line(CX + corner_r + 8, CZ, CX + corner_r + 210, CZ, width=5)
path_line(CX, CZ - corner_r - 8, CX, CZ - corner_r - 210, width=5)
# anneau
ring_r = 100
steps = 260
prev = None
for i in range(steps + 1):
    t = 2 * math.pi * i / steps
    x = round(CX + ring_r * math.cos(t))
    z = round(CZ + ring_r * math.sin(t))
    if prev:
        path_line(prev[0], prev[1], x, z, width=3)
    prev = (x, z)
# desserte de chaque maison + lac
for hx, hz, _, _ in houses:
    path_line(hx, hz, CX + round((hx - CX) * 0.55), CZ + round((hz - CZ) * 0.55), width=2)
path_line(LAKE_X, LAKE_Z - LAKE_R - 2, CX - 70, CZ + 100, width=3)
print("  blocs:", len(b.blocks))


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
while tcount < 60 and attempts < 3000:
    attempts += 1
    ang = rng.uniform(0, 2 * math.pi)
    r = rng.uniform(60, 235)
    tx = round(CX + r * math.cos(ang))
    tz = round(CZ + r * math.sin(ang))
    if not (5 <= tx < SIZE - 5 and 5 <= tz < SIZE - 5):
        continue
    if any((tx - ox) ** 2 + (tz - oz) ** 2 < 14 ** 2 for ox, oz, _, _ in houses):
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
