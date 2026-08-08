from structure_lib import StructureBuilder

SIZE_XZ = 25
SIZE_Y = 12
CY = 12
sb = StructureBuilder(SIZE_XZ, SIZE_Y, SIZE_XZ)

# ============================= ILE FLOTTANTE (taille en cone vers le bas) =============================
sb.fill_disk(CY, 5, CY, 10, "minecraft:grass_block")
sb.fill_disk(CY, 4, CY, 10, "minecraft:dirt")
sb.fill_disk(CY, 3, CY, 9, "minecraft:stone")
sb.fill_disk(CY, 2, CY, 7, "minecraft:stone")
sb.fill_disk(CY, 1, CY, 5, "minecraft:stone")
sb.fill_disk(CY, 0, CY, 3, "minecraft:stone")

# ============================= ZONES DE SPAWN (symetriques est/ouest) =============================
sb.fill(CY - 8, 6, CY - 2, CY - 6, 6, CY + 2, "minecraft:red_concrete")
sb.fill(CY + 6, 6, CY - 2, CY + 8, 6, CY + 2, "minecraft:blue_concrete")

# ============================= COUVERTS SYMETRIQUES (4 blocs casse-ligne-de-vue) =============================
for (dx, dz) in [(-4, -4), (4, -4), (-4, 4), (4, 4)]:
    x, z = CY + dx, CY + dz
    sb.fill(x, 6, z, x, 7, z, "minecraft:cobblestone_wall")

# ============================= MARQUAGE CENTRAL =============================
sb.set_block(CY, 6, CY, "minecraft:white_concrete")

path = "pvp_island.nbt"
count, palette_count = sb.save(path)
print(f"OK pvp : {count} blocs, {palette_count} etats de palette")
