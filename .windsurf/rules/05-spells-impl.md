---
trigger: glob
globs: src/main/java/com/manaborn/spells/**/*.java
---

# Spell System Rules

## SpellType Enum Structure
```java
public enum SpellType {
    FIREBALL("Fireball", 20, "fireball", Formatting.RED),
    ICE_SHARD("Ice Shard", 35, "ice_shard", Formatting.AQUA);
    
    private final String displayName;
    private final int manaCost;
    private final String id;
    private final Formatting color;
    
    // Constructor and getters...
}
```

## Spell Implementation Pattern
Location: `src/main/java/com/manaborn/spells/impl/`

```java
public class MySpell {
    public static void cast(World world, PlayerEntity player) {
        if (world.isClient()) return; // Server-side only!
        
        // 1. Create effect/entity
        // 2. Play sound
        world.playSound(null, player.getBlockPos(), 
            SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        
        // 3. Apply effects
        // 4. Spawn particles (via packet to clients)
    }
}
```

## SpellManager Pattern
```java
public static void castSpell(ServerPlayerEntity player, SpellType spell) {
    int cost = spell.getManaCost();
    
    if (!ManaManager.tryConsume(player, cost)) {
        player.sendMessage(Text.literal("Not enough mana!").formatted(Formatting.RED), true);
        return;
    }
    
    World world = player.getWorld();
    
    switch (spell) {
        case FIREBALL -> FireballSpell.cast(world, player);
        case ICE_SHARD -> IceShardSpell.cast(world, player);
        // etc...
    }
    
    // Sync mana to client
    ManaManager.sendSync(player);
}
```

## Cooldown System
```java
// Use item cooldown for spells
player.getItemCooldownManager().set(staffItem, cooldownTicks);

// Check cooldown
if (player.getItemCooldownManager().isCoolingDown(staffItem)) {
    return ActionResult.FAIL;
}
```
