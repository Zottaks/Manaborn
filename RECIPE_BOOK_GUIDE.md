# 📖 Руководство по Recipe Book (Книга рецептов)

## Что это?

Recipe Book (Книга рецептов) - это зелёная книжка в верстаке, которая показывает доступные рецепты крафта.

## 📁 Структура файлов

Для каждого рецепта нужны **два файла**:

### 1. Файл рецепта
**Путь**: `data/manaborn/recipe/[название].json`

Пример: `data/manaborn/recipe/fireball_spell_book.json`

### 2. Файл advancement (достижение)
**Путь**: `data/manaborn/advancement/recipes/[категория]/[название].json`

Пример: `data/manaborn/advancement/recipes/combat/fireball_spell_book.json`

## 📂 Категории рецептов

Advancement'ы должны быть в правильной папке по категории:

- **combat** - оружие и боевые предметы
- **equipment** - инструменты и снаряжение
- **misc** - разное (зелья, материалы)
- **building** - строительные блоки
- **redstone** - редстоун механизмы

## 🔧 Структура advancement файла

```json
{
  "parent": "minecraft:recipes/root",
  "criteria": {
    "has_ingredient_1": {
      "trigger": "minecraft:inventory_changed",
      "conditions": {
        "items": [
          {
            "items": "namespace:item_id"
          }
        ]
      }
    },
    "has_the_recipe": {
      "trigger": "minecraft:recipe_unlocked",
      "conditions": {
        "recipe": "namespace:recipe_id"
      }
    }
  },
  "requirements": [
    [
      "has_ingredient_1",
      "has_the_recipe"
    ]
  ],
  "rewards": {
    "recipes": [
      "namespace:recipe_id"
    ]
  }
}
```

## ✅ Созданные advancement'ы

### 1. Fireball Spell Book
- **Категория**: combat
- **Условия**: Magic Dust, Fire Charge, Book
- **Путь**: `advancement/recipes/combat/fireball_spell_book.json`

### 2. Mana Wand
- **Категория**: equipment
- **Условия**: Magic Dust, Stick
- **Путь**: `advancement/recipes/equipment/mana_wand.json`

### 3. Mana Crystal
- **Категория**: misc
- **Условия**: Magic Dust, Lapis Lazuli
- **Путь**: `advancement/recipes/misc/mana_crystal.json`

## 🎮 Как работает в игре

1. **Получение ингредиента**: Когда игрок получает любой из ингредиентов рецепта
2. **Разблокировка**: Рецепт автоматически появляется в книге рецептов
3. **Отображение**: Зелёная книжка в верстаке показывает доступные рецепты
4. **Быстрый крафт**: Клик по рецепту автоматически расставляет предметы

## 📝 Добавление нового рецепта в книгу

### Шаг 1: Создайте рецепт
```json
// data/manaborn/recipe/my_item.json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": ["ABC", "DEF", "GHI"],
  "key": {
    "A": "minecraft:item1"
  },
  "result": {
    "id": "manaborn:my_item",
    "count": 1
  }
}
```

### Шаг 2: Создайте advancement
```json
// data/manaborn/advancement/recipes/equipment/my_item.json
{
  "parent": "minecraft:recipes/root",
  "criteria": {
    "has_item1": {
      "trigger": "minecraft:inventory_changed",
      "conditions": {
        "items": [{"items": "minecraft:item1"}]
      }
    },
    "has_the_recipe": {
      "trigger": "minecraft:recipe_unlocked",
      "conditions": {
        "recipe": "manaborn:my_item"
      }
    }
  },
  "requirements": [["has_item1", "has_the_recipe"]],
  "rewards": {
    "recipes": ["manaborn:my_item"]
  }
}
```

### Шаг 3: Перезапустите игру
Advancement'ы загружаются при старте мира.

## 🔍 Проверка

В игре:
1. Откройте верстак
2. Получите любой ингредиент рецепта
3. Нажмите на зелёную книжку
4. Рецепт должен появиться в соответствующей категории

## ⚠️ Важные моменты

1. **Имена файлов**: Имя advancement файла должно совпадать с именем рецепта
2. **Категория**: Папка категории должна совпадать с полем `category` в рецепте
3. **ID рецепта**: В `rewards.recipes` и `criteria.has_the_recipe.conditions.recipe` должен быть одинаковый ID
4. **Условия**: Добавьте условие для каждого ключевого ингредиента рецепта

## 🎯 Логика условий

### OR (ИЛИ) - любое условие
```json
"requirements": [
  ["has_item1"],
  ["has_item2"]
]
```
Рецепт разблокируется при получении item1 **ИЛИ** item2

### AND (И) - все условия
```json
"requirements": [
  ["has_item1", "has_item2"]
]
```
Рецепт разблокируется при получении item1 **И** item2

### Рекомендация
Используйте OR для всех ингредиентов + has_the_recipe:
```json
"requirements": [
  [
    "has_ingredient1",
    "has_ingredient2",
    "has_the_recipe"
  ]
]
```

---

**Версия**: 1.0.0  
**Обновлено**: Октябрь 2025
