# 🔮 Система улучшения заклинаний - Статус реализации

## ✅ Что уже готово:

### 1. **Система уровней заклинаний**
- **Файл**: `SpellLevelManager.java`
- **Функции**:
  - Хранение уровней заклинаний (1-3) для каждого игрока
  - `getSpellLevel(UUID, SpellType)` - получить уровень
  - `setSpellLevel(UUID, SpellType, int)` - установить уровень
  - `upgradeSpell(UUID, SpellType)` - улучшить на 1 уровень
  - `canUpgrade(UUID, SpellType)` - проверка возможности улучшения
  - Сохранение/загрузка в NBT

### 2. **Интерфейс уровневых заклинаний**
- **Файл**: `LeveledSpell.java`
- **Функции**:
  - `getManaCost(int level)` - стоимость маны для уровня
  - `getCooldown(int level)` - кулдаун для уровня
  - `supportsLevel(int level)` - проверка поддержки уровня

### 3. **Обновлённый SpellManager**
- **Изменения в `SpellManager.java`**:
  - При касте заклинания автоматически проверяет уровень игрока
  - Использует `getManaCost(level)` и `getCooldown(level)` для уровневых заклинаний
  - Работает прозрачно для всех заклинаний

### 4. **Пример реализации - Fireball**
- **Файл**: `FireballSpell.java`
- **Реализует**: `LeveledSpell`
- **Характеристики по уровням**:
  - Уровень 1 (слабый): Взрыв 1.0, скорость 0.5x, кд 7с, мана 25
  - Уровень 2 (средний): Взрыв 1.5, скорость 1.25x, кд 6с, мана 22
  - Уровень 3 (ОРИГИНАЛ): Взрыв 2.0, скорость 2.0x, кд 5с, мана 20

**ВАЖНО**: Уровень 3 = оригинальные характеристики (до добавления системы уровней)

### 5. **Документация**
- **`SPELL_LEVELS.md`** - полная таблица характеристик всех заклинаний по уровням

---

## ⏳ Что нужно доделать:

### 1. **Обновить остальные 8 заклинаний**

Нужно обновить следующие файлы по образцу `FireballSpell.java`:

#### Для каждого заклинания:
1. Добавить `implements LeveledSpell` к классу
2. Добавить методы `getManaCost(int level)` и `getCooldown(int level)`
3. Обновить логику `cast()` для учёта уровня (где нужно)

#### Список файлов для обновления:
- `IceShardSpell.java` - замедление зависит от уровня
- `FireWallSpell.java` - размер стены зависит от уровня
- `InnerHeatSpell.java` - эффект скорости зависит от уровня
- `FrostAuraSpell.java` - длительность и радиус зависят от уровня
- `AbsorbingBarrierSpell.java` - **ВАЖНО**: восстановление маны только с уровня 2+
- `WeaknessCurseSpell.java` - дальность и сила эффекта зависят от уровня
- `IceWallSpell.java` - **ВАЖНО**: доступные режимы зависят от уровня
- `HeatWeaponSpell.java` - **ВАЖНО**: совместимость с оружием и взрыв зависят от уровня

### 2. **Создать блок Spell Enhancement Altar**

#### Структура:
```
blocks/SpellEnhancementAltarBlock.java
blocks/entity/SpellEnhancementAltarBlockEntity.java  
screen/SpellEnhancementAltarScreenHandler.java
client/ui/SpellEnhancementAltarScreen.java
```

#### Функциональность:
1. **GUI**: Отображение всех изученных заклинаний
2. **Выбор заклинания**: Нажатие на заклинание показывает требования
3. **Слоты для ресурсов**: Куда класть предметы для улучшения
4. **Кнопка "Улучшить"**: Проверяет ресурсы и повышает уровень
5. **Отображение уровня**: Показать текущий уровень каждого заклинания

#### Требования для улучшения:

##### Уровень 1 → 2:
```java
REQUIREMENTS_LVL_1_TO_2 = {
    Items.DIAMOND -> 2,
    ManabornMod.MAGIC_DUST -> 32,
    // Книга заклинания соответствующего типа
}
```

##### Уровень 2 → 3:
```java
REQUIREMENTS_LVL_2_TO_3 = {
    Items.NETHERITE_INGOT -> 1,
    ManabornMod.ETHER -> 4,
    ManabornMod.MAGIC_DUST -> 64,
    // Книга заклинания соответствующего типа
}
```

### 3. **Добавить сетевую синхронизацию**

#### Создать пакет:
```java
network/packets/SpellLevelSyncS2CPacket.java
```

#### Функции:
- Отправка всех уровней заклинаний при подключении
- Отправка обновления при улучшении
- Клиентское хранилище: `client/state/ClientSpellLevelState.java`

### 4. **Интеграция с существующими системами**

#### Обновить:
1. **SpellHudOverlay** - показывать уровень заклинания на HUD
2. **RadialSpellMenuScreen** - показывать уровень возле заклинания
3. **MagicStaffItem.appendTooltip()** - показывать уровень в tooltip

### 5. **Persistent Data Storage**

#### Добавить в PlayerDataManager:
- Сохранение уровней заклинаний при выходе
- Загрузка уровней при входе
- Интеграция с `ServerPlayConnectionEvents.JOIN`

---

## 📝 Порядок реализации (рекомендуемый):

1. ✅ **Базовая система уровней** (готово)
2. ⏳ **Обновить все 9 заклинаний** (Fireball готов, осталось 8)
3. ⏳ **Создать блок алтаря улучшения**
4. ⏳ **Создать GUI для алтаря**
5. ⏳ **Добавить рецепт крафта алтаря**
6. ⏳ **Создать сетевую синхронизацию**
7. ⏳ **Добавить persistent storage**
8. ⏳ **Обновить HUD и UI для отображения уровней**
9. ⏳ **Тестирование**

---

## 🔧 Пример использования API:

### Получить уровень заклинания:
```java
int level = SpellLevelManager.getSpellLevel(player.getUuid(), SpellType.FIREBALL);
```

### Улучшить заклинание:
```java
boolean success = SpellLevelManager.upgradeSpell(player.getUuid(), SpellType.FIREBALL);
if (success) {
    player.sendMessage(Text.literal("Fireball улучшен до уровня " + 
        SpellLevelManager.getSpellLevel(player.getUuid(), SpellType.FIREBALL)));
}
```

### Проверить возможность улучшения:
```java
if (SpellLevelManager.canUpgrade(player.getUuid(), SpellType.FIREBALL)) {
    // Показать кнопку "Улучшить"
}
```

---

## 🎯 Текущий статус:

**Готовность: ~20%**

- ✅ Базовая архитектура системы
- ✅ Fireball полностью обновлён
- ✅ SpellManager учитывает уровни
- ⏳ Осталось 8 заклинаний
- ⏳ Блок алтаря не создан
- ⏳ GUI не создано
- ⏳ Сетевая синхронизация не реализована

**Следующий шаг**: Обновить остальные заклинания по образцу Fireball.
