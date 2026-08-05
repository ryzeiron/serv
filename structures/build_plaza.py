from nbtlib import Compound, List, Int, String, IntArray
from nbtlib.tag import Compound as C

SIZE_X, SIZE_Y, SIZE_Z = 21, 8, 21

# --- palette : on construit la liste des etats de bloc utilises (dedupliques) ---
palette = []
palette_index = {}


def block_state(name, **props):
    key = (name, tuple(sorted(props.items())))
    if key in palette_index:
        return palette_index[key]
    entry = C({"Name": String(name)})
    if props:
        entry["Properties"] = C({k: String(str(v)) for k, v in props.items()})
    palette.append(entry)
    idx = len(palette) - 1
    palette_index[key] = idx
    return idx


blocks = {}  # (x,y,z) -> palette index, ecrase dans l'ordre d'appel


def set_block(x, y, z, name, **props):
    if not (0 <= x < SIZE_X and 0 <= y < SIZE_Y and 0 <= z < SIZE_Z):
        raise ValueError(f"Hors limites: {(x, y, z)}")
    blocks[(x, y, z)] = block_state(name, **props)


def fill(x1, y1, z1, x2, y2, z2, name, **props):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                set_block(x, y, z, name, **props)


# ============================= SOL DE LA PLAZA =============================
fill(0, 0, 0, 20, 0, 20, "minecraft:stone_bricks")
for x in range(21):
    set_block(x, 0, 10, "minecraft:smooth_stone")
for z in range(21):
    set_block(10, 0, z, "minecraft:smooth_stone")

# ============================= FONTAINE CENTRALE =============================
fill(9, 0, 9, 11, 0, 11, "minecraft:cobblestone")
for x in range(9, 12):
    for z in range(9, 12):
        if (x, z) != (10, 10):
            set_block(x, 1, z, "minecraft:cobblestone")
set_block(10, 1, 10, "minecraft:water", level="0")

# 4 lampadaires aux coins de la fontaine
for (px, pz) in [(8, 8), (12, 8), (8, 12), (12, 12)]:
    set_block(px, 1, pz, "minecraft:oak_fence")
    set_block(px, 2, pz, "minecraft:oak_fence")
    set_block(px, 3, pz, "minecraft:lantern", hanging="false")

# ============================= BORDURE DE LA PLAZA =============================
for x in range(21):
    if x in (9, 10, 11):
        continue
    set_block(x, 1, 0, "minecraft:oak_fence")
    set_block(x, 1, 20, "minecraft:oak_fence")
for z in range(21):
    if z in (9, 10, 11):
        continue
    set_block(0, 1, z, "minecraft:oak_fence")
    set_block(20, 1, z, "minecraft:oak_fence")


# ============================= ETALS =============================
def stall_north():
    # dos au nord (z=2), ouvert vers le sud (plaza), largeur en x [8..12]
    fill(8, 1, 2, 12, 3, 2, "minecraft:oak_planks")  # mur du fond
    fill(8, 1, 2, 8, 3, 4, "minecraft:oak_log", axis="y")  # poteau gauche
    fill(12, 1, 2, 12, 3, 4, "minecraft:oak_log", axis="y")  # poteau droit
    for x in range(8, 13):
        set_block(x, 1, 4, "minecraft:oak_fence")  # comptoir
    fill(8, 4, 2, 12, 4, 4, "minecraft:oak_slab", type="bottom")  # toit
    set_block(10, 3, 2, "minecraft:yellow_concrete")  # marqueur : consommables
    set_block(10, 1, 3, "minecraft:composter", level="0")  # metier : fermier


def stall_south():
    fill(8, 1, 18, 12, 3, 18, "minecraft:oak_planks")
    fill(8, 1, 16, 8, 3, 18, "minecraft:oak_log", axis="y")
    fill(12, 1, 16, 12, 3, 18, "minecraft:oak_log", axis="y")
    for x in range(8, 13):
        set_block(x, 1, 16, "minecraft:oak_fence")
    fill(8, 4, 16, 12, 4, 18, "minecraft:oak_slab", type="bottom")
    set_block(10, 3, 18, "minecraft:light_blue_concrete")  # marqueur : rares
    set_block(10, 1, 17, "minecraft:cartography_table")  # metier : cartographe


def stall_east():
    fill(18, 1, 8, 18, 3, 12, "minecraft:oak_planks")
    fill(16, 1, 8, 18, 3, 8, "minecraft:oak_log", axis="y")
    fill(16, 1, 12, 18, 3, 12, "minecraft:oak_log", axis="y")
    for z in range(8, 13):
        set_block(16, 1, z, "minecraft:oak_fence")
    fill(16, 4, 8, 18, 4, 12, "minecraft:oak_slab", type="bottom")
    set_block(18, 3, 10, "minecraft:light_gray_concrete")  # marqueur : minerais
    set_block(17, 1, 10, "minecraft:grindstone", face="floor", facing="north")  # metier : armurier

def stall_west():
    fill(2, 1, 8, 2, 3, 12, "minecraft:oak_planks")
    fill(2, 1, 8, 4, 3, 8, "minecraft:oak_log", axis="y")
    fill(2, 1, 12, 4, 3, 12, "minecraft:oak_log", axis="y")
    for z in range(8, 13):
        set_block(4, 1, z, "minecraft:oak_fence")
    fill(2, 4, 8, 4, 4, 12, "minecraft:oak_slab", type="bottom")
    set_block(2, 3, 10, "minecraft:brown_concrete")  # marqueur : matieres premieres
    set_block(3, 1, 10, "minecraft:stonecutter", facing="north")  # metier : macon


stall_north()
stall_south()
stall_east()
stall_west()

# ============================= EXPORT NBT =============================
blocks_list = List[C]([
    C({"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(state)})
    for (x, y, z), state in sorted(blocks.items())
])

structure = Compound({
    "DataVersion": Int(3465),  # 1.20.1 ; le jeu applique ses propres data fixers a la volee
    "size": List[Int]([Int(SIZE_X), Int(SIZE_Y), Int(SIZE_Z)]),
    "entities": List[C]([]),
    "blocks": blocks_list,
    "palette": List[C](palette),
})

from nbtlib import File

nbt_file = File(structure, gzipped=True)
nbt_file.save("place_du_marche.nbt")

print(f"OK : {len(blocks)} blocs places, {len(palette)} etats dans la palette")
