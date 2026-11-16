"""
Скрипт для генерации текстуры маг-торговца.
Создаёт PNG файл 64x64 с дизайном мага.
Использует ПРАВИЛЬНЫЙ UV-маппинг модели Villager.

UV-маппинг модели жителя (64x64):
- Голова (8x8x8): начало @ (0,0)
  - Верх: (8,0) 8x8, Низ: (16,0) 8x8
  - Право: (0,8) 8x8, Перед: (8,8) 8x8, Лево: (16,8) 8x8, Зад: (24,8) 8x8
- Шляпа/Капюшон (overlay, 8x8x8): начало @ (32,0)
  - Верх: (40,0) 8x8, Низ: (48,0) 8x8
  - Право: (32,8) 8x8, Перед: (40,8) 8x8, Лево: (48,8) 8x8, Зад: (56,8) 8x8
- Нос: (24,0) 2x4x2
- Тело: (16,20) 8x12x4
- Руки: (44,22) и зеркально

Требует: pip install Pillow
"""

from PIL import Image, ImageDraw

def create_mage_merchant_texture():
    # Текстура 64x64, начинаем с прозрачного фона
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Цвета мага
    ROBE_DARK = (50, 20, 80, 255)         # Тёмно-фиолетовая роба
    ROBE_MID = (80, 40, 120, 255)         # Средняя роба
    ROBE_LIGHT = (100, 60, 150, 255)      # Светлая роба
    GOLD = (255, 215, 0, 255)             # Золотой
    GOLD_DARK = (180, 150, 0, 255)        # Тёмное золото
    SKIN = (228, 195, 158, 255)           # Кожа (как у жителя)
    SKIN_SHADOW = (198, 165, 128, 255)    # Тень кожи
    SKIN_DARK = (168, 135, 98, 255)       # Тёмная кожа
    HAT_DARK = (40, 15, 60, 255)          # Шляпа тёмная
    HAT_MID = (60, 30, 90, 255)           # Шляпа средняя
    WHITE = (255, 255, 255, 255)          # Белый (глаза)
    BROWN = (82, 50, 30, 255)             # Коричневый (зрачки)
    NOSE_COLOR = (210, 175, 138, 255)     # Нос (чуть темнее кожи)
    
    # ========== ГОЛОВА (Head) ==========
    # Верх головы @ (8,0) размер 8x8
    for px in range(8, 16):
        for py in range(0, 8):
            draw.point((px, py), fill=SKIN)
    
    # Низ головы @ (16,0) размер 8x8
    for px in range(16, 24):
        for py in range(0, 8):
            draw.point((px, py), fill=SKIN_DARK)
    
    # Правая сторона головы @ (0,8) размер 8x8
    for px in range(0, 8):
        for py in range(8, 16):
            draw.point((px, py), fill=SKIN_SHADOW)
    
    # Перед лица @ (8,8) размер 8x8
    for px in range(8, 16):
        for py in range(8, 16):
            draw.point((px, py), fill=SKIN)
    
    # ГЛАЗА - на ЛИЦЕ, ниже края шляпы
    # Лицо занимает (8,8)-(15,15)
    # Глаза на y=12-13, зрачки ЧЁРНЫЕ для контраста
    BLACK = (0, 0, 0, 255)
    # Левый глаз
    draw.point((9, 12), fill=WHITE)
    draw.point((10, 12), fill=WHITE)
    draw.point((9, 13), fill=WHITE)
    draw.point((10, 13), fill=BLACK)  # Зрачок
    # Правый глаз
    draw.point((13, 12), fill=WHITE)
    draw.point((14, 12), fill=WHITE)
    draw.point((14, 13), fill=WHITE)
    draw.point((13, 13), fill=BLACK)  # Зрачок
    
    # Левая сторона головы @ (16,8) размер 8x8
    for px in range(16, 24):
        for py in range(8, 16):
            draw.point((px, py), fill=SKIN_SHADOW)
    
    # Зад головы @ (24,8) размер 8x8
    for px in range(24, 32):
        for py in range(8, 16):
            draw.point((px, py), fill=SKIN_DARK)
    
    # ========== НОС ==========
    # Нос жителя большой! @ (24,0) размер развёртки примерно 8x6
    # Верх носа
    draw.rectangle([26, 0, 27, 1], fill=NOSE_COLOR)
    # Низ носа  
    draw.rectangle([28, 0, 29, 1], fill=SKIN_DARK)
    # Стороны носа
    draw.rectangle([24, 2, 25, 5], fill=NOSE_COLOR)   # Право
    draw.rectangle([26, 2, 27, 5], fill=NOSE_COLOR)   # Перед
    draw.rectangle([28, 2, 29, 5], fill=NOSE_COLOR)   # Лево
    draw.rectangle([30, 2, 31, 5], fill=SKIN_SHADOW)  # Зад
    
    # ========== ШЛЯПА/КАПЮШОН (Hat Layer) ==========
    # Полностью фиолетовая шляпа с ОДНОЙ тонкой жёлтой линией вдоль полей
    # ВСЕ жёлтые линии на ОДНОМ уровне y=12
    
    # Верх шляпы @ (40,0) размер 8x8
    draw.rectangle([40, 0, 47, 7], fill=HAT_DARK)
    
    # Низ шляпы @ (48,0) размер 8x8
    draw.rectangle([48, 0, 55, 7], fill=HAT_MID)
    
    # Правая сторона шляпы @ (32,8) размер 8x8 - полностью заполняем
    draw.rectangle([32, 8, 39, 15], fill=HAT_MID)
    # Жёлтая линия толщиной 2 пикселя (y=11-12)
    draw.rectangle([32, 11, 39, 12], fill=GOLD)
    
    # Перед шляпы @ (40,8) размер 8x8 - заполняем верхнюю часть
    draw.rectangle([40, 8, 47, 11], fill=HAT_DARK)
    # Жёлтая линия толщиной 2 пикселя (y=11-12)
    draw.rectangle([40, 11, 47, 12], fill=GOLD)
    # (40,13)-(47,15) - ПРОЗРАЧНО для глаз
    
    # Левая сторона шляпы @ (48,8) размер 8x8 - полностью заполняем
    draw.rectangle([48, 8, 55, 15], fill=HAT_MID)
    # Жёлтая линия толщиной 2 пикселя (y=11-12)
    draw.rectangle([48, 11, 55, 12], fill=GOLD)
    
    # Зад шляпы @ (56,8) размер 8x8 - полностью заполняем
    draw.rectangle([56, 8, 63, 15], fill=HAT_DARK)
    # Жёлтая линия толщиной 2 пикселя (y=11-12)
    draw.rectangle([56, 11, 63, 12], fill=GOLD)
    
    # НЕ заполняем область под шляпой спереди - там должно быть видно лицо!
    # Область (40,13)-(47,15) остаётся ПРОЗРАЧНОЙ для глаз
    
    # ========== ТЕЛО ==========
    # Тело @ (16,20) размер 8x12x4
    
    # Верх тела @ (20,16) 8x4
    draw.rectangle([20, 16, 27, 19], fill=ROBE_DARK)
    # Низ тела @ (28,16) 8x4
    draw.rectangle([28, 16, 35, 19], fill=ROBE_DARK)
    
    # Правая сторона тела @ (16,20) 4x12
    draw.rectangle([16, 20, 19, 31], fill=ROBE_DARK)
    
    # Перед тела @ (20,20) 8x12
    draw.rectangle([20, 20, 27, 31], fill=ROBE_MID)
    # Золотая застёжка/полоса по центру
    draw.line([(23, 20), (23, 31)], fill=GOLD)
    draw.line([(24, 20), (24, 31)], fill=GOLD_DARK)
    # Золотые пуговицы
    draw.point((23, 22), fill=GOLD)
    draw.point((23, 25), fill=GOLD)
    draw.point((23, 28), fill=GOLD)
    
    # Левая сторона тела @ (28,20) 4x12
    draw.rectangle([28, 20, 31, 31], fill=ROBE_DARK)
    
    # Зад тела @ (32,20) 8x12
    draw.rectangle([32, 20, 39, 31], fill=ROBE_DARK)
    # Магический символ на спине
    draw.point((35, 24), fill=GOLD)
    draw.point((34, 25), fill=GOLD_DARK)
    draw.point((36, 25), fill=GOLD_DARK)
    draw.point((35, 26), fill=GOLD)
    
    # ========== РУКИ ==========
    # Заполняем ВСЮ область рук робой без пропусков
    # Включая ВСЕ стороны - верх, низ, перед, зад, лево, право
    
    # Правая рука - заполняем ПОЛНОСТЬЮ область (40-55, 16-31)
    draw.rectangle([40, 16, 55, 31], fill=ROBE_DARK)
    # Перед руки чуть светлее
    draw.rectangle([44, 20, 47, 30], fill=ROBE_MID)
    
    # Левая рука - заполняем ПОЛНОСТЬЮ область (0-15, 16-31)
    draw.rectangle([0, 16, 15, 31], fill=ROBE_DARK)
    # Перед руки чуть светлее
    draw.rectangle([4, 20, 7, 30], fill=ROBE_MID)
    
    # Дополнительно заполняем области 56-63 для рук (если есть)
    draw.rectangle([56, 16, 63, 31], fill=ROBE_DARK)
    
    # ========== НИЖНЯЯ ЧАСТЬ ТЕКСТУРЫ ==========
    # Заполняем ВСЮ нижнюю часть текстуры робой чтобы не было пропусков
    
    # Полностью заливаем всё от y=32 до y=63
    draw.rectangle([0, 32, 63, 63], fill=ROBE_DARK)
    
    # Немного вариации цвета для ног
    draw.rectangle([16, 32, 31, 47], fill=ROBE_MID)
    
    # Золотые звёзды на робе (декор)
    stars = [(2, 35), (8, 40), (5, 44), (20, 36), (26, 42), (22, 45),
             (40, 38), (55, 42), (48, 50), (35, 55), (10, 52), (25, 58),
             (50, 35), (58, 55), (45, 60)]
    for pos in stars:
        draw.point(pos, fill=GOLD)
    
    # Сохраняем
    import os
    output_dir = r"src\main\resources\assets\manaborn\textures\entity"
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, "mage_merchant.png")
    img.save(output_path)
    print(f"Текстура сохранена: {output_path}")
    
    # Превью (увеличенное)
    preview = img.resize((256, 256), Image.NEAREST)
    preview.save("mage_merchant_preview.png")
    print("Превью сохранено: mage_merchant_preview.png")

if __name__ == "__main__":
    create_mage_merchant_texture()
