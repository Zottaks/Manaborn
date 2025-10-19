package com.manaborn.client;

import com.manaborn.client.state.ClientManaState;
import com.manaborn.network.ManaSyncS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ManabornClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudRenderCallback.EVENT.register(new ManaHudOverlay());
		HudRenderCallback.EVENT.register(new SpellHudOverlay());
		ClientPlayNetworking.registerGlobalReceiver(ManaSyncS2CPacket.ID, (payload, context) -> {
			context.client().execute(() -> ClientManaState.set(payload.mana(), payload.maxMana()));
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				ClientManaState.clientTick();
			}
		});
	}
}


