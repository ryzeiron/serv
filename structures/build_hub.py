import math
from structure_lib import StructureBuilder

SIZE = 49
CY = 24  # centre x et z
sb = StructureBuilder(SIZE, 24, SIZE)

R = 22  # rayon de la plaza

# ============================= PLATEFORME =============================
sb.fill_disk(CY, 0, CY, R, "minecraft:quartz_block")
sb.ring(CY, 0, CY, R - 2, R, "minecraft:chiseled_quartz_block")

# chemin en croix
for x in range(SIZE):
    if (x - CY) ** 2 <= R * R:
        sb.set_block(x, 0, CY - 1, "minecraft:smooth_stone")
        sb.set_block(x, 0, CY, "minecraft:smooth_stone")
        sb.set_block(x, 0, CY + 1, "minecraft:smooth_stone")
for z in range(SIZE):
    if (z - CY) ** 2 <= R * R:
        sb.set_block(CY - 1, 0, z, "minecraft:smooth_stone")
        sb.set_block(CY, 0, z, "minecraft:smooth_stone")
        sb.set_block(CY + 1, 0, z, "minecraft:smooth_stone")

# ============================= PARAPET =============================
# muret bas sur le pourtour, avec 4 ouvertures (N/S/E/W) alignees sur le chemin en croix
for angle_deg in range(0, 360, 2):
    angle = math.radians(angle_deg)
    x = round(CY + R * math.cos(angle))
    z = round(CY + R * math.sin(angle))
    if abs(x - CY) <= 2 or abs(z - CY) <= 2:
        continue  # ouverture au niveau des 4 chemins
    if 0 <= x < SIZE and 0 <= z < SIZE:
        sb.set_block(x, 1, z, "minecraft:quartz_block")

# ============================= TOUR CENTRALE (pyramide a degres + beacon) =============================
sb.fill_disk(CY, 1, CY, 8, "minecraft:gold_block")
sb.fill_disk(CY, 2, CY, 8, "minecraft:gold_block")
sb.fill_disk(CY, 3, CY, 6, "minecraft:quartz_block")
sb.fill_disk(CY, 4, CY, 6, "minecraft:quartz_block")
sb.fill_disk(CY, 5, CY, 4, "minecraft:gold_block")
sb.fill_disk(CY, 6, CY, 4, "minecraft:gold_block")
sb.fill_disk(CY, 7, CY, 2, "minecraft:quartz_block")
# socle mineral du beacon (3x3 minimum pour activer un beacon niveau 1)
sb.fill(CY - 1, 8, CY - 1, CY + 1, 8, CY + 1, "minecraft:iron_block")
sb.set_block(CY, 9, CY, "minecraft:beacon")

# ============================= COLONNES LUMINEUSES (8 autour de la plateforme) =============================
POST_RADIUS = 18
for angle_deg in range(0, 360, 45):
    angle = math.radians(angle_deg)
    x = round(CY + POST_RADIUS * math.cos(angle))
    z = round(CY + POST_RADIUS * math.sin(angle))
    sb.fill(x, 1, z, x, 4, z, "minecraft:quartz_pillar", axis="y")
    sb.set_block(x, 5, z, "minecraft:sea_lantern")

path = "spawn_hub.nbt"
count, palette_count = sb.save(path)
print(f"OK hub : {count} blocs, {palette_count} etats de palette")
