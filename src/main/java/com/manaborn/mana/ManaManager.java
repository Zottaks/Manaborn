package com.manaborn.mana;

import com.manaborn.network.ManaSyncS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ManaManager {
	public static final int MAX_MANA = 100;
	private static final int DEFAULT_MANA = MAX_MANA;

	// Регенерация: 20 маны за 5 секунд
	private static final int REGEN_UNIT_PER_5S = 20; // 20 маны
	private static final int TICKS_PER_5S = 100; // 5 секунд при 20 тиков/сек
	private static final int TICKS_PER_1_MANA = TICKS_PER_5S / REGEN_UNIT_PER_5S; // 5 тиков на 1 ману
	private static final int REGEN_DELAY_TICKS = 40; // задержка 2 секунды после траты

	private static final Map<UUID, Integer> playerIdToMana = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> lastSpendTick = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> regenTickCounter = new ConcurrentHashMap<>();

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
		// запомним момент траты маны и сбросим счетчик регена
		lastSpendTick.put(id, currentServerTick);
		regenTickCounter.put(id, 0);
		// синхронизируем клиенту
		sendSync(player);
		return true;
	}

	public static void ensureInitialized(ServerPlayerEntity player) {
		playerIdToMana.putIfAbsent(player.getUuid(), DEFAULT_MANA);
	}

	// ===== Регистрация тиков сервера и регенерация =====
	private static long currentServerTick = 0L;

	public static void onServerTick(Iterable<ServerPlayerEntity> players) {
		currentServerTick++;
		for (ServerPlayerEntity player : players) {
			UUID id = player.getUuid();
			ensureInitialized(player);
			int mana = getMana(id);
			if (mana >= MAX_MANA) {
				continue;
			}
			long lastSpend = lastSpendTick.getOrDefault(id, -1L);
			boolean delayPassed = (lastSpend < 0) || (currentServerTick - lastSpend >= REGEN_DELAY_TICKS);
			if (!delayPassed) continue;
			int counter = regenTickCounter.getOrDefault(id, 0) + 1;
			if (counter >= TICKS_PER_1_MANA) {
				// прибавим 1 ману и сбросим счетчик
				counter -= TICKS_PER_1_MANA;
				setMana(id, mana + 1);
				sendSync(player);
			}
			regenTickCounter.put(id, counter);
		}
	}

	public static void sendSync(ServerPlayerEntity player) {
		ManaSyncS2CPacket pkt = new ManaSyncS2CPacket(getMana(player.getUuid()), MAX_MANA);
		ServerPlayNetworking.send(player, pkt);
	}
}


