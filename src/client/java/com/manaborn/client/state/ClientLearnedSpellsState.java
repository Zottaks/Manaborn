package com.manaborn.client.state;

import com.manaborn.spells.SpellType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Клиентское состояние изученных заклинаний
 * 
 * Хранит информацию о заклинаниях, которые изучил текущий игрок.
 * Синхронизируется с сервером через пакеты.
 */
public class ClientLearnedSpellsState {
    private static final Set<SpellType> learnedSpells = new HashSet<>();
    
    /**
     * Устанавливает изученные заклинания из пакета
     */
    public static void setLearnedSpells(List<String> spellIds) {
        learnedSpells.clear();
        for (String id : spellIds) {
            SpellType spell = SpellType.fromId(id);
            learnedSpells.add(spell);
        }
    }
    
    /**
     * Проверяет, изучено ли заклинание
     */
    public static boolean hasLearned(SpellType spell) {
        return learnedSpells.contains(spell);
    }
    
    /**
     * Получает все изученные заклинания
     */
    public static Set<SpellType> getLearnedSpells() {
        return new HashSet<>(learnedSpells);
    }
    
    /**
     * Очищает состояние (при выходе с сервера)
     */
    public static void clear() {
        learnedSpells.clear();
    }
}
