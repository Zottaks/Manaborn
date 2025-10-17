package com.manaborn;

import com.manaborn.items.MagicStaffItem;
import com.manaborn.mana.ManaManager;
import com.manaborn.network.ManaSyncS2CPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ManabornMod implements ModInitializer {
	public static final String MOD_ID = "manaborn";

	public static Item MAGIC_STAFF;
	
	// Lev: тут происходят инициализации
	@Override
	public void onInitialize() {
		// Регистрация типа S2C пакета
		PayloadTypeRegistry.playS2C().register(ManaSyncS2CPacket.ID, ManaSyncS2CPacket.CODEC);
		RegistryKey<Item> staffKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "magic_staff"));
		MAGIC_STAFF = Registry.register(Registries.ITEM, staffKey, new MagicStaffItem(new Item.Settings().maxCount(1).registryKey(staffKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(MAGIC_STAFF));

		// Тик сервера: регенерация маны
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ManaManager.onServerTick(server.getPlayerManager().getPlayerList());
		});

		// Синхронизация при входе
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			ManaManager.ensureInitialized(player);
			ManaManager.sendSync(player);
		});

		System.out.println("✨ Manaborn loaded — awaken your inner magic!");
	}
}
