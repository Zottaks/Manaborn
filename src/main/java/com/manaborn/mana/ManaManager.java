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
        // Естественная регенерация отключена, но оставляем структуру данных нетронутой
        lastSpendTick.put(id, currentServerTick);
        regenTickCounter.put(id, 0);
        // синхронизируем клиенту
        sendSync(player);
        return true;
    }

    public static void addMana(ServerPlayerEntity player, int amount) {
        if (amount <= 0) return;
        UUID id = player.getUuid();
        int current = getMana(id);
        setMana(id, Math.min(MAX_MANA, current + amount));
        sendSync(player);
    }

    public static void ensureInitialized(ServerPlayerEntity player) {
        playerIdToMana.putIfAbsent(player.getUuid(), DEFAULT_MANA);
    }

    // ===== Регистрация тиков сервера и регенерация =====
    private static long currentServerTick = 0L;

    public static void onServerTick(Iterable<ServerPlayerEntity> players) {
        currentServerTick++;
        // Естественная регенерация отключена: только инициализация игроков и поддержка тиков
        for (ServerPlayerEntity player : players) {
            ensureInitialized(player);
        }
    }

    public static void sendSync(ServerPlayerEntity player) {
        ManaSyncS2CPacket pkt = new ManaSyncS2CPacket(getMana(player.getUuid()), MAX_MANA);
        ServerPlayNetworking.send(player, pkt);
    }
}
