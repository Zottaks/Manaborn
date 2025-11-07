---
trigger: always_on
---

# Manaborn - Структура проекта (Minecraft 1.21.10)

## 📁 Обзор проекта

**Manaborn** - мод для Minecraft, добавляющий систему маны и магические предметы.

**Целевая платформа**: Minecraft **1.21.10** + Fabric API **0.134.1+1.21.10** + Java **21**

### Основные компоненты:
- ✨ Система маны (100 единиц максимум)
- 🔮 Магический посох (Mana Wand)
- 🔄 Автоматическая регенерация маны
- 📊 HUD отображение маны и заклинаний
- 🌐 Синхронизация клиент-сервер через CustomPayload
- 📚 Система изучения заклинаний через Алтарь Изучения
- 🧪 Магические предметы (зелья, сосуды, пыль)
- 🏺 Магические блоки (руда, конвертер, алтарь)

---

## 🗂️ Структура файлов

```
src/
├── main/java/com/manaborn/
│   ├── ManabornMod.java              # Главный класс мода (ModInitializer)
│   ├── items/
│   │   ├── MagicStaffItem.java       # Магический посох
│   │   ├── SpellBookItem.java        # Книги заклинаний
│   │   ├── ManaPotionItem.java       # Зелья маны
│   │   ├── MagicDustItem.java        # Магическая пыль
│   │   └── ManaVesselItem.java       # Сосуды маны
│   ├── mana/
│   │   └── ManaManager.java          # Управление маной (серверная логика)
│   ├── spells/
│   │   ├── SpellManager.java         # Логика заклинаний
│   │   ├── SpellRegistry.java        # Реестр заклинаний
│   │   ├── SpellType.java            # Типы заклинаний
│   │   ├── LearnedSpellsManager.java # Управление изученными заклинаниями
│   │   └── impl/                     # Реализации заклинаний
│   │       ├── FireballSpell.java
│   │       ├── IceShardSpell.java
│   │       ├── FireWallSpell.java
│   │       ├── AbsorbingBarrierSpell.java
│   │       └── ... (остальные заклинания)
│   ├── blocks/
│   │   ├── LearningAltarBlock.java   # Алтарь изучения
│   │   ├── MagicOreBlock.java        # Магическая руда
│   │   └── ManaConverterBlock.java   # Конвертер маны
│   ├── blocks/entity/
│   │   ├── LearningAltarBlockEntity.java
│   │   └── ManaConverterBlockEntity.java
│   ├── entities/
│   │   ├── IceShardEntity.java       # Ледяной осколок
│   │   └── FireWallEntity.java       # Стена огня
│   └── network/
│       ├── packets/                  # CustomPayload пакеты
│       │   ├── ManaSyncS2CPacket.java
│       │   ├── LearnedSpellsSyncS2CPacket.java
│       │   └── SpellCastC2SPacket.java
│       └── NetworkConstants.java     # Константы для пакетов
│
├── client/java/com/manaborn/client/
│   ├── ManabornClient.java           # Клиентская инициализация
│   ├── ManaHudOverlay.java           # Отображение маны на HUD
│   ├── SpellHudOverlay.java          # Отображение заклинаний
│   ├── CooldownHudOverlay.java       # Отображение кулдаунов
│   ├── ui/                           # Пользовательские интерфейсы
│   │   ├── LearningAltarScreen.java  # Экран алтаря изучения
│   │   ├── SpellTreeScreen.java      # Дерево заклинаний
│   │   └── ManaConverterScreen.java  # Экран конвертера
│   └── state/
│       ├── ClientManaState.java      # Клиентское состояние маны
│       └── ClientLearnedSpellsState.java # Изученные заклинания
│
└── main/resources/
    ├── fabric.mod.json               # Метаданные мода
    ├── assets/manaborn/
    │   ├── lang/
    │   │   └── en_us.json            # Локализация
    │   ├── models/item/
    │   │   ├── magic_staff.json      # Модель посоха
    │   │   ├── spell_book.json       # Модель книги
    │   │   └── mana_potion.json      # Модель зелья
    │   ├── textures/
    │   │   ├── item/                 # Текстуры предметов
    │   │   └── block/                # Текстуры блоков
    │   └── items/                    # Описания предметов (1.21+)
    │       ├── magic_staff.json
    │       └── spell_book.json
    └── data/
        ├── manaborn/
        │   ├── loot_tables/          # Таблицы лута
        │   ├── recipes/              # Рецепты крафта
        │   └── worldgen/             # Генерация руды
```

