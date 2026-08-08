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
def build_lithium_ingot():
    img = new_item_canvas()
    d = ImageDraw.Draw(img)
    outline = (55, 56, 62, 255)
    shadow = (100, 101, 108, 255)
    base = (158, 160, 168, 255)
    light = (205, 207, 214, 255)
    hilite = (230, 232, 238, 255)

    # silhouette trapezoidale (large en bas, plus etroite en haut), style lingot vanilla
    poly = [(4, 6), (11, 6), (13, 9), (13, 11), (10, 12), (5, 12), (2, 11), (2, 9)]
    d.polygon(poly, fill=base, outline=outline)
    # face superieure plus claire
    d.polygon([(4, 6), (11, 6), (12, 7), (11, 8), (4, 8), (3, 7)], fill=light)
    d.line([(5, 7), (10, 7)], fill=hilite)
    # ombre basse
    d.line([(3, 11), (10, 11)], fill=shadow)
    d.point([(3, 10)], fill=shadow)
    d.point([(12, 10)], fill=shadow)
    img.save(f"{ITEM_DIR}/lithium_ingot.png")


# ------------------------------------------------------------------- Plastique
def build_plastic():
    img = new_item_canvas()
    d = ImageDraw.Draw(img)
    outline = (120, 122, 126, 255)
    base = (214, 216, 219, 255)
    light = (238, 239, 241, 255)
    shadow = (172, 174, 178, 255)

    # eclat de plastique froisse : forme irreguliere
    poly = [(3, 5), (7, 3), (11, 4), (13, 7), (12, 10), (13, 12), (9, 13),
            (6, 12), (3, 13), (2, 10), (4, 8), (2, 6)]
    d.polygon(poly, fill=base, outline=outline)
    # plis / reflets
    d.line([(5, 5), (8, 7)], fill=light)
    d.line([(9, 6), (11, 8)], fill=light)
    d.line([(4, 10), (7, 11)], fill=shadow)
    d.line([(8, 9), (11, 11)], fill=shadow)
    d.point([(6, 8)], fill=light)
    d.point([(10, 5)], fill=light)
    img.save(f"{ITEM_DIR}/plastic.png")


# -------------------------------------------------------- Ordinateur (item icon)
def build_computer_item():
    img = new_item_canvas()
    d = ImageDraw.Draw(img)
    case = (86, 90, 96, 255)
    case_light = (130, 134, 140, 255)
    case_dark = (52, 55, 60, 255)
    screen = (18, 22, 30, 255)
    text_green = (90, 230, 130, 255)
    text_dim = (50, 150, 85, 255)
    power = (255, 90, 90, 255)

    # bezel du moniteur
    d.rectangle([1, 1, 14, 10], fill=case, outline=case_dark)
    d.line([(1, 1), (14, 1)], fill=case_light)
    d.line([(1, 1), (1, 10)], fill=case_light)
    # ecran
    d.rectangle([3, 3, 12, 8], fill=screen)
    # lignes de terminal
    d.line([(4, 4), (9, 4)], fill=text_green)
    d.line([(4, 6), (11, 6)], fill=text_dim)
    d.line([(4, 7), (7, 7)], fill=text_dim)
    # led d'alimentation
    d.point([(13, 9)], fill=power)
    # pied + socle
    d.rectangle([7, 11, 8, 11], fill=case_dark)
    d.rectangle([5, 12, 10, 13], fill=case)
    d.line([(5, 12), (10, 12)], fill=case_light)
    img.save(f"{ITEM_DIR}/computer.png")


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
build_computer_item()
build_calcite_ore()
build_computer_block()
build_laptop_base()
build_laptop_screen()
print("textures generees")
