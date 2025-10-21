# Система изучения заклинаний

## 📚 Обзор

Система изучения заклинаний добавляет прогрессию в использование магии. Игроки должны находить и изучать книги заклинаний, прежде чем смогут использовать их через Магический посох.

## 🎯 Основные компоненты

### 1. LearnedSpellsManager (Серверная логика)
**Путь**: `src/main/java/com/manaborn/spells/LearnedSpellsManager.java`

Управляет изученными заклинаниями на сервере:
- Хранит данные о том, какие заклинания изучил каждый игрок
- Синхронизирует данные с клиентом через пакеты
- По умолчанию игроки не знают никаких заклинаний

### 2. SpellBookItem (Предмет книги)
**Путь**: `src/main/java/com/manaborn/items/SpellBookItem.java`

Книга заклинания:
- Каждая книга содержит одно конкретное заклинание
- При использовании (ПКМ) изучает заклинание
- После изучения книга исчезает
- Нельзя изучить заклинание дважды

### 3. LearnedSpellsSyncS2CPacket (Сетевой пакет)
**Путь**: `src/main/java/com/manaborn/network/LearnedSpellsSyncS2CPacket.java`

Синхронизирует изученные заклинания с клиентом:
- Отправляется при входе игрока
- Отправляется при изучении нового заклинания

### 4. ClientLearnedSpellsState (Клиентское состояние)
**Путь**: `src/client/java/com/manaborn/client/state/ClientLearnedSpellsState.java`

Хранит информацию об изученных заклинаниях на клиенте:
- Используется для отображения в UI
- Синхронизируется с сервером

## 🎮 Игровая механика

### Изучение заклинания
1. Игрок находит книгу заклинания (крафт или добыча)
2. Держа книгу в руке, нажимает ПКМ
3. Заклинание изучается, книга исчезает
4. Появляется сообщение: "✨ Вы изучили заклинание [Название]!"

### Использование посоха
- **ПКМ** - Использовать активное заклинание
- **Shift + ПКМ** - Переключить на следующее изученное заклинание
- Посох показывает только изученные заклинания в tooltip
- Нельзя использовать неизученные заклинания

### Проверки
- При попытке использовать неизученное заклинание появляется сообщение: "Вы не изучили заклинание [Название]!"
- При переключении заклинаний посох циклически переключается только между изученными

## 📦 Доступные книги заклинаний

### Fireball Spell Book (Книга заклинания: Огненный шар)
**ID**: `manaborn:fireball_spell_book`

**Рецепт крафта**:
```
D F D
D B D
D D D

D = Magic Dust (Магическая пыль)
F = Fire Charge (Огненный заряд)
B = Book (Книга)
```

**Заклинание**: Огненный шар (20 маны)

## 🔧 Добавление новых книг заклинаний

### 1. Создать предмет книги в ManabornMod.java:
```java
RegistryKey<Item> iceShardBookKey = RegistryKey.of(
    Registries.ITEM.getKey(), 
    Identifier.of(MOD_ID, "ice_shard_spell_book")
);
ICE_SHARD_SPELL_BOOK = Registry.register(
    Registries.ITEM, 
    iceShardBookKey, 
    new SpellBookItem(
        new Item.Settings().maxCount(1).registryKey(iceShardBookKey), 
        SpellType.ICE_SHARD
    )
);
ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
    .register(entries -> entries.add(ICE_SHARD_SPELL_BOOK));
```

### 2. Добавить локализацию в `en_us.json`:
```json
"item.manaborn.ice_shard_spell_book": "Spell Book: Ice Shard"
```

### 3. Создать рецепт в `data/manaborn/recipe/`:
```json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": ["DID", "DBD", "DDD"],
  "key": {
    "D": "manaborn:magic_dust",
    "I": "minecraft:ice",
    "B": "minecraft:book"
  },
  "result": {
    "id": "manaborn:ice_shard_spell_book",
    "count": 1
  }
}
```

### 4. Создать модель и текстуру:
- `assets/manaborn/models/item/ice_shard_spell_book.json`
- `assets/manaborn/textures/item/ice_shard_spell_book.png`

## 🔄 Поток данных

### При входе игрока:
```
1. ServerPlayConnectionEvents.JOIN
2. LearnedSpellsManager.ensureInitialized(player)
3. LearnedSpellsManager.sendSync(player)
4. LearnedSpellsSyncS2CPacket → Client
5. ClientLearnedSpellsState.setLearnedSpells(spellIds)
```

### При изучении заклинания:
```
1. Player использует SpellBookItem (ПКМ)
2. SpellBookItem.use() проверяет, не изучено ли уже
3. LearnedSpellsManager.learnSpell(player, spell)
4. Книга исчезает (stack.decrementUnlessCreative)
5. LearnedSpellsManager.sendSync(player)
6. LearnedSpellsSyncS2CPacket → Client
7. ClientLearnedSpellsState обновляется
8. HUD обновляется автоматически
```

### При использовании посоха:
```
1. Player использует MagicStaffItem
2. SpellManager.castSpell() проверяет изученные заклинания
3. Если не изучено - сообщение об ошибке
4. Если изучено - проверка маны и каст заклинания
```

## 🎨 UI/UX особенности

### Tooltip посоха:
- Показывает активное заклинание
- Список изученных заклинаний с индикатором активного (►)
- Если нет изученных заклинаний: "Нет изученных заклинаний! Найдите книги заклинаний."

### Tooltip книги:
- Название заклинания с цветом
- Стоимость маны
- Инструкция по использованию
- Эффект свечения (как зачарованный предмет)

### Сообщения:
- ✨ При изучении: "✨ Вы изучили заклинание [Название]!"
- ❌ При попытке изучить дважды: "Вы уже изучили заклинание [Название]!"
- ⚠️ При попытке использовать неизученное: "Вы не изучили заклинание [Название]!"

## 🔍 Отладка

### Проверить изученные заклинания игрока:
```java
Set<SpellType> learned = LearnedSpellsManager.getLearnedSpells(player.getUuid());
System.out.println("Learned spells: " + learned);
```

### Очистить данные игрока:
```java
LearnedSpellsManager.clearPlayer(player.getUuid());
```

### Принудительно изучить заклинание:
```java
LearnedSpellsManager.learnSpell(player, SpellType.FIREBALL);
```

---

**Версия**: 1.0.0  
**Дата создания**: Октябрь 2025
