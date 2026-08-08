"""Genere les textures 16x16 des items/blocs du mod MarketEconomy : ce sont de vraies
nouvelles textures pour de vrais nouveaux items/blocs enregistres (pas des reskins de
textures vanilla comme dans la version plugin Paper).
"""
import os
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(__file__), "src", "main", "resources", "assets", "marketeconomy", "textures")
ITEM_DIR = os.path.join(BASE, "item")
BLOCK_DIR = os.path.join(BASE, "block")
os.makedirs(ITEM_DIR, exist_ok=True)
os.makedirs(BLOCK_DIR, exist_ok=True)

S = 16


def new_canvas():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def build_lithium_ingot():
    img = new_canvas()
    d = ImageDraw.Draw(img)
    outline = (50, 51, 58, 255)
    shadow = (92, 93, 102, 255)
    base = (146, 148, 156, 255)
    light = (196, 198, 206, 255)
    hilite = (222, 224, 232, 255)
    violet = (168, 150, 205, 255)
    violet_light = (196, 180, 224, 255)

    poly = [(4, 6), (11, 6), (13, 9), (13, 11), (10, 12), (5, 12), (2, 11), (2, 9)]
    d.polygon(poly, fill=base, outline=outline)
    d.polygon([(4, 6), (11, 6), (12, 7), (11, 8), (4, 8), (3, 7)], fill=light)
    d.line([(5, 7), (9, 7)], fill=hilite)
    d.point([(6, 6)], fill=violet_light)
    d.point([(9, 6)], fill=violet)
    d.point([(7, 7)], fill=violet)
    d.line([(3, 11), (10, 11)], fill=shadow)
    d.point([(3, 10)], fill=shadow)
    d.point([(12, 10)], fill=shadow)
    img.save(f"{ITEM_DIR}/lithium_ingot.png")


def build_plastic():
    img = new_canvas()
    d = ImageDraw.Draw(img)
    outline = (135, 137, 140, 255)
    base = (218, 219, 221, 255)
    light = (240, 241, 242, 255)
    shadow = (185, 186, 189, 255)
    crease = (160, 161, 165, 255)

    poly = [(3, 4), (6, 3), (8, 4), (10, 2), (12, 4), (13, 6), (12, 8),
            (13, 10), (11, 12), (12, 13), (8, 13), (7, 12), (4, 13),
            (3, 11), (4, 9), (2, 8), (3, 6), (2, 5)]
    d.polygon(poly, fill=base, outline=outline)
    for x, y, c in [
        (5, 5, light), (9, 4, light), (11, 6, shadow), (6, 8, shadow),
        (9, 9, light), (4, 10, crease), (10, 11, crease), (7, 6, crease),
        (8, 10, light), (5, 11, shadow),
    ]:
        d.point([(x, y)], fill=c)
        d.point([(x + 1, y)], fill=c)
    img.save(f"{ITEM_DIR}/plastic.png")


def build_ordinateur_case():
    case = (70, 74, 80, 255)
    case_dark = (44, 47, 52, 255)
    case_light = (108, 112, 118, 255)

    img = Image.new("RGBA", (S, S), case)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=case_dark)
    d.line([(0, 5), (S - 1, 5)], fill=case_dark)
    d.line([(0, 11), (S - 1, 11)], fill=case_dark)
    d.rectangle([1, 1, S - 2, 1], fill=case_light)
    d.rectangle([6, 7, 9, 9], fill=case_dark)
    d.rectangle([6, 7, 9, 9], outline=(20, 22, 26, 255))
    img.save(f"{BLOCK_DIR}/ordinateur_case.png")


def build_ordinateur_base():
    case = (150, 152, 156, 255)
    case_dark = (110, 112, 116, 255)
    key = (95, 97, 102, 255)
    key_dark = (70, 72, 76, 255)
    trackpad = (170, 172, 176, 255)

    img = Image.new("RGBA", (S, S), case)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=case_dark)
    for row in range(3):
        for col in range(5):
            x0 = 1 + col * 3
            y0 = 1 + row * 3
            d.rectangle([x0, y0, x0 + 2, y0 + 2], fill=key, outline=key_dark)
    d.rectangle([5, 11, 10, 14], fill=trackpad, outline=case_dark)
    img.save(f"{BLOCK_DIR}/ordinateur_base.png")


def build_ordinateur_screen():
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
    img.save(f"{BLOCK_DIR}/ordinateur_screen.png")


build_lithium_ingot()
build_plastic()
build_ordinateur_case()
build_ordinateur_base()
build_ordinateur_screen()
print("textures generees")
