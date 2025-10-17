package com.manaborn.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ManaSyncS2CPacket(int mana, int maxMana) implements CustomPayload {
    public static final CustomPayload.Id<ManaSyncS2CPacket> ID = new CustomPayload.Id<>(Identifier.of("manaborn", "mana_sync"));
    public static final PacketCodec<RegistryByteBuf, ManaSyncS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, ManaSyncS2CPacket::mana,
            PacketCodecs.VAR_INT, ManaSyncS2CPacket::maxMana,
            ManaSyncS2CPacket::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(mana);
        buf.writeVarInt(maxMana);
    }

    public static ManaSyncS2CPacket read(PacketByteBuf buf) {
        int mana = buf.readVarInt();
        int max = buf.readVarInt();
        return new ManaSyncS2CPacket(mana, max);
    }
}


