package com.manaborn.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Пакет для отправки выбранного заклинания с клиента на сервер
 * Используется при выборе заклинания через радиальное меню
 */
public record SelectSpellC2SPacket(String spellId) implements CustomPayload {
    public static final CustomPayload.Id<SelectSpellC2SPacket> ID = new CustomPayload.Id<>(
            Identifier.of("manaborn", "select_spell"));

    public static final PacketCodec<RegistryByteBuf, SelectSpellC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, SelectSpellC2SPacket::spellId,
            SelectSpellC2SPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
