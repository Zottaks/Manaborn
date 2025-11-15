# 🔮 TODO: Завершение Spell Enhancement Altar

## ✅ Что уже создано:

1. **SpellEnhancementAltarBlock.java** - блок алтаря
2. **SpellEnhancementAltarBlockEntity.java** - логика инвентаря и улучшения
3. **SpellEnhancementAltarScreenHandler.java** - обработчик GUI (5 слотов)
4. **SpellLevelManager.java** - система уровней заклинаний
5. **LeveledSpell interface** - интерфейс для уровневых заклинаний

## ⚠️ КРИТИЧЕСКИ ВАЖНО: Исправить текстуры!

Файлы имеют двойное расширение `.png.png` - нужно переименовать в `.png`:

**Текущие файлы:**
- `spell_enhancement_altar_top.png.png`
- `spell_enhancement_altar_side.png.png`

**Должны быть:**
- `spell_enhancement_altar_top.png`
- `spell_enhancement_altar_side.png`

**Путь:** `src/main/resources/assets/manaborn/textures/block/`

---

## 📝 Шаг 1: Добавить регистрацию в ManabornMod.java

### 1.1 Добавить статические поля (после строки ~107):

```java
public static Block SPELL_ENHANCEMENT_ALTAR;
public static BlockEntityType<SpellEnhancementAltarBlockEntity> SPELL_ENHANCEMENT_ALTAR_BE;
public static ScreenHandlerType<SpellEnhancementAltarScreenHandler> SPELL_ENHANCEMENT_ALTAR_SH;
```

### 1.2 Зарегистрировать блок (в методе onInitialize, после других блоков ~line 350):

```java
// Регистрация Spell Enhancement Altar блока
RegistryKey<Block> spellEnhancementAltarBlockKey = RegistryKey.of(Registries.BLOCK.getKey(),
		Identifier.of(MOD_ID, "spell_enhancement_altar"));
SPELL_ENHANCEMENT_ALTAR = Registry.register(
		Registries.BLOCK,
		spellEnhancementAltarBlockKey,
		new SpellEnhancementAltarBlock(AbstractBlock.Settings.create()
				.strength(3.0f)
				.luminance(state -> 10)
				.requiresTool()));

RegistryKey<Item> spellEnhancementAltarItemKey = RegistryKey.of(Registries.ITEM.getKey(),
		Identifier.of(MOD_ID, "spell_enhancement_altar"));
Registry.register(
		Registries.ITEM,
		spellEnhancementAltarItemKey,
		new BlockItem(SPELL_ENHANCEMENT_ALTAR, new Item.Settings().registryKey(spellEnhancementAltarItemKey)));

ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
		.register(entries -> entries.add(SPELL_ENHANCEMENT_ALTAR));
```

### 1.3 Зарегистрировать BlockEntity (после других BE ~line 380):

```java
// Регистрация Spell Enhancement Altar Block Entity
SPELL_ENHANCEMENT_ALTAR_BE = Registry.register(
		Registries.BLOCK_ENTITY_TYPE,
		Identifier.of(MOD_ID, "spell_enhancement_altar"),
		BlockEntityType.Builder.create(
				SpellEnhancementAltarBlockEntity::new,
				SPELL_ENHANCEMENT_ALTAR)
				.build());
```

### 1.4 Зарегистрировать ScreenHandler (после других SH ~line 400):

```java
// Регистрация Spell Enhancement Altar Screen Handler
SPELL_ENHANCEMENT_ALTAR_SH = Registry.register(
		Registries.SCREEN_HANDLER,
		Identifier.of(MOD_ID, "spell_enhancement_altar"),
		new ScreenHandlerType<>(SpellEnhancementAltarScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
```

---

## 📝 Шаг 2: Создать GUI файлы (клиент)

### 2.1 Создать SpellEnhancementAltarScreen.java

**Путь:** `src/client/java/com/manaborn/client/ui/SpellEnhancementAltarScreen.java`

**Функции:**
- Отображение всех изученных заклинаний с их текущим уровнем
- Кликабельные иконки заклинаний
- Отображение требований для улучшения
- Кнопка "Улучшить"
- 5 слотов для ресурсов

### 2.2 Зарегистрировать Screen в ManabornClient.java:

```java
HandledScreens.register(ManabornMod.SPELL_ENHANCEMENT_ALTAR_SH, SpellEnhancementAltarScreen::new);
```

