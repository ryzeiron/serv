from structure_lib import StructureBuilder

SIZE = 9
sb = StructureBuilder(SIZE, 9, SIZE)
CY = 4

# ============================= ILE (petit bloc de terre classique skyblock) =============================
sb.fill_disk(CY, 2, CY, 3, "minecraft:grass_block")
sb.fill_disk(CY, 1, CY, 3, "minecraft:dirt")
sb.fill_disk(CY, 0, CY, 2, "minecraft:stone")

# ============================= ARBRE =============================
tx, tz = CY - 2, CY - 2
sb.fill(tx, 3, tz, tx, 6, tz, "minecraft:oak_log", axis="y")
for y in (5, 6):
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if abs(dx) == 2 and abs(dz) == 2:
                continue
            sb.set_block(tx + dx, y, tz + dz, "minecraft:oak_leaves", persistent="true", distance="1")
sb.set_block(tx, 7, tz, "minecraft:oak_leaves", persistent="true", distance="1")

# ============================= COFFRE DE DEPART (rempli par le code Java a la pose) =============================
sb.set_block(CY + 1, 3, CY + 1, "minecraft:chest", facing="south")

path = "starter_island.nbt"
count, palette_count = sb.save(path)
print(f"OK starter island : {count} blocs, {palette_count} etats de palette")
