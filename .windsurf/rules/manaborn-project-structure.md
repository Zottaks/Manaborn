---
trigger: always_on
---

# Manaborn - Структура проекта

## 📁 Обзор проекта

**Manaborn** - мод для Minecraft, добавляющий систему маны и магические предметы.

**Целевая платформа**: Minecraft 1.20.1 + Fabric API 0.92.2+1.20.1

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
│       └── NetworkConstants.java     # Константы для сетевых пакетов
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
        ├── models/item/
        │   └── magic_staff.json      # Модель посоха
        └── items/                    # ADDED: Для 1.21+ (item model descriptions)
            └── magic_staff.json      # Не требуется в 1.20.1!
```

## 🔧 Компоненты системы

### 1. ManabornMod (Серверная инициализация)

**Путь**: `src/main/java/com/manaborn/ManabornMod.java`

**Функции**:
- Регистрация предметов (Magic Staff)
- Регистрация обработчиков сетевых пакетов
- Настройка серверных событий (тики, подключение игроков)

**Ключевые моменты (1.20.1)**:
```java
public static final String MOD_ID = "manaborn";
public static Item MAGIC_STAFF;

// REPLACED: В 1.20.1 используется new Identifier()
public static final Identifier MANA_SYNC_PACKET = new Identifier(MOD_ID, "mana_sync");

