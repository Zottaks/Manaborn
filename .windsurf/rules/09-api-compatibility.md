---
trigger: model_decision
description: Apply when discussing API compatibility, version migration, or when code uses deprecated/old APIs
---

# API Compatibility Check (1.21.10)

## Quick Reference Table

| Component | ❌ OLD (1.20.1) | ✅ NEW (1.21.10) |
|-----------|-----------------|------------------|
| Identifier | `new Identifier()` | `Identifier.of()` |
| Networking | `PacketByteBuf` | `CustomPayload` + `PacketCodec` |
| HUD | `HudRenderCallback` | `HudElementRegistry` |
| Tick param | `float tickDelta` | `RenderTickCounter` |
| Item.Settings | No registryKey | `.registryKey(key)` required |
| Recipe folder | `recipes/` | `recipe/` |
| Recipe result | `"item": "..."` | `"id": "..."` |
| Java | 17 | 21 |

## Deprecated APIs - DO NOT USE

### Networking
```java
// ❌ DEPRECATED
PacketByteBuf buf = PacketByteBufs.create();
buf.writeVarInt(value);
ServerPlayNetworking.send(player, PACKET_ID, buf);

// ✅ USE INSTEAD
ServerPlayNetworking.send(player, new MyPayload(value));
```

### HUD Rendering
```java
// ❌ DEPRECATED  
HudRenderCallback.EVENT.register((context, tickDelta) -> {});

// ✅ USE INSTEAD
HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, id, 
    (context, tickCounter) -> {
        float tickDelta = tickCounter.getTickDelta(false);
    });
```

### Item Registration
```java
// ❌ DEPRECATED
Registry.register(Registries.ITEM, new Identifier("mod", "item"),
    new Item(new Item.Settings()));

// ✅ USE INSTEAD
RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("mod", "item"));
Registry.register(Registries.ITEM, key,
    new Item(new Item.Settings().registryKey(key)));
```

## When to Apply This Rule
- Migrating code from older versions
- Reviewing existing code for compatibility
- When user asks about deprecated warnings
- When compilation fails due to API changes
