package com.manaborn.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Обновление двух активных слотов пассивок (Client -> Server).
 */
public record PassiveSlotsUpdateC2SPacket(
        String slot0,
        String slot1
) implements CustomPayload {

    public static final Id<PassiveSlotsUpdateC2SPacket> ID =
            new Id<>(Identifier.of("manaborn", "passive_slots_update"));

    public static final PacketCodec<RegistryByteBuf, PassiveSlotsUpdateC2SPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, PassiveSlotsUpdateC2SPacket::slot0,
            PacketCodecs.STRING, PassiveSlotsUpdateC2SPacket::slot1,
            PassiveSlotsUpdateC2SPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
