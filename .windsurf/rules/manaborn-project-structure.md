---
trigger: always_on
---

# Manaborn - Структура проекта

## 📁 Обзор проекта

**Manaborn** - мод для Minecraft, добавляющий систему маны и магические предметы.

### Основные компоненты:
- ✨ Система маны (100 единиц максимум)
- 🔮 Магический посох (Magic Staff)
- 🔄 Автоматическая регенерация маны
- 📊 HUD отображение маны
- 🌐 Синхронизация клиент-сервер

## 🗂️ Структура файлов

```
src/
├── main/java/com/manaborn/
│   ├── ManabornMod.java              # Главный класс мода
│   ├── items/
│   │   └── MagicStaffItem.java       # Магический посох
│   ├── mana/
│   │   └── ManaManager.java          # Управление маной (серверная логика)
│   └── network/
│       └── ManaSyncS2CPacket.java    # Пакет синхронизации маны
│
├── client/java/com/manaborn/client/
│   ├── ManabornClient.java           # Клиентская инициализация
│   ├── ManaHudOverlay.java           # Отображение маны на HUD
│   └── state/
│       └── ClientManaState.java      # Клиентское состояние маны
│
└── main/resources/
    ├── fabric.mod.json               # Метаданные мода
    ├── modid.mixins.json             # Конфигурация миксинов
    └── assets/manaborn/
        ├── lang/
        │   └── en_us.json            # Локализация
        └── models/item/
            └── magic_staff.json      # Модель посоха
```

## 🔧 Компоненты системы

### 1. ManabornMod (Серверная инициализация)

**Путь**: `src/main/java/com/manaborn/ManabornMod.java`

**Функции**:
- Регистрация предметов (Magic Staff)
- Регистрация сетевых пакетов
- Настройка серверных событий (тики, подключение игроков)

**Ключевые моменты**:
```java
public static final String MOD_ID = "manaborn";
public static Item MAGIC_STAFF;
```

### 2. ManaManager (Управление маной)

**Путь**: `src/main/java/com/manaborn/mana/ManaManager.java`

**Константы**:
- `MAX_MANA = 100` - максимальная мана
- `REGEN_UNIT_PER_5S = 20` - регенерация 20 маны за 5 секунд
- `REGEN_DELAY_TICKS = 40` - задержка 2 секунды после траты

**Хранилища данных**:
- `playerIdToMana` - текущая мана игроков
- `lastSpendTick` - время последней траты маны
- `regenTickCounter` - счетчик для регенерации

**Основные методы**:
- `getMana(UUID)` - получить ману игрока
- `setMana(UUID, int)` - установить ману
- `tryConsume(ServerPlayerEntity, int)` - попытка потратить ману
- `onServerTick(Iterable<ServerPlayerEntity>)` - обработка регенерации
- `sendSync(ServerPlayerEntity)` - синхронизация с клиентом

### 3. ManaSyncS2CPacket (Сетевой пакет)

**Путь**: `src/main/java/com/manaborn/network/ManaSyncS2CPacket.java`

**Структура**:
```java
public record ManaSyncS2CPacket(int mana, int maxMana) implements CustomPayload
```

**Поля**:
- `ID` - идентификатор пакета `manaborn:mana_sync`
- `CODEC` - кодек для сериализации (VAR_INT для обоих полей)

### 4. MagicStaffItem (Магический посох)

**Путь**: `src/main/java/com/manaborn/items/MagicStaffItem.java`

**Функциональность**:
- Стоимость использования: 20 маны
- Эффект: запуск огненного шара
- Кулдаун: 100 тиков (5 секунд)
- Звук: `ITEM_FIRECHARGE_USE`

**Логика использования**:
1. Проверка, что вызов на сервере
2. Попытка потратить 20 маны через `ManaManager.tryConsume()`
3. Если успешно - создание и запуск `SmallFireballEntity`
4. Установка кулдауна на предмет

### 5. ManabornClient (Клиентская инициализация)

**Путь**: `src/client/java/com/manaborn/client/ManabornClient.java`

**Функции**:
- Регистрация HUD элемента через `HudElementRegistry`
- Регистрация обработчика пакета `ManaSyncS2CPacket`
- Настройка клиентских тиков для регенерации

**Важно**: Использует новый API `HudElementRegistry.attachElementBefore()` вместо устаревшего `HudRenderCallback`

