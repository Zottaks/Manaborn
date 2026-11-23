package com.manaborn.spells;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Управление уровнями заклинаний игроков
 * Уровни: 1 (базовый), 2 (улучшенный), 3 (максимальный)
 * 
 * ВАЖНО: Для dev окружения уровни хранятся глобально в файле.
 */
public class SpellLevelManager {
    // SpellType -> уровень (1-3) - глобальное хранилище для dev
    private static final Map<SpellType, Integer> globalSpellLevels = new ConcurrentHashMap<>();
    
    // Путь к файлу сохранения
    private static final String SAVE_FILE = "manaborn_data/spell_levels.txt";
    private static boolean loaded = false;
    
    // Legacy: для совместимости (не используется в dev режиме)
    private static final Map<UUID, Map<SpellType, Integer>> spellLevels = new ConcurrentHashMap<>();
    
    /**
     * Загружает уровни заклинаний из файла при первом вызове
     */
    public static void loadFromDisk() {
        if (loaded) return;
        loaded = true;
        
        try {
            Path path = Paths.get(SAVE_FILE);
            if (!Files.exists(path)) {
                System.out.println("[Manaborn] Файл уровней заклинаний не найден, начинаем с базовых уровней");
                return;
            }
            
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && line.contains("=")) {
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        SpellType spell = SpellType.fromId(parts[0].trim());
                        int level = Integer.parseInt(parts[1].trim());
                        if (spell != null && level >= 1 && level <= 3) {
                            globalSpellLevels.put(spell, level);
                        }
                    }
                }
            }
            
            System.out.println("[Manaborn] Загружено " + globalSpellLevels.size() + " уровней заклинаний из файла");
        } catch (IOException e) {
            System.err.println("[Manaborn] Ошибка при загрузке уровней заклинаний: " + e.getMessage());
        }
    }
    
    /**
     * Сохраняет уровни заклинаний в файл
     */
    public static void saveToDisk() {
        try {
            Path path = Paths.get(SAVE_FILE);
            
            // Создаём директорию если не существует
            Files.createDirectories(path.getParent());
            
            // Записываем spell_id=level построчно
            java.util.List<String> lines = globalSpellLevels.entrySet().stream()
                .map(e -> e.getKey().getId() + "=" + e.getValue())
                .sorted()
                .toList();
            
            Files.write(path, lines);
            
            System.out.println("[Manaborn] Сохранено " + lines.size() + " уровней заклинаний в файл");
        } catch (IOException e) {
            System.err.println("[Manaborn] Ошибка при сохранении уровней заклинаний: " + e.getMessage());
        }
    }

    /**
     * Получить уровень заклинания (глобально для dev)
     * @return уровень от 1 до 3, по умолчанию 1
     */
    public static int getSpellLevel(UUID playerId, SpellType spell) {
        loadFromDisk(); // Загружаем при первом обращении
        return globalSpellLevels.getOrDefault(spell, 1);
    }
    
    /**
     * Установить уровень заклинания (глобально)
     */
    public static void setSpellLevel(UUID playerId, SpellType spell, int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Spell level must be between 1 and 3");
        }
        
        globalSpellLevels.put(spell, level);
        saveToDisk(); // Сохраняем сразу
    }
    
    /**
     * Улучшить заклинание на 1 уровень
     * @return true если улучшение успешно, false если уже максимальный уровень
     */
    public static boolean upgradeSpell(UUID playerId, SpellType spell) {
        int currentLevel = getSpellLevel(playerId, spell);
        if (currentLevel >= 3) {
            return false;
        }
        setSpellLevel(playerId, spell, currentLevel + 1);
        return true;
    }
    
    /**
     * Проверить, может ли игрок улучшить заклинание
     */
    public static boolean canUpgrade(UUID playerId, SpellType spell) {
        return getSpellLevel(playerId, spell) < 3;
    }
    
    /**
     * Сохранить уровни заклинаний в NBT
     */
    public static NbtCompound saveToNbt(UUID playerId) {
        NbtCompound nbt = new NbtCompound();
        Map<SpellType, Integer> levels = spellLevels.get(playerId);
        
        if (levels != null) {
            for (Map.Entry<SpellType, Integer> entry : levels.entrySet()) {
                nbt.putInt(entry.getKey().getId(), entry.getValue());
            }
        }
        
        return nbt;
    }
    
    /**
     * Загрузить уровни заклинаний из NBT
     */
    public static void loadFromNbt(UUID playerId, NbtCompound nbt) {
        Map<SpellType, Integer> levels = new HashMap<>();
        
        for (SpellType spell : SpellType.values()) {
            if (nbt.contains(spell.getId())) {
                levels.put(spell, nbt.getInt(spell.getId()).orElse(1));
            }
        }
        
        if (!levels.isEmpty()) {
            spellLevels.put(playerId, levels);
        }
    }
    
    /**
     * Синхронизировать уровни заклинаний с клиентом
     */
    public static void sendSync(ServerPlayerEntity player) {
        loadFromDisk(); // Убеждаемся что данные загружены
        
        // Используем globalSpellLevels (глобальное хранилище)
        Map<SpellType, Integer> levels = new HashMap<>(globalSpellLevels);
        
        // Отправляем пакет с уровнями заклинаний
        com.manaborn.network.SpellLevelSyncS2CPacket packet = 
            new com.manaborn.network.SpellLevelSyncS2CPacket(levels);
        
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, packet);
        
        System.out.println("[Manaborn] Синхронизация уровней заклинаний с клиентом: " + levels.size() + " записей");
    }
    
    /**
     * Очистить данные игрока.
     * В dev режиме хранилище глобальное — сбрасываем все уровни до базовых
     * (вызывается из /manaborn reset_spells).
     */
    public static void clearPlayer(UUID playerId) {
        globalSpellLevels.clear();
        saveToDisk();
    }
    
    /**
     * Получить все уровни заклинаний (глобально)
     */
    public static Map<SpellType, Integer> getAllLevels(UUID playerId) {
        loadFromDisk();
        return new HashMap<>(globalSpellLevels);
    }
}
