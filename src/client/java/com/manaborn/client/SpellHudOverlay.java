package com.manaborn.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;

/**
 * Отображение выбранного заклинания на HUD
 * Использует новый HudElementRegistry API вместо устаревшего HudRenderCallback
 */
public final class SpellHudOverlay {
    
    /**
     * Метод рендеринга информации о заклинании на HUD
     * 
     * @param drawContext Контекст для рисования
     * @param tickCounter Счетчик тиков для анимаций
     */
    public static void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) return;

        // Проверяем, держит ли игрок волшебную палочку
        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();
        
        ItemStack staff = null;
        if (mainHand.getItem().getClass().getSimpleName().equals("MagicStaffItem")) {
            staff = mainHand;
        } else if (offHand.getItem().getClass().getSimpleName().equals("MagicStaffItem")) {
            staff = offHand;
        }
        
        if (staff == null) return;

        SpellType selectedSpell = SpellManager.getSelectedSpell(staff);
        
        int screenWidth = drawContext.getScaledWindowWidth();

        // Позиция в правом верхнем углу
        int x = screenWidth - 120;
        int y = 20;

        // Фон
        int backgroundColor = 0x80000000; // полупрозрачный черный
        int borderColor = 0xFF4DA3FF; // синий цвет границы
        int textColor = 0xFFFFFFFF; // белый текст
        
        // Рисуем фон
        drawContext.fill(x - 2, y - 2, x + 110, y + 30, backgroundColor);
        drawContext.fill(x - 1, y - 1, x + 109, y + 29, borderColor);
        drawContext.fill(x, y, x + 108, y + 28, backgroundColor);
        
        // Текст с названием заклинания
        Text spellText = Text.literal(selectedSpell.getDisplayName());
        drawContext.drawText(client.textRenderer, spellText, x + 5, y + 5, textColor, false);
        
        // Текст со стоимостью маны
        Text costText = Text.literal("Стоимость: " + selectedSpell.getManaCost() + " маны");
        drawContext.drawText(client.textRenderer, costText, x + 5, y + 15, 0xFFAAAAAA, false);
    }
}