### 6. ManaHudOverlay (Отображение HUD)

**Путь**: `src/client/java/com/manaborn/client/ManaHudOverlay.java`

**Визуализация**:
- Позиция: правый нижний угол (над хотбаром)
- Формат: пузырьки маны (10 маны = 1 пузырек)
- Цвета:
  - Заполненные: `0xFF4DA3FF` (голубой)
  - Пустые: `0xFF000000` (черный)
  - Обводка: `0xFF1F1F1F` (темно-серый)

**Размеры**:
- Размер пузырька: 9x9 пикселей
- Промежуток между пузырьками: 1 пиксель

### 7. ClientManaState (Клиентское состояние)

**Путь**: `src/client/java/com/manaborn/client/state/ClientManaState.java`

**Назначение**: Хранение и обработка маны на стороне клиента

**Функции**:
- Хранение текущей и максимальной маны
- Клиентская регенерация (для плавности отображения)
- Синхронизация с серверными данными

**Важно**: Клиентская регенерация - только для визуала, истинные данные на сервере

## 🔄 Поток данных

### Инициализация при входе игрока:
```
1. ServerPlayConnectionEvents.JOIN
2. ManaManager.ensureInitialized(player)
3. ManaManager.sendSync(player)
4. ManaSyncS2CPacket → Client
5. ClientManaState.set(mana, maxMana)
```

### Использование маны:
```
1. Player использует Magic Staff
2. MagicStaffItem.use() (на сервере)
3. ManaManager.tryConsume(player, 20)
4. Если успешно:
   - Обновление серверных данных
   - ManaManager.sendSync(player)
   - ManaSyncS2CPacket → Client
   - ClientManaState.set(newMana, maxMana)
   - Обновление HUD
```

### Регенерация маны:
```
Сервер (каждый тик):
1. ServerTickEvents.END_SERVER_TICK
2. ManaManager.onServerTick(players)
3. Проверка задержки после траты
4. Увеличение счетчика регенерации
5. Каждые 5 тиков: +1 мана
6. ManaManager.sendSync(player)

Клиент (каждый тик):
1. ClientTickEvents.END_CLIENT_TICK
2. ClientManaState.clientTick()
3. Локальная регенерация для плавности
4. Обновление HUD
```

## 🎯 Ключевые особенности реализации

### 1. Разделение клиент/сервер
- Сервер - источник истины для маны
- Клиент получает обновления через пакеты
- Клиентская регенерация только для визуала

### 2. Использование современного API
- ✅ `HudElementRegistry` вместо deprecated `HudRenderCallback`
- ✅ `Identifier.of()` вместо `new Identifier()`
- ✅ `CustomPayload` с `PacketCodec` для сети
- ✅ `RegistryKey` при регистрации предметов

### 3. Потокобезопасность
- Использование `ConcurrentHashMap` для хранения данных игроков
- Выполнение клиентской логики в главном потоке через `context.client().execute()`

### 4. Оптимизация
- Регенерация только для игроков с неполной маной
- Отправка пакетов только при изменении значений
- Проверка `hudHidden` перед рендерингом

## 📝 Конфигурация

### fabric.mod.json
```json
{
  "id": "manaborn",
  "version": "1.0.0",
  "name": "Manaborn",
  "description": "A magic mod with spells, mana, and mystic power.",
  "entrypoints": {
    "main": ["com.manaborn.ManabornMod"],
    "client": ["com.manaborn.client.ManabornClient"]
  },
  "depends": {
    "fabricloader": ">=0.17.2",
    "minecraft": "~1.21.10",
    "java": ">=21",
    "fabric-api": "*"
  }
}
```

## 🐛 Известные проблемы и решения

### Проблема: HudRenderCallback deprecated
**Решение**: Использовать `HudElementRegistry` из пакета `net.fabricmc.fabric.api.client.rendering.v1.hud`

### Проблема: Десинхронизация маны
**Решение**: Всегда отправлять пакет синхронизации после изменения маны на сервере

### Проблема: Мана не регенерируется
**Решение**: Проверить, что `ServerTickEvents` зарегистрирован и вызывается `ManaManager.onServerTick()`

---

**Версия проекта**: 1.0.0
**Последнее обновление**: Октябрь 2025