@Override
public void onInitialize() {
    // Регистрация предметов
    registerItems();
    
    // Регистрация обработчиков пакетов C2S
    registerServerPacketHandlers();
    
    // События
    ServerTickEvents.END_SERVER_TICK.register(server -> {
        ManaManager.onServerTick(server.getPlayerManager().getPlayerList());
    });
    
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
        ManaManager.sendSync(handler.player);
    });
}
```

### 2. ManaManager (Управление маной)

**Путь**: `src/main/java/com/manaborn/mana/ManaManager.java`

**Константы**:
- `MAX_MANA = 100` - максимальная мана
- `REGEN_UNIT_PER_5S = 20` - регенерация 20 маны за 5 секунд
- `REGEN_DELAY_TICKS = 40` - задержка 2 секунды после траты

**Хранилища данных**:
- `playerIdToMana` - текущая мана игроков (`ConcurrentHashMap<UUID, Integer>`)
- `lastSpendTick` - время последней траты маны (`ConcurrentHashMap<UUID, Long>`)
- `regenTickCounter` - счетчик для регенерации (`ConcurrentHashMap<UUID, Integer>`)

**Основные методы**:
- `getMana(UUID)` - получить ману игрока
- `setMana(UUID, int)` - установить ману
- `tryConsume(ServerPlayerEntity, int)` - попытка потратить ману
- `onServerTick(Iterable<ServerPlayerEntity>)` - обработка регенерации
- `sendSync(ServerPlayerEntity)` - синхронизация с клиентом

**Thread-safety**: Все Map используют `ConcurrentHashMap` для безопасного доступа из разных потоков.

### 3. Сетевая синхронизация (1.20.1)

**Путь**: `src/main/java/com/manaborn/network/NetworkConstants.java`

**REPLACED**: В 1.20.1 используется `PacketByteBuf` вместо `CustomPayload`:
```java
public class NetworkConstants {
    // Идентификаторы пакетов
    public static final Identifier MANA_SYNC_S2C = new Identifier(MOD_ID, "mana_sync");
    public static final Identifier SPELL_CAST_C2S = new Identifier(MOD_ID, "spell_cast");
}
```

**Отправка пакета (Server -> Client)**:
```java
// В ManaManager.sendSync()
public static void sendSync(ServerPlayerEntity player) {
    UUID playerId = player.getUuid();
    int mana = getMana(playerId);
    int maxMana = MAX_MANA;
    
    // REPLACED: PacketByteBuf вместо CustomPayload
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarInt(mana);
    buf.writeVarInt(maxMana);
    
    ServerPlayNetworking.send(player, NetworkConstants.MANA_SYNC_S2C, buf);
}
```

**Импорты для networking**:
```java
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
```

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

**Регистрация (1.20.1)**:
```java
// REPLACED: Без registryKey в Settings
public static void registerItems() {
    MAGIC_STAFF = Registry.register(
        Registries.ITEM,
        new Identifier(MOD_ID, "magic_staff"),
        new MagicStaffItem(new Item.Settings().maxCount(1))
    );
}
```

### 5. ManabornClient (Клиентская инициализация)

**Путь**: `src/client/java/com/manaborn/client/ManabornClient.java`

**Функции (1.20.1)**:
- Регистрация HUD через `HudRenderCallback` (НЕ `HudElementRegistry`!)
- Регистрация обработчика пакета синхронизации маны
- Настройка клиентских тиков для регенерации

**REPLACED**: Использует `HudRenderCallback` для 1.20.1:
```java
@Override
public void onInitializeClient() {
    // Регистрация HUD
    HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
        ManaHudOverlay.render(drawContext, tickDelta);
    });
    
    // Регистрация обработчика пакета
    ClientPlayNetworking.registerGlobalReceiver(
        NetworkConstants.MANA_SYNC_S2C,
        (client, handler, buf, responseSender) -> {
            // Читаем данные из буфера
            int mana = buf.readVarInt();
            int maxMana = buf.readVarInt();
            
            // Выполняем в главном потоке клиента
            client.execute(() -> {
                ClientManaState.set(mana, maxMana);
            });
        }
    );
    
    // Клиентские тики для плавной регенерации
    ClientTickEvents.END_CLIENT_TICK.register(client -> {
        if (client.player != null) {
            ClientManaState.clientTick();
        }
    });
}
```

**Импорты**:
```java
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
```

### 6. ManaHudOverlay (Отображение HUD)

**Путь**: `src/client/java/com/manaborn/client/ManaHudOverlay.java`

**Сигнатура метода (1.20.1)**:
```java
public static void render(DrawContext context, float tickDelta) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.player == null || client.options.hudHidden) {
        return;
    }
    
    // tickDelta - прогресс между тиками (0.0 - 1.0)
    // Получаем данные
    int currentMana = ClientManaState.getMana();
    int maxMana = ClientManaState.getMaxMana();
    
    // Рисуем пузырьки маны
    renderManaBubbles(context, currentMana, maxMana);
}
```

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

**DrawContext методы (доступны в 1.20.1)**:
```java
context.fill(x1, y1, x2, y2, color);  // Заполнение прямоугольника
context.drawBorder(x, y, width, height, color);  // Рамка
context.drawText(textRenderer, text, x, y, color, shadow);  // Текст
```

### 7. ClientManaState (Клиентское состояние)

**Путь**: `src/client/java/com/manaborn/client/state/ClientManaState.java`

**Назначение**: Хранение и обработка маны на стороне клиента

**Функции**:
- Хранение текущей и максимальной маны
- Клиентская регенерация (для плавности отображения)
- Синхронизация с серверными данными

**Важно**: Клиентская регенерация - только для визуала, истинные данные на сервере

**Thread-safety**: Не требуется синхронизация, т.к. доступ только из главного потока клиента.

## 🔄 Поток данных (1.20.1)

### Инициализация при входе игрока:
```
1. ServerPlayConnectionEvents.JOIN
2. ManaManager.ensureInitialized(player)
3. ManaManager.sendSync(player)
   ├─ Создание PacketByteBuf
   ├─ buf.writeVarInt(mana)
   ├─ buf.writeVarInt(maxMana)
   └─ ServerPlayNetworking.send(player, MANA_SYNC_S2C, buf)
4. ClientPlayNetworking.registerGlobalReceiver обработчик
   ├─ mana = buf.readVarInt()
   ├─ maxMana = buf.readVarInt()
   └─ client.execute(() -> ClientManaState.set(mana, maxMana))
