"""
Скрипт для генерации текстуры маг-торговца.
Создаёт PNG файл 64x64 с дизайном мага в волшебной шляпе.
Использует правильный UV-маппинг для модели жителя Minecraft.

Требует: pip install Pillow
"""

from PIL import Image, ImageDraw

def create_mage_merchant_texture():
    # Создаём изображение 64x64 (стандартный размер текстуры жителя)
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Цвета мага
    PURPLE_DARK = (75, 0, 130, 255)       # Тёмно-фиолетовый (основа робы)
    PURPLE_MID = (106, 27, 154, 255)      # Средний фиолетовый
    PURPLE_LIGHT = (138, 43, 226, 255)    # Светло-фиолетовый (детали)
    GOLD = (255, 215, 0, 255)             # Золотой (звёзды, отделка)
    GOLD_DARK = (218, 165, 32, 255)       # Тёмное золото
    SKIN = (198, 164, 114, 255)           # Цвет кожи
    SKIN_SHADOW = (168, 134, 84, 255)     # Тень кожи
    HAT_DARK = (48, 25, 88, 255)          # Тёмная шляпа
    HAT_MID = (68, 45, 108, 255)          # Средняя шляпа
    EYES_WHITE = (255, 255, 255, 255)     # Белки глаз
    EYES_PUPIL = (40, 40, 40, 255)        # Зрачки
    NOSE = (178, 144, 94, 255)            # Нос (темнее кожи)
    BROW = (139, 90, 43, 255)             # Брови
    
    # ============================================
    # VILLAGER TEXTURE UV MAP (64x64):
    # ============================================
    # HEAD (8x8x8):
    #   Top:    (0,0)  8x8
    #   Bottom: (8,0)  8x8
    #   Front:  (0,8)  8x8
    #   Back:   (16,8) 8x8
    #   Right:  (8,8)  8x8
    #   Left:   (24,8) 8x8 (но часто не используется)
    #
    # HAT/HEADWEAR (10x10x10 overlay):
    #   Starts at (32,0)
    #
    # BODY (8x12x6):
    #   Top:    (16,20) 8x6
    #   Bottom: (24,20) 8x6
    #   Front:  (16,26) 8x12
    #   Back:   (28,26) 8x12
    #   Right:  (24,26) 6x12
    #   Left:   (36,26) 6x12
    #
    # ARMS (crossed villager style):
    #   Starts around (40,38)
    #
    # LEGS (hidden under robe for villager):
    #   Starts around (0,20)
    # ============================================
    
    # === ГОЛОВА ===
    # Верх головы (8x8 начиная с 0,0)
    draw.rectangle([0, 0, 7, 7], fill=SKIN)
    
    # Низ головы (8x8 начиная с 8,0)
    draw.rectangle([8, 0, 15, 7], fill=SKIN_SHADOW)
    
    # Перед головы - ЛИЦО (8x8 начиная с 0,8)
    draw.rectangle([0, 8, 7, 15], fill=SKIN)
    # Глаза (белки)
    draw.rectangle([1, 10, 2, 11], fill=EYES_WHITE)
    draw.rectangle([5, 10, 6, 11], fill=EYES_WHITE)
    # Зрачки
    draw.point((2, 10), fill=EYES_PUPIL)
    draw.point((5, 10), fill=EYES_PUPIL)
    # Брови
    draw.rectangle([1, 9, 2, 9], fill=BROW)
    draw.rectangle([5, 9, 6, 9], fill=BROW)
    # Нос (большой, как у жителя)
    draw.rectangle([3, 11, 4, 14], fill=NOSE)
    draw.rectangle([2, 12, 5, 13], fill=NOSE)
    
    # Правая сторона головы (8x8 начиная с 8,8)
    draw.rectangle([8, 8, 15, 15], fill=SKIN)
    draw.rectangle([8, 8, 8, 15], fill=SKIN_SHADOW)
    
    # Зад головы (8x8 начиная с 16,8)
    draw.rectangle([16, 8, 23, 15], fill=SKIN)
    draw.rectangle([19, 10, 20, 14], fill=SKIN_SHADOW)  # Затылок
    
    # Левая сторона головы (8x8 начиная с 24,8)
    draw.rectangle([24, 8, 31, 15], fill=SKIN)
    draw.rectangle([31, 8, 31, 15], fill=SKIN_SHADOW)
    
    # === ШЛЯПА (HAT OVERLAY) ===
    # Шляпа жителя начинается с (32,0), размер 10x10x10
    # Верх шляпы (10x10 начиная с 32,0)
    draw.rectangle([32, 0, 41, 9], fill=HAT_DARK)
    # Золотые звёзды на верхушке
    draw.point((35, 3), fill=GOLD)
    draw.point((38, 5), fill=GOLD)
    draw.point((36, 7), fill=GOLD_DARK)
    
    # Перед шляпы (10x10 начиная с 32,10)
    draw.rectangle([32, 10, 41, 19], fill=HAT_DARK)
    # Золотая лента
    draw.rectangle([32, 17, 41, 18], fill=GOLD)
    draw.rectangle([32, 18, 41, 18], fill=GOLD_DARK)
    # Звезда на шляпе
    draw.point((36, 13), fill=GOLD)
    
    # Правая сторона шляпы (10x10 начиная с 42,10)
    draw.rectangle([42, 10, 51, 19], fill=HAT_MID)
    draw.rectangle([42, 17, 51, 18], fill=GOLD)
    
    # Левая сторона шляпы (10x10 начиная с 52,10) - может выходить за границы
    draw.rectangle([52, 10, 61, 19], fill=HAT_MID)
    draw.rectangle([52, 17, 61, 18], fill=GOLD)
    
    # Зад шляпы
    draw.rectangle([42, 0, 51, 9], fill=HAT_DARK)
    draw.point((45, 4), fill=GOLD)
    
    # === ТЕЛО ===
    # Верх тела (8x6 начиная с 16,20)
    draw.rectangle([16, 20, 23, 25], fill=PURPLE_DARK)
    
    # Низ тела (8x6 начиная с 24,20)
    draw.rectangle([24, 20, 31, 25], fill=PURPLE_DARK)
    
    # Перед тела (8x12 начиная с 16,26)
    draw.rectangle([16, 26, 23, 37], fill=PURPLE_DARK)
    # Золотые пуговицы по центру
    draw.point((19, 28), fill=GOLD)
    draw.point((19, 31), fill=GOLD)
    draw.point((19, 34), fill=GOLD)
    # Светлые края
    draw.rectangle([16, 26, 16, 37], fill=PURPLE_LIGHT)
    draw.rectangle([23, 26, 23, 37], fill=PURPLE_LIGHT)
    # Воротник
    draw.rectangle([17, 26, 22, 27], fill=PURPLE_LIGHT)
    
    # Правая сторона тела (6x12 начиная с 24,26)
    draw.rectangle([24, 26, 29, 37], fill=PURPLE_MID)
    
    # Зад тела (8x12 начиная с 28,26) - перекрывается, корректируем
    draw.rectangle([30, 26, 37, 37], fill=PURPLE_DARK)
    # Звезда на спине
    draw.point((33, 30), fill=GOLD)
    draw.point((34, 32), fill=GOLD_DARK)
    
    # Левая сторона тела (6x12 начиная с 36,26) - перекрывается
    draw.rectangle([38, 26, 43, 37], fill=PURPLE_MID)
    
    # === РУКИ (сложенные, как у жителя) ===
    # Руки жителя: область около (40,38)
    # Верх рук
    draw.rectangle([40, 38, 47, 41], fill=PURPLE_DARK)
    # Перед рук (сложенные)
    draw.rectangle([40, 42, 47, 53], fill=PURPLE_DARK)
    draw.rectangle([40, 51, 47, 53], fill=SKIN)  # Кисти рук
    # Бока рук
    draw.rectangle([48, 42, 51, 53], fill=PURPLE_MID)
    draw.rectangle([36, 42, 39, 53], fill=PURPLE_MID)
    # Зад рук
    draw.rectangle([52, 42, 59, 53], fill=PURPLE_DARK)
    
    # === НОГИ (скрыты под робой, но нужны для UV) ===
    # Правая нога (4x12 начиная с 0,20)
    draw.rectangle([0, 20, 3, 31], fill=PURPLE_DARK)
    draw.rectangle([4, 20, 7, 31], fill=PURPLE_DARK)
    draw.rectangle([8, 20, 11, 31], fill=PURPLE_DARK)
    draw.rectangle([12, 20, 15, 31], fill=PURPLE_DARK)
    
    # Левая нога (4x12 начиная с 0,36 или 16,52)
    draw.rectangle([0, 36, 3, 47], fill=PURPLE_DARK)
    draw.rectangle([4, 36, 7, 47], fill=PURPLE_DARK)
    draw.rectangle([8, 36, 11, 47], fill=PURPLE_DARK)
    draw.rectangle([12, 36, 15, 47], fill=PURPLE_DARK)
    
    # === ДОПОЛНИТЕЛЬНАЯ ДЕТАЛИЗАЦИЯ ===
    # Заполняем оставшиеся области робой
    draw.rectangle([0, 48, 15, 63], fill=PURPLE_DARK)
    draw.rectangle([16, 38, 31, 51], fill=PURPLE_DARK)
    draw.rectangle([16, 52, 31, 63], fill=PURPLE_DARK)
    
    # Сохраняем
    import os
    output_dir = r"src\main\resources\assets\manaborn\textures\entity"
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, "mage_merchant.png")
    img.save(output_path)
    print(f"Текстура сохранена: {output_path}")
    
    # Также создаём превью в большем размере
    preview = img.resize((256, 256), Image.NEAREST)
    preview.save("mage_merchant_preview.png")
    print("Превью сохранено: mage_merchant_preview.png")

if __name__ == "__main__":
    create_mage_merchant_texture()
