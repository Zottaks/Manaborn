# Маноскоп - Реализация новой системы добычи магической пыли

## 📋 Обзор изменений

Система добычи магической пыли полностью переработана. Теперь магическая пыль больше **не генерируется в виде руды**, а находится на поверхности мира в виде невидимых "пылинок" (DustWisp), которые можно обнаружить только с помощью специального предмета - **Маноскопа**.

---

## ✨ Новые возможности

### 1. Маноскоп (Manoscope)
- **Тип**: Магический предмет, надевается в слот головы
- **Функция**: Позволяет видеть магические частицы вокруг пылинок
- **Эффект**: Имеет магическое свечение (enchantment glint)
- **Расположение**: Tools и Combat группы в креативе

### 2. Пылинки (Dust Wisp)
- **Невидимая сущность**, спавнящаяся на поверхности мира
- **Спавн**: Вокруг игроков с шансом 2% за тик
- **Лимит**: Максимум 6 пылинок в радиусе 32 блоков от игрока
- **Lifetime**: 60 секунд, после чего исчезает
- **Дроп**: 1-2 магической пыли при приближении с Маноскопом

### 3. Визуальные эффекты
- **END_ROD частицы**: Основной магический эффект (аква-цвет)
- **PORTAL частицы**: Дополнительный эффект с шансом 30%
- **Видимость**: Частицы видны **только** игрокам с надетым Маноскопом
- **Радиус видимости**: 32 блока

---

## 🗂️ Структура файлов

### Серверная часть (src/main/java)

```
com/manaborn/
├── ManabornMod.java                      # ✏️ ИЗМЕНЕНО - добавлена регистрация
├── items/
│   ├── ManoscopeItem.java                # ✅ НОВЫЙ - магические очки
│   └── MagicDustItem.java                # ✏️ ИЗМЕНЕНО - обновлен tooltip
├── entities/
│   └── DustWispEntity.java               # ✅ НОВЫЙ - невидимая сущность пылинки
└── world/
    └── DustWispManager.java              # ✅ НОВЫЙ - управление спавном пылинок
```

### Клиентская часть (src/client/java)

```
com/manaborn/client/
├── ManabornClient.java                   # ✏️ ИЗМЕНЕНО - добавлен рендерер
└── DustWispParticleRenderer.java         # ✅ НОВЫЙ - отрисовка частиц
```

### Ресурсы (src/main/resources)

```
assets/manaborn/
├── items/
│   └── manoscope.json                    # ✅ НОВЫЙ - дескриптор предмета
├── models/item/
│   └── manoscope.json                    # ✅ НОВЫЙ - модель предмета
└── lang/
    ├── en_us.json                        # ✏️ ИЗМЕНЕНО - добавлен Manoscope
    └── ru_ru.json                        # ✏️ ИЗМЕНЕНО - добавлен Маноскоп

data/manaborn/
├── recipe/
│   └── manoscope.json                    # ✅ НОВЫЙ - рецепт крафта
└── fabric/biome_modifier/
    └── add_magic_ore.json                # ✏️ ИЗМЕНЕНО - отключена генерация руды
```

---

## 🔧 Технические детали

### Регистрация в ManabornMod

```java
// Регистрация сущности пылинки
DUST_WISP_ENTITY = Registry.register(
    Registries.ENTITY_TYPE,
    wispKey,
    EntityType.Builder.<DustWispEntity>create(DustWispEntity::new, SpawnGroup.MISC)
        .dimensions(0.1f, 0.1f)
        .build(wispKey));

// Регистрация Manoscope
MANOSCOPE = Registry.register(Registries.ITEM, scopeKey,
    new ManoscopeItem(new Item.Settings().maxCount(1).registryKey(scopeKey)));

// Тик сервера: спавн пылинок
ServerTickEvents.END_SERVER_TICK.register(server -> {
    // ... другие тики ...
    DustWispManager.tick(server.getPlayerManager().getPlayerList());
});
```

### Клиентский рендеринг

```java
// В ManabornClient.onInitializeClient()
registerDustWispRenderer(); // EmptyEntityRenderer - невидимый

// В ClientTickEvents.END_CLIENT_TICK
DustWispParticleRenderer.tick(client); // Отрисовка частиц
```

### API совместимость (Minecraft 1.21.10)

- **Entity API**: `initDataTracker(DataTracker.Builder)`, `readCustomData(ReadView)`, `writeCustomData(WriteView)`
- **Networking**: Использует старые импорты (работают в 1.21.10)
- **Particles**: `MinecraftClient.getInstance().particleManager.addParticle()`

---

## ⚙️ Константы и настройки

### DustWispManager

```java
MAX_WISPS_NEAR_PLAYER = 6;           // Макс пылинок вокруг игрока
SCAN_RADIUS = 32;                     // Радиус спавна (блоков)
SPAWN_CHANCE_PER_TICK = 0.02;        // Шанс спавна 2% за тик
```

### DustWispEntity

```java
LIFETIME_TICKS = 20 * 60;            // Lifetime 60 секунд
PICKUP_RADIUS = 1.5;                  // Радиус подбора (блоков)
```

### DustWispParticleRenderer

```java
PARTICLE_RADIUS = 32.0;               // Радиус видимости частиц
PARTICLES_PER_WISP = 2;               // Частиц на пылинку за тик
```

