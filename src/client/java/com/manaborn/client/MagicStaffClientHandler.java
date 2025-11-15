package com.manaborn.client;

import com.manaborn.client.config.SpellMenuMode;
import com.manaborn.items.MagicStaffItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Клиентский обработчик для магического посоха
 * 
 * Одна клавиша с двумя режимами:
 * - Press (нажатие): Shift + клавиша → меню остаётся открытым
 * - Hold (удержание): Shift + клавиша зажаты → меню открыто, отпустил → закрылось
 * 
 * Режим переключается отдельной клавишей (TOGGLE_SPELL_MENU_MODE)
 */
public class MagicStaffClientHandler {
    private static boolean wasKeyHeld = false;

    /**
     * Регистрирует обработчик клиентских событий для посоха
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                wasKeyHeld = false;
                return;
            }

            // Проверяем переключение режима
            while (KeyBindings.TOGGLE_SPELL_MENU_MODE.wasPressed()) {
                SpellMenuMode config = SpellMenuMode.getInstance();
                config.toggle();
                
                // Показываем сообщение о смене режима
                String modeName = config.isPressMode ? "Press (нажатие)" : "Hold (удержание)";
                client.player.sendMessage(
                    Text.literal("§6[Manaborn] §fРежим меню: §e" + modeName),
                    true // actionBar
                );
            }

            // Проверяем, что в руке посох
            ItemStack mainHandStack = client.player.getMainHandStack();
            ItemStack offHandStack = client.player.getOffHandStack();

            boolean hasStaff = mainHandStack.getItem() instanceof MagicStaffItem ||
                    offHandStack.getItem() instanceof MagicStaffItem;

            if (!hasStaff) {
                wasKeyHeld = false;
                return;
            }

            // Проверяем, что Shift зажат
            boolean isShiftDown = GLFW.glfwGetKey(client.getWindow().getHandle(),
                    GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                    GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

            SpellMenuMode config = SpellMenuMode.getInstance();
            boolean isRadialMenuOpen = client.currentScreen instanceof com.manaborn.client.ui.RadialSpellMenuScreen;

            if (config.isPressMode) {
                // === РЕЖИМ: НАЖАТИЕ (Press) ===
                // Открываем по нажатию Shift + клавиша, меню остается открытым
                if (isShiftDown && client.currentScreen == null) {
                    while (KeyBindings.OPEN_SPELL_MENU.wasPressed()) {
                        RadialMenuHandler.openRadialMenu();
                    }
                }
            } else {
                // === РЕЖИМ: УДЕРЖАНИЕ (Hold) ===
                // Зажал Shift + клавишу = меню открыто, отпустил Shift = закрыто
                if (isShiftDown) {
                    // Shift зажат - открываем меню при нажатии клавиши
                    if (!wasKeyHeld && !isRadialMenuOpen) {
                        // Проверяем нажатие клавиши меню
                        while (KeyBindings.OPEN_SPELL_MENU.wasPressed()) {
                            RadialMenuHandler.openRadialMenu();
                            wasKeyHeld = true;
                        }
                    }
                    // Пока Shift зажат, меню остается открытым
                } else {
                    // Shift отпущен - закрываем меню
                    if (wasKeyHeld && isRadialMenuOpen) {
                        client.currentScreen.close();
                    }
                    wasKeyHeld = false;
                }
            }
        });
    }
}
