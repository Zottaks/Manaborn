package com.manaborn;

import com.manaborn.items.MagicStaffItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class ManabornMod implements ModInitializer {
	public static final String MOD_ID = "manaborn";

	public static Item MAGIC_STAFF;

	@Override
	public void onInitialize() {
		RegistryKey<Item> staffKey = RegistryKey.of(Registries.ITEM.getKey(), Identifier.of(MOD_ID, "magic_staff"));
		MAGIC_STAFF = Registry.register(Registries.ITEM, staffKey, new MagicStaffItem(new Item.Settings().maxCount(1).registryKey(staffKey)));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(MAGIC_STAFF));

		System.out.println("✨ Manaborn loaded — awaken your inner magic!");
	}
}
