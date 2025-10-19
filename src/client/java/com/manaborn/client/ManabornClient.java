package com.manaborn.client;

import com.manaborn.client.state.ClientManaState;
import com.manaborn.network.ManaSyncS2CPacket;
import com.manaborn.ManabornMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.util.Identifier;

public class ManabornClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Регистрируем рендерер для ледяного осколка (КРИТИЧНО для предотвращения краша!)
		// EntityRendererRegistry deprecated, но это всё ещё официальный способ для Fabric 1.21.10
		// Новый API появится в будущих версиях Fabric API
		registerIceShardRenderer();
		
		// Регистрируем HUD элемент для отображения маны перед чатом
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.of("manaborn", "mana_display"),
				ManaHudOverlay::render);
		
		// Регистрируем HUD элемент для отображения выбранного заклинания
		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.of("manaborn", "spell_display"),
				SpellHudOverlay::render);

		ClientPlayNetworking.registerGlobalReceiver(ManaSyncS2CPacket.ID, (payload, context) -> {
			context.client().execute(() -> ClientManaState.set(payload.mana(), payload.maxMana()));
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				ClientManaState.clientTick();
			}
		});
	}
	
	/**
	 * Регистрирует рендерер для сущности ледяного осколка
	 * 
	 * Используем FlyingItemEntityRenderer для отображения снежка.
	 * EntityRendererRegistry помечен как deprecated в Fabric API,
	 * но остается рабочим и официальным способом для версии 1.21.10.
	 * Новый API для регистрации рендереров планируется в будущих версиях.
	 */
	@SuppressWarnings("deprecation")
	private void registerIceShardRenderer() {
		EntityRendererRegistry.register(ManabornMod.ICE_SHARD_ENTITY, FlyingItemEntityRenderer::new);
	}
}
