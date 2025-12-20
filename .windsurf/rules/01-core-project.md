---
trigger: always_on
---

# Manaborn Core Project Info

## Stack
- **Minecraft**: 1.21.10
- **Fabric API**: 0.134.1+1.21.10
- **Fabric Loader**: 0.17.2
- **Java**: 21
- **Mod ID**: `manaborn`

## Project Structure
```
src/main/java/com/manaborn/     # Server-side code
src/client/java/com/manaborn/   # Client-side code (NEVER mix!)
src/main/resources/assets/      # Textures, models, lang
src/main/resources/data/        # Recipes, loot tables, worldgen
```

## Key Classes
- `ManabornMod.java` - Main mod initializer
- `ManabornClient.java` - Client initializer
- `ManaManager.java` - Mana system (server)
- `SpellManager.java` - Spell casting logic
- `SpellType.java` - Spell enum definitions

## Critical Rules
1. **Client/Server separation**: Never import client classes in server code
2. **Use 1.21.10 API**: `Identifier.of()`, `CustomPayload`, `HudElementRegistry`
3. **Thread safety**: Use `ConcurrentHashMap` for player data
4. **Network**: Always use `context.client().execute()` or `context.server().execute()`