5. HUD обновляется
```

### Использование маны:
```
1. Player использует Magic Staff
2. MagicStaffItem.use() (на сервере)
3. ManaManager.tryConsume(player, 20)
4. Если успешно:
   - Обновление серверных данных
   - ManaManager.sendSync(player)
   - PacketByteBuf → Client
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
4. HudRenderCallback вызывается автоматически
5. ManaHudOverlay.render() отрисовывает HUD
```

## 🎯 Ключевые особенности реализации (1.20.1)

### 1. Разделение клиент/сервер
- Сервер - источник истины для маны
- Клиент получает обновления через пакеты
- Клиентская регенерация только для визуала

### 2. Использование правильного API для 1.20.1
- ✅ `PacketByteBuf` для сетевого взаимодействия
- ✅ `HudRenderCallback` для HUD рендеринга
- ✅ `new Identifier()` для создания идентификаторов
- ✅ `Item.Settings()` без `registryKey()`
- ✅ `DrawContext` для рисования (появился в 1.20+)

### 3. Потокобезопасность
- Использование `ConcurrentHashMap` для хранения данных игроков
- Выполнение клиентской логики в главном потоке через `context.client().execute()`
- Выполнение серверной логики в главном потоке через `server.execute()`

### 4. Оптимизация
- Регенерация только для игроков с неполной маной
- Отправка пакетов только при изменении значений (не каждый тик!)
- Проверка `hudHidden` перед рендерингом
- Использование `VarInt` для компактности данных

## 📝 Конфигурация (1.20.1)

### fabric.mod.json
```json
{
  "schemaVersion": 1,
  "id": "manaborn",
  "version": "1.0.0",
  "name": "Manaborn",
  "description": "A magic mod with spells, mana, and mystic power.",
  "authors": ["Your Name"],
  "contact": {
    "homepage": "https://example.com/",
    "sources": "https://github.com/example/manaborn"
  },
  "license": "MIT",
  "icon": "assets/manaborn/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": ["com.manaborn.ManabornMod"],
    "client": ["com.manaborn.client.ManabornClient"]
  },
  "mixins": [],
  "depends": {
    "fabricloader": ">=0.15.0",
    "minecraft": "~1.20.1",
    "java": ">=17",
    "fabric-api": "*"
  },
  "suggests": {}
}
```

### build.gradle (фрагмент для 1.20.1)
```groovy
dependencies {
    minecraft "com.mojang:minecraft:1.20.1"
    mappings "net.fabricmc:yarn:1.20.1+build.10:v2"
    modImplementation "net.fabricmc:fabric-loader:0.15.11"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.92.2+1.20.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 17
}
```

## 🐛 Известные проблемы и решения (1.20.1)

### Проблема: Попытка использовать CustomPayload
**Симптом**: `ClassNotFoundException: net.minecraft.network.packet.CustomPayload`
**Решение**: Использовать `PacketByteBuf` - `CustomPayload` появился в 1.20.5+

### Проблема: HudElementRegistry не найден
**Симптом**: `ClassNotFoundException` или compile error
**Решение**: Использовать `HudRenderCallback` - `HudElementRegistry` появился в 1.21+

### Проблема: Identifier.of() не найден
**Симптом**: Compile error - метод не существует
**Решение**: Использовать `new Identifier()` - `Identifier.of()` появился в 1.21+

### Проблема: Десинхронизация маны
**Решение**: Всегда отправлять пакет синхронизации после изменения маны на сервере

### Проблема: Мана не регенерируется
**Решение**: Проверить, что `ServerTickEvents` зарегистрирован и вызывается `ManaManager.onServerTick()`

### Проблема: Крах при чтении пакета
**Симптом**: `IndexOutOfBoundsException` или `DecoderException`
**Решение**: Проверить, что порядок `read*()` точно соответствует порядку `write*()` в буфере

## 🔄 Миграция на 1.21+

При переходе на Minecraft 1.21+ потребуются изменения:

### Networking (1.20.5+)
```java
// Старый код (1.20.1)
PacketByteBuf buf = PacketByteBufs.create();
buf.writeVarInt(mana);
ServerPlayNetworking.send(player, PACKET_ID, buf);

// Новый код (1.20.5+)
public record ManaSyncS2C(int mana) implements CustomPayload {
    public static final Id<ManaSyncS2C> ID = 
        new Id<>(new Identifier("manaborn", "mana_sync"));
    public static final PacketCodec<RegistryByteBuf, ManaSyncS2C> CODEC = 
        PacketCodec.tuple(PacketCodecs.VAR_INT, ManaSyncS2C::mana, ManaSyncS2C::new);
    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
```

### HUD (1.21+)
```java
// Старый код (1.20.1)
HudRenderCallback.EVENT.register((context, tickDelta) -> {});

// Новый код (1.21+)
HudElementRegistry.attachElementBefore(
    VanillaHudElements.CHAT,
    new Identifier("manaborn", "mana_hud"),
    (context, tickCounter) -> {}
);
```

### Identifier (1.21+)
```java
// Старый код (1.20.1)
new Identifier("modid", "name")

// Новый код (1.21+)
Identifier.of("modid", "name")
```

---

**Версия проекта**: 1.0.0 для Minecraft 1.20.1
**Последнее обновление**: Ноябрь 2024
**Совместимость**: Minecraft 1.20.1 + Fabric API 0.92.2+1.20.1
