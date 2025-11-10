package com.manaborn;

import com.manaborn.items.MagicStaffItem;
import com.manaborn.items.ManaPotionItem;
import com.manaborn.items.MagicDustItem;
import com.manaborn.items.SpellBookItem;
import com.manaborn.blocks.MagicOreBlock;
import com.manaborn.mana.ManaManager;
import com.manaborn.blocks.LearningAltarBlock;
import com.manaborn.blocks.entity.LearningAltarBlockEntity;
import com.manaborn.blocks.ManaConverterBlock;
import com.manaborn.blocks.entity.ManaConverterBlockEntity;
import com.manaborn.screen.ManaConverterScreenHandler;
import com.manaborn.items.ManaVesselItem;
import com.manaborn.items.ManoscopeItem;
import com.manaborn.items.ManaConverterBlockItem;
import com.manaborn.items.LearningAltarBlockItem;
import com.manaborn.items.MagicOreBlockItem;
import com.manaborn.screen.LearningAltarScreenHandler;
import com.manaborn.network.ManaSyncS2CPacket;
import net.minecraft.item.BlockItem;
import com.manaborn.network.LearnedSpellsSyncS2CPacket;
import com.manaborn.network.RequestLearnedSpellsC2SPacket;
import com.manaborn.network.SelectSpellC2SPacket;
import com.manaborn.spells.LearnedSpellsManager;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import com.manaborn.entities.IceShardEntity;
import com.manaborn.entities.DustWispEntity;
import com.manaborn.world.DustWispManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
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
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
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
	public static Item WEAKNESS_CURSE_SPELL_BOOK;
	public static Item ICE_WALL_SPELL_BOOK;
	public static Item HEAT_WEAPON_SPELL_BOOK;
	public static EntityType<IceShardEntity> ICE_SHARD_ENTITY;
	public static Block LEARNING_ALTAR;
	public static BlockEntityType<LearningAltarBlockEntity> LEARNING_ALTAR_BE;
	public static ScreenHandlerType<LearningAltarScreenHandler> LEARNING_ALTAR_SH;
	public static Block MANA_CONVERTER;
	public static BlockEntityType<ManaConverterBlockEntity> MANA_CONVERTER_BE;
	public static ScreenHandlerType<ManaConverterScreenHandler> MANA_CONVERTER_SH;
	public static Item MANA_VESSEL;
	public static Item MANOSCOPE;
	public static EntityType<DustWispEntity> DUST_WISP_ENTITY;
	public static Block MANA_MAGNET;
	public static BlockEntityType<com.manaborn.blocks.entity.ManaMagnetBlockEntity> MANA_MAGNET_ENTITY;

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

		// Регистрация типа C2S пакета для выбора заклинания
		PayloadTypeRegistry.playC2S().register(SelectSpellC2SPacket.ID, SelectSpellC2SPacket.CODEC);

		// Обработчик выбора заклинания с клиента
		ServerPlayNetworking.registerGlobalReceiver(SelectSpellC2SPacket.ID, (payload, context) -> {
			context.server().execute(() -> {
				ServerPlayerEntity player = context.player();
				if (player != null) {
					// Находим посох в руках игрока
					net.minecraft.item.ItemStack mainHandStack = player.getMainHandStack();
					net.minecraft.item.ItemStack offHandStack = player.getOffHandStack();

					net.minecraft.item.ItemStack staffStack = null;
					if (mainHandStack.getItem() instanceof MagicStaffItem) {
						staffStack = mainHandStack;
					} else if (offHandStack.getItem() instanceof MagicStaffItem) {
						staffStack = offHandStack;
					}

					if (staffStack != null) {
						// Устанавливаем выбранное заклинание
						SpellType spell = SpellType.fromId(payload.spellId());
						SpellManager.setSelectedSpell(staffStack, spell);

						// Отправляем подтверждение игроку
						player.sendMessage(
								net.minecraft.text.Text.literal("Выбрано заклинание: ")
										.append(net.minecraft.text.Text.literal(spell.getDisplayName())
												.formatted(spell.getColor()))
										.append(net.minecraft.text.Text
												.literal(" (стоимость: " + spell.getManaCost() + " маны)")),
								true);
					}
				}
			});
		});

		// Регистрация сущности ледяного осколка
		RegistryKey<EntityType<?>> iceShardKey = RegistryKey.of(Registries.ENTITY_TYPE.getKey(),
				Identifier.of(MOD_ID, "ice_shard"));
		ICE_SHARD_ENTITY = Registry.register(
				Registries.ENTITY_TYPE,
				iceShardKey,
				EntityType.Builder.<IceShardEntity>create(IceShardEntity::new, SpawnGroup.MISC)
						.dimensions(0.5f, 0.5f)
						.build(iceShardKey));

		// Регистрация сущности пылинки (невидимая метка для магической пыли)
		RegistryKey<EntityType<?>> wispKey = RegistryKey.of(Registries.ENTITY_TYPE.getKey(),
				Identifier.of(MOD_ID, "dust_wisp"));
		DUST_WISP_ENTITY = Registry.register(
				Registries.ENTITY_TYPE,
				wispKey,
				EntityType.Builder.<DustWispEntity>create(DustWispEntity::new, SpawnGroup.MISC)
						.dimensions(0.1f, 0.1f)
						.build(wispKey));

		// Регистрация Mana Wand
		RegistryKey<Item> wandKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "mana_wand"));
		MANA_WAND = Registry.register(Registries.ITEM, wandKey,
				new MagicStaffItem(new Item.Settings().maxCount(1).registryKey(wandKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(MANA_WAND));
		System.out.println(" [DEBUG] Mana Wand зарегистрирована с ID: " + wandKey.getValue());

		// Регистрация Mana Crystal
		RegistryKey<Item> potionKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "mana_potion"));
		MANA_POTION = Registry.register(Registries.ITEM, potionKey,
				new ManaPotionItem(new Item.Settings().maxCount(16).registryKey(potionKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> entries.add(MANA_POTION));
		System.out.println(" [DEBUG] Mana Crystal зарегистрирован с ID: " + potionKey.getValue());

		// Регистрация Manoscope (магические очки для обнаружения пыли)
		RegistryKey<Item> scopeKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "manoscope"));
		MANOSCOPE = Registry.register(Registries.ITEM, scopeKey,
				new ManoscopeItem(new Item.Settings()
						.maxCount(1)
						.registryKey(scopeKey)
						.component(DataComponentTypes.EQUIPPABLE,
								EquippableComponent.builder(EquipmentSlot.HEAD).build())));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(MANOSCOPE));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(MANOSCOPE));
		System.out.println(" [DEBUG] Manoscope зарегистрирован с ID: " + scopeKey.getValue());

		// Регистрация Magic Dust
		RegistryKey<Item> dustKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "magic_dust"));
		MAGIC_DUST = Registry.register(Registries.ITEM, dustKey,
				new MagicDustItem(new Item.Settings().maxCount(64).registryKey(dustKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(MAGIC_DUST));
		System.out.println(" [DEBUG] Magic Dust зарегистрирована с ID: " + dustKey.getValue());

		// Регистрация Fireball Spell Book
		RegistryKey<Item> fireballBookKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "fireball_spell_book"));
		FIREBALL_SPELL_BOOK = Registry.register(Registries.ITEM, fireballBookKey,
				new SpellBookItem(new Item.Settings().maxCount(1).registryKey(fireballBookKey), SpellType.FIREBALL));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(FIREBALL_SPELL_BOOK));
		System.out.println(" [DEBUG] Fireball Spell Book зарегистрирована с ID: " + fireballBookKey.getValue());

		// Регистрация Ice Shard Spell Book
		RegistryKey<Item> iceShardBookKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "ice_shard_spell_book"));
		ICE_SHARD_SPELL_BOOK = Registry.register(Registries.ITEM, iceShardBookKey,
				new SpellBookItem(new Item.Settings().maxCount(1).registryKey(iceShardBookKey), SpellType.ICE_SHARD));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(ICE_SHARD_SPELL_BOOK));
		System.out.println(" [DEBUG] Ice Shard Spell Book зарегистрирована с ID: " + iceShardBookKey.getValue());

		// Регистрация Fire Wall Spell Book
		RegistryKey<Item> fireWallBookKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "fire_wall_spell_book"));
		FIRE_WALL_SPELL_BOOK = Registry.register(Registries.ITEM, fireWallBookKey,
				new SpellBookItem(new Item.Settings().maxCount(1).registryKey(fireWallBookKey), SpellType.FIRE_WALL));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(FIRE_WALL_SPELL_BOOK));
		System.out.println(" [DEBUG] Fire Wall Spell Book зарегистрирована с ID: " + fireWallBookKey.getValue());

		// Регистрация Inner Heat Spell Book
		RegistryKey<Item> innerHeatBookKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "inner_heat_spell_book"));
		INNER_HEAT_SPELL_BOOK = Registry.register(Registries.ITEM, innerHeatBookKey,
				new SpellBookItem(new Item.Settings().maxCount(1).registryKey(innerHeatBookKey), SpellType.INNER_HEAT));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(INNER_HEAT_SPELL_BOOK));
		System.out.println(" [DEBUG] Inner Heat Spell Book зарегистрирована с ID: " + innerHeatBookKey.getValue());

		// Регистрация Frost Aura Spell Book
		RegistryKey<Item> frostAuraBookKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "frost_aura_spell_book"));
		FROST_AURA_SPELL_BOOK = Registry.register(Registries.ITEM, frostAuraBookKey,
				new SpellBookItem(new Item.Settings().maxCount(1).registryKey(frostAuraBookKey), SpellType.FROST_AURA));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(FROST_AURA_SPELL_BOOK));
		System.out.println(" [DEBUG] Frost Aura Spell Book зарегистрирована с ID: " + frostAuraBookKey.getValue());

		// Регистрация Absorbing Barrier Spell Book
		RegistryKey<Item> barrierBookKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "absorbing_barrier_spell_book"));
		ABSORBING_BARRIER_SPELL_BOOK = Registry.register(Registries.ITEM, barrierBookKey, new SpellBookItem(
				new Item.Settings().maxCount(1).registryKey(barrierBookKey), SpellType.ABSORBING_BARRIER));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
				.register(entries -> entries.add(ABSORBING_BARRIER_SPELL_BOOK));
		System.out.println(" [DEBUG] Absorbing Barrier Spell Book зарегистрирована с ID: " + barrierBookKey.getValue());

		// Регистрация Weakness Curse Spell Book
		RegistryKey<Item> weaknessCurseBookKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "weakness_curse_spell_book"));
		WEAKNESS_CURSE_SPELL_BOOK = Registry.register(Registries.ITEM, weaknessCurseBookKey, new SpellBookItem(
				new Item.Settings().maxCount(1).registryKey(weaknessCurseBookKey), SpellType.WEAKNESS_CURSE));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
				.register(entries -> entries.add(WEAKNESS_CURSE_SPELL_BOOK));
		System.out.println(
				" [DEBUG] Weakness Curse Spell Book зарегистрирована с ID: " + weaknessCurseBookKey.getValue());

		// Регистрация Ice Wall Spell Book
		RegistryKey<Item> iceWallBookKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "ice_wall_spell_book"));
		ICE_WALL_SPELL_BOOK = Registry.register(Registries.ITEM, iceWallBookKey,
				new SpellBookItem(new Item.Settings().maxCount(1).registryKey(iceWallBookKey), SpellType.ICE_WALL));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(ICE_WALL_SPELL_BOOK));
		System.out.println(" [DEBUG] Ice Wall Spell Book зарегистрирована с ID: " + iceWallBookKey.getValue());

		// Регистрация Heat Weapon Spell Book
		RegistryKey<Item> heatWeaponBookKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "heat_weapon_spell_book"));
		HEAT_WEAPON_SPELL_BOOK = Registry.register(Registries.ITEM, heatWeaponBookKey, new SpellBookItem(
				new Item.Settings().maxCount(1).registryKey(heatWeaponBookKey), SpellType.HEAT_WEAPON));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(HEAT_WEAPON_SPELL_BOOK));
		System.out.println(" [DEBUG] Heat Weapon Spell Book зарегистрирована с ID: " + heatWeaponBookKey.getValue());

		// Регистрация Learning Altar блока
		RegistryKey<Block> altarBlockKey = RegistryKey.of(Registries.BLOCK.getKey(),
				Identifier.of(MOD_ID, "learning_altar"));
		LEARNING_ALTAR = Registry.register(
				Registries.BLOCK,
				altarBlockKey,
				new LearningAltarBlock(AbstractBlock.Settings.create()
						.strength(2.0f, 3.0f)
						.sounds(BlockSoundGroup.STONE)
						.registryKey(altarBlockKey)));
		RegistryKey<Item> altarItemKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "learning_altar"));
		Registry.register(Registries.ITEM, altarItemKey,
				new LearningAltarBlockItem((LearningAltarBlock) LEARNING_ALTAR,
						new Item.Settings().registryKey(altarItemKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(LEARNING_ALTAR));

		// Регистрация BlockEntityType для алтаря
		LEARNING_ALTAR_BE = Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				RegistryKey.of(Registries.BLOCK_ENTITY_TYPE.getKey(), Identifier.of(MOD_ID, "learning_altar")),
				FabricBlockEntityTypeBuilder.create(LearningAltarBlockEntity::new, LEARNING_ALTAR).build());

		// Регистрация ScreenHandlerType для алтаря
		Factory<LearningAltarScreenHandler> altarFactory = (syncId, inv) -> new LearningAltarScreenHandler(syncId, inv);
		LEARNING_ALTAR_SH = Registry.register(
				Registries.SCREEN_HANDLER,
				Identifier.of(MOD_ID, "learning_altar"),
				new ScreenHandlerType<>(altarFactory, FeatureFlags.VANILLA_FEATURES));

		// Регистрация Mana Converter блока
		RegistryKey<Block> converterBlockKey = RegistryKey.of(Registries.BLOCK.getKey(),
				Identifier.of(MOD_ID, "mana_converter"));
		MANA_CONVERTER = Registry.register(
				Registries.BLOCK,
				converterBlockKey,
				new ManaConverterBlock(AbstractBlock.Settings.create()
						.strength(2.0f, 3.0f)
						.sounds(BlockSoundGroup.STONE)
						.registryKey(converterBlockKey)));
		RegistryKey<Item> converterItemKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "mana_converter"));
		Registry.register(Registries.ITEM, converterItemKey,
				new ManaConverterBlockItem((ManaConverterBlock) MANA_CONVERTER,
						new Item.Settings().registryKey(converterItemKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(MANA_CONVERTER));

		// BE и ScreenHandler для конвертора
		MANA_CONVERTER_BE = Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				RegistryKey.of(Registries.BLOCK_ENTITY_TYPE.getKey(), Identifier.of(MOD_ID, "mana_converter")),
				FabricBlockEntityTypeBuilder.create(ManaConverterBlockEntity::new, MANA_CONVERTER).build());
		Factory<ManaConverterScreenHandler> converterFactory = (syncId, inv) -> new ManaConverterScreenHandler(syncId,
				inv);
		MANA_CONVERTER_SH = Registry.register(
				Registries.SCREEN_HANDLER,
				Identifier.of(MOD_ID, "mana_converter"),
				new ScreenHandlerType<>(converterFactory, FeatureFlags.VANILLA_FEATURES));

		// Регистрация Mana Magnet блока
		RegistryKey<Block> magnetBlockKey = RegistryKey.of(Registries.BLOCK.getKey(),
				Identifier.of(MOD_ID, "mana_magnet"));
		MANA_MAGNET = Registry.register(
				Registries.BLOCK,
				magnetBlockKey,
				new com.manaborn.blocks.ManaMagnetBlock(AbstractBlock.Settings.create()
						.strength(3.0f, 4.0f)
						.sounds(BlockSoundGroup.AMETHYST_BLOCK)
						.luminance(state -> 8)
						.registryKey(magnetBlockKey)));
		RegistryKey<Item> magnetItemKey = RegistryKey.of(Registries.ITEM.getKey(),
				Identifier.of(MOD_ID, "mana_magnet"));
		Registry.register(Registries.ITEM, magnetItemKey,
				new BlockItem(MANA_MAGNET, new Item.Settings().registryKey(magnetItemKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(MANA_MAGNET));

		// BE для Mana Magnet
		MANA_MAGNET_ENTITY = Registry.register(
				Registries.BLOCK_ENTITY_TYPE,
				RegistryKey.of(Registries.BLOCK_ENTITY_TYPE.getKey(), Identifier.of(MOD_ID, "mana_magnet")),
				FabricBlockEntityTypeBuilder.create(com.manaborn.blocks.entity.ManaMagnetBlockEntity::new, MANA_MAGNET)
						.build());

		// Регистрация Mana Vessel
		RegistryKey<Item> vesselKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "mana_vessel"));
		MANA_VESSEL = Registry.register(Registries.ITEM, vesselKey,
				new ManaVesselItem(new Item.Settings().maxCount(1).registryKey(vesselKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(MANA_VESSEL));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(MANA_VESSEL));

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
						.registryKey(oreBlockKey)));

		// Регистрация предмета блока
		RegistryKey<Item> oreItemKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "magic_ore"));
		Registry.register(Registries.ITEM, oreItemKey,
				new MagicOreBlockItem((MagicOreBlock) MAGIC_ORE, new Item.Settings().registryKey(oreItemKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(MAGIC_ORE));
		System.out.println(" [DEBUG] Magic Ore зарегистрирована с ID: " + oreBlockKey.getValue());

		// Тик сервера: регенерация маны, обработка стен огня и ауры стужи, спавн
		// пылинок
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ManaManager.onServerTick(server.getPlayerManager().getPlayerList());
			com.manaborn.spells.SpellManager.tickFireWalls();
			com.manaborn.spells.SpellManager.tickFrostAuras(server.getPlayerManager().getPlayerList());
			com.manaborn.spells.SpellManager.tickAbsorbingBarriers(server.getPlayerManager().getPlayerList());
			com.manaborn.spells.SpellManager.tickIceWalls();
			DustWispManager.tick(server.getPlayerManager().getPlayerList());
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

		// Обработка атак для заклинания Heat Weapon
		AttackEntityCallback.EVENT.register((player, world, hand, target, hitResult) -> {
			if (player instanceof ServerPlayerEntity serverPlayer && target instanceof LivingEntity livingTarget) {
				com.manaborn.spells.SpellManager.onPlayerAttack(serverPlayer, livingTarget, world);
			}
			return ActionResult.PASS;
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
							.with(ItemEntry.builder(ICE_WALL_SPELL_BOOK).weight(5))
							.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(5))
							.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(3))
							.with(ItemEntry.builder(WEAKNESS_CURSE_SPELL_BOOK).weight(3)));
				}

				// Заброшенные шахты (3% шанс)
				if (LootTables.ABANDONED_MINESHAFT_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
							.rolls(ConstantLootNumberProvider.create(1))
							.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(3))
							.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(3))
							.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(3))
							.with(ItemEntry.builder(ICE_WALL_SPELL_BOOK).weight(3))
							.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(3))
							.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(2))
							.with(ItemEntry.builder(WEAKNESS_CURSE_SPELL_BOOK).weight(2)));
				}

				// Библиотека крепости (15% шанс)
				if (LootTables.STRONGHOLD_LIBRARY_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
							.rolls(ConstantLootNumberProvider.create(1))
							.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(15))
							.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(15))
							.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(15))
							.with(ItemEntry.builder(ICE_WALL_SPELL_BOOK).weight(15))
							.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(15))
							.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(10))
							.with(ItemEntry.builder(WEAKNESS_CURSE_SPELL_BOOK).weight(10)));
				}

				// Деревня (дом картографа) (8% шанс)
				if (LootTables.VILLAGE_CARTOGRAPHER_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
							.rolls(ConstantLootNumberProvider.create(1))
							.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(8))
							.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(8))
							.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(8))
							.with(ItemEntry.builder(ICE_WALL_SPELL_BOOK).weight(8))
							.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(8))
							.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(5))
							.with(ItemEntry.builder(WEAKNESS_CURSE_SPELL_BOOK).weight(5)));
				}

				// Крепость Незера (10% шанс)
				if (LootTables.NETHER_BRIDGE_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
							.rolls(ConstantLootNumberProvider.create(1))
							.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(10))
							.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(10))
							.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(10))
							.with(ItemEntry.builder(ICE_WALL_SPELL_BOOK).weight(10))
							.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(10))
							.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(7))
							.with(ItemEntry.builder(WEAKNESS_CURSE_SPELL_BOOK).weight(7)));
				}

				// Город Энда (20% шанс)
				if (LootTables.END_CITY_TREASURE_CHEST.equals(key)) {
					tableBuilder.pool(LootPool.builder()
							.rolls(ConstantLootNumberProvider.create(1))
							.with(ItemEntry.builder(FIREBALL_SPELL_BOOK).weight(20))
							.with(ItemEntry.builder(ICE_SHARD_SPELL_BOOK).weight(20))
							.with(ItemEntry.builder(FIRE_WALL_SPELL_BOOK).weight(20))
							.with(ItemEntry.builder(ICE_WALL_SPELL_BOOK).weight(20))
							.with(ItemEntry.builder(FROST_AURA_SPELL_BOOK).weight(20))
							.with(ItemEntry.builder(ABSORBING_BARRIER_SPELL_BOOK).weight(14))
							.with(ItemEntry.builder(WEAKNESS_CURSE_SPELL_BOOK).weight(14)));
				}
			}
		});

		// Загрузка изученных заклинаний при старте сервера
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LearnedSpellsManager.loadFromDisk();
		});

		// Сохранение изученных заклинаний при остановке сервера
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LearnedSpellsManager.saveToDisk();
		});

		System.out.println("✨ Manaborn loaded — awaken your inner magic!");
	}
}