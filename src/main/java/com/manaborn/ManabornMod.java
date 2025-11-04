package com.manaborn;

import com.manaborn.items.MagicStaffItem;
import com.manaborn.items.ManaPotionItem;
import com.manaborn.items.MagicDustItem;
import com.manaborn.items.SpellBookItem;
import com.manaborn.blocks.MagicOreBlock;
import com.manaborn.mana.ManaManager;
import com.manaborn.blocks.LearningAltarBlock;
import com.manaborn.blocks.entity.LearningAltarBlockEntity;
import com.manaborn.screen.LearningAltarScreenHandler;
import com.manaborn.network.ManaSyncS2CPacket;
import com.manaborn.network.LearnedSpellsSyncS2CPacket;
import com.manaborn.network.RequestLearnedSpellsC2SPacket;
import com.manaborn.spells.LearnedSpellsManager;
import com.manaborn.spells.SpellType;
import com.manaborn.entities.IceShardEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.LootTables;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.ScreenHandlerType.Factory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.resource.featuretoggle.FeatureFlags;

public class ManabornMod implements ModInitializer {
	public static final String MOD_ID = "manaborn";

	public static Item MANA_WAND;
	public static Item MANA_POTION;
	public static Item MAGIC_DUST;
	public static Block MAGIC_ORE;
	public static Item FIREBALL_SPELL_BOOK;
	public static Item ICE_SHARD_SPELL_BOOK;
	public static Item FIRE_WALL_SPELL_BOOK;
	public static Item INNER_HEAT_SPELL_BOOK;
	public static Item FROST_AURA_SPELL_BOOK;
	public static Item ABSORBING_BARRIER_SPELL_BOOK;
	public static EntityType<IceShardEntity> ICE_SHARD_ENTITY;
	public static Block LEARNING_ALTAR;
	public static BlockEntityType<LearningAltarBlockEntity> LEARNING_ALTAR_BE;
	public static ScreenHandlerType<LearningAltarScreenHandler> LEARNING_ALTAR_SH;
	
	// Lev: тут происходят инициализации
	@Override
	public void onInitialize() {
		// Регистрация типов S2C пакетов
		PayloadTypeRegistry.playS2C().register(ManaSyncS2CPacket.ID, ManaSyncS2CPacket.CODEC);
		PayloadTypeRegistry.playS2C().register(LearnedSpellsSyncS2CPacket.ID, LearnedSpellsSyncS2CPacket.CODEC);
		// Регистрация типа C2S пакета для запроса синхронизации изученных заклинаний
		PayloadTypeRegistry.playC2S().register(RequestLearnedSpellsC2SPacket.ID, RequestLearnedSpellsC2SPacket.CODEC);

		// Обработчик запроса с клиента — отправляем текущий список изученных заклинаний
		ServerPlayNetworking.registerGlobalReceiver(RequestLearnedSpellsC2SPacket.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (player != null) {
				LearnedSpellsManager.ensureInitialized(player);
				LearnedSpellsManager.sendSync(player);
			}
		});
		
		// Регистрация сущности ледяного осколка
		RegistryKey<EntityType<?>> iceShardKey = RegistryKey.of(Registries.ENTITY_TYPE.getKey(), Identifier.of(MOD_ID, "ice_shard"));
		ICE_SHARD_ENTITY = Registry.register(
			Registries.ENTITY_TYPE,
			iceShardKey,
			EntityType.Builder.<IceShardEntity>create(IceShardEntity::new, SpawnGroup.MISC)
				.dimensions(0.5f, 0.5f)
				.build(iceShardKey)
		);
		