---

## 🔧 Ключевые компоненты системы

### 1. ManabornMod (Серверная инициализация)

**Путь**: `src/main/java/com/manaborn/ManabornMod.java`

**Функции**:
- Регистрация предметов, блоков, сущностей
- Регистрация CustomPayload пакетов через PayloadTypeRegistry
- Настройка серверных событий (тики, подключение игроков)

**Ключевые моменты (1.21.10)**:
```java
public class ManabornMod implements ModInitializer {
    public static final String MOD_ID = "manaborn";
    
    // Предметы
    public static final RegistryKey<Item> MAGIC_STAFF_KEY = 
        RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "magic_staff"));
    public static Item MAGIC_STAFF;
    
    // Блоки
    public static final RegistryKey<Block> LEARNING_ALTAR_KEY = 
        RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "learning_altar"));
    public static Block LEARNING_ALTAR;
    
    @Override
    public void onInitialize() {
        // Регистрация контента
        registerItems();
        registerBlocks();
        registerEntities();
        
        // Регистрация сетевых пакетов
        registerNetworkPackets();
        
        // События
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ManaManager.onServerTick(server.getPlayerManager().getPlayerList());
        });
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            ManaManager.sendSync(player);
            LearnedSpellsManager.sendSync(player);
        });
    }
    
    private static void registerNetworkPackets() {
        // Регистрация S2C пакетов
        PayloadTypeRegistry.playS2C().register(
            ManaSyncS2CPacket.ID, 
            ManaSyncS2CPacket.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
            LearnedSpellsSyncS2CPacket.ID,
            LearnedSpellsSyncS2CPacket.CODEC
        );
        
        // Регистрация C2S пакетов
        PayloadTypeRegistry.playC2S().register(
            SpellCastC2SPacket.ID,
            SpellCastC2SPacket.CODEC
        );
        
        // Обработчики C2S пакетов
        ServerPlayNetworking.registerGlobalReceiver(
            SpellCastC2SPacket.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    SpellManager.castSpell(context.player(), payload.spellId());
                });
            }
        );
    }
}
```

### 2. ManaManager (Управление маной)

**Путь**: `src/main/java/com/manaborn/mana/ManaManager.java`

**Константы**:
- `MAX_MANA = 100` - максимальная мана
- `REGEN_UNIT_PER_5S = 20` - регенерация 20 маны за 5 секунд
- `REGEN_DELAY_TICKS = 40` - задержка 2 секунды после траты

**Хранилища данных** (Thread-safe):
- `playerIdToMana` - текущая мана игроков (`ConcurrentHashMap<UUID, Integer>`)
- `lastSpendTick` - время последней траты маны (`ConcurrentHashMap<UUID, Long>`)
- `regenTickCounter` - счетчик для регенерации (`ConcurrentHashMap<UUID, Integer>`)

**Основные методы**:
- `getMana(UUID)` - получить ману игрока
- `setMana(UUID, int)` - установить ману
- `tryConsume(ServerPlayerEntity, int)` - попытка потратить ману
- `onServerTick(Iterable<ServerPlayerEntity>)` - обработка регенерации
- `sendSync(ServerPlayerEntity)` - синхронизация с клиентом

### 3. Сетевая синхронизация (1.21.10 CustomPayload)

**Путь**: `src/main/java/com/manaborn/network/packets/ManaSyncS2CPacket.java`

