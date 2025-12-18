"""
Скрипт для генерации текстуры маг-торговца.
Создаёт PNG файл 64x64 с дизайном мага в волшебной шляпе.
Использует UV-маппинг модели VillagerResembling (странствующий торговец).

Требует: pip install Pillow
"""

from PIL import Image, ImageDraw

def create_mage_merchant_texture():
    # Текстура жителя/странствующего торговца 64x64
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
    BLACK = (30, 30, 30, 255)             # Чёрный (зрачки)
    NOSE = (180, 140, 100, 255)           # Нос
    
    # Заполняем всё фиолетовым фоном (роба)
    draw.rectangle([0, 0, 63, 63], fill=ROBE_DARK)
    
    # ========== ГОЛОВА (верхняя часть текстуры) ==========
    # Верх головы 8x8 @ (0,0)
    draw.rectangle([0, 0, 7, 7], fill=SKIN)
    
    # Низ головы 8x8 @ (16,0)
    draw.rectangle([16, 0, 23, 7], fill=SKIN_DARK)
    
    # Перед лица 8x8 @ (8,8)
    draw.rectangle([8, 8, 15, 15], fill=SKIN)
    # Глаза
    draw.rectangle([9, 11, 10, 12], fill=WHITE)
    draw.rectangle([13, 11, 14, 12], fill=WHITE)
    draw.point((10, 11), fill=BLACK)
    draw.point((13, 11), fill=BLACK)
    # Нос (большой как у жителя)
    draw.rectangle([11, 12, 12, 15], fill=NOSE)
    
    # Правая сторона головы 8x8 @ (0,8)
    draw.rectangle([0, 8, 7, 15], fill=SKIN)
    
    # Левая сторона головы 8x8 @ (16,8)
    draw.rectangle([16, 8, 23, 15], fill=SKIN)
    
    # Зад головы 8x8 @ (24,8)
    draw.rectangle([24, 8, 31, 15], fill=SKIN_DARK)
    
    # ========== ШЛЯПА (HAT) - overlay на голове ==========
    # Шляпа начинается @ (32,0)
    # Верх шляпы
    draw.rectangle([32, 0, 39, 7], fill=HAT)
    draw.point((35, 3), fill=GOLD)
    draw.point((37, 5), fill=GOLD_DARK)
    
    # Низ шляпы
    draw.rectangle([48, 0, 55, 7], fill=HAT_LIGHT)
    
    # Перед шляпы @ (40,8)
    draw.rectangle([40, 8, 47, 15], fill=HAT)
    # Золотая лента
    draw.rectangle([40, 14, 47, 15], fill=GOLD)
    draw.point((43, 10), fill=GOLD)
    
    # Бока шляпы
    draw.rectangle([32, 8, 39, 15], fill=HAT_LIGHT)
    draw.rectangle([32, 14, 39, 15], fill=GOLD_DARK)
    draw.rectangle([48, 8, 55, 15], fill=HAT_LIGHT)
    draw.rectangle([48, 14, 55, 15], fill=GOLD_DARK)
    
    # Зад шляпы
    draw.rectangle([56, 8, 63, 15], fill=HAT)
    draw.rectangle([56, 14, 63, 15], fill=GOLD)
    
    # ========== ТЕЛО ==========
    # Тело @ (16,20) - верх
    draw.rectangle([16, 16, 35, 19], fill=ROBE_DARK)
    
    # Перед тела @ (20,20) размер 8x12
    draw.rectangle([20, 20, 27, 31], fill=ROBE_MID)
    # Золотая отделка по центру
    draw.line([(23, 20), (23, 31)], fill=GOLD, width=1)
    draw.line([(24, 20), (24, 31)], fill=GOLD_DARK, width=1)
    # Пуговицы
    draw.point((23, 22), fill=GOLD)
    draw.point((24, 25), fill=GOLD)
    draw.point((23, 28), fill=GOLD)
    
    # Бока тела
    draw.rectangle([16, 20, 19, 31], fill=ROBE_DARK)
    draw.rectangle([28, 20, 35, 31], fill=ROBE_DARK)
    
    # Зад тела
    draw.rectangle([32, 20, 39, 31], fill=ROBE_DARK)
    draw.point((35, 24), fill=GOLD_DARK)
    
    # ========== РУКИ (сложенные) ==========
    # Руки @ (44,22)
    draw.rectangle([44, 22, 51, 29], fill=ROBE_MID)
    draw.rectangle([44, 28, 51, 29], fill=SKIN)  # Кисти
    
    draw.rectangle([52, 22, 59, 29], fill=ROBE_DARK)
    draw.rectangle([36, 22, 43, 29], fill=ROBE_DARK)
    
    # ========== НОС (отдельная часть модели жителя) ==========
    # Нос @ (24,0) размер 2x4
    draw.rectangle([24, 0, 25, 3], fill=NOSE)
    draw.rectangle([26, 0, 27, 3], fill=SKIN_DARK)
    
    # ========== НИЖНЯЯ ЧАСТЬ (роба/ноги) ==========
    # Заполняем нижнюю часть робой
    draw.rectangle([0, 32, 15, 47], fill=ROBE_DARK)
    draw.rectangle([0, 48, 15, 63], fill=ROBE_MID)
    
    draw.rectangle([16, 32, 31, 47], fill=ROBE_MID)
    draw.rectangle([16, 48, 31, 63], fill=ROBE_DARK)
    
    draw.rectangle([32, 32, 47, 47], fill=ROBE_DARK)
    draw.rectangle([32, 48, 47, 63], fill=ROBE_MID)
    
    draw.rectangle([48, 32, 63, 47], fill=ROBE_MID)
    draw.rectangle([48, 48, 63, 63], fill=ROBE_DARK)
    
    # Добавляем звёзды на робу
    for pos in [(5, 35), (10, 42), (3, 55), (12, 60), 
                (20, 38), (27, 52), (35, 40), (42, 58),
                (52, 36), (58, 50)]:
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
