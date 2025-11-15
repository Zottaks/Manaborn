package com.manaborn.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Пакет для запроса улучшения заклинания (Client -> Server)
 */
public record SpellUpgradeC2SPacket(
    String spellId
) implements CustomPayload {
    
    public static final Id<SpellUpgradeC2SPacket> ID = 
        new Id<>(Identifier.of("manaborn", "spell_upgrade"));
    
    public static final PacketCodec<RegistryByteBuf, SpellUpgradeC2SPacket> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.STRING, SpellUpgradeC2SPacket::spellId,
            SpellUpgradeC2SPacket::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
