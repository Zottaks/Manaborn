package com.manaborn.client;

import com.manaborn.items.MagicStaffItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * Клиентский обработчик для магического посоха
 * 
 * Отслеживает нажатие Shift + ПКМ для открытия радиального меню
 */
public class MagicStaffClientHandler {
    private static boolean wasShiftRightClickPressed = false;

    /**
     * Регистрирует обработчик клиентских событий для посоха
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) {
                wasShiftRightClickPressed = false;
                return;
            }

            // Проверяем, что в руке посох
            ItemStack mainHandStack = client.player.getMainHandStack();
            ItemStack offHandStack = client.player.getOffHandStack();

            boolean hasStaff = mainHandStack.getItem() instanceof MagicStaffItem ||
                    offHandStack.getItem() instanceof MagicStaffItem;

            if (!hasStaff) {
                wasShiftRightClickPressed = false;
                return;
            }

            // Проверяем Shift + ПКМ
            boolean isShiftDown = GLFW.glfwGetKey(client.getWindow().getHandle(),
                    GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                    GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

            boolean isRightMouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(),
                    GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

            boolean isShiftRightClick = isShiftDown && isRightMouseDown;

            // Edge detection: открываем меню только при нажатии, а не при удержании
            if (isShiftRightClick && !wasShiftRightClickPressed) {
                RadialMenuHandler.openRadialMenu();
            }

            wasShiftRightClickPressed = isShiftRightClick;
        });
    }
}
