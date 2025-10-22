package com.manaborn.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.ArrayList;

/**
 * Пакет для синхронизации изученных заклинаний с клиентом
 * 
 * Отправляется с сервера клиенту при:
 * - Входе игрока на сервер
 * - Изучении нового заклинания
 */
public record LearnedSpellsSyncS2CPacket(List<String> spellIds) implements CustomPayload {
    public static final CustomPayload.Id<LearnedSpellsSyncS2CPacket> ID = 
        new CustomPayload.Id<>(Identifier.of("manaborn", "learned_spells_sync"));
    
    // Кодек для списка строк (ID заклинаний)
    public static final PacketCodec<RegistryByteBuf, LearnedSpellsSyncS2CPacket> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING),
            LearnedSpellsSyncS2CPacket::spellIds,
            LearnedSpellsSyncS2CPacket::new
        );
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
