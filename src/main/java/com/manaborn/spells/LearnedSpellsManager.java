package com.manaborn.spells;

import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.manaborn.network.LearnedSpellsSyncS2CPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управляет изученными заклинаниями игроков
 * 
 * Хранит информацию о том, какие заклинания изучил каждый игрок.
 * По умолчанию игроки не знают никаких заклинаний.
 */
public class LearnedSpellsManager {
    // Хранилище изученных заклинаний: UUID игрока -> Set заклинаний
    private static final Map<UUID, Set<SpellType>> learnedSpells = new ConcurrentHashMap<>();
    
    /**
     * Инициализирует игрока с пустым набором заклинаний
     */
    public static void ensureInitialized(ServerPlayerEntity player) {
        learnedSpells.putIfAbsent(player.getUuid(), ConcurrentHashMap.newKeySet());
    }
    
    /**
     * Проверяет, изучил ли игрок заклинание
     */
    public static boolean hasLearned(UUID playerId, SpellType spell) {
        Set<SpellType> spells = learnedSpells.get(playerId);
        return spells != null && spells.contains(spell);
    }
    
    /**
     * Изучает заклинание для игрока
     * @return true если заклинание было изучено (не было изучено ранее)
     */
    public static boolean learnSpell(ServerPlayerEntity player, SpellType spell) {
        ensureInitialized(player);
        Set<SpellType> spells = learnedSpells.get(player.getUuid());
        boolean learned = spells.add(spell);
        
        if (learned) {
            sendSync(player);
        }
        
        return learned;
    }
    
    /**
     * Получает все изученные заклинания игрока
     */
    public static Set<SpellType> getLearnedSpells(UUID playerId) {
        return learnedSpells.getOrDefault(playerId, Collections.emptySet());
    }
    
    /**
     * Синхронизирует изученные заклинания с клиентом
     */
    public static void sendSync(ServerPlayerEntity player) {
        Set<SpellType> spells = getLearnedSpells(player.getUuid());
        List<String> spellIds = spells.stream()
            .map(SpellType::getId)
            .toList();
        
        ServerPlayNetworking.send(player, new LearnedSpellsSyncS2CPacket(spellIds));
    }
    
    /**
     * Очищает данные игрока (для отладки)
     */
    public static void clearPlayer(UUID playerId) {
        learnedSpells.remove(playerId);
    }
}
