package com.manaborn.spells;

public enum SpellType {
    FIREBALL("Огненный шар", 20, "fireball"),
    ICE_SHARD("Ледяной осколок", 35, "ice_shard");
    
    private final String displayName;
    private final int manaCost;
    private final String id;
    
    SpellType(String displayName, int manaCost, String id) {
        this.displayName = displayName;
        this.manaCost = manaCost;
        this.id = id;
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
    
    public static SpellType fromId(String id) {
        for (SpellType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return FIREBALL; // по умолчанию
    }
}

