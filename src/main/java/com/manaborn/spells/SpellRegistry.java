package com.manaborn.spells;

import com.manaborn.spells.impl.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all spell instances
 */
public class SpellRegistry {
    private static final Map<SpellType, Spell> spells = new HashMap<>();

    static {
        // Register all spell implementations
        register(SpellType.FIREBALL, new FireballSpell());
        register(SpellType.ICE_SHARD, new IceShardSpell());
        register(SpellType.FIRE_WALL, new FireWallSpell());
        register(SpellType.INNER_HEAT, new InnerHeatSpell());
        register(SpellType.FROST_AURA, new FrostAuraSpell());
        register(SpellType.ABSORBING_BARRIER, new AbsorbingBarrierSpell());
        register(SpellType.WEAKNESS_CURSE, new WeaknessCurseSpell());
        register(SpellType.ICE_WALL, new IceWallSpell());
        register(SpellType.HEAT_WEAPON, new HeatWeaponSpell());
        register(SpellType.SOUND_STRIKE, new SoundStrikeSpell());
        register(SpellType.BATTLE_HYMN, new BattleHymnSpell());
        register(SpellType.SILENCE_ZONE, new SilenceZoneSpell());
    }

    private static void register(SpellType type, Spell spell) {
        spells.put(type, spell);
    }

    /**
     * Get spell instance by type
     */
    public static Spell getSpell(SpellType type) {
        return spells.get(type);
    }

    /**
     * Get all registered spells
     */
    public static Map<SpellType, Spell> getAllSpells() {
        return new HashMap<>(spells);
    }

    /**
     * Check if a spell type is registered
     */
    public static boolean hasSpell(SpellType type) {
        return spells.containsKey(type);
    }
}
