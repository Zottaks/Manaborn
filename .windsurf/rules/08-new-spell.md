---
trigger: manual
description: Step-by-step guide for creating a new spell in Manaborn
---

# Creating a New Spell (Manual Guide)

## Checklist
Follow these steps in order:

### Step 1: Add to SpellType Enum
File: `src/main/java/com/manaborn/spells/SpellType.java`

```java
public enum SpellType {
    // ... existing spells ...
    NEW_SPELL("Display Name", 30, "new_spell", Formatting.GOLD);
    
    // Parameters: displayName, manaCost, id, color
}
```

### Step 2: Create Spell Implementation
File: `src/main/java/com/manaborn/spells/impl/NewSpellSpell.java`

```java
package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

public class NewSpellSpell {
    public static void cast(World world, PlayerEntity player) {
        if (world.isClient()) return;
        
        // Your spell logic here
        
        // Play sound
        world.playSound(null, player.getBlockPos(),
            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
            SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
}
```

### Step 3: Register in SpellManager
File: `src/main/java/com/manaborn/spells/SpellManager.java`

Add case to `castSpell()` method:
```java
case NEW_SPELL -> NewSpellSpell.cast(world, player);
```

### Step 4: Create Spell Book Item
File: `src/main/java/com/manaborn/items/SpellBookItem.java`

Register new book variant or ensure generic SpellBookItem handles the new type.

### Step 5: Add Translations
File: `src/main/resources/assets/manaborn/lang/en_us.json`

```json
{
  "spell.manaborn.new_spell": "New Spell",
  "item.manaborn.spell_book_new_spell": "Spell Book: New Spell"
}
```

### Step 6: (Optional) Create Spell Book Texture
File: `src/main/resources/assets/manaborn/textures/item/spell_book_new_spell.png`

### Step 7: Test
1. Run `./gradlew runClient`
2. Use `/give @s manaborn:spell_book_new_spell`
3. Learn at altar
4. Test with magic staff

## Common Spell Patterns

### Projectile Spell
```java
// Spawn projectile entity
MyProjectileEntity projectile = new MyProjectileEntity(world, player);
projectile.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, 1.5F, 0F);
world.spawnEntity(projectile);
```

### Area Effect
```java
Box area = player.getBoundingBox().expand(5.0);
List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, area,
    e -> e != player && e.isAlive());
for (LivingEntity target : targets) {
    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
}
```

### Self Buff
```java
player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
```

### Block Placement
```java
BlockPos targetPos = player.getBlockPos().offset(player.getHorizontalFacing(), 2);
world.setBlockState(targetPos, Blocks.ICE.getDefaultState());
```
