package com.manaborn.spells;

/**
 * Интерфейс для заклинаний с поддержкой уровней
 */
public interface LeveledSpell extends Spell {
    /**
     * Получить стоимость маны для определённого уровня
     */
    int getManaCost(int level);
    
    /**
     * Получить кулдаун для определённого уровня
     */
    int getCooldown(int level);
    
    /**
     * Проверка по умолчанию - все заклинания поддерживают уровни 1-3
     */
    default boolean supportsLevel(int level) {
        return level >= 1 && level <= 3;
    }
}
