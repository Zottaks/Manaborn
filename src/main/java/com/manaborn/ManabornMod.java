package com.manaborn;

import com.manaborn.items.MagicStaffItem;
import com.manaborn.items.ManaPotionItem;
import com.manaborn.items.MagicDustItem;
import com.manaborn.items.SpellBookItem;
import com.manaborn.blocks.MagicOreBlock;
import com.manaborn.mana.ManaManager;
import com.manaborn.network.ManaSyncS2CPacket;
import com.manaborn.network.LearnedSpellsSyncS2CPacket;
import com.manaborn.spells.LearnedSpellsManager;
import com.manaborn.spells.SpellType;
import com.manaborn.entities.IceShardEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ManabornMod implements ModInitializer {
	public static final String MOD_ID = "manaborn";

	public static Item MANA_WAND;
	public static Item MANA_POTION;
	public static Item MAGIC_DUST;
	public static Block MAGIC_ORE;
	public static Item FIREBALL_SPELL_BOOK;
	public static EntityType<IceShardEntity> ICE_SHARD_ENTITY;
	
	// Lev: тут происходят инициализации
	@Override
	public void onInitialize() {
		// Регистрация типов S2C пакетов
		PayloadTypeRegistry.playS2C().register(ManaSyncS2CPacket.ID, ManaSyncS2CPacket.CODEC);
		PayloadTypeRegistry.playS2C().register(LearnedSpellsSyncS2CPacket.ID, LearnedSpellsSyncS2CPacket.CODEC);
		
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

		// Тик сервера: регенерация маны
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ManaManager.onServerTick(server.getPlayerManager().getPlayerList());
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

		System.out.println("✨ Manaborn loaded — awaken your inner magic!");
	}
}