---
trigger: always_on
---

# Правила разработки Manaborn для Minecraft 1.21.10

## 🎯 КРИТИЧЕСКИ ВАЖНО

**Целевая версия:** Minecraft **1.21.10** + Fabric API **0.134.1+1.21.10** + Java **21**

Эти правила ЗАМЕНЯЮТ устаревшие rule_1, rule_2, rule_3 (которые для 1.20.1).

---

## 📋 Версии проекта

```properties
minecraft_version=1.21.10
yarn_mappings=1.21.10+build.1
loader_version=0.17.2
fabric_version=0.134.1+1.21.10
java_version=21
```

**Источник:** `/build.gradle` и `/gradle.properties`

---

## 🔧 Правильные API для 1.21.10

### 1. Networking (НОВЫЙ API с 1.20.5+)

#### ❌ НЕПРАВИЛЬНО (1.20.1 API - устарел):
```java
PacketByteBuf buf = PacketByteBufs.create();
buf.writeVarInt(mana);
ServerPlayNetworking.send(player, PACKET_ID, buf);
```

#### ✅ ПРАВИЛЬНО (1.21.10 API):
```java
// 1. Создаем record с данными
public record ManaSyncS2CPayload(int mana, int maxMana) implements CustomPayload {
    public static final CustomPayload.Id<ManaSyncS2CPayload> ID = 
        new CustomPayload.Id<>(Identifier.of("manaborn", "mana_sync"));
    
    public static final PacketCodec<RegistryByteBuf, ManaSyncS2CPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, ManaSyncS2CPayload::mana,
            PacketCodecs.VAR_INT, ManaSyncS2CPayload::maxMana,
            ManaSyncS2CPayload::new
        );
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}

// 2. Регистрация пакета (в ModInitializer)
PayloadTypeRegistry.playS2C().register(ManaSyncS2CPayload.ID, ManaSyncS2CPayload.CODEC);

// 3. Отправка (Server -> Client)
ServerPlayNetworking.send(player, new ManaSyncS2CPayload(mana, maxMana));

// 4. Получение (в ClientModInitializer)
ClientPlayNetworking.registerGlobalReceiver(
    ManaSyncS2CPayload.ID,
    (payload, context) -> {
        context.client().execute(() -> {
            ClientManaState.set(payload.mana(), payload.maxMana());
        });
    }
);
```

**Импорты для networking (1.21.10):**
```java
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
```

### 2. HUD Rendering (НОВЫЙ API с 1.21+)

#### ❌ НЕПРАВИЛЬНО (1.20.1 API - deprecated):
```java
HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
    // ...
});
```

#### ✅ ПРАВИЛЬНО (1.21.10 API):
```java
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

// В ClientModInitializer
@Override
public void onInitializeClient() {
    HudElementRegistry.attachElementBefore(
        VanillaHudElements.HOTBAR,
        Identifier.of("manaborn", "mana_hud"),
        (context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.options.hudHidden) {
                return;
            }
            
            // tickCounter.getTickDelta(false) - прогресс между тиками
            float tickDelta = tickCounter.getTickDelta(false);
            
            // Рисуем HUD
            ManaHudOverlay.render(context, tickDelta);
        }
    );
}
```

**Ключевые изменения:**
- `HudRenderCallback` → `HudElementRegistry`
- `float tickDelta` → `RenderTickCounter tickCounter`
- `tickDelta` получаем через `tickCounter.getTickDelta(false)`

**Импорты для HUD (1.21.10):**
```java
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
```

### 3. Identifier Creation

#### ❌ НЕПРАВИЛЬНО (1.20.1 синтаксис):
```java
new Identifier("manaborn", "magic_staff")
```

#### ✅ ПРАВИЛЬНО (1.21+ синтаксис):
```java
Identifier.of("manaborn", "magic_staff")
```

**Импорт:**
```java
import net.minecraft.util.Identifier;
```

