package com.manaborn.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Пустой пакет-запрос с клиента на сервер для принудительной
 * пересинхронизации изученных заклинаний.
 */
public record RequestLearnedSpellsC2SPacket() implements CustomPayload {
    public static final CustomPayload.Id<RequestLearnedSpellsC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of("manaborn", "request_learned_spells"));

    // Кодек без данных
    public static final PacketCodec<RegistryByteBuf, RequestLearnedSpellsC2SPacket> CODEC =
            PacketCodec.unit(new RequestLearnedSpellsC2SPacket());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
