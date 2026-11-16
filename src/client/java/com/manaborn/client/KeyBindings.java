package com.manaborn.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Регистрация клавиш управления для мода Manaborn
 */
public class KeyBindings {
    
    // Категория для группировки в настройках
    private static final KeyBinding.Category MANABORN_CATEGORY = 
        KeyBinding.Category.create(Identifier.of("manaborn", "general"));
    
    // Клавиша для открытия меню заклинаний (требует Shift)
    public static KeyBinding OPEN_SPELL_MENU;
    
    // Клавиша для переключения режима (нажатие/удержание)
    public static KeyBinding TOGGLE_SPELL_MENU_MODE;
    
    /**
     * Регистрирует все клавиши мода
     */
    public static void register() {
        OPEN_SPELL_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.manaborn.open_spell_menu",     // Ключ перевода
            InputUtil.Type.KEYSYM,                // Тип: клавиатура
            GLFW.GLFW_KEY_Z,                      // По умолчанию: Z
            MANABORN_CATEGORY                     // Категория
        ));
        
        TOGGLE_SPELL_MENU_MODE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.manaborn.toggle_spell_menu_mode", // Ключ перевода
            InputUtil.Type.KEYSYM,                 // Тип: клавиатура
            GLFW.GLFW_KEY_UNKNOWN,                // По умолчанию: не назначено
            MANABORN_CATEGORY                     // Категория
        ));
    }
}