### 4. Item Registration

#### ❌ НЕПРАВИЛЬНО (1.20.1):
```java
Item item = Registry.register(
    Registries.ITEM,
    new Identifier("manaborn", "item"),
    new CustomItem(new Item.Settings().maxCount(1))
);
```

#### ✅ ПРАВИЛЬНО (1.21.10):
```java
// 1. Создаем RegistryKey
RegistryKey<Item> itemKey = RegistryKey.of(
    RegistryKeys.ITEM,
    Identifier.of("manaborn", "magic_staff")
);

// 2. Регистрируем предмет с ключом в Settings
Item item = Registry.register(
    Registries.ITEM,
    itemKey,
    new MagicStaffItem(new Item.Settings()
        .maxCount(1)
        .registryKey(itemKey))
);

// 3. Добавляем в ItemGroup
ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
    .register(entries -> entries.add(item));
```

**Ключевые изменения:**
- Обязательно создаем `RegistryKey`
- В `Item.Settings()` добавляем `.registryKey(itemKey)`
- `Identifier.of()` вместо `new Identifier()`

**Импорты для регистрации (1.21.10):**
```java
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
```

### 5. DrawContext (без изменений)

DrawContext остался таким же в 1.21.10:

```java
context.fill(x1, y1, x2, y2, color);
context.drawBorder(x, y, width, height, color);
context.drawText(textRenderer, text, x, y, color, shadow);
context.drawTexture(...);
context.drawItem(stack, x, y);
```

---

## 🚨 Критические отличия от 1.20.1

| Компонент | 1.20.1 | 1.21.10 |
|-----------|--------|---------|
| **Networking** | `PacketByteBuf` | `CustomPayload` + `PacketCodec` |
| **HUD** | `HudRenderCallback` | `HudElementRegistry` |
| **Tick parameter** | `float tickDelta` | `RenderTickCounter` |
| **Identifier** | `new Identifier()` | `Identifier.of()` |
| **Item.Settings** | Без `registryKey` | С `.registryKey(key)` |
| **Java** | 17 | 21 |
| **Packet registration** | `registerGlobalReceiver` | `PayloadTypeRegistry` + receiver |

---

## 📦 Структура сетевых пакетов (1.21.10)

### Шаблон для S2C (Server → Client) пакета

```java
package com.manaborn.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ManaSyncS2CPayload(
    int currentMana,
    int maxMana
) implements CustomPayload {
    
    // Уникальный ID пакета
    public static final Id<ManaSyncS2CPayload> ID = 
        new Id<>(Identifier.of("manaborn", "mana_sync"));
    
    // Codec для сериализации/десериализации
    public static final PacketCodec<RegistryByteBuf, ManaSyncS2CPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, ManaSyncS2CPayload::currentMana,
            PacketCodecs.VAR_INT, ManaSyncS2CPayload::maxMana,
            ManaSyncS2CPayload::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
```

### Регистрация в ModInitializer (сервер)

```java
@Override
public void onInitialize() {
    // Регистрируем тип пакета
    PayloadTypeRegistry.playS2C().register(
        ManaSyncS2CPayload.ID,
        ManaSyncS2CPayload.CODEC
    );
}
```

### Отправка пакета (сервер)

```java
public static void syncMana(ServerPlayerEntity player) {
    int mana = ManaManager.getMana(player.getUuid());
    int maxMana = ManaManager.MAX_MANA;
    
    ServerPlayNetworking.send(
        player,
        new ManaSyncS2CPayload(mana, maxMana)
    );
}
```

### Получение в ClientModInitializer

```java
@Override
public void onInitializeClient() {
    ClientPlayNetworking.registerGlobalReceiver(
        ManaSyncS2CPayload.ID,
        (payload, context) -> {
            // Обработка в главном потоке клиента
            context.client().execute(() -> {
                ClientManaState.set(payload.currentMana(), payload.maxMana());
            });
        }
    );
}
```