		// Регистрация Mana Wand
		RegistryKey<Item> wandKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "mana_wand"));
		MANA_WAND = Registry.register(Registries.ITEM, wandKey, new MagicStaffItem(new Item.Settings().maxCount(1).registryKey(wandKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(MANA_WAND));
		System.out.println(" [DEBUG] Mana Wand зарегистрирована с ID: " + wandKey.getValue());
		
		// Регистрация Mana Crystal
		RegistryKey<Item> potionKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "mana_potion"));
		MANA_POTION = Registry.register(Registries.ITEM, potionKey, new ManaPotionItem(new Item.Settings().maxCount(16).registryKey(potionKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> entries.add(MANA_POTION));
		System.out.println(" [DEBUG] Mana Crystal зарегистрирован с ID: " + potionKey.getValue());
		
		// Регистрация Magic Dust
		RegistryKey<Item> dustKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "magic_dust"));
		MAGIC_DUST = Registry.register(Registries.ITEM, dustKey, new MagicDustItem(new Item.Settings().maxCount(64).registryKey(dustKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(MAGIC_DUST));
		System.out.println(" [DEBUG] Magic Dust зарегистрирована с ID: " + dustKey.getValue());
		
		// Регистрация Fireball Spell Book
		RegistryKey<Item> fireballBookKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "fireball_spell_book"));
		FIREBALL_SPELL_BOOK = Registry.register(Registries.ITEM, fireballBookKey, new SpellBookItem(new Item.Settings().maxCount(1).registryKey(fireballBookKey), SpellType.FIREBALL));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(FIREBALL_SPELL_BOOK));
		System.out.println(" [DEBUG] Fireball Spell Book зарегистрирована с ID: " + fireballBookKey.getValue());
		
		// Регистрация Ice Shard Spell Book
		RegistryKey<Item> iceShardBookKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "ice_shard_spell_book"));
		ICE_SHARD_SPELL_BOOK = Registry.register(Registries.ITEM, iceShardBookKey, new SpellBookItem(new Item.Settings().maxCount(1).registryKey(iceShardBookKey), SpellType.ICE_SHARD));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(ICE_SHARD_SPELL_BOOK));
		System.out.println(" [DEBUG] Ice Shard Spell Book зарегистрирована с ID: " + iceShardBookKey.getValue());
		
		// Регистрация Fire Wall Spell Book
		RegistryKey<Item> fireWallBookKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "fire_wall_spell_book"));
		FIRE_WALL_SPELL_BOOK = Registry.register(Registries.ITEM, fireWallBookKey, new SpellBookItem(new Item.Settings().maxCount(1).registryKey(fireWallBookKey), SpellType.FIRE_WALL));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(FIRE_WALL_SPELL_BOOK));
		System.out.println(" [DEBUG] Fire Wall Spell Book зарегистрирована с ID: " + fireWallBookKey.getValue());

		// Регистрация Inner Heat Spell Book
		RegistryKey<Item> innerHeatBookKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "inner_heat_spell_book"));
		INNER_HEAT_SPELL_BOOK = Registry.register(Registries.ITEM, innerHeatBookKey, new SpellBookItem(new Item.Settings().maxCount(1).registryKey(innerHeatBookKey), SpellType.INNER_HEAT));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(INNER_HEAT_SPELL_BOOK));
		System.out.println(" [DEBUG] Inner Heat Spell Book зарегистрирована с ID: " + innerHeatBookKey.getValue());

		// Регистрация Frost Aura Spell Book
		RegistryKey<Item> frostAuraBookKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "frost_aura_spell_book"));
		FROST_AURA_SPELL_BOOK = Registry.register(Registries.ITEM, frostAuraBookKey, new SpellBookItem(new Item.Settings().maxCount(1).registryKey(frostAuraBookKey), SpellType.FROST_AURA));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(FROST_AURA_SPELL_BOOK));
		System.out.println(" [DEBUG] Frost Aura Spell Book зарегистрирована с ID: " + frostAuraBookKey.getValue());

		// Регистрация Absorbing Barrier Spell Book
		RegistryKey<Item> barrierBookKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "absorbing_barrier_spell_book"));
		ABSORBING_BARRIER_SPELL_BOOK = Registry.register(Registries.ITEM, barrierBookKey, new SpellBookItem(new Item.Settings().maxCount(1).registryKey(barrierBookKey), SpellType.ABSORBING_BARRIER));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(ABSORBING_BARRIER_SPELL_BOOK));
		System.out.println(" [DEBUG] Absorbing Barrier Spell Book зарегистрирована с ID: " + barrierBookKey.getValue());

		// Регистрация Learning Altar блока
		RegistryKey<Block> altarBlockKey = RegistryKey.of(Registries.BLOCK.getKey(), Identifier.of(MOD_ID, "learning_altar"));
		LEARNING_ALTAR = Registry.register(
			Registries.BLOCK,
			altarBlockKey,
			new LearningAltarBlock(AbstractBlock.Settings.create()
				.strength(2.0f, 3.0f)
				.sounds(BlockSoundGroup.STONE)
				.registryKey(altarBlockKey))
		);
		RegistryKey<Item> altarItemKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "learning_altar"));
		Registry.register(Registries.ITEM, altarItemKey, new BlockItem(LEARNING_ALTAR, new Item.Settings().registryKey(altarItemKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(LEARNING_ALTAR));

		// Регистрация BlockEntityType для алтаря
		LEARNING_ALTAR_BE = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			RegistryKey.of(Registries.BLOCK_ENTITY_TYPE.getKey(), Identifier.of(MOD_ID, "learning_altar")),
			FabricBlockEntityTypeBuilder.create(LearningAltarBlockEntity::new, LEARNING_ALTAR).build()
		);

		// Регистрация ScreenHandlerType для алтаря
        Factory<LearningAltarScreenHandler> altarFactory = (syncId, inv) -> new LearningAltarScreenHandler(syncId, inv);
        LEARNING_ALTAR_SH = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MOD_ID, "learning_altar"),
            new ScreenHandlerType<>(altarFactory, FeatureFlags.VANILLA_FEATURES)
        );
		
		// Регистрация Magic Ore Block
		RegistryKey<Block> oreBlockKey = RegistryKey.of(Registries.BLOCK.getKey(), Identifier.of(MOD_ID, "magic_ore"));
		MAGIC_ORE = Registry.register(
			Registries.BLOCK,
			oreBlockKey,
			new MagicOreBlock(AbstractBlock.Settings.create()
				.strength(3.0f, 3.0f) // Прочность как у железной руды
				.requiresTool() // Требуется кирка
				.sounds(BlockSoundGroup.STONE)
				.luminance(state -> 7) // Небольшое свечение (уровень 7)
				.registryKey(oreBlockKey))
		);
		
		// Регистрация предмета блока
		RegistryKey<Item> oreItemKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "magic_ore"));
		Registry.register(Registries.ITEM, oreItemKey, new BlockItem(MAGIC_ORE, new Item.Settings().registryKey(oreItemKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(MAGIC_ORE));
		System.out.println(" [DEBUG] Magic Ore зарегистрирована с ID: " + oreBlockKey.getValue());

		// Тик сервера: регенерация маны, обработка стен огня и ауры стужи
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ManaManager.onServerTick(server.getPlayerManager().getPlayerList());
			com.manaborn.spells.SpellManager.tickFireWalls();
			com.manaborn.spells.SpellManager.tickFrostAuras(server.getPlayerManager().getPlayerList());
			com.manaborn.spells.SpellManager.tickAbsorbingBarriers(server.getPlayerManager().getPlayerList());
		});

		// Синхронизация при входе
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			ManaManager.ensureInitialized(player);
			ManaManager.sendSync(player);
			
			// Инициализация и синхронизация изученных заклинаний
			LearnedSpellsManager.ensureInitialized(player);
			LearnedSpellsManager.sendSync(player);
		});
		
		// Добавление книг заклинаний в loot tables сундуков
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (source.isBuiltin()) {
				// Подземелья (5% шанс на каждую книгу)
				if (LootTables.SIMPLE_DUNGEON_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1))
						.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(5))
						.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(5))
						.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(5))
						.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(5))
						.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(3)));
				}
				
				// Заброшенные шахты (3% шанс)
				if (LootTables.ABANDONED_MINESHAFT_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1))
						.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(3))
						.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(3))
						.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(3))
						.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(3))
						.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(2)));
				}
				
				// Библиотека крепости (15% шанс)
				if (LootTables.STRONGHOLD_LIBRARY_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1))
						.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(15))
						.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(15))
						.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(15))
						.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(15))
						.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(10)));
				}
				
				// Деревня (дом картографа) (8% шанс)
				if (LootTables.VILLAGE_CARTOGRAPHER_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1))
						.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(8))
						.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(8))
						.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(8))
						.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(8))
						.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(5)));
				}
				
				// Крепость Незера (10% шанс)
				if (LootTables.NETHER_BRIDGE_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1))
						.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(10))
						.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(10))
						.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(10))
						.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(10))
						.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(7)));
				}
				
				// Город Энда (20% шанс)
				if (LootTables.END_CITY_TREASURE_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
						.rolls(ConstantLootNumberProvider.create(1))
						.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(20))
						.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(20))
						.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(20))
						.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(20))
						.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(14)));
				}
			}
		});

		System.out.println("✨ Manaborn loaded — awaken your inner magic!");
	}
}