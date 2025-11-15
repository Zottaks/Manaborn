package com.manaborn.spells;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управление уровнями заклинаний игроков
 * Уровни: 1 (базовый), 2 (улучшенный), 3 (максимальный)
 */
public class SpellLevelManager {
    // UUID игрока -> SpellType -> уровень (1-3)
    private static final Map<UUID, Map<SpellType, Integer>> spellLevels = new ConcurrentHashMap<>();
    
    /**
     * Получить уровень заклинания у игрока
     * @return уровень от 1 до 3, по умолчанию 1
     */
    public static int getSpellLevel(UUID playerId, SpellType spell) {
        return spellLevels
            .getOrDefault(playerId, new HashMap<>())
            .getOrDefault(spell, 1); // По умолчанию 1 уровень
    }
    
    /**
     * Установить уровень заклинания
     */
    public static void setSpellLevel(UUID playerId, SpellType spell, int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Spell level must be between 1 and 3");
        }
        
        spellLevels.computeIfAbsent(playerId, k -> new HashMap<>()).put(spell, level);
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
        Map<SpellType, Integer> levels = spellLevels.getOrDefault(player.getUuid(), new HashMap<>());
        
        // Отправляем пакет с уровнями заклинаний
        com.manaborn.network.SpellLevelSyncS2CPacket packet = 
            new com.manaborn.network.SpellLevelSyncS2CPacket(levels);
        
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, packet);
    }
    
    /**
     * Очистить данные игрока (при выходе)
     */
    public static void clearPlayer(UUID playerId) {
        spellLevels.remove(playerId);
    }
    
    /**
     * Получить все уровни заклинаний игрока
     */
    public static Map<SpellType, Integer> getAllLevels(UUID playerId) {
        return new HashMap<>(spellLevels.getOrDefault(playerId, new HashMap<>()));
    }
}
