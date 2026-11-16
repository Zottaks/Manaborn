---
trigger: glob
globs: src/main/java/com/manaborn/passives/**/*.java
---

# Passives System Architecture

## Overview

Passives are permanent abilities that provide continuous effects. Players can learn passives and equip up to **2 active slots**.

## PassiveType Enum

**Location:** `src/main/java/com/manaborn/passives/PassiveType.java`

```java
public enum PassiveType {
    // Dark Magic
    SHADOW_SHELTER("Тень укрытия", "shadow_shelter", Formatting.DARK_PURPLE),
    
    // Fire Magic
    COAL_HEAT("Жар углей", "coal_heat", Formatting.GOLD),
    
    // Ice Magic
    FROST_SKIN("Иней на коже", "frost_skin", Formatting.AQUA),
    REACTIVE_FROST("Лёд реакции", "reactive_frost", Formatting.AQUA);
    
    private final String displayName;
    private final String id;
    private final Formatting color;
    
    // Constructor, getters...
    
    public static PassiveType fromId(String id) {
        for (PassiveType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;  // Note: returns null, not default!
    }
}
```

## PassiveManager

**Location:** `src/main/java/com/manaborn/passives/PassiveManager.java`

### Data Storage
```java
// Player UUID -> Set of learned passives
private static final Map<UUID, Set<PassiveType>> learnedPassives = new ConcurrentHashMap<>();

// Player UUID -> Active slot 0
private static final Map<UUID, PassiveType> activeSlot0 = new ConcurrentHashMap<>();

// Player UUID -> Active slot 1
private static final Map<UUID, PassiveType> activeSlot1 = new ConcurrentHashMap<>();
```

### Core Methods
```java
public static void loadOrInit(ServerPlayerEntity player) {
    UUID id = player.getUuid();
    learnedPassives.putIfAbsent(id, new HashSet<>());
}

public static boolean hasLearned(UUID playerId, PassiveType passive) {
    return learnedPassives.getOrDefault(playerId, Collections.emptySet()).contains(passive);
}

public static void learnPassive(ServerPlayerEntity player, PassiveType passive) {
    UUID id = player.getUuid();
    learnedPassives.computeIfAbsent(id, k -> new HashSet<>()).add(passive);
    sendSync(player);
}

public static Set<PassiveType> getActivePassives(UUID playerId) {
    Set<PassiveType> active = new HashSet<>();
    PassiveType s0 = activeSlot0.get(playerId);
    PassiveType s1 = activeSlot1.get(playerId);
    if (s0 != null) active.add(s0);
    if (s1 != null) active.add(s1);
    return active;
}

public static void setActiveSlots(ServerPlayerEntity player, PassiveType slot0, PassiveType slot1) {
    UUID id = player.getUuid();
    
    // Validate - must be learned
    if (slot0 != null && !hasLearned(id, slot0)) slot0 = null;
    if (slot1 != null && !hasLearned(id, slot1)) slot1 = null;
    
    // Can't have same passive in both slots
    if (slot0 != null && slot0 == slot1) slot1 = null;
    
    if (slot0 != null) activeSlot0.put(id, slot0);
    else activeSlot0.remove(id);
    
    if (slot1 != null) activeSlot1.put(id, slot1);
    else activeSlot1.remove(id);
    
    sendSync(player);
}
```

## Network Synchronization

### PassiveSyncS2CPacket
```java
public record PassiveSyncS2CPacket(
    Set<String> learnedIds,
    String slot0,
    String slot1
) implements CustomPayload {
    
    public static final CustomPayload.Id<PassiveSyncS2CPacket> ID = 
        new CustomPayload.Id<>(Identifier.of("manaborn", "passive_sync"));
    
    public static final PacketCodec<RegistryByteBuf, PassiveSyncS2CPacket> CODEC = 
        PacketCodec.tuple(
            STRING_SET_CODEC, PassiveSyncS2CPacket::learnedIds,
            PacketCodecs.STRING, PassiveSyncS2CPacket::slot0,
            PacketCodecs.STRING, PassiveSyncS2CPacket::slot1,
            PassiveSyncS2CPacket::new
        );
}
```

