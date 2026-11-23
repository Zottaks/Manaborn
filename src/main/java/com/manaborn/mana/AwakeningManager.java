package com.manaborn.mana;

import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * «Пробуждение магии» — состояние игрока.
 *
 * Пока игрок не прочитал Книгу Пробуждения, у него нет маны (максимум 0)
 * и он не может изучать заклинания. После прочтения максимум маны
 * становится {@link #AWAKENED_MAX_MANA}.
 *
 * Список пробуждённых хранится в manaborn_data/awakened.txt.
 */
public final class AwakeningManager {

    /** Максимум маны сразу после пробуждения */
    public static final int AWAKENED_MAX_MANA = 15;

    /** Абсолютный предел личного максимума маны */
    public static final int ABSOLUTE_MAX_MANA = 100;

    private static final String SAVE_FILE = "manaborn_data/awakened.txt";

    private static final Set<UUID> awakened = ConcurrentHashMap.newKeySet();
    /** Бонус к максимуму маны (маназитные катализаторы) */
    private static final Map<UUID, Integer> manaBonus = new ConcurrentHashMap<>();

    private AwakeningManager() {}

    public static boolean isAwakened(UUID playerId) {
        return awakened.contains(playerId);
    }

    /** Текущий личный максимум маны игрока (0 до пробуждения) */
    public static int getMaxMana(UUID playerId) {
        if (!isAwakened(playerId)) {
            return 0;
        }
        return Math.min(ABSOLUTE_MAX_MANA, AWAKENED_MAX_MANA + manaBonus.getOrDefault(playerId, 0));
    }

    /**
     * Увеличить личный максимум маны. Возвращает новый максимум
     * или -1, если игрок не пробуждён, или 0 при достигнутом пределе.
     */
    public static int addMaxManaBonus(ServerPlayerEntity player, int amount) {
        UUID id = player.getUuid();
        if (!isAwakened(id)) {
            return -1;
        }
        int current = getMaxMana(id);
        if (current >= ABSOLUTE_MAX_MANA) {
            return 0;
        }
        int add = Math.min(amount, ABSOLUTE_MAX_MANA - current);
        manaBonus.put(id, manaBonus.getOrDefault(id, 0) + add);
        saveToDisk();
        return getMaxMana(id);
    }

    /** Пробудить магию игрока (идемпотентно) */
    public static void awaken(ServerPlayerEntity player) {
        if (awakened.add(player.getUuid())) {
            saveToDisk();
        }
    }

    // ── Персистентность ──────────────────────────────────────────────────

    public static void loadFromDisk() {
        awakened.clear();
        manaBonus.clear();
        try {
            Path path = Paths.get(SAVE_FILE);
            if (!Files.exists(path)) {
                return;
            }
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Формат: "uuid" или "uuid;bonus"
                String[] parts = line.split(";");
                try {
                    UUID id = UUID.fromString(parts[0]);
                    awakened.add(id);
                    if (parts.length > 1) {
                        manaBonus.put(id, Integer.parseInt(parts[1]));
                    }
                } catch (IllegalArgumentException ignored) {
                    // пропускаем битые строки
                }
            }
        } catch (IOException e) {
            System.err.println("[Manaborn] Не удалось загрузить список пробуждённых: " + e.getMessage());
        }
    }

    public static void saveToDisk() {
        try {
            Path path = Paths.get(SAVE_FILE);
            Files.createDirectories(path.getParent());
            List<String> lines = new ArrayList<>();
            for (UUID id : awakened) {
                int bonus = manaBonus.getOrDefault(id, 0);
                lines.add(bonus > 0 ? id + ";" + bonus : id.toString());
            }
            Files.write(path, lines);
        } catch (IOException e) {
            System.err.println("[Manaborn] Не удалось сохранить список пробуждённых: " + e.getMessage());
        }
    }
}