---

## 📝 Локализация

### English (en_us.json)
```json
{
  "item.manaborn.manoscope": "Manoscope"
}
```

### Russian (ru_ru.json)
```json
{
  "item.manaborn.manoscope": "Маноскоп"
}
```

---

## 🎨 Требования к текстуре

Создайте текстуру по адресу:
```
src/main/resources/assets/manaborn/textures/item/manoscope.png
```

**Параметры:**
- Размер: 16x16 пикселей
- Формат: PNG с прозрачностью
- Стиль: Магические очки/монокль
- Цвета: Аква/голубой/фиолетовый для линз, металл для оправы

**Детали дизайна смотрите в файле:** `TEXTURE_REQUIREMENTS_MANOSCOPE.md`

---

## 🎮 Игровой процесс

### Для игрока:

1. **Скрафтите Маноскоп** (6 лазурита + 2 стекла + 1 палка)
2. **Наденьте Маноскоп** в слот головы (правый клик или через инвентарь)
3. **Ищите частицы** - магические аква-цветные и фиолетовые частицы появятся вокруг невидимых пылинок
4. **Подойдите к частицам** - в радиусе 1.5 блоков вы автоматически получите 1-2 магической пыли
5. **Пылинка исчезнет**, и вам нужно искать следующую

### Баланс:

- Пылинки спавнятся **постоянно** вокруг игроков (2% шанс за тик)
- Максимум **6 пылинок** одновременно в радиусе 32 блоков
- Каждая пылинка живет **60 секунд**, затем исчезает
- **1-2 пыли** с каждой пылинки
- **Только на поверхности** (требуется solid блок снизу и воздух сверху)

---

## ⚠️ Важные изменения

### Отключена генерация руды

Файл: `data/manaborn/fabric/biome_modifier/add_magic_ore.json`

```json
{
  "type": "fabric:add_features",
  "biomes": "#minecraft:is_overworld",
  "step": "underground_ores",
  "features": []  // ← Пустой массив = руда не генерируется
}
```

**Примечание:** Магическая руда (блок) остается в игре для обратной совместимости, но больше не генерируется естественно.

### Обновлен tooltip Magic Dust

Старый текст:
> "Мерцающая магическая пыль,  
> полученная из магической руды."

Новый текст:
> "Мерцающая магическая пыль,  
> обнаруживаемая с помощью маноскопа."

---

## 🐛 Отладка

### Проверка спавна пылинок:

```
/execute as @a run execute at @s run summon manaborn:dust_wisp ~ ~1 ~
```

### Выдать Маноскоп:

```
/give @p manaborn:manoscope
```

### Проверить количество пылинок:

```
/execute at @p run execute as @e[type=manaborn:dust_wisp,distance=..32] run say Found wisp
```

---

## 📚 Дополнительная документация

- **Текстуры**: `TEXTURE_REQUIREMENTS_MANOSCOPE.md`
- **Структура проекта**: `.windsurf/rules/manaborn-project-structure.md`
- **API правила**: `.windsurf/rules/windsurf_api_rules.md`

---

## ✅ Статус реализации

| Компонент | Статус | Примечания |
|-----------|--------|------------|
| ManoscopeItem | ✅ Готово | Работает как обычный Item |
| DustWispEntity | ✅ Готово | API 1.21.10 методы |
| DustWispManager | ✅ Готово | Спавн логика |
| Клиентский рендерер | ✅ Готово | EmptyEntityRenderer |
| Particle система | ✅ Готово | Только с Маноскопом |
| Локализация | ✅ Готово | EN + RU |
| Модели/дескрипторы | ✅ Готово | JSON файлы |
| **Рецепт крафта** | ✅ Готово | Лазурит + стекло + палка |
| **Текстура** | ⏳ Требуется | 16x16 PNG |

---

## 🛠️ Рецепт крафта

Маноскоп создается на верстаке по следующему рецепту:

```
[Лазурит] [Лазурит] [Лазурит]
[Стекло]   [Палка]   [Стекло]
[Лазурит] [Лазурит] [Лазурит]
```

**Материалы:**
- 6x Лазурит (Lapis Lazuli) - магическая энергия
- 2x Стекло (Glass) - линзы для обнаружения
- 1x Палка (Stick) - оправа

**Стоимость:** Средняя (требует лазурита, но не редкие материалы)

**Файл:** `data/manaborn/recipe/manoscope.json`

---

## 🚀 Что дальше?

1. **Создайте текстуру** для Маноскопа (`textures/item/manoscope.png`)
   - См. детали в `TEXTURE_REQUIREMENTS_MANOSCOPE.md`
   - **Цвета**: Синий (лазурит), аква (стекло), коричневый (палка)
2. **Протестируйте** механику в игре:
   - Наденьте Маноскоп
   - Найдите частицы
   - Получите пыль
4. **Настройте баланс** при необходимости (константы в Manager классах)

---

## 📞 Контакты и поддержка

Если возникнут вопросы по реализации:
- Проверьте `.windsurf/rules/` для правил API
- Изучите `TEXTURE_REQUIREMENTS_MANOSCOPE.md` для дизайна

**Версия документа:** 1.0  
**Дата:** 2025-11-10  
**Minecraft:** 1.21.10  
**Fabric API:** 0.134.1+1.21.10