```java
public record ManaSyncS2CPacket(
    int currentMana,
    int maxMana
) implements CustomPayload {
    
    public static final Id<ManaSyncS2CPacket> ID = 
        new Id<>(Identifier.of("manaborn", "mana_sync"));
    
    public static final PacketCodec<RegistryByteBuf, ManaSyncS2CPacket> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, ManaSyncS2CPacket::currentMana,
            PacketCodecs.VAR_INT, ManaSyncS2CPacket::maxMana,
            ManaSyncS2CPacket::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
```

**Отправка пакета (Server → Client)**:
```java
public static void sendSync(ServerPlayerEntity player) {
    UUID playerId = player.getUuid();
    int mana = getMana(playerId);
    int maxMana = MAX_MANA;
    
    ServerPlayNetworking.send(
        player, 
        new ManaSyncS2CPacket(mana, maxMana)
    );
}
```

**Импорты для networking (1.21.10)**:
```java
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
```

### 4. MagicStaffItem (Магический посох)

**Путь**: `src/main/java/com/manaborn/items/MagicStaffItem.java`

**Функциональность**:
- Стоимость использования заклинаний (разная для каждого)
- Переключение между изученными заклинаниями
- Кулдаун 5 секунд после использования
- Звуковые эффекты и визуализация

**Регистрация (1.21.10)**:
```java
public static void registerItems() {
    // Создаем RegistryKey
    RegistryKey<Item> staffKey = RegistryKey.of(
        RegistryKeys.ITEM,
        Identifier.of(MOD_ID, "magic_staff")
    );
    
    // Регистрируем предмет с ключом в Settings
    MAGIC_STAFF = Registry.register(
        Registries.ITEM,
        staffKey,
        new MagicStaffItem(new Item.Settings()
            .maxCount(1)
            .registryKey(staffKey))
    );
    
    // Добавляем в ItemGroup
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
        .register(entries -> entries.add(MAGIC_STAFF));
}
```

### 5. ManabornClient (Клиентская инициализация)

**Путь**: `src/client/java/com/manaborn/client/ManabornClient.java`

**Функции (1.21.10)**:
- Регистрация HUD через `HudElementRegistry` (НЕ `HudRenderCallback`!)
- Регистрация обработчиков CustomPayload пакетов
- Настройка клиентских тиков для плавной анимации

```java
public class ManabornClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Регистрация HUD элементов
        registerHudElements();
        
        // Регистрация обработчиков пакетов
        registerPacketHandlers();
        
        // Клиентские тики для анимаций
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                ClientManaState.clientTick();
                ClientCooldownState.clientTick();
            }
        });
    }
    
    private void registerHudElements() {
        // HUD для маны
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.HOTBAR,
            Identifier.of("manaborn", "mana_hud"),
            (context, tickCounter) -> {
                float tickDelta = tickCounter.getTickDelta(false);
                ManaHudOverlay.render(context, tickDelta);
            }
        );
        
        // HUD для заклинаний
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.HOTBAR,
            Identifier.of("manaborn", "spell_hud"),
            (context, tickCounter) -> {
                float tickDelta = tickCounter.getTickDelta(false);
                SpellHudOverlay.render(context, tickDelta);
            }
        );
    }
    
    private void registerPacketHandlers() {
        // Обработчик синхронизации маны
        ClientPlayNetworking.registerGlobalReceiver(
            ManaSyncS2CPacket.ID,
            (payload, context) -> {
                context.client().execute(() -> {
                    ClientManaState.set(payload.currentMana(), payload.maxMana());
                });
            }
        );
        
        // Обработчик синхронизации изученных заклинаний
        ClientPlayNetworking.registerGlobalReceiver(
            LearnedSpellsSyncS2CPacket.ID,
            (payload, context) -> {
                context.client().execute(() -> {
                    ClientLearnedSpellsState.set(payload.learnedSpells());
                });
            }
        );
    }
}
```

**Импорты для HUD (1.21.10)**:
```java
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
```

