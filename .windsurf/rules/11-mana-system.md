---
trigger: glob
globs: src/main/java/com/manaborn/mana/**/*.java
---

# Mana System Architecture

## ManaManager Overview

**Location:** `src/main/java/com/manaborn/mana/ManaManager.java`

### Constants
```java
public static final int MAX_MANA = 100;           // Maximum mana capacity
private static final int DEFAULT_MANA = MAX_MANA; // Starting mana
private static final int VESSEL_REFILL_PER_TICK = 1;  // Mana transfer rate from vessel
```

### Data Storage (Thread-Safe)
```java
// Player UUID -> Current mana
private static final Map<UUID, Integer> playerIdToMana = new ConcurrentHashMap<>();

// Player UUID -> Last tick when mana was spent (for regen delay)
private static final Map<UUID, Long> lastSpendTick = new ConcurrentHashMap<>();

// Player UUID -> Ticks since last regen tick
private static final Map<UUID, Integer> regenTickCounter = new ConcurrentHashMap<>();
```

## Core Methods

### Get/Set Mana
```java
public static int getMana(UUID playerId) {
    return playerIdToMana.getOrDefault(playerId, DEFAULT_MANA);
}

public static void setMana(UUID playerId, int mana) {
    playerIdToMana.put(playerId, Math.max(0, Math.min(MAX_MANA, mana)));
}
```

### Consume Mana (for spell casting)
```java
public static boolean tryConsume(ServerPlayerEntity player, int amount) {
    UUID id = player.getUuid();
    int current = getMana(id);
    
    if (current < amount) {
        return false;  // Not enough mana
    }
    
    setMana(id, current - amount);
    lastSpendTick.put(id, currentServerTick);
    regenTickCounter.put(id, 0);
    
    // IMPORTANT: Always sync after change!
    sendSync(player);
    return true;
}
```

### Add Mana (for potions, vessels)
```java
public static void addMana(ServerPlayerEntity player, int amount) {
    if (amount <= 0) return;
    
    UUID id = player.getUuid();
    int current = getMana(id);
    setMana(id, Math.min(MAX_MANA, current + amount));
    
    // IMPORTANT: Always sync after change!
    sendSync(player);
}
```

### Initialize Player
```java
public static void ensureInitialized(ServerPlayerEntity player) {
    playerIdToMana.putIfAbsent(player.getUuid(), DEFAULT_MANA);
}
```

## Mana Regeneration

**Current Implementation:** No natural regeneration. Mana only restores from Mana Vessel.

```java
// Called from ServerTickEvents.END_SERVER_TICK
public static void onServerTick(Iterable<ServerPlayerEntity> players) {
    currentServerTick++;
    
    for (ServerPlayerEntity player : players) {
        ensureInitialized(player);
        UUID id = player.getUuid();
        int current = getMana(id);
        
        if (current >= MAX_MANA) continue;

        // Find vessel with charge in inventory
        ItemStack vessel = findVesselWithCharge(player);
        if (vessel.isEmpty()) continue;
        
        int stored = ManaVesselItem.getStored(vessel);
        if (stored <= 0) continue;

        int need = MAX_MANA - current;
        int move = Math.min(VESSEL_REFILL_PER_TICK, Math.min(need, stored));
        
        if (move > 0) {
            ManaVesselItem.setStored(vessel, stored - move);
            setMana(id, current + move);
            sendSync(player);
        }
    }
}

private static ItemStack findVesselWithCharge(ServerPlayerEntity player) {
    // Check offhand first (quick access)
    ItemStack off = player.getOffHandStack();
    if (!off.isEmpty() && off.getItem() instanceof ManaVesselItem 
        && ManaVesselItem.getStored(off) > 0) {
        return off;
    }
    
    // Then check main inventory
    PlayerInventory inv = player.getInventory();
    for (int i = 0; i < inv.size(); i++) {
        ItemStack s = inv.getStack(i);
        if (!s.isEmpty() && s.getItem() instanceof ManaVesselItem 
            && ManaVesselItem.getStored(s) > 0) {
            return s;
        }
    }
    
    return ItemStack.EMPTY;
}
```

## Network Synchronization

### Sending Sync Packet
```java
public static void sendSync(ServerPlayerEntity player) {
    ManaSyncS2CPacket pkt = new ManaSyncS2CPacket(
        getMana(player.getUuid()), 
        MAX_MANA
    );
    ServerPlayNetworking.send(player, pkt);
}
```

### ManaSyncS2CPacket
```java
public record ManaSyncS2CPacket(int mana, int maxMana) implements CustomPayload {
    public static final CustomPayload.Id<ManaSyncS2CPacket> ID = 
        new CustomPayload.Id<>(Identifier.of("manaborn", "mana_sync"));
    
    public static final PacketCodec<RegistryByteBuf, ManaSyncS2CPacket> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, ManaSyncS2CPacket::mana,
            PacketCodecs.VAR_INT, ManaSyncS2CPacket::maxMana,
            ManaSyncS2CPacket::new
        );
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
```

## Client-Side State

**Location:** `src/client/java/com/manaborn/client/state/ClientManaState.java`

```java
public class ClientManaState {
    private static int mana = 0;
    private static int maxMana = 100;
    private static int displayMana = 0;  // For smooth animation
    
    public static void set(int newMana, int newMax) {
        mana = newMana;
        maxMana = newMax;
    }
    
    public static int getMana() { return mana; }
    public static int getMaxMana() { return maxMana; }
    public static int getDisplayMana() { return displayMana; }
    
    // Smooth animation - call every client tick
    public static void clientTick() {
        if (displayMana < mana) {
            displayMana = Math.min(mana, displayMana + 2);
        } else if (displayMana > mana) {
            displayMana = Math.max(mana, displayMana - 2);
        }
    }
}
```

## Integration Points

### When to Call sendSync()
1. After `tryConsume()` succeeds (spell cast)
2. After `addMana()` (potion used)
3. In `onServerTick()` when vessel transfers mana
4. On player join (`ServerPlayConnectionEvents.JOIN`)

### Mana-Consuming Items
```java
// ManaPotionItem.use()
@Override
public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
    if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
        ManaManager.addMana(serverPlayer, 50);  // Restore 50 mana
        user.playSound(SoundEvents.ENTITY_GENERIC_DRINK, 1.0f, 1.0f);
        
        if (!user.isCreative()) {
            stack.decrement(1);
        }
    }
    return TypedActionResult.success(user.getStackInHand(hand));
}
```

### Mana Cost Calculation in SpellManager
```java
// With Ether Scepter discount
int baseCost = spell.getManaCost();
int finalCost = baseCost;

if (stack.getItem() instanceof EtherScepterItem) {
    finalCost = Math.max(0, baseCost - EtherScepterItem.MANA_DISCOUNT);
}

if (!ManaManager.tryConsume(serverPlayer, finalCost)) {
    serverPlayer.sendMessage(Text.literal("Недостаточно маны: нужно " + finalCost), true);
    return;
}
```

## Mana Vessel

**Location:** `src/main/java/com/manaborn/items/ManaVesselItem.java`

```java
public class ManaVesselItem extends Item {
    public static final int MAX_CAPACITY = 100;
    
    public static int getStored(ItemStack stack) {
        NbtComponent nbt = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbt == null) return 0;
        return nbt.copyNbt().getInt("mana");
    }
    
    public static void setStored(ItemStack stack, int amount) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, 
            NbtComponent.DEFAULT).copyNbt();
        nbt.putInt("mana", Math.max(0, Math.min(MAX_CAPACITY, amount)));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}
```
