---
trigger: glob
globs: src/main/resources/assets/manaborn/items/**/*.json
---

# Item Asset Descriptors (1.21+)

## Location
`assets/manaborn/items/<item_id>.json`

## Basic Item Template
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "manaborn:item/<item_id>"
  }
}
```

## Item with Tint (e.g., potions)
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "manaborn:item/mana_potion",
    "tints": [
      {
        "type": "minecraft:constant",
        "value": 5592575
      }
    ]
  }
}
```

## Related Files Structure
```
assets/manaborn/
├── items/
│   └── magic_staff.json      # Item descriptor (NEW in 1.21)
├── models/item/
│   └── magic_staff.json      # 3D model definition
├── textures/item/
│   └── magic_staff.png       # Texture file
└── lang/
    └── en_us.json            # Translations
```

## Model File (models/item/)
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "manaborn:item/magic_staff"
  }
}
```

## Lang Entry
```json
{
  "item.manaborn.magic_staff": "Magic Staff"
}
```