### 6. ManaHudOverlay (Отображение HUD)

**Путь**: `src/client/java/com/manaborn/client/ManaHudOverlay.java`

**Сигнатура метода (1.21.10)**:
```java
public static void render(DrawContext context, float tickDelta) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.player == null || client.options.hudHidden) {
        return;
    }
    
    // tickDelta - прогресс между тиками (0.0 - 1.0)
    // Получаем данные из клиентского состояния
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

---

## 🔄 Поток данных (1.21.10)

### Инициализация при входе игрока:
```
1. ServerPlayConnectionEvents.JOIN
2. ManaManager.ensureInitialized(player)
3. LearnedSpellsManager.ensureInitialized(player)
4. ManaManager.sendSync(player)
   ├─ Создание ManaSyncS2CPacket
   └─ ServerPlayNetworking.send(player, packet)
5. LearnedSpellsManager.sendSync(player)
   ├─ Создание LearnedSpellsSyncS2CPacket
   └─ ServerPlayNetworking.send(player, packet)
6. ClientPlayNetworking обработчики
   ├─ ClientManaState.set(mana, maxMana)
   └─ ClientLearnedSpellsState.set(learnedSpells)
7. HUD обновляется через HudElementRegistry
```

### Использование заклинания:
```
1. Player использует Magic Staff (ПКМ)
2. MagicStaffItem.use() (на сервере)
3. SpellManager.castSpell(player, spellId)
4. Проверка изученных заклинаний
5. ManaManager.tryConsume(player, cost)
6. Если успешно:
   - Обновление серверных данных
   - ManaManager.sendSync(player)
   - CustomPayload → Client
   - ClientManaState.set(newMana, maxMana)
   - Обновление HUD
   - Применение эффектов заклинания
```

---

## 🎯 Ключевые особенности реализации (1.21.10)

### 1. Разделение клиент/сервер
- Сервер - источник истины для маны и заклинаний
- Клиент получает обновления через CustomPayload пакеты
- Клиентская анимация только для визуала

### 2. Использование правильного API для 1.21.10
- ✅ `CustomPayload` + `PacketCodec` для сетевого взаимодействия
- ✅ `HudElementRegistry` для HUD рендеринга
- ✅ `Identifier.of()` для создания идентификаторов
- ✅ `Item.Settings()` с обязательным `.registryKey()`
- ✅ `RenderTickCounter` вместо `float tickDelta`
- ✅ `PayloadTypeRegistry` для регистрации пакетов

### 3. Потокобезопасность
- Использование `ConcurrentHashMap` для хранения данных игроков
- Выполнение клиентской логики в главном потоке через `context.client().execute()`
- Выполнение серверной логики в главном потоке через `context.server().execute()`

### 4. Оптимизация
- Регенерация только для игроков с неполной маной
- Отправка пакетов только при изменении значений
- Проверка `hudHidden` перед рендерингом
- Использование `VarInt` для компактности данных в PacketCodec

---

## 📝 Конфигурация (1.21.10)

### fabric.mod.json
```json
{
  "schemaVersion": 1,
  "id": "manaborn",
  "version": "1.0.0",
  "name": "Manaborn",
  "description": "A magic mod with spells, mana, and mystic power.",
  "authors": ["Zottaks"],
  "license": "CC0-1.0",
  "icon": "assets/manaborn/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": ["com.manaborn.ManabornMod"],
    "client": ["com.manaborn.client.ManabornClient"]
  },
  "mixins": [],
  "depends": {
    "fabricloader": ">=0.17.2",
    "minecraft": "~1.21.10",
    "java": ">=21",
    "fabric-api": "*"
  }
}
```

### build.gradle (фрагмент для 1.21.10)
```groovy
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
    it.options.compilerArgs << "-Xlint:deprecation"
}
```

### gradle.properties
```properties
minecraft_version=1.21.10
yarn_mappings=1.21.10+build.1
loader_version=0.17.2
fabric_version=0.134.1+1.21.10
java_version=21
```

---

## 🐛 Известные проблемы и решения (1.21.10)

### Проблема: Попытка использовать PacketByteBuf
**Симптом**: `IllegalArgumentException: Cannot use PacketByteBuf with CustomPayload system`
**Решение**: Использовать `CustomPayload` + `PacketCodec` - `PacketByteBuf` устарел в 1.21.10

### Проблема: HudRenderCallback не работает
**Симптом**: HUD не отображается или работает некорректно
**Решение**: Использовать `HudElementRegistry` - `HudRenderCallback` deprecated в 1.21.10

### Проблема: new Identifier() не найден
**Симптом**: Compile error - метод не существует
**Решение**: Использовать `Identifier.of()` - `new Identifier()` устарел в 1.21.10

### Проблема: Item.Settings требует registryKey
**Симптом**: `IllegalArgumentException: Item.Settings missing registry key`
**Решение**: Создать `RegistryKey` и добавить `.registryKey(key)` в `Item.Settings()`

### Проблема: Десинхронизация маны
**Решение**: Всегда отправлять CustomPayload пакет синхронизации после изменения маны на сервере

### Проблема: Крах при обработке CustomPayload
**Симптом**: `DecoderException` или `IndexOutOfBoundsException`
**Решение**: Проверить, что порядок полей в record соответствует порядку в PacketCodec.tuple()

---

## 🔄 Миграция с 1.20.1 на 1.21.10

### Networking изменения
```java
// Старый код (1.20.1)
PacketByteBuf buf = PacketByteBufs.create();
buf.writeVarInt(mana);
ServerPlayNetworking.send(player, PACKET_ID, buf);