---

## 📝 Шаг 3: Создать модели и локализацию

### 3.1 Blockstate JSON

**Файл:** `src/main/resources/assets/manaborn/blockstates/spell_enhancement_altar.json`

```json
{
  "variants": {
    "": {
      "model": "manaborn:block/spell_enhancement_altar"
    }
  }
}
```

### 3.2 Block Model JSON

**Файл:** `src/main/resources/assets/manaborn/models/block/spell_enhancement_altar.json`

```json
{
  "parent": "minecraft:block/cube",
  "textures": {
    "particle": "manaborn:block/spell_enhancement_altar_side",
    "down": "manaborn:block/spell_enhancement_altar_side",
    "up": "manaborn:block/spell_enhancement_altar_top",
    "north": "manaborn:block/spell_enhancement_altar_side",
    "east": "manaborn:block/spell_enhancement_altar_side",
    "south": "manaborn:block/spell_enhancement_altar_side",
    "west": "manaborn:block/spell_enhancement_altar_side"
  }
}
```

### 3.3 Item Model JSON

**Файл:** `src/main/resources/assets/manaborn/models/item/spell_enhancement_altar.json`

```json
{
  "parent": "manaborn:block/spell_enhancement_altar"
}
```

### 3.4 Локализация

**en_us.json:**
```json
"block.manaborn.spell_enhancement_altar": "Spell Enhancement Altar",
"item.manaborn.spell_enhancement_altar": "Spell Enhancement Altar",
"container.manaborn.spell_enhancement_altar": "Spell Enhancement"
```

**ru_ru.json:**
```json
"block.manaborn.spell_enhancement_altar": "Алтарь Улучшения Заклинаний",
"item.manaborn.spell_enhancement_altar": "Алтарь Улучшения Заклинаний",
"container.manaborn.spell_enhancement_altar": "Улучшение Заклинаний"
```

---

## 📝 Шаг 4: Создать рецепт крафта

**Файл:** `src/main/resources/data/manaborn/recipe/spell_enhancement_altar.json`

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": [
    "EME",
    "OAO",
    "OOO"
  ],
  "key": {
    "E": "manaborn:ether",
    "M": "manaborn:magic_dust",
    "O": "minecraft:obsidian",
    "A": "manaborn:learning_altar"
  },
  "result": {
    "id": "manaborn:spell_enhancement_altar",
    "count": 1
  }
}
```

---

## 📝 Шаг 5: Обновить остальные 8 заклинаний

Каждое заклинание нужно обновить по образцу `FireballSpell.java`:

1. Добавить `implements LeveledSpell`
2. Добавить методы `getManaCost(int level)` и `getCooldown(int level)`
3. Обновить логику `cast()` для учёта уровня

**Список заклинаний:**
- IceShardSpell
- FireWallSpell
- InnerHeatSpell
- FrostAuraSpell
- AbsorbingBarrierSpell (важно: мана за урон только с уровня 2+)
- WeaknessCurseSpell
- IceWallSpell (важно: режимы зависят от уровня)
- HeatWeaponSpell (важно: совместимость с оружием зависит от уровня)

---

## 🎯 Приоритет выполнения:

1. **ПЕРВОЕ**: Исправить расширения текстур (.png.png → .png)
2. **ВТОРОЕ**: Добавить регистрацию в ManabornMod.java
3. **ТРЕТЬЕ**: Создать модели и локализацию
4. **ЧЕТВЁРТОЕ**: Скомпилировать и протестировать базовый блок
5. **ПЯТОЕ**: Создать GUI
6. **ШЕСТОЕ**: Обновить остальные заклинания

---

## 🔧 Команды для тестирования:

```bash
.\gradlew build
.\gradlew runClient
```

**В игре:**
```
/give @p manaborn:spell_enhancement_altar
```

---

## 📊 Текущий статус: ~40%

- ✅ Система уровней (SpellLevelManager)
- ✅ Fireball с 3 уровнями
- ✅ Блок алтаря (SpellEnhancementAltarBlock)
- ✅ Block Entity с логикой улучшения
- ✅ ScreenHandler (5 слотов)
- ⏳ Регистрация в ManabornMod (нужно добавить)
- ⏳ Модели и текстуры (текстуры есть, модели нужны)
- ⏳ GUI (не создано)
- ⏳ Остальные 8 заклинаний (не обновлены)
