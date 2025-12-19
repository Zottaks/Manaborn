"""
Скрипт для генерации текстуры маг-торговца.
Создаёт PNG файл 64x64 с дизайном мага.
Использует ПРАВИЛЬНЫЙ UV-маппинг модели VillagerResembling (странствующий торговец).

UV-маппинг модели жителя (64x64):
- Голова: верх (8,0), низ (16,0), перед (8,8), зад (24,8), право (0,8), лево (16,8)
- Шляпа (hat layer): верх (40,0), низ (48,0), перед (40,8), зад (56,8), право (32,8), лево (48,8)
- Нос: @ (24,0) размер 2x4x2
- Тело: @ (16,20)
- Руки: @ (44,22) и (4,22)

Требует: pip install Pillow
"""

from PIL import Image, ImageDraw

def create_mage_merchant_texture():
    # Текстура 64x64
    img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Цвета мага
    ROBE_DARK = (60, 20, 100, 255)        # Тёмно-фиолетовая роба
    ROBE_MID = (90, 40, 140, 255)         # Средняя роба
    ROBE_LIGHT = (120, 60, 180, 255)      # Светлая роба
    GOLD = (255, 200, 50, 255)            # Золотой
    GOLD_DARK = (200, 150, 30, 255)       # Тёмное золото
    SKIN = (200, 160, 120, 255)           # Кожа
    SKIN_DARK = (170, 130, 90, 255)       # Тень кожи
    HAT = (40, 15, 70, 255)               # Шляпа тёмная
    HAT_LIGHT = (70, 35, 110, 255)        # Шляпа светлая
    WHITE = (255, 255, 255, 255)          # Белый (глаза)
    BLACK = (20, 20, 20, 255)             # Чёрный (зрачки)
    PURPLE_MASK = (90, 40, 140, 255)      # Фиолетовая маска (нос спереди)
    
    # Заполняем всё фиолетовым фоном
    draw.rectangle([0, 0, 63, 63], fill=ROBE_DARK)
    
    # ========== ГОЛОВА ==========
    # UV-маппинг головы жителя (8x8x8):
    # Верх головы @ (8,0) размер 8x8
    draw.rectangle([8, 0, 15, 7], fill=SKIN)
    
    # Низ головы @ (16,0) размер 8x8
    draw.rectangle([16, 0, 23, 7], fill=SKIN_DARK)
    
    # Правая сторона головы @ (0,8) размер 8x8
    draw.rectangle([0, 8, 7, 15], fill=SKIN)
    
    # Перед лица @ (8,8) размер 8x8
    draw.rectangle([8, 8, 15, 15], fill=SKIN)
    # Глаза (светящиеся магические) - позиция относительно (8,8)
    # Левый глаз @ x=10,11 y=10,11
    draw.rectangle([10, 10, 11, 11], fill=WHITE)
    draw.point((10, 10), fill=BLACK)
    # Правый глаз @ x=13,14 y=10,11
    draw.rectangle([13, 10, 14, 11], fill=WHITE)
    draw.point((14, 10), fill=BLACK)
    
    # Левая сторона головы @ (16,8) размер 8x8
    draw.rectangle([16, 8, 23, 15], fill=SKIN)
    
    # Зад головы @ (24,8) размер 8x8
    draw.rectangle([24, 8, 31, 15], fill=SKIN_DARK)
    
    # ========== НОС (отдельная часть модели) ==========
    # Нос жителя @ (24,0) размер 2x4x2
    # Развёртка: верх(2x2), низ(2x2), право(2x4), перед(2x4), лево(2x4), зад(2x4)
    # Начинается с (24,0):
    # - Верх носа @ (26,0) 2x2
    # - Низ носа @ (28,0) 2x2  
    # - Право носа @ (24,2) 2x4
    # - Перед носа @ (26,2) 2x4 <-- ЭТО ВИДНО СПЕРЕДИ
    # - Лево носа @ (28,2) 2x4
    # - Зад носа @ (30,2) 2x4
    
    # Верх и низ носа
    draw.rectangle([26, 0, 27, 1], fill=PURPLE_MASK)  # Верх
    draw.rectangle([28, 0, 29, 1], fill=PURPLE_MASK)  # Низ
    
    # Бока носа - цвет кожи
    draw.rectangle([24, 2, 25, 5], fill=SKIN)         # Право
    draw.rectangle([28, 2, 29, 5], fill=SKIN)         # Лево
    draw.rectangle([30, 2, 31, 5], fill=SKIN_DARK)    # Зад
    
    # Перед носа - фиолетовая маска!
    draw.rectangle([26, 2, 27, 5], fill=PURPLE_MASK)  # Перед
    
    # ========== ШЛЯПА (HAT LAYER) ==========
    # Шляпа - это overlay поверх головы
    # ВАЖНО: Перед шляпы должен быть ПРОЗРАЧНЫМ чтобы было видно лицо!
    
    # Верх шляпы @ (40,0) размер 8x8
    draw.rectangle([40, 0, 47, 7], fill=HAT)
    draw.point((43, 3), fill=GOLD)
    draw.point((44, 4), fill=GOLD_DARK)
    
    # Низ шляпы @ (48,0) размер 8x8 - прозрачный (видно лицо снизу)
    # Оставляем прозрачным
    
    # Правая сторона шляпы @ (32,8) размер 8x8
    # Только верхняя часть (поля шляпы), нижняя прозрачная
    draw.rectangle([32, 8, 39, 9], fill=HAT_LIGHT)
    draw.rectangle([32, 9, 39, 10], fill=GOLD_DARK)
    
    # Перед шляпы @ (40,8) размер 8x8
    # Только верхняя часть (поля шляпы), остальное ПРОЗРАЧНОЕ чтобы видеть лицо!
    draw.rectangle([40, 8, 47, 9], fill=HAT)
    draw.rectangle([40, 9, 47, 10], fill=GOLD)
    
    # Левая сторона шляпы @ (48,8) размер 8x8
    draw.rectangle([48, 8, 55, 9], fill=HAT_LIGHT)
    draw.rectangle([48, 9, 55, 10], fill=GOLD_DARK)
    
    # Зад шляпы @ (56,8) размер 8x8
    draw.rectangle([56, 8, 63, 9], fill=HAT)
    draw.rectangle([56, 9, 63, 10], fill=GOLD)
    
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
    # Золотая отделка
    draw.line([(23, 20), (23, 31)], fill=GOLD, width=1)
    draw.line([(24, 20), (24, 31)], fill=GOLD_DARK, width=1)
    draw.point((23, 22), fill=GOLD)
    draw.point((24, 25), fill=GOLD)
    draw.point((23, 28), fill=GOLD)
    
    # Левая сторона тела @ (28,20) 4x12
    draw.rectangle([28, 20, 31, 31], fill=ROBE_DARK)
    
    # Зад тела @ (32,20) 8x12
    draw.rectangle([32, 20, 39, 31], fill=ROBE_DARK)
    draw.point((35, 25), fill=GOLD_DARK)
    
    # ========== РУКИ ==========
    # Правая рука @ (44,22) размер 4x12x4
    draw.rectangle([44, 16, 47, 19], fill=ROBE_DARK)  # Верх
    draw.rectangle([48, 16, 51, 19], fill=ROBE_DARK)  # Низ
    draw.rectangle([40, 20, 43, 31], fill=ROBE_DARK)  # Право
    draw.rectangle([44, 20, 47, 31], fill=ROBE_MID)   # Перед
    draw.rectangle([44, 30, 47, 31], fill=SKIN)       # Кисть
    draw.rectangle([48, 20, 51, 31], fill=ROBE_DARK)  # Лево
    draw.rectangle([52, 20, 55, 31], fill=ROBE_DARK)  # Зад
    
    # Левая рука @ (4,22) размер 4x12x4
    draw.rectangle([4, 16, 7, 19], fill=ROBE_DARK)    # Верх
    draw.rectangle([8, 16, 11, 19], fill=ROBE_DARK)   # Низ
    draw.rectangle([0, 20, 3, 31], fill=ROBE_DARK)    # Право
    draw.rectangle([4, 20, 7, 31], fill=ROBE_MID)     # Перед
    draw.rectangle([4, 30, 7, 31], fill=SKIN)         # Кисть
    draw.rectangle([8, 20, 11, 31], fill=ROBE_DARK)   # Лево
    draw.rectangle([12, 20, 15, 31], fill=ROBE_DARK)  # Зад
    
    # ========== НИЖНЯЯ ЧАСТЬ (роба) ==========
    # Заполняем оставшееся пространство робой со звёздами
    draw.rectangle([0, 32, 63, 63], fill=ROBE_DARK)
    
    # Звёзды на робе
    for pos in [(5, 35), (10, 42), (3, 55), (12, 60), 
                (20, 38), (27, 52), (35, 40), (42, 58),
                (52, 36), (58, 50), (25, 45), (45, 48)]:
        draw.point(pos, fill=GOLD)
    
    # Сохраняем
    import os
    output_dir = r"src\main\resources\assets\manaborn\textures\entity"
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, "mage_merchant.png")
    img.save(output_path)
    print(f"Текстура сохранена: {output_path}")
    
    # Превью
    preview = img.resize((256, 256), Image.NEAREST)
    preview.save("mage_merchant_preview.png")
    print("Превью сохранено: mage_merchant_preview.png")

if __name__ == "__main__":
    create_mage_merchant_texture()
