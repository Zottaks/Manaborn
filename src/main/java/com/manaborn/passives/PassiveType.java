package com.manaborn.passives;

import net.minecraft.util.Formatting;

public enum PassiveType {
    // Магия тьмы
    SHADOW_SHELTER("Тень укрытия", "shadow_shelter", Formatting.DARK_PURPLE),
    
    // Магия огня
    COAL_HEAT("Жар углей", "coal_heat", Formatting.GOLD),
    
    // Магия льда
    FROST_SKIN("Иней на коже", "frost_skin", Formatting.AQUA),
    REACTIVE_FROST("Лёд реакции", "reactive_frost", Formatting.AQUA);
    
    private final String displayName;
    private final String id;
    private final Formatting color;
    
    PassiveType(String displayName, String id, Formatting color) {
        this.displayName = displayName;
        this.id = id;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getId() {
        return id;
    }
    
    public Formatting getColor() {
        return color;
    }
    
    public static PassiveType fromId(String id) {
        for (PassiveType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
