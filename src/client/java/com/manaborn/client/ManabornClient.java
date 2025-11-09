package com.manaborn.client;

import com.manaborn.client.state.ClientManaState;
import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.client.ui.SpellTreeScreen;
import com.manaborn.client.ui.LearningAltarScreen;
import com.manaborn.network.ManaSyncS2CPacket;
import com.manaborn.network.LearnedSpellsSyncS2CPacket;
import com.manaborn.network.RequestLearnedSpellsC2SPacket;
import com.manaborn.ManabornMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class ManabornClient implements ClientModInitializer {
    private static boolean openMenuWasDown = false;

    @Override
    public void onInitializeClient() {
        // Регистрируем рендерер для ледяного осколка (КРИТИЧНО для предотвращения
        // краша!)
        // EntityRendererRegistry deprecated, но это всё ещё официальный способ для
        // Fabric 1.21.10
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

        // Регистрируем HUD элемент для отображения кулдауна выбранного заклинания (под
        // маной)
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of("manaborn", "cooldown_display"),
                CooldownHudOverlay::render);

        // Регистрируем HUD элемент для отображения прогресса стены льда
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HOTBAR,
                Identifier.of("manaborn", "ice_wall_progress"),
                IceWallProgressOverlay::render);

        // Открытие меню заклинаний по клавише I (edge-detection)

        // Обработчик пакета синхронизации маны
        ClientPlayNetworking.registerGlobalReceiver(ManaSyncS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> ClientManaState.set(payload.mana(), payload.maxMana()));
        });

        // Обработчик пакета синхронизации изученных заклинаний
        ClientPlayNetworking.registerGlobalReceiver(LearnedSpellsSyncS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> ClientLearnedSpellsState.setLearnedSpells(payload.spellIds()));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                ClientManaState.clientTick();
            }
            // Открытие меню заклинаний по клавише I
            if (client.player != null) {
                boolean isDown = InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_I);
                if (isDown && !openMenuWasDown && client.currentScreen == null) {
                    // Перед открытием запрашиваем актуальные изученные заклинания у сервера
                    ClientPlayNetworking.send(new RequestLearnedSpellsC2SPacket());
                    client.setScreen(new SpellTreeScreen());
                }
                openMenuWasDown = isDown;
            }
        });

        // Register client screens
        HandledScreens.register(ManabornMod.LEARNING_ALTAR_SH, LearningAltarScreen::new);
        HandledScreens.register(ManabornMod.MANA_CONVERTER_SH, com.manaborn.client.ui.ManaConverterScreen::new);

        // Предикат модели для сосуда временно отключён до стабилизации импорта
    }

    /**
     * Регистрирует рендерер для сущности ледяного осколка
     * 
     * Используем FlyingItemEntityRenderer для отображения снежка.
     * EntityRendererRegistry deprecated, но это всё ещё официальный способ для
     * Fabric 1.21.10
     * Новый API появится в будущих версиях Fabric API
     * но остается рабочим и официальным способом для версии 1.21.10.
     * Новый API для регистрации рендереров планируется в будущих версиях.
     */
    @SuppressWarnings("deprecation")
    private void registerIceShardRenderer() {
        EntityRendererRegistry.register(ManabornMod.ICE_SHARD_ENTITY, FlyingItemEntityRenderer::new);
    }
}
