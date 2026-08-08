"""Genere les textures 16x16 pour les items/blocs custom du metier Hacker/Mineur :
Lingot de Lithium, Plastique, Ordinateur (item + bloc jukebox reskinne), et le
"minerai de lithium" (bloc calcite reskinne). Complete le meme resource pack
que build_texture.py (menu de marche).
"""
import os
from PIL import Image, ImageDraw

ITEM_DIR = "assets/minecraft/textures/item"
BLOCK_DIR = "assets/minecraft/textures/block"
os.makedirs(ITEM_DIR, exist_ok=True)
os.makedirs(BLOCK_DIR, exist_ok=True)

S = 16


def new_item_canvas():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


# ------------------------------------------------------------ Lingot de Lithium
# Remplace directement la texture vanilla de l'amethyst_shard (pas de modele/override
# custom_model_data : sur 1.21.2+, Mojang a change la selection de modele d'ITEM et
# l'ancien systeme d'overrides n'est plus fiable -- alors qu'un simple remplacement de
# texture, comme pour les blocs, marche quelle que soit la version)
def build_lithium_ingot():
    img = new_item_canvas()
    d = ImageDraw.Draw(img)
    outline = (50, 51, 58, 255)
    shadow = (92, 93, 102, 255)
    base = (146, 148, 156, 255)
    light = (196, 198, 206, 255)
    hilite = (222, 224, 232, 255)
    violet = (168, 150, 205, 255)
    violet_light = (196, 180, 224, 255)

    # silhouette trapezoidale (large en bas, plus etroite en haut), style lingot vanilla
    poly = [(4, 6), (11, 6), (13, 9), (13, 11), (10, 12), (5, 12), (2, 11), (2, 9)]
    d.polygon(poly, fill=base, outline=outline)
    # face superieure plus claire, avec un reflet violet (le "lithium" du lingot de fer)
    d.polygon([(4, 6), (11, 6), (12, 7), (11, 8), (4, 8), (3, 7)], fill=light)
    d.line([(5, 7), (9, 7)], fill=hilite)
    d.point([(6, 6)], fill=violet_light)
    d.point([(9, 6)], fill=violet)
    d.point([(7, 7)], fill=violet)
    # ombre basse
    d.line([(3, 11), (10, 11)], fill=shadow)
    d.point([(3, 10)], fill=shadow)
    d.point([(12, 10)], fill=shadow)
    img.save(f"{ITEM_DIR}/amethyst_shard.png")


# ------------------------------------------------------------------- Plastique
# Remplace directement la texture vanilla du slime_ball, meme logique que le lithium
def build_plastic():
    img = new_item_canvas()
    d = ImageDraw.Draw(img)
    outline = (135, 137, 140, 255)
    base = (218, 219, 221, 255)
    light = (240, 241, 242, 255)
    shadow = (185, 186, 189, 255)
    crease = (160, 161, 165, 255)

    # eclat de "papier" plastique, contour volontairement irregulier/pas net (bosses,
    # coins casses) plutot qu'une silhouette propre
    poly = [(3, 4), (6, 3), (8, 4), (10, 2), (12, 4), (13, 6), (12, 8),
            (13, 10), (11, 12), (12, 13), (8, 13), (7, 12), (4, 13),
            (3, 11), (4, 9), (2, 8), (3, 6), (2, 5)]
    d.polygon(poly, fill=base, outline=outline)
    # bosses / plis irreguliers (petites taches claires et sombres, pas des lignes nettes)
    for x, y, c in [
        (5, 5, light), (9, 4, light), (11, 6, shadow), (6, 8, shadow),
        (9, 9, light), (4, 10, crease), (10, 11, crease), (7, 6, crease),
        (8, 10, light), (5, 11, shadow),
    ]:
        d.point([(x, y)], fill=c)
        d.point([(x + 1, y)], fill=c)
    img.save(f"{ITEM_DIR}/slime_ball.png")