// Новый код (1.21.10)
public record ManaSyncS2CPacket(int mana) implements CustomPayload {
    public static final Id<ManaSyncS2CPacket> ID = 
        new Id<>(Identifier.of("manaborn", "mana_sync"));
    public static final PacketCodec<RegistryByteBuf, ManaSyncS2CPacket> CODEC = 
        PacketCodec.tuple(PacketCodecs.VAR_INT, ManaSyncS2CPacket::mana, ManaSyncS2CPacket::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
PayloadTypeRegistry.playS2C().register(ManaSyncS2CPacket.ID, ManaSyncS2CPacket.CODEC);
ServerPlayNetworking.send(player, new ManaSyncS2CPacket(mana));
```

### HUD изменения
```java
// Старый код (1.20.1)
HudRenderCallback.EVENT.register((context, tickDelta) -> {});

// Новый код (1.21.10)
HudElementRegistry.attachElementBefore(
    VanillaHudElements.HOTBAR,
    Identifier.of("manaborn", "hud"),
    (context, tickCounter) -> {
        float tickDelta = tickCounter.getTickDelta(false);
    }
);
```

### Identifier изменения
```java
// Старый код (1.20.1)
new Identifier("modid", "name")

// Новый код (1.21.10)
Identifier.of("modid", "name")
```

### Item Settings изменения
```java
// Старый код (1.20.1)
new Item.Settings().maxCount(1)

// Новый код (1.21.10)
RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
new Item.Settings().maxCount(1).registryKey(key)
```

---

## 📚 Полезные ресурсы для 1.21.10

- [Fabric Documentation - Networking](https://docs.fabricmc.net/develop/networking/networking)
- [Fabric Wiki 1.21](https://fabricmc.net/2024/05/31/121.html)
- [Fabric API Javadoc 0.134.1](https://maven.fabricmc.net/docs/fabric-api-0.134.1+1.21.10/)
- [Minecraft Wiki 1.21](https://minecraft.wiki/wiki/Java_Edition_1.21.10)

---

**Версия проекта**: 1.0.0 для Minecraft 1.21.10
**Последнее обновление**: Ноябрь 2024
**Совместимость**: Minecraft 1.21.10 + Fabric API 0.134.1+1.21.10 + Java 21