### Шаблон для C2S (Client → Server) пакета

```java
public record SpellCastC2SPayload(
    String spellId
) implements CustomPayload {
    
    public static final Id<SpellCastC2SPayload> ID = 
        new Id<>(Identifier.of("manaborn", "spell_cast"));
    
    public static final PacketCodec<RegistryByteBuf, SpellCastC2SPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.STRING, SpellCastC2SPayload::spellId,
            SpellCastC2SPayload::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

// Регистрация (в ModInitializer)
PayloadTypeRegistry.playC2S().register(SpellCastC2SPayload.ID, SpellCastC2SPayload.CODEC);

// Получение на сервере
ServerPlayNetworking.registerGlobalReceiver(
    SpellCastC2SPayload.ID,
    (payload, context) -> {
        context.server().execute(() -> {
            ServerPlayerEntity player = context.player();
            SpellManager.castSpell(player, payload.spellId());
        });
    }
);

// Отправка с клиента
ClientPlayNetworking.send(new SpellCastC2SPayload("fireball"));
```

---

## 🎨 PacketCodecs (доступные типы)

```java
// Примитивные типы
PacketCodecs.BYTE
PacketCodecs.SHORT
PacketCodecs.INTEGER
PacketCodecs.VAR_INT        // Эффективнее для небольших чисел
PacketCodecs.LONG
PacketCodecs.VAR_LONG
PacketCodecs.FLOAT
PacketCodecs.DOUBLE
PacketCodecs.BOOLEAN
PacketCodecs.STRING

// Minecraft типы
PacketCodecs.BLOCK_POS
PacketCodecs.IDENTIFIER
// ItemStack - требует кастомного codec
// UUID - требует кастомного codec
```

### Пример кастомного codec для сложных типов

```java
// Для списка строк
public static final PacketCodec<RegistryByteBuf, List<String>> STRING_LIST_CODEC = 
    PacketCodecs.STRING.collect(PacketCodecs.toList());

// Для Set<SpellType> (enum)
public static final PacketCodec<RegistryByteBuf, Set<SpellType>> SPELL_SET_CODEC =
    PacketCodec.of(
        (value, buf) -> {
            buf.writeVarInt(value.size());
            for (SpellType spell : value) {
                buf.writeString(spell.name());
            }
        },
        buf -> {
            int size = buf.readVarInt();
            Set<SpellType> spells = new HashSet<>();
            for (int i = 0; i < size; i++) {
                spells.add(SpellType.valueOf(buf.readString()));
            }
            return spells;
        }
    );
```

---

## 🔄 Миграция кода с 1.20.1 на 1.21.10

### 1. Замена Networking

**Старый код (1.20.1):**
```java
PacketByteBuf buf = PacketByteBufs.create();
buf.writeVarInt(mana);
buf.writeVarInt(maxMana);
ServerPlayNetworking.send(player, PACKET_ID, buf);

ClientPlayNetworking.registerGlobalReceiver(PACKET_ID, (client, handler, buf, sender) -> {
    int mana = buf.readVarInt();
    int maxMana = buf.readVarInt();
    client.execute(() -> ClientManaState.set(mana, maxMana));
});
```

**Новый код (1.21.10):**
```java
// Создаем payload record
public record ManaSyncS2CPayload(int mana, int maxMana) implements CustomPayload {
    public static final Id<ManaSyncS2CPayload> ID = 
        new Id<>(Identifier.of("manaborn", "mana_sync"));
    public static final PacketCodec<RegistryByteBuf, ManaSyncS2CPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, ManaSyncS2CPayload::mana,
            PacketCodecs.VAR_INT, ManaSyncS2CPayload::maxMana,
            ManaSyncS2CPayload::new
        );
    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}

// Регистрация
PayloadTypeRegistry.playS2C().register(ManaSyncS2CPayload.ID