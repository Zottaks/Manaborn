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
                    ClientLear