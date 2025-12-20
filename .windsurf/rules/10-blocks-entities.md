---
trigger: glob
globs: src/main/java/com/manaborn/blocks/**/*.java, src/main/java/com/manaborn/entities/**/*.java
---

# Blocks & Entities Patterns (1.21.10)

## Existing Blocks in Manaborn

| Block | BlockEntity | ScreenHandler | Purpose |
|-------|-------------|---------------|---------|
| LearningAltarBlock | LearningAltarBlockEntity | LearningAltarScreenHandler | Learn spells from books |
| ManaConverterBlock | ManaConverterBlockEntity | ManaConverterScreenHandler | Convert items to mana |
| EtherConverterBlock | EtherConverterBlockEntity | EtherConverterScreenHandler | Work with ether |
| MagicCraftingTableBlock | MagicCraftingTableBlockEntity | MagicCraftingTableScreenHandler | Magic crafting |
| SpellEnhancementAltarBlock | SpellEnhancementAltarBlockEntity | SpellEnhancementAltarScreenHandler | Upgrade spells |
| ManaMagnetBlock | ManaMagnetBlockEntity | - | Attract dust wisps |
| MagicOreBlock | - | - | Drop magic dust |

## Block Registration Pattern
```java
// 1. Create block
RegistryKey<Block> blockKey = RegistryKey.of(
    Registries.BLOCK.getKey(),
    Identifier.of(MOD_ID, "my_block")
);

MY_BLOCK = Registry.register(
    Registries.BLOCK,
    blockKey,
    new MyBlock(AbstractBlock.Settings.create()
        .strength(2.0f, 3.0f)          // hardness, resistance
        .sounds(BlockSoundGroup.STONE)
        .luminance(state -> 8)          // light level (optional)
        .registryKey(blockKey))
);

// 2. Create block item
RegistryKey<Item> blockItemKey = RegistryKey.of(
    Registries.ITEM.getKey(),
    Identifier.of(MOD_ID, "my_block")
);
Registry.register(Registries.ITEM, blockItemKey,
    new BlockItem(MY_BLOCK, new Item.Settings().registryKey(blockItemKey)));

// 3. Add to creative tab
ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS)
    .register(entries -> entries.add(MY_BLOCK));
```

## Block with GUI (ScreenHandler)
```java
// Block class
public class MyBlock extends BlockWithEntity {
    public MyBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MyBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, 
            PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof MyBlockEntity myBe) {
                player.openHandledScreen(myBe);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
```

## BlockEntity Pattern
```java
public class MyBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    
    public MyBlockEntity(BlockPos pos, BlockState state) {
        super(ManabornMod.MY_BLOCK_BE, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.manaborn.my_block");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MyScreenHandler(syncId, playerInventory, this);
    }

    // Save/load NBT
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("myValue", myValue);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        myValue = nbt.getInt("myValue");
    }
}
```

## BlockEntityType Registration
```java
MY_BLOCK_BE = Registry.register(
    Registries.BLOCK_ENTITY_TYPE,
    RegistryKey.of(Registries.BLOCK_ENTITY_TYPE.getKey(), Identifier.of(MOD_ID, "my_block")),
    FabricBlockEntityTypeBuilder.create(MyBlockEntity::new, MY_BLOCK).build()
);
```

## ScreenHandlerType Registration
```java
MY_BLOCK_SH = Registry.register(
    Registries.SCREEN_HANDLER,
    Identifier.of(MOD_ID, "my_block"),
    new ScreenHandlerType<>(
        (syncId, inv) -> new MyScreenHandler(syncId, inv),
        FeatureFlags.VANILLA_FEATURES
    )
);
```

---

## Existing Entities in Manaborn

| Entity | Type | Purpose |
|--------|------|---------|
| IceShardEntity | Projectile | Ice Shard spell projectile |
| FireWallEntity | Area | Fire Wall spell effect |
| DustWispEntity | Marker | Invisible, visible with Manoscope |
| MageMerchantEntity | NPC | Trading for magic items |

## Entity Registration Pattern
```java
// 1. Create entity type
RegistryKey<EntityType<?>> entityKey = RegistryKey.of(
    Registries.ENTITY_TYPE.getKey(),
    Identifier.of(MOD_ID, "my_entity")
);

MY_ENTITY = Registry.register(
    Registries.ENTITY_TYPE,
    entityKey,
    EntityType.Builder.<MyEntity>create(MyEntity::new, SpawnGroup.MISC)
        .dimensions(0.5f, 0.5f)  // width, height
        .build(entityKey)
);

// 2. For living entities - REQUIRED!
FabricDefaultAttributeRegistry.register(MY_ENTITY, MyEntity.createMobAttributes());
```

## Projectile Entity Pattern
```java
public class IceShardEntity extends ThrownItemEntity {
    
    public IceShardEntity(EntityType<? extends IceShardEntity> entityType, World world) {
        super(entityType, world);
    }

    public IceShardEntity(World world, LivingEntity owner) {
        super(ManabornMod.ICE_SHARD_ENTITY, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;  // Visual item
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity target = entityHitResult.getEntity();
        
        // Apply damage
        target.damage(this.getDamageSources().thrown(this, this.getOwner()), 4.0f);
        
        // Apply effect
        if (target instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS, 100, 2
            ));
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient) {
            this.discard();
        }
    }
}
```

## Living Entity (NPC) Pattern
```java
public class MageMerchantEntity extends MerchantEntity {
    
    public MageMerchantEntity(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.MAX_HEALTH, 20.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void fillRecipes() {
        TradeOfferList offers = this.getOffers();
        offers.add(new TradeOffer(
            new TradedItem(Items.EMERALD, 5),
            new ItemStack(ManabornMod.MAGIC_DUST, 3),
            12, 1, 0.05f
        ));
    }

    // Prevent despawning
    @Override
    public boolean cannotDespawn() {
        return true;
    }
}
```

## Client-Side Entity Renderer Registration
```java
// In ManabornClient.onInitializeClient()
@SuppressWarnings("deprecation")
private void registerRenderers() {
    // Projectile with item model
    EntityRendererRegistry.register(ManabornMod.ICE_SHARD_ENTITY, FlyingItemEntityRenderer::new);
    
    // Invisible entity
    EntityRendererRegistry.register(ManabornMod.DUST_WISP_ENTITY,
        ctx -> new EmptyEntityRenderer<>(ctx));
    
    // Custom model entity
    EntityRendererRegistry.register(ManabornMod.MAGE_MERCHANT_ENTITY,
        MageMerchantRenderer::new);
}
```

## Block with Ticker (periodic updates)
```java
public class ManaMagnetBlock extends BlockWithEntity {
    
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ManabornMod.MANA_MAGNET_ENTITY,
            (w, pos, s, be) -> ((ManaMagnetBlockEntity) be).tick(w, pos, s));
    }
}

// In BlockEntity
public void tick(World world, BlockPos pos, BlockState state) {
    if (world.isClient) return;
    
    tickCounter++;
    if (tickCounter >= 20) {  // Every second
        tickCounter = 0;
        // Do something...
    }
}
```
