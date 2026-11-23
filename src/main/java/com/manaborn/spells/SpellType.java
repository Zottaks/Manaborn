package com.manaborn.spells;

import net.minecraft.util.Formatting;

public enum SpellType {
    // Стоимости соответствуют getManaCost() классов-реализаций (уровень 1)
    FIREBALL("Огненный шар", 25, "fireball", Formatting.RED),
    ICE_SHARD("Ледяной осколок", 30, "ice_shard", Formatting.AQUA),
    FROST_COFFIN("Морозный гроб", 35, "frost_coffin", Formatting.AQUA),
    FIRE_WALL("Стена огня", 30, "fire_wall", Formatting.GOLD),
    INNER_HEAT("Внутренний жар", 25, "inner_heat", Formatting.GOLD),
    FROST_AURA("Аура Стужи", 30, "frost_aura", Formatting.AQUA),
    ABSORBING_BARRIER("Поглощающий барьер", 50, "absorbing_barrier", Formatting.DARK_PURPLE),
    WEAKNESS_CURSE("Проклятие Слабости", 25, "weakness_curse", Formatting.DARK_PURPLE),
    ICE_WALL("Стена льда", 10, "ice_wall", Formatting.AQUA),
    HEAT_WEAPON("Накаливание", 35, "heat_weapon", Formatting.GOLD),
    SOUND_STRIKE("Звуковой удар", 25, "sound_strike", Formatting.YELLOW),
    BATTLE_HYMN("Боевой гимн", 30, "battle_hymn", Formatting.YELLOW),
    SILENCE_ZONE("Область тишины", 80, "silence_zone", Formatting.GRAY),
    CLARIFYING_TONE("Проясняющий тон", 25, "clarifying_tone", Formatting.YELLOW),
    TRANSLOCATION("Транслокация", 30, "translocation", Formatting.GRAY);
    
    private final String displayName;
    private final int manaCost;
    private final String id;
    private final Formatting color;
    
    SpellType(String displayName, int manaCost, String id, Formatting color) {
        this.displayName = displayName;
        this.manaCost = manaCost;
        this.id = id;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getManaCost() {
        return manaCost;
    }
    
    public String getId() {
        return id;
    }
    
    public Formatting getColor() {
        return color;
    }
    
    public static SpellType fromId(String id) {
        for (SpellType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return FIREBALL; // по умолчанию
    }
}
