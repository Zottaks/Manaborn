package com.manaborn.spells;

import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.manaborn.network.LearnedSpellsSyncS2CPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Управляет изученными заклинаниями игроков (пока в памяти, до внедрения персистентности)
 */
public class LearnedSpellsManager {
    // UUID игрока -> Set изученных заклинаний
    private static final Map<UUID, Set<SpellType>> learnedSpells = new ConcurrentHashMap<>();

    public static void ensureInitialized(ServerPlayerEntity player) {
        learnedSpells.putIfAbsent(player.getUuid(), ConcurrentHashMap.newKeySet());
    }

    public static boolean hasLearned(UUID playerId, SpellType spell) {
        Set<SpellType> spells = learnedSpells.get(playerId);
        return spells != null && spells.contains(spell);
    }

    public static boolean learnSpell(ServerPlayerEntity player, SpellType spell) {
        ensureInitialized(player);
        Set<SpellType> spells = learnedSpells.get(player.getUuid());
        boolean learned = spells.add(spell);
        if (learned) {
            sendSync(player);
        }
        return learned;
    }

    public static Set<SpellType> getLearnedSpells(UUID playerId) {
        return learnedSpells.getOrDefault(playerId, Collections.emptySet());
    }

    public static void sendSync(ServerPlayerEntity player) {
        Set<SpellType> spells = getLearnedSpells(player.getUuid());
        List<String> spellIds = spells.stream().map(SpellType::getId).toList();
        ServerPlayNetworking.send(player, new LearnedSpellsSyncS2CPacket(spellIds));
    }

    public static void clearPlayer(UUID playerId) {
        learnedSpells.remove(playerId);
    }
}
