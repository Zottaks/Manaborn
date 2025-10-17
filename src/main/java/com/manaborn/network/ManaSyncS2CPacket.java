package com.manaborn.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record ManaSyncS2CPacket(int mana, int maxMana) {
	public static final Identifier ID = Identifier.of("manaborn", "mana_sync");

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


