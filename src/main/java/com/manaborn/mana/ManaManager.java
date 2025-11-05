package com.manaborn.mana;

import com.manaborn.network.ManaSyncS2CPacket;
import com.manaborn.items.ManaVesselItem;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ManaManager {
    public static final int MAX_MANA = 100;
    private static final int DEFAULT_MANA = MAX_MANA;
    private static final int VESSEL_REFILL_PER_TICK = 1; // 1 мана/тик (20 в сек.)

    

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
        // Естественная регенерация отключена. Восполняем ТОЛЬКО из сосуда, если есть заряд.
        for (ServerPlayerEntity player : players) {
            ensureInitialized(player);
            UUID id = player.getUuid();
            int current = getMana(id);
            if (current >= MAX_MANA) {
                continue;
            }

            // Ищем сосуд с зарядом в инвентаре/второй руке
            ItemStack vessel = findVesselWithCharge(player);
            if (vessel.isEmpty()) {
                continue; // нет сосуда или в нём 0 — мана не восстанавливается
            }
            int stored = ManaVesselItem.getStored(vessel);
            if (stored <= 0) {
                continue;
            }

            int need = MAX_MANA - current;
            int move = Math.min(VESSEL_REFILL_PER_TICK, Math.min(need, stored));
            if (move > 0) {
                ManaVesselItem.setStored(vessel, stored - move);
                setMana(id, current + move);
                sendSync(player);
            }
        }
    }

    private static ItemStack findVesselWithCharge(ServerPlayerEntity player) {
        // Offhand first (быстрый доступ)
        ItemStack off = player.getOffHandStack();
        if (!off.isEmpty() && off.getItem() instanceof ManaVesselItem && ManaVesselItem.getStored(off) > 0) {
            return off;
        }
        // Затем — основной инвентарь
        PlayerInventory inv = player.getInventory();
        int size = inv.size();
        for (int i = 0; i < size; i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof ManaVesselItem && ManaVesselItem.getStored(s) > 0) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void sendSync(ServerPlayerEntity player) {
        ManaSyncS2CPacket pkt = new ManaSyncS2CPacket(getMana(player.getUuid()), MAX_MANA);
        ServerPlayNetworking.send(player, pkt);
    }
}