# --------------------------------------------------- Minerai de lithium (calcite)
def build_calcite_ore():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    rock_dark = (58, 60, 66, 255)
    rock = (72, 74, 81, 255)
    rock_light = (90, 93, 100, 255)
    crystal = (214, 218, 224, 255)
    crystal_light = (238, 240, 244, 255)
    crystal_glow = (176, 200, 235, 255)

    # texture de roche sombre en fond, avec un peu de bruit deterministe
    for y in range(S):
        for x in range(S):
            h = (x * 928371 + y * 128371) % 100
            c = rock_dark if h < 35 else rock if h < 75 else rock_light
            d.point([(x, y)], fill=c)

    # eclats de cristal (grappes anguleuses) representant le lithium
    clusters = [(3, 3), (11, 2), (2, 11), (12, 10), (7, 7)]
    for cx, cy in clusters:
        d.point([(cx, cy)], fill=crystal_light)
        for dx, dz in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            d.point([(cx + dx, cy + dz)], fill=crystal)
        for dx, dz in [(-1, -1), (1, 1)]:
            d.point([(cx + dx, cy + dz)], fill=crystal_glow)
    img.save(f"{BLOCK_DIR}/calcite.png")


# ------------------------------------------------------- Ordinateur (bloc jukebox)
def build_computer_block():
    case = (70, 74, 80, 255)
    case_dark = (44, 47, 52, 255)
    case_light = (108, 112, 118, 255)
    vent = (30, 33, 38, 255)
    led = (95, 235, 140, 255)

    # dessus : grille de ventilation + led
    top = Image.new("RGBA", (S, S), case)
    dt = ImageDraw.Draw(top)
    dt.rectangle([0, 0, S - 1, S - 1], outline=case_dark)
    for i in range(1, 15, 2):
        dt.line([(i, 2), (i, 13)], fill=vent)
    dt.rectangle([1, 1, S - 2, 2], fill=case_light)
    dt.point([(13, 13)], fill=led)
    dt.point([(12, 13)], fill=led)
    top.save(f"{BLOCK_DIR}/jukebox_top.png")

    # cotes : panneau avec lignes de tole + un petit port/prise
    side = Image.new("RGBA", (S, S), case)
    ds = ImageDraw.Draw(side)
    ds.rectangle([0, 0, S - 1, S - 1], outline=case_dark)
    ds.line([(0, 5), (S - 1, 5)], fill=case_dark)
    ds.line([(0, 11), (S - 1, 11)], fill=case_dark)
    ds.rectangle([1, 1, S - 2, 1], fill=case_light)
    ds.rectangle([6, 7, 9, 9], fill=case_dark)
    ds.rectangle([6, 7, 9, 9], outline=(20, 22, 26, 255))
    side.save(f"{BLOCK_DIR}/jukebox_side.png")


# -------------------------------------------------- Ordinateur portable (bloc pose)
def build_laptop_base():
    case = (150, 152, 156, 255)
    case_dark = (110, 112, 116, 255)
    key = (95, 97, 102, 255)
    key_dark = (70, 72, 76, 255)
    trackpad = (170, 172, 176, 255)

    img = Image.new("RGBA", (S, S), case)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=case_dark)
    # grille de touches
    for row in range(3):
        for col in range(5):
            x0 = 1 + col * 3
            y0 = 1 + row * 3
            d.rectangle([x0, y0, x0 + 2, y0 + 2], fill=key, outline=key_dark)
    # trackpad
    d.rectangle([5, 11, 10, 14], fill=trackpad, outline=case_dark)
    img.save(f"{BLOCK_DIR}/laptop_base.png")


def build_laptop_screen():
    bezel = (58, 60, 65, 255)
    bezel_dark = (35, 37, 40, 255)
    screen = (16, 20, 28, 255)
    text_green = (95, 235, 140, 255)
    text_dim = (55, 150, 90, 255)
    cam = (20, 20, 22, 255)

    img = Image.new("RGBA", (S, S), bezel)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=bezel_dark)
    d.point([(8, 1)], fill=cam)
    d.rectangle([2, 2, 13, 13], fill=screen)
    d.line([(3, 4), (10, 4)], fill=text_green)
    d.line([(3, 6), (12, 6)], fill=text_dim)
    d.line([(3, 8), (9, 8)], fill=text_dim)
    d.line([(3, 10), (11, 10)], fill=text_dim)
    d.line([(3, 12), (7, 12)], fill=text_green)
    img.save(f"{BLOCK_DIR}/laptop_screen.png")


build_lithium_ingot()
build_plastic()
build_calcite_ore()
build_computer_block()
build_laptop_base()
build_laptop_screen()
print("textures generees")
