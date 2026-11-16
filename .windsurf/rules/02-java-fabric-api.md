---
trigger: glob
globs: **/*.java
---

# Fabric 1.21.10 Java API Rules

## Identifier Creation
```java
// ✅ CORRECT
Identifier.of("manaborn", "item_name")

// ❌ WRONG (1.20.1 syntax)
new Identifier("manaborn", "item_name")
```

## Item Registration
```java
// 1. Create RegistryKey
RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("manaborn", "item"));

// 2. Register with key in Settings
Item item = Registry.register(Registries.ITEM, key,
    new CustomItem(new Item.Settings().maxCount(1).registryKey(key)));

// 3. Add to ItemGroup
ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
    .register(entries -> entries.add(item));
```

## Required Imports
```java
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
```

## Block Registration
```java
RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of("manaborn", "block"));
Block block = Registry.register(Registries.BLOCK, blockKey,
    new CustomBlock(AbstractBlock.Settings.create().registryKey(blockKey)));

// Block item
RegistryKey<Item> blockItemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("manaborn", "block"));
Registry.register(Registries.ITEM, blockItemKey,
    new BlockItem(block, new Item.Settings().registryKey(blockItemKey)));
```
