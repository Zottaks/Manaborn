"""
Генератор UV-совместимой текстуры для `MageModel`.

Вдохновение:
- Villager Magician: капюшон, читаемое лицо торговца
- Minecraft Dungeons Mystery Merchant: тёмная фиолетовая мантия
- Luxury Merchant: золотая отделка и ощущение дорогого торговца
"""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parent
TEXTURE_PATH = ROOT / "src" / "main" / "resources" / "assets" / "manaborn" / "textures" / "entity" / "mage.png"
PREVIEW_PATH = ROOT / "mage_merchant_preview.png"

TEXTURE_SIZE = 128
PREVIEW_SIZE = 512

PURPLE_SHADOW = (24, 13, 43, 255)
PURPLE_DEEP = (40, 21, 72, 255)
PURPLE_DARK = (58, 32, 101, 255)
PURPLE_MID = (83, 53, 138, 255)
PURPLE_LIGHT = (111, 79, 176, 255)

GOLD_SHADOW = (120, 84, 11, 255)
GOLD_DARK = (170, 125, 21, 255)
GOLD_MID = (216, 173, 43, 255)
GOLD_LIGHT = (250, 219, 120, 255)

SKIN_SHADOW = (138, 104, 78, 255)
SKIN_MID = (177, 137, 104, 255)
SKIN_LIGHT = (206, 166, 128, 255)
SKIN_HIGHLIGHT = (227, 194, 162, 255)

LEATHER_DARK = (68, 42, 25, 255)
LEATHER_MID = (104, 67, 40, 255)
LEATHER_LIGHT = (140, 95, 60, 255)

BOOT_DARK = (34, 21, 18, 255)
BOOT_LIGHT = (63, 43, 36, 255)

EYE_WHITE = (242, 243, 247, 255)
EYE_DARK = (36, 26, 20, 255)
ARCANE_BLUE = (83, 178, 255, 255)
ARCANE_CYAN = (171, 238, 255, 255)
VOID_BLACK = (0, 0, 0, 255)
VOID_DARK = (10, 10, 10, 255)
VOID_SOFT = (22, 22, 22, 255)


def box_faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "left": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "right": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


def fill_rect(draw, rect, color):
    x, y, w, h = rect
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def line_h(draw, x0, x1, y, color):
    draw.line((x0, y, x1, y), fill=color)


def line_v(draw, x, y0, y1, color):
    draw.line((x, y0, x, y1), fill=color)


def gradient_face(draw, rect, top, mid, bottom):
    x, y, w, h = rect
    for row in range(h):
        ratio = row / max(1, h - 1)
        if ratio < 0.35:
            color = top
        elif ratio < 0.75:
            color = mid
        else:
            color = bottom
        line_h(draw, x, x + w - 1, y + row, color)


def cloth_face(draw, rect, base, shadow, highlight, stripe_every=3):
    x, y, w, h = rect
    fill_rect(draw, rect, base)
    for column in range(w):
        actual_x = x + column
        if column % stripe_every == 0:
            line_v(draw, actual_x, y, y + h - 1, highlight)
        elif column % stripe_every == stripe_every - 1:
            line_v(draw, actual_x, y, y + h - 1, shadow)


def robe_face(draw, rect, base, shadow, highlight, folds=()):
    x, y, w, h = rect
    gradient_face(draw, rect, highlight, base, shadow)
    line_h(draw, x, x + w - 1, y, highlight)
    line_h(draw, x, x + w - 1, y + h - 1, shadow)
    if w >= 2:
        line_v(draw, x, y, y + h - 1, shadow)
        line_v(draw, x + w - 1, y, y + h - 1, shadow)
    for fold_x in folds:
        actual = x + max(0, min(w - 1, fold_x))
        line_v(draw, actual, y, y + h - 1, highlight)
        if actual + 1 <= x + w - 1:
            line_v(draw, actual + 1, y, y + h - 1, shadow)


def bordered_face(draw, rect, fill, border, inner=None):
    x, y, w, h = rect
    fill_rect(draw, rect, border)
    if w > 2 and h > 2:
        fill_rect(draw, (x + 1, y + 1, w - 2, h - 2), fill)
    if inner and w > 4 and h > 4:
        fill_rect(draw, (x + 2, y + 2, w - 4, h - 4), inner)


