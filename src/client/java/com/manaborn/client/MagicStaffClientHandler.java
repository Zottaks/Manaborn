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
            boolean isKeyDown = KeyBindings.OPEN_SPELL_MENU.isPressed();

            if (config.isPressMode) {
                // === РЕЖИМ: НАЖАТИЕ (Press) ===
                if (isShiftDown && client.currentScreen == null) {
                    while (KeyBindings.OPEN_SPELL_MENU.wasPressed()) {
                        RadialMenuHandler.openRadialMenu();
                    }
                }
            } else {
                // === РЕЖИМ: УДЕРЖАНИЕ (Hold) ===
                // Если Shift + клавиша зажаты и экран не открыт
                if (isShiftDown && isKeyDown && client.currentScreen == null) {
                    if (!wasKeyHeld) {
                        // Первый кадр зажатия - открываем меню
                        RadialMenuHandler.openRadialMenu();
                        wasKeyHeld = true;
                    }
                    // Меню остаётся открытым, пока клавиша зажата
                } else {
                    // Клавиша отпущена или Shift отпущен
                    if (wasKeyHeld) {
                        // Закрываем меню
                        if (client.currentScreen instanceof com.manaborn.client.ui.RadialSpellMenuScreen) {
                            client.currentScreen.close();
                        }
                        wasKeyHeld = false;
                    }
                }
            }
        });
    }
}
