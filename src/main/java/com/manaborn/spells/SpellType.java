package com.manaborn.spells;

import net.minecraft.util.Formatting;

public enum SpellType {
    FIREBALL("Огненный шар", 20, "fireball", Formatting.RED),
    ICE_SHARD("Ледяной осколок", 35, "ice_shard", Formatting.AQUA);
    
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

