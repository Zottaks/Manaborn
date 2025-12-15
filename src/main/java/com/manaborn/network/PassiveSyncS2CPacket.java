package com.manaborn.network;

import com.manaborn.passives.PassiveType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Синхронизация пассивок (изученные + 2 активных слота) Server -> Client.
 */
public record PassiveSyncS2CPacket(
        List<String> learnedIds,
        String slot0,
        String slot1
) implements CustomPayload {

    public static final Id<PassiveSyncS2CPacket> ID =
            new Id<>(Identifier.of("manaborn", "passive_sync"));

    public static final PacketCodec<RegistryByteBuf, PassiveSyncS2CPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING), PassiveSyncS2CPacket::learnedIds,
            PacketCodecs.STRING, PassiveSyncS2CPacket::slot0,
            PacketCodecs.STRING, PassiveSyncS2CPacket::slot1,
            PassiveSyncS2CPacket::new
    );

    public static PassiveSyncS2CPacket fromState(Collection<PassiveType> learned, PassiveType[] slots) {
        List<String> ids = new ArrayList<>();
        for (PassiveType type : learned) {
            ids.add(type.getId());
        }
        String s0 = slots != null && slots.length > 0 && slots[0] != null ? slots[0].getId() : "";
        String s1 = slots != null && slots.length > 1 && slots[1] != null ? slots[1].getId() : "";
        return new PassiveSyncS2CPacket(ids, s0, s1);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