def add_wear(draw, rect, colors):
    x, y, w, h = rect
    points = [
        (x + 1, y + h - 2),
        (x + w - 2, y + h - 3),
        (x + w // 2, y + h - 1),
    ]
    for idx, (px, py) in enumerate(points):
        draw.point((px, py), colors[idx % len(colors)])


def add_eye(draw, x, y):
    draw.point((x, y), EYE_WHITE)
    draw.point((x + 1, y), EYE_WHITE)
    draw.point((x + 1, y + 1), ARCANE_BLUE)


def paint_head(draw):
    head = box_faces(0, 11, 8, 8, 8)

    for face in ("top", "left", "right", "back"):
        gradient_face(draw, head[face], VOID_SOFT, VOID_DARK, VOID_BLACK)

    gradient_face(draw, head["front"], VOID_SOFT, VOID_DARK, VOID_BLACK)
    x, y, _, _ = head["front"]
    line_h(draw, x, x + 7, y, PURPLE_SHADOW)
    fill_rect(draw, (x + 1, y + 1, 6, 6), VOID_BLACK)
    fill_rect(draw, (x + 2, y + 2, 4, 4), VOID_DARK)
    draw.point((x + 2, y + 3), GOLD_LIGHT)
    draw.point((x + 5, y + 3), GOLD_LIGHT)
    draw.point((x + 3, y + 3), GOLD_MID)
    draw.point((x + 6, y + 3), GOLD_MID)
    draw.point((x + 2, y + 4), GOLD_DARK)
    draw.point((x + 5, y + 4), GOLD_DARK)
    line_h(draw, x + 1, x + 6, y + 6, PURPLE_SHADOW)
    draw.point((x + 3, y + 5), GOLD_DARK)
    draw.point((x + 4, y + 5), GOLD_DARK)

    brim = box_faces(0, 0, 10, 1, 10)
    fill_rect(draw, brim["top"], PURPLE_DARK)
    fill_rect(draw, brim["bottom"], PURPLE_SHADOW)
    for face in ("left", "front", "right", "back"):
        fill_rect(draw, brim[face], GOLD_MID if face == "front" else PURPLE_MID)
        if face != "front":
            fill_rect(draw, (brim[face][0], brim[face][1], brim[face][2], 1), GOLD_LIGHT)

    hood = box_faces(0, 27, 8, 4, 8)
    crown = box_faces(0, 39, 6, 4, 6)
    for face_name, face in hood.items():
        robe_face(draw, face, PURPLE_DARK, PURPLE_SHADOW, PURPLE_MID, folds=(3,) if face_name in ("front", "back") else ())
    for face_name, face in crown.items():
        robe_face(draw, face, PURPLE_DEEP, PURPLE_SHADOW, PURPLE_LIGHT, folds=(2,) if face_name in ("front", "back") else ())

    jewel_x, jewel_y, _, _ = brim["front"]
    draw.point((jewel_x + 4, jewel_y), ARCANE_CYAN)


def paint_body(draw):
    torso = box_faces(32, 31, 8, 12, 4)
    robe_face(draw, torso["front"], PURPLE_DEEP, PURPLE_SHADOW, PURPLE_MID, folds=(2, 5))
    robe_face(draw, torso["back"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_MID, folds=(3,))
    robe_face(draw, torso["left"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_MID, folds=())
    robe_face(draw, torso["right"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_MID, folds=())
    gradient_face(draw, torso["top"], PURPLE_LIGHT, PURPLE_MID, PURPLE_SHADOW)
    fill_rect(draw, torso["bottom"], PURPLE_DEEP)

    fx, fy, _, fh = torso["front"]
    fill_rect(draw, (fx, fy, 8, 3), GOLD_MID)
    line_h(draw, fx, fx + 7, fy + 2, GOLD_DARK)
    fill_rect(draw, (fx + 2, fy + 3, 4, 1), GOLD_LIGHT)
    fill_rect(draw, (fx + 1, fy + 3, 2, 7), PURPLE_MID)
    fill_rect(draw, (fx + 5, fy + 3, 2, 7), PURPLE_MID)
    line_h(draw, fx + 1, fx + 2, fy + 9, GOLD_MID)
    line_h(draw, fx + 5, fx + 6, fy + 9, GOLD_MID)
    fill_rect(draw, (fx + 2, fy + 4, 4, 2), LEATHER_DARK)
    fill_rect(draw, (fx + 3, fy + 4, 2, 2), GOLD_MID)
    fill_rect(draw, (fx + 2, fy + 6, 4, 3), ARCANE_BLUE)
    fill_rect(draw, (fx + 3, fy + 7, 2, 1), ARCANE_CYAN)
    fill_rect(draw, (fx, fy + 10, 8, 2), PURPLE_DARK)
    line_h(draw, fx, fx + 7, fy + 11, GOLD_DARK)
    line_v(draw, fx + 1, fy + 3, fy + 10, PURPLE_LIGHT)
    line_v(draw, fx + 6, fy + 3, fy + 10, PURPLE_SHADOW)
    draw.point((fx + 2, fy + 8), GOLD_LIGHT)
    draw.point((fx + 5, fy + 8), GOLD_LIGHT)
    add_wear(draw, torso["front"], (GOLD_DARK, PURPLE_LIGHT, GOLD_LIGHT))

    cape = box_faces(32, 11, 10, 19, 1)
    for face_name, face in cape.items():
        if face_name == "back":
            robe_face(draw, face, PURPLE_DARK, PURPLE_SHADOW, PURPLE_MID, folds=(3, 6))
            x, y, w, h = face
            draw.rectangle([x, y, x + w - 1, y + h - 1], outline=GOLD_DARK)
            draw.rectangle([x + 1, y + 1, x + w - 2, y + h - 2], outline=GOLD_LIGHT)
            draw.point((x + w // 2, y + 5), GOLD_LIGHT)
            draw.point((x + w // 2 - 1, y + 6), GOLD_MID)
            draw.point((x + w // 2 + 1, y + 6), GOLD_MID)
            draw.point((x + w // 2, y + 7), GOLD_LIGHT)
            add_wear(draw, face, (PURPLE_LIGHT, GOLD_DARK, PURPLE_MID))
        else:
            fill_rect(draw, face, PURPLE_SHADOW)

    shoulder_r = box_faces(54, 16, 4, 3, 5)
    shoulder_l = box_faces(57, 24, 4, 3, 5)
    for shoulder in (shoulder_r, shoulder_l):
        for face_name, face in shoulder.items():
            if face_name in ("front", "back", "top"):
                bordered_face(draw, face, GOLD_MID, GOLD_DARK, GOLD_LIGHT)
            else:
                fill_rect(draw, face, PURPLE_DARK)

    clasp = box_faces(56, 32, 10, 2, 1)
    for face in clasp.values():
        fill_rect(draw, face, GOLD_DARK)

    talisman_r = box_faces(16, 49, 1, 12, 1)
    talisman_l = box_faces(20, 49, 1, 12, 1)
    for part in (talisman_r, talisman_l):
        for face_name, face in part.items():
            fill_rect(draw, face, GOLD_MID if face_name in ("front", "back") else LEATHER_DARK)


def paint_arm(draw, u, v, bracelet=True):
    arm = box_faces(u, v, 4, 12, 4)
    robe_face(draw, arm["front"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_MID, folds=(1,))
    robe_face(draw, arm["back"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_MID, folds=(1,))
    robe_face(draw, arm["left"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_LIGHT, folds=())
    robe_face(draw, arm["right"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_LIGHT, folds=())
    gradient_face(draw, arm["top"], PURPLE_LIGHT, PURPLE_MID, PURPLE_SHADOW)
    fill_rect(draw, arm["bottom"], BOOT_DARK)
    if bracelet:
        x, y, w, h = arm["front"]
        fill_rect(draw, (x, y + h - 2, w, 2), GOLD_MID)
        line_h(draw, x, x + w - 1, y + h - 2, GOLD_LIGHT)
    add_wear(draw, arm["front"], (PURPLE_LIGHT, GOLD_DARK, PURPLE_MID))


def paint_leg(draw, u, v):
    leg = box_faces(u, v, 4, 12, 4)
    robe_face(draw, leg["front"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_MID, folds=(1,))
    robe_face(draw, leg["back"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_MID, folds=(1,))
    robe_face(draw, leg["left"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_LIGHT, folds=())
    robe_face(draw, leg["right"], PURPLE_DARK, PURPLE_SHADOW, PURPLE_LIGHT, folds=())
    gradient_face(draw, leg["top"], PURPLE_MID, PURPLE_DARK, PURPLE_SHADOW)
    fill_rect(draw, leg["bottom"], BOOT_DARK)
    x, y, w, h = leg["front"]
    line_h(draw, x, x + w - 1, y + h - 3, BOOT_LIGHT)
    line_h(draw, x, x + w - 1, y + h - 2, BOOT_DARK)
    add_wear(draw, leg["front"], (PURPLE_LIGHT, BOOT_LIGHT, PURPLE_MID))


def add_detail_sparkles(draw):
    sparkles = [
        (11, 59), (18, 62), (27, 55), (35, 74), (48, 79), (67, 68),
        (75, 88), (92, 51), (101, 76), (112, 83), (63, 98), (87, 104)
    ]
    for x, y in sparkles:
        draw.point((x, y), GOLD_LIGHT)
        draw.point((x + 1, y + 1), ARCANE_BLUE)


def create_mage_merchant_texture():
    img = Image.new("RGBA", (TEXTURE_SIZE, TEXTURE_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    paint_head(draw)
    paint_body(draw)
    paint_arm(draw, 24, 47)
    paint_arm(draw, 40, 47)
    paint_leg(draw, 0, 49)
    paint_leg(draw, 54, 0)
    add_detail_sparkles(draw)

    TEXTURE_PATH.parent.mkdir(parents=True, exist_ok=True)
    img.save(TEXTURE_PATH)
    img.resize((PREVIEW_SIZE, PREVIEW_SIZE), Image.NEAREST).save(PREVIEW_PATH)

    print(f"Texture saved: {TEXTURE_PATH}")
    print(f"Preview saved: {PREVIEW_PATH}")


if __name__ == "__main__":
    create_mage_merchant_texture()
