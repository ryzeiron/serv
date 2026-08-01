from PIL import Image, ImageDraw, ImageFont
import random

# Minecraft calcule les UV de la texture generique de conteneur sur une toile
# de 256x256 (convention historique des textures gui/container/*), meme si le
# contenu reellement dessine ne remplit que 176x222 en haut a gauche. Sans ce
# padding, le rendu en jeu n'echantillonne qu'une fraction de l'image et
# l'affiche etiree sur toute la fenetre (effet "trop zoome").
CANVAS = 256
W, H = 176, 222

WOOD = (90, 58, 28, 255)
WOOD_DARK = (58, 36, 16, 255)
PARCH_LIGHT = (230, 209, 156, 255)
PARCH_DARK = (162, 128, 63, 255)
SLOT_FILL = (184, 154, 92, 255)
INK = (74, 47, 22, 255)
HILITE = (240, 224, 173, 255)

img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
draw = ImageDraw.Draw(img)

# fond parchemin en degrade vertical
for y in range(H):
    t = y / H
    r = int(PARCH_LIGHT[0] + (PARCH_DARK[0] - PARCH_LIGHT[0]) * t)
    g = int(PARCH_LIGHT[1] + (PARCH_DARK[1] - PARCH_LIGHT[1]) * t)
    b = int(PARCH_LIGHT[2] + (PARCH_DARK[2] - PARCH_LIGHT[2]) * t)
    draw.line([(0, y), (W, y)], fill=(r, g, b, 255))

# taches douces (aspect vieilli)
random.seed(7)
for _ in range(16):
    cx = random.randint(0, W)
    cy = random.randint(0, H)
    r = random.randint(10, 26)
    lighten = random.random() > 0.5
    blot = Image.new("RGBA", (r * 2, r * 2), (0, 0, 0, 0))
    bd = ImageDraw.Draw(blot)
    if lighten:
        bd.ellipse([0, 0, r * 2, r * 2], fill=(255, 255, 255, 16))
    else:
        bd.ellipse([0, 0, r * 2, r * 2], fill=(60, 38, 12, 14))
    img.alpha_composite(blot, (cx - r, cy - r))

draw = ImageDraw.Draw(img)

# cadre bois
BORDER = 4
draw.rectangle([0, 0, W - 1, BORDER - 1], fill=WOOD)
draw.rectangle([0, H - BORDER, W - 1, H - 1], fill=WOOD)
draw.rectangle([0, 0, BORDER - 1, H - 1], fill=WOOD)
draw.rectangle([W - BORDER, 0, W - 1, H - 1], fill=WOOD)
draw.rectangle([BORDER, BORDER, W - BORDER - 1, BORDER], fill=WOOD_DARK)
draw.rectangle([BORDER, H - BORDER - 1, W - BORDER - 1, H - BORDER], fill=WOOD_DARK)


def draw_slot(x, y):
    # Le jeu dessine l'icone de l'item calee (sans marge) sur (x, y) en 16x16 :
    # la case visuelle doit donc etre centree autour de ce point, pas partir
    # de (x, y) comme origine haut-gauche, sinon l'icone parait collee en
    # haut a gauche de sa case.
    x0, y0, x1, y1 = x - 1, y - 1, x + 16, y + 16
    draw.rectangle([x0, y0, x1, y1], fill=SLOT_FILL)
    draw.line([(x0, y0), (x1, y0)], fill=INK)
    draw.line([(x0, y0), (x0, y1)], fill=INK)
    draw.line([(x1, y0), (x1, y1)], fill=HILITE)
    draw.line([(x0, y1), (x1, y1)], fill=HILITE)


# Coordonnees alignees sur ChestMenu (vanilla) : addSlot(x, y) = (8 + col*18, 18 + row*18)
# pour les rangees de contenu. Le bloc "inventaire joueur" est toujours
# echantillonne a v=126 dans la texture source, quel que soit le nombre de
# rangees du conteneur (126 = 18 + 6*18, soit exactement la fin du bloc
# contenu pour un conteneur a 6 rangees) : les deux blocs sont donc adjacents
# sans marge dans le fichier source.
SLOT_X0 = 8
CONTENT_Y0 = 18
COLS = 9
CONTENT_ROWS = 6
PLAYER_INV_Y0 = 140
# vanilla laisse un espace de 4px de plus entre les 3 rangees d'inventaire et
# la hotbar (invY + 58, pas +54) pour bien separer visuellement les deux
HOTBAR_Y0 = PLAYER_INV_Y0 + 58

for row in range(CONTENT_ROWS):
    for col in range(COLS):
        draw_slot(SLOT_X0 + col * 18, CONTENT_Y0 + row * 18)

for row in range(3):
    for col in range(COLS):
        draw_slot(SLOT_X0 + col * 18, PLAYER_INV_Y0 + row * 18)

for col in range(COLS):
    draw_slot(SLOT_X0 + col * 18, HOTBAR_Y0)

canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
canvas.alpha_composite(img, (0, 0))
canvas.save("assets/minecraft/textures/gui/container/generic_54.png")

# icone du resource pack
icon = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
idraw = ImageDraw.Draw(icon)
idraw.rectangle([0, 0, 127, 127], fill=(205, 176, 104, 255))
idraw.ellipse([32, 32, 96, 96], fill=(110, 28, 20, 255))
idraw.ellipse([32, 32, 96, 96], outline=(70, 16, 11, 255), width=3)
try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf", 40)
    bbox = idraw.textbbox((0, 0), "M", font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    idraw.text((64 - tw / 2 - bbox[0], 64 - th / 2 - bbox[1]), "M", font=font, fill=(233, 201, 143, 255))
except Exception as e:
    print("font fallback:", e)
icon.save("pack.png")

print("done")
