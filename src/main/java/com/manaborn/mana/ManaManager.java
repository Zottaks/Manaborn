package com.manaborn.mana;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ManaManager {
	public static final int MAX_MANA = 100;
	private static final int DEFAULT_MANA = MAX_MANA;

	private static final Map<UUID, Integer> playerIdToMana = new ConcurrentHashMap<>();

	private ManaManager() {}

	public static int getMana(UUID playerId) {
		return playerIdToMana.getOrDefault(playerId, DEFAULT_MANA);
	}

	public static void setMana(UUID playerId, int mana) {
		playerIdToMana.put(playerId, Math.max(0, Math.min(MAX_MANA, mana)));
	}

	public static boolean tryConsume(ServerPlayerEntity player, int amount) {
		UUID id = player.getUuid();
		int current = getMana(id);
		if (current < amount) {
			return false;
		}
		setMana(id, current - amount);
		return true;
	}

	public static void ensureInitialized(ServerPlayerEntity player) {
		playerIdToMana.putIfAbsent(player.getUuid(), DEFAULT_MANA);
	}
}


