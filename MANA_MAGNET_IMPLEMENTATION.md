# Магнит Маны (Mana Magnet) - План реализации

## 📋 Общая информация

**Название:** Магнит Маны (Mana Magnet)
**Функция:** Автоматически производит магическую пыль
**Производительность:** 1 магическая пыль каждые 60 секунд (1200 тиков)
**Тип:** Блок с BlockEntity (имеет внутреннее состояние)

---

## 🧪 Рецепт крафта

```
[L] [D] [L]
[D] [W] [D]
[L] [V] [L]

L = Lapis Lazuli (лазурит) - 4 штуки
D = Magic Dust (магическая пыль) - 3 штуки
W = Lodestone (wodestone) - 1 штука
V = Mana Vessel (сосуд маны, любой) - 1 штука
```

**Позиции в сетке 3x3:**
- Углы (4 штуки): Lapis Lazuli
- Верх-центр, средний-слева, средний-справа: Magic Dust
- Центр: Lodestone
- Низ-центр: Mana Vessel

---

## 🎨 Текстура

**Стиль:** Похож на Mana Converter (конвертер маны)

**Описание:**
- **Основа:** Темно-фиолетовый/серый камень (как lodestone)
- **Центр:** Светящийся лазуритовый кристалл (синий/аква)
- **Акценты:** Магические руны или частицы вокруг кристалла
- **Края:** Темная рамка с металлическими вставками

**Файлы текстур:**
1. `mana_magnet_top.png` - верх (светящийся кристалл)
2. `mana_magnet_side.png` - бока (руны и узоры)
3. `mana_magnet_bottom.png` - низ (можно как side)

**Цветовая палитра:**
- Фон: #3A3A5A (темно-серый с фиолетовым)
- Кристалл: #3A5FCD, #1E90FF (лазуритовый синий)
- Свечение: #87CEEB (светлая аква)
- Руны: #9370DB (средний фиолетовый)
- Акценты: #4B0082 (индиго)

---

## ⚙️ Техническая реализация

### Файлы для создания:

1. **Java классы:**
   - `ManaMagnetBlock.java` - сам блок
   - `ManaMagnetBlockEntity.java` - логика генерации
   
2. **Ресурсы:**
   - `blockstates/mana_magnet.json`
   - `models/block/mana_magnet.json`
   - `models/item/mana_magnet.json`
   - `recipes/mana_magnet.json`
   - `textures/block/mana_magnet_*.png`
   - `lang/en_us.json` + `lang/ru_ru.json`

### Логика работы:

```java
// В ManaMagnetBlockEntity.tick()
timer++;
if (timer >= 1200) { // 60 секунд
    timer = 0;
    // Спавн 1 Magic Dust в инвентаре или выброс
}
```

---

## 🔧 Интеграция в ManabornMod

Добавить в `ManabornMod.java`:
```java
public static Block MANA_MAGNET;
public static BlockEntityType<ManaMagnetBlockEntity> MANA_MAGNET_ENTITY;
```

Регистрация:
- Блок в `Registries.BLOCK`
- Предмет в `Registries.ITEM`
- BlockEntity в `Registries.BLOCK_ENTITY_TYPE`
- Добавить в creative tabs

---

## 🎮 Геймплей

**Использование:**
1. Скрафтить Mana Magnet
2. Поставить в мире
3. Блок автоматически производит пыль
4. Собирать пыль вручную или хопперами

**Балансировка:**
- 1 пыль / 60 сек = 60 пыли / час
- Стоит дорого (lodestone + vessel)
- Пассивная генерация ресурса

---

Готово к реализации! Следующий шаг: создание Java классов.
