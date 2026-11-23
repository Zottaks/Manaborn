package com.manaborn.client;

import com.manaborn.client.state.ClientManaState;
import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.client.state.ClientSpellLevelsState;
import com.manaborn.items.MagicStaffItem;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import com.manaborn.client.ui.SpellTreeScreen;
import com.manaborn.client.ui.LearningAltarScreen;
import com.manaborn.client.ui.SpellEnhancementAltarScreen;
import com.manaborn.network.ManaSyncS2CPacket;
import com.manaborn.network.LearnedSpellsSyncS2CPacket;
import com.manaborn.network.RequestLearnedSpellsC2SPacket;
import com.manaborn.network.PassiveSyncS2CPacket;
import com.manaborn.client.state.ClientPassivesState;
import com.manaborn.ManabornMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;

public class ManabornClient implements ClientModInitializer {
    private static boolean openMenuWasDown = false;
    private static int clarifyingTonePreviewTick = 0;

    @Override
    public void onInitializeClient() {
        // Регистрируем клавиши управления
        KeyBindings.register();

        // Регистрируем открытие радиального меню по Shift + ПКМ
        RadialMenuHandler.register();

        // Регистрируем рендерер для ледяного осколка (КРИТИЧНО для предотвращения
        // краша!)
        // EntityRendererRegistry deprecated, но это всё ещё официальный способ для
        // Fabric 1.21.10
        // Новый API появится в будущих версиях Fabric API
        registerIceShardRenderer();
        registerDustWispRenderer();
        registerMageMerchantModelLayer();
        registerMageMerchantRenderer();

        // Квадрат прогресса создания пыли на верхней грани магнита маны
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                ManabornMod.MANA_MAGNET_ENTITY, ManaMagnetProgressRenderer::new);

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

        // Регистрируем HUD элемент для отображения прогресса транслокации
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.HOTBAR,
                Identifier.of("manaborn", "translocation_progress"),
                TranslocationProgressOverlay::render);

        // Открытие меню заклинаний по клавише I (edge-detection)

        // Обработчик пакета синхронизации маны
        ClientPlayNetworking.registerGlobalReceiver(ManaSyncS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> ClientManaState.set(payload.mana(), payload.maxMana()));
        });

        // Обработчик пакета синхронизации изученных заклинаний
        ClientPlayNetworking.registerGlobalReceiver(LearnedSpellsSyncS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> ClientLearnedSpellsState.setLearnedSpells(payload.spellIds()));
        });
        
        // Обработчик пакета синхронизации уровней заклинаний
        ClientPlayNetworking.registerGlobalReceiver(com.manaborn.network.SpellLevelSyncS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> ClientSpellLevelsState.setLevels(payload.levels()));
        });

        ClientPlayNetworking.registerGlobalReceiver(PassiveSyncS2CPacket.ID, (payload, context) -> {
            context.client().execute(() -> ClientPassivesState.setFromPacket(payload.learnedIds(), payload.slot0(), payload.slot1()));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                ClientManaState.clientTick();
            }
            // Отрисовка частиц пылинок (видны только с Маноскопом)
            DustWispParticleRenderer.tick(client);
            
            // Превью радиуса AoE для заклинания Проясняющий тон при зажатом Shift
            if (client.player != null && client.world != null) {
                clarifyingTonePreviewTick++;

                ItemStack mainHand = client.player.getMainHandStack();
                ItemStack offHand = client.player.getOffHandStack();

                ItemStack staff = null;
                if (mainHand.getItem() instanceof MagicStaffItem) {
                    staff = mainHand;
                } else if (offHand.getItem() instanceof MagicStaffItem) {
                    staff = offHand;
                }

                if (staff != null && client.currentScreen == null) {
                    // Проверяем, что выбран именно Проясняющий тон
                    SpellType selected = SpellManager.getSelectedSpell(staff);
                    if (selected == SpellType.CLARIFYING_TONE) {
                        // Проверяем, что Shift зажат
                        boolean isShiftDown = GLFW.glfwGetKey(client.getWindow().getHandle(),
                                GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                                GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

                        // Рисуем кольцо только раз в несколько тиков, чтобы не оставлять плотный след при движении
                        if (isShiftDown && (clarifyingTonePreviewTick % 4 == 0)) {
                            int level = ClientSpellLevelsState.getSpellLevel(SpellType.CLARIFYING_TONE);
                            int adjustedLevel = Math.max(1, Math.min(3, level));

                            double radius = switch (adjustedLevel) {
                                case 1 -> 2.0;
                                case 2 -> 3.0;
                                default -> 5.0;
                            };

                            // Рисуем кольцо частиц вокруг игрока
                            double centerX = client.player.getX();
                            double centerY = client.player.getY() + 0.1; // чуть выше ног
                            double centerZ = client.player.getZ();

                            int points = 40;
                            for (int i = 0; i < points; i++) {
                                double angle = (2 * Math.PI * i) / points;
                                double px = centerX + Math.cos(angle) * radius;
                                double pz = centerZ + Math.sin(angle) * radius;
                                MinecraftClient.getInstance().particleManager.addParticle(
                                        ParticleTypes.END_ROD,
                                        px, centerY, pz,
                                        0.0, 0.0, 0.0
                                );
                            }
                        }
                    }
                }
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
        HandledScreens.register(ManabornMod.ETHER_CONVERTER_SH, com.manaborn.client.ui.EtherConverterScreen::new);
        HandledScreens.register(ManabornMod.MAGIC_CRAFTING_TABLE_SH, com.manaborn.client.ui.MagicCraftingTableScreen::new);
        HandledScreens.register(ManabornMod.SPELL_ENHANCEMENT_ALTAR_SH, SpellEnhancementAltarScreen::new);

        // Книга рецептов: открытие экрана по ПКМ (прокси из main-кода)
        com.manaborn.items.RecipeBookItem.clientOpener = () -> MinecraftClient.getInstance()
                .setScreen(new com.manaborn.client.ui.RecipeBookScreen());

        // Регистрируем обработчик радиального меню для магического посоха
        MagicStaffClientHandler.register();

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

    /**
     * Регистрирует рендерер для сущности пылинки
     * 
     * Используем EmptyEntityRenderer для невидимой сущности.
     * Клиентский код будет рисовать частицы только когда игрок носит Manoscope.
     */
    @SuppressWarnings("deprecation")
    private void registerDustWispRenderer() {
        EntityRendererRegistry.register(ManabornMod.DUST_WISP_ENTITY, 
            (context) -> new net.minecraft.client.render.entity.EmptyEntityRenderer<>(context));
    }

    /**
     * Регистрирует рендерер для маг-торговца
     */
    @SuppressWarnings("deprecation")
    private void registerMageMerchantRenderer() {
        EntityRendererRegistry.register(ManabornMod.MAGE_MERCHANT_ENTITY,
            com.manaborn.client.render.MageMerchantRenderer::new);
    }

    private void registerMageMerchantModelLayer() {
        net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry.registerModelLayer(
            com.manaborn.client.model.MageModel.LAYER, com.manaborn.client.model.MageModel::getTexturedModelData);
    }
}