### PassiveSlotsUpdateC2SPacket
```java
public record PassiveSlotsUpdateC2SPacket(
    String slot0,
    String slot1
) implements CustomPayload {
    // ... ID, CODEC
}

// Handler in ManabornMod
ServerPlayNetworking.registerGlobalReceiver(PassiveSlotsUpdateC2SPacket.ID, (payload, context) -> {
    context.server().execute(() -> {
        ServerPlayerEntity player = context.player();
        if (player != null) {
            PassiveType s0 = PassiveType.fromId(payload.slot0());
            PassiveType s1 = PassiveType.fromId(payload.slot1());
            PassiveManager.setActiveSlots(player, s0, s1);
        }
    });
});
```

## PassiveEffectsManager

**Location:** `src/main/java/com/manaborn/passives/PassiveEffectsManager.java`

Applies effects of active passives during gameplay.

```java
public class PassiveEffectsManager {
    
    // Called from ServerTickEvents or damage events
    public static void applyPassiveEffects(ServerPlayerEntity player) {
        Set<PassiveType> active = PassiveManager.getActivePassives(player.getUuid());
        
        for (PassiveType passive : active) {
            switch (passive) {
                case SHADOW_SHELTER -> applyShadowShelter(player);
                case COAL_HEAT -> applyCoalHeat(player);
                case FROST_SKIN -> applyFrostSkin(player);
                case REACTIVE_FROST -> applyReactiveFrost(player);
            }
        }
    }
    
    private static void applyShadowShelter(ServerPlayerEntity player) {
        // Reduce damage taken in darkness
        // Or: invisibility when sneaking in dark
    }
    
    private static void applyCoalHeat(ServerPlayerEntity player) {
        // Fire resistance when near fire
        // Or: bonus damage with fire spells
    }
    
    private static void applyFrostSkin(ServerPlayerEntity player) {
        // Slow attackers on hit
        // Or: reduced ice damage taken
    }
    
    private static void applyReactiveFrost(ServerPlayerEntity player) {
        // Freeze water around player
        // Or: auto-apply slowness to nearby enemies
    }
}
```

## Client State

**Location:** `src/client/java/com/manaborn/client/state/ClientPassivesState.java`

```java
public class ClientPassivesState {
    private static Set<PassiveType> learned = new HashSet<>();
    private static PassiveType slot0 = null;
    private static PassiveType slot1 = null;
    
    public static void setFromPacket(Set<String> learnedIds, String s0, String s1) {
        learned.clear();
        for (String id : learnedIds) {
            PassiveType p = PassiveType.fromId(id);
            if (p != null) learned.add(p);
        }
        slot0 = PassiveType.fromId(s0);
        slot1 = PassiveType.fromId(s1);
    }
    
    public static Set<PassiveType> getLearned() { return learned; }
    public static PassiveType getSlot0() { return slot0; }
    public static PassiveType getSlot1() { return slot1; }
    
    public static boolean isActive(PassiveType passive) {
        return passive == slot0 || passive == slot1;
    }
}
```

## Adding a New Passive

### Step 1: Add to PassiveType
```java
NEW_PASSIVE("Название", "new_passive", Formatting.GREEN);
```

### Step 2: Implement effect in PassiveEffectsManager
```java
case NEW_PASSIVE -> applyNewPassive(player);

private static void applyNewPassive(ServerPlayerEntity player) {
    // Your passive logic
}
```

### Step 3: Create learning method (book, altar, etc.)
```java
// When player learns passive:
PassiveManager.learnPassive(player, PassiveType.NEW_PASSIVE);
```

### Step 4: Add translations
```json
{
  "passive.manaborn.new_passive": "New Passive Name"
}
```

## Integration with Events

### On Player Damaged
```java
ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
    if (entity instanceof ServerPlayerEntity player) {
        Set<PassiveType> active = PassiveManager.getActivePassives(player.getUuid());
        
        if (active.contains(PassiveType.FROST_SKIN)) {
            // Apply slowness to attacker
            Entity attacker = source.getAttacker();
            if (attacker instanceof LivingEntity living) {
                living.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, 40, 1
                ));
            }
        }
    }
    return true;  // Allow damage
});
```

### On Player Attack
```java
AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
    if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
        Set<PassiveType> active = PassiveManager.getActivePassives(serverPlayer.getUuid());
        
        if (active.contains(PassiveType.COAL_HEAT)) {
            // Set target on fire
            entity.setOnFireFor(3);
        }
    }
    return ActionResult.PASS;
});
```
