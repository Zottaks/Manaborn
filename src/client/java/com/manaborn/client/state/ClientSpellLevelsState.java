package com.manaborn.client.state;

import com.manaborn.spells.SpellType;
import java.util.HashMap;
import java.util.Map;

/**
 * Клиентское состояние для хранения уровней заклинаний
 */
public class ClientSpellLevelsState {
    private static final Map<SpellType, Integer> spellLevels = new HashMap<>();
    
    /**
     * Получить уровень заклинания (по умолчанию 1)
     */
    public static int getSpellLevel(SpellType spell) {
        return spellLevels.getOrDefault(spell, 1);
    }
    
    /**
     * Установить уровни всех заклинаний (синхронизация с сервером)
     */
    public static void setLevels(Map<SpellType, Integer> levels) {
        spellLevels.clear();
        spellLevels.putAll(levels);
    }
    
    /**
     * Установить уровень одного заклинания
     */
    public static void setSpellLevel(SpellType spell, int level) {
        spellLevels.put(spell, level);
    }
    
    /**
     * Очистить все данные (при выходе из мира)
     */
    public static void clear() {
        spellLevels.clear();
    }
    
    /**
     * Получить копию всех уровней
     */
    public static Map<SpellType, Integer> getAllLevels() {
        return new HashMap<>(spellLevels);
    }
}
