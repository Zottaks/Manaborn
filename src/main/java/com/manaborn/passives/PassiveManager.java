package com.manaborn.passives;

import com.manaborn.network.PassiveSyncS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Управляет пассивками игроков: изученные пассивки и два активных слота.
 */
public class PassiveManager {
    private static final Map<UUID, Set<PassiveType>> learnedPassives = new ConcurrentHashMap<>();
    private static final Map<UUID, PassiveType[]> activeSlots = new ConcurrentHashMap<>();

    public static Set<PassiveType> getLearned(UUID playerId) {
        return EnumSet.copyOf(learnedPassives.getOrDefault(playerId, EnumSet.noneOf(PassiveType.class)));
    }

    public static PassiveType[] getActiveSlots(UUID playerId) {
        PassiveType[] slots = activeSlots.get(playerId);
        if (slots == null) {
            slots = new PassiveType[2];
            activeSlots.put(playerId, slots);
        }
        return slots.clone();
    }

    public static boolean learnPassive(ServerPlayerEntity player, PassiveType passive) {
        UUID id = player.getUuid();
        Set<PassiveType> set = learnedPassives.computeIfAbsent(id, k -> EnumSet.noneOf(PassiveType.class));
        boolean added = set.add(passive);
        if (added) {
            sendSync(player);
        }
        return added;
    }

    public static void setActiveSlots(ServerPlayerEntity player, PassiveType slot0, PassiveType slot1) {
        UUID id = player.getUuid();
        Set<PassiveType> learned = learnedPassives.computeIfAbsent(id, k -> EnumSet.noneOf(PassiveType.class));
        PassiveType[] slots = activeSlots.computeIfAbsent(id, k -> new PassiveType[2]);

        // Валидируем, что пассивки изучены (или null)
        if (slot0 != null && !learned.contains(slot0)) slot0 = null;
        if (slot1 != null && !learned.contains(slot1)) slot1 = null;

        slots[0] = slot0;
        slots[1] = slot1;

        saveToDisk(id);

        sendSync(player);
    }

    public static void clearPlayer(UUID playerId) {
        learnedPassives.remove(playerId);
        activeSlots.remove(playerId);
    }

    // === NBT сохранение/загрузка (персонально на игрока) ===

    public static NbtCompound saveToNbt(UUID playerId) {
        NbtCompound nbt = new NbtCompound();
        Set<PassiveType> learned = learnedPassives.get(playerId);
        if (learned != null && !learned.isEmpty()) {
            int i = 0;
            for (PassiveType type : learned) {
                nbt.putString("learned_" + i, type.getId());
                i++;
            }
            nbt.putInt("learned_count", i);
        }

        PassiveType[] slots = activeSlots.get(playerId);
        if (slots != null) {
            if (slots[0] != null) nbt.putString("slot0", slots[0].getId());
            if (slots[1] != null) nbt.putString("slot1", slots[1].getId());
        }
        return nbt;
    }

    public static void loadFromNbt(UUID playerId, NbtCompound nbt) {
        Set<PassiveType> learned = EnumSet.noneOf(PassiveType.class);
        int count = nbt.getInt("learned_count").orElse(0);
        for (int i = 0; i < count; i++) {
            String id = nbt.getString("learned_" + i).orElse("");
            PassiveType type = PassiveType.fromId(id);
            if (type != null) learned.add(type);
        }
        if (!learned.isEmpty()) {
            learnedPassives.put(playerId, learned);
        }

        PassiveType[] slots = new PassiveType[2];
        String s0 = nbt.getString("slot0").orElse("");
        String s1 = nbt.getString("slot1").orElse("");
        if (!s0.isEmpty()) slots[0] = PassiveType.fromId(s0);
        if (!s1.isEmpty()) slots[1] = PassiveType.fromId(s1);
        activeSlots.put(playerId, slots);
    }

    // === Синхронизация с клиентом ===

    public static void sendSync(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        Set<PassiveType> learned = learnedPassives.getOrDefault(id, EnumSet.noneOf(PassiveType.class));
        PassiveType[] slots = activeSlots.getOrDefault(id, new PassiveType[2]);

        PassiveSyncS2CPacket packet = PassiveSyncS2CPacket.fromState(learned, slots);
        ServerPlayNetworking.send(player, packet);
    }

    public static void loadOrInit(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        boolean loaded = loadFromDisk(id);
        if (!loaded) {
            // Для старта/теста: выдаём все пассивки, чтобы их можно было выбрать в UI
            Set<PassiveType> set = EnumSet.noneOf(PassiveType.class);
            set.addAll(EnumSet.allOf(PassiveType.class));
            learnedPassives.put(id, set);
            activeSlots.put(id, new PassiveType[2]);
            saveToDisk(id);
        }
    }

    private static Path getSavePath(UUID playerId) {
        return Paths.get("manaborn_data", "passives_" + playerId + ".txt");
    }

    private static boolean loadFromDisk(UUID playerId) {
        try {
            Path path = getSavePath(playerId);
            if (!Files.exists(path)) {
                return false;
            }
            List<String> lines = Files.readAllLines(path);
            Set<PassiveType> learned = EnumSet.noneOf(PassiveType.class);
            PassiveType[] slots = new PassiveType[2];

            for (String line : lines) {
                if (line == null) continue;
                String v = line.trim();
                if (v.isEmpty()) continue;

                if (v.startsWith("slot0=")) {
                    String id = v.substring("slot0=".length());
                    slots[0] = id.isEmpty() ? null : PassiveType.fromId(id);
                    continue;
                }
                if (v.startsWith("slot1=")) {
                    String id = v.substring("slot1=".length());
                    slots[1] = id.isEmpty() ? null : PassiveType.fromId(id);
                    continue;
                }

                PassiveType type = PassiveType.fromId(v);
                if (type != null) {
                    learned.add(type);
                }
            }

            learnedPassives.put(playerId, learned);
            activeSlots.put(playerId, slots);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void saveToDisk(UUID playerId) {
        try {
            Path path = getSavePath(playerId);
            Files.createDirectories(path.getParent());

            List<String> out = new ArrayList<>();
            PassiveType[] slots = activeSlots.getOrDefault(playerId, new PassiveType[2]);
            out.add("slot0=" + (slots[0] != null ? slots[0].getId() : ""));
            out.add("slot1=" + (slots[1] != null ? slots[1].getId() : ""));

            Set<PassiveType> learned = learnedPassives.getOrDefault(playerId, EnumSet.noneOf(PassiveType.class));
            for (PassiveType type : learned) {
                out.add(type.getId());
            }
            Files.write(path, out);
        } catch (Exception ignored) {
        }
    }

    // saveToDisk вызывается при смене слотов
    public static void persist(UUID playerId) {
        saveToDisk(playerId);
    }
}
