---
trigger: glob
globs: src/main/resources/data/manaborn/recipe/**/*.json
---

# Recipe Format (Minecraft 1.21+)

## CRITICAL: Folder Name
- **1.21+**: `data/<namespace>/recipe/` (singular!)
- **Before 1.21**: `data/<namespace>/recipes/` (plural)

## Shaped Recipe Template
```json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    "  A",
    " S ",
    "S  "
  ],
  "key": {
    "S": "minecraft:stick",
    "A": "minecraft:amethyst_shard"
  },
  "result": {
    "id": "manaborn:magic_staff",
    "count": 1
  }
}
```

## Key Differences from 1.20.1
| Field | 1.20.1 | 1.21+ |
|-------|--------|-------|
| Folder | `recipes/` | `recipe/` |
| result item | `"item": "..."` | `"id": "..."` |
| key values | `{"item": "..."}` | `"minecraft:item"` (direct string) |

## Shapeless Recipe Template
```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [
    "minecraft:glass_bottle",
    "manaborn:magic_dust",
    "manaborn:magic_dust"
  ],
  "result": {
    "id": "manaborn:mana_potion",
    "count": 1
  }
}
```

## Smelting Recipe Template
```json
{
  "type": "minecraft:smelting",
  "ingredient": "manaborn:magic_ore",
  "result": {
    "id": "manaborn:magic_dust",
    "count": 1
  },
  "experience": 0.7,
  "cookingtime": 200
}
```

## Categories
- `equipment` - tools, weapons, armor
- `building` - blocks
- `misc` - everything else
- `redstone` - redstone components
