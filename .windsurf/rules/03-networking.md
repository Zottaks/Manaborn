---
trigger: glob
globs: src/main/java/com/manaborn/network/**/*.java
---

# Network Packets (1.21.10 CustomPayload)

## S2C Packet Template (Server → Client)
```java
public record MySyncS2CPayload(int value, String text) implements CustomPayload {
    
    public static final Id<MySyncS2CPayload> ID = 
        new Id<>(Identifier.of("manaborn", "my_sync"));
    
    public static final PacketCodec<RegistryByteBuf, MySyncS2CPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, MySyncS2CPayload::value,
            PacketCodecs.STRING, MySyncS2CPayload::text,
            MySyncS2CPayload::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
```

## C2S Packet Template (Client → Server)
```java
public record MyRequestC2SPayload(String action) implements CustomPayload {
    
    public static final Id<MyRequestC2SPayload> ID = 
        new Id<>(Identifier.of("manaborn", "my_request"));
    
    public static final PacketCodec<RegistryByteBuf, MyRequestC2SPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.STRING, MyRequestC2SPayload::action,
            MyRequestC2SPayload::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
```

## Registration (ModInitializer)
```java
// S2C packets
PayloadTypeRegistry.playS2C().register(MySyncS2CPayload.ID, MySyncS2CPayload.CODEC);

// C2S packets  
PayloadTypeRegistry.playC2S().register(MyRequestC2SPayload.ID, MyRequestC2SPayload.CODEC);

// C2S handler
ServerPlayNetworking.registerGlobalReceiver(MyRequestC2SPayload.ID, (payload, context) -> {
    context.server().execute(() -> {
        ServerPlayerEntity player = context.player();
        // Handle on main thread
    });
});
```

## Client Handler (ClientModInitializer)
```java
ClientPlayNetworking.registerGlobalReceiver(MySyncS2CPayload.ID, (payload, context) -> {
    context.client().execute(() -> {
        // Update client state on main thread
    });
});
```

## PacketCodecs Available
- `VAR_INT`, `INTEGER`, `LONG`, `VAR_LONG`
- `FLOAT`, `DOUBLE`, `BOOLEAN`, `STRING`
- `BLOCK_POS`, `IDENTIFIER`
- `.collect(PacketCodecs.toList())` for lists
