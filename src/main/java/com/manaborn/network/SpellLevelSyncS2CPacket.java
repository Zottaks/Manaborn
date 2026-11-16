package com.manaborn.network;

import com.manaborn.spells.SpellType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Пакет для синхронизации уровней заклинаний с клиентом (Server -> Client)
 */
public record SpellLevelSyncS2CPacket(
    Map<SpellType, Integer> levels
) implements CustomPayload {
    
    public static final Id<SpellLevelSyncS2CPacket> ID = 
        new Id<>(Identifier.of("manaborn", "spell_level_sync"));
    
    public static final PacketCodec<RegistryByteBuf, SpellLevelSyncS2CPacket> CODEC = 
        PacketCodec.of(
            (packet, buf) -> write(buf, packet),
            SpellLevelSyncS2CPacket::read
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    private static void write(RegistryByteBuf buf, SpellLevelSyncS2CPacket packet) {
        // Записываем количество заклинаний
        buf.writeVarInt(packet.levels.size());
        
        // Записываем каждое заклинание и его уровень
        for (Map.Entry<SpellType, Integer> entry : packet.levels.entrySet()) {
            buf.writeString(entry.getKey().getId());
            buf.writeVarInt(entry.getValue());
        }
    }
    
    private static SpellLevelSyncS2CPacket read(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        Map<SpellType, Integer> levels = new HashMap<>();
        
        for (int i = 0; i < size; i++) {
            String spellId = buf.readString();
            int level = buf.readVarInt();
            
            SpellType spell = SpellType.fromId(spellId);
            if (spell != null) {
                levels.put(spell, level);
            }
        }
        
        return new SpellLevelSyncS2CPacket(levels);
    }
}
