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
 * 
 * Каждое заклинание имеет свой уникальный визуальный стиль:
 * - Fireball: огненные красно-оранжевые цвета с эффектом лавы
 * - Ice Shard: холодные сине-голубые цвета со снежным эффектом
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
        int x = screenWidth - 140;
        int y = 20;
        int width = 130;
        int height = 50;
        
        // Получаем время для анимации
        float time = (System.currentTimeMillis() % 2000) / 2000.0f;
        
        // Рисуем в зависимости от типа заклинания
        switch (selectedSpell) {
            case FIREBALL:
                renderFireballStyle(drawContext, x, y, width, height, time, client);
                break;
            case ICE_SHARD:
                renderIceShardStyle(drawContext, x, y, width, height, time, client);
                break;
        }
        
        // Текст с названием заклинания
        Text spellText = Text.literal(selectedSpell.getDisplayName());
        drawContext.drawText(client.textRenderer, spellText, x + 10, y + 10, 0xFFFFFFFF, true);
        
        // Текст со стоимостью маны
        Text costText = Text.literal("Мана: " + selectedSpell.getManaCost());
        drawContext.drawText(client.textRenderer, costText, x + 10, y + 25, 0xFFFFFFFF, true);
        
        // Подсказка
        Text hintText = Text.literal("ПКМ - использовать");
        drawContext.drawText(client.textRenderer, hintText, x + 10, y + 37, 0xFFAAAAAA, false);
    }
    
    /**
     * Рендерит UI в стиле огненного шара
     * Красно-оранжевые цвета с эффектом пульсации лавы
     */
    private static void renderFireballStyle(DrawContext ctx, int x, int y, int width, int height, float time, MinecraftClient client) {
        // Анимация пульсации
        float pulse = (float) Math.sin(time * Math.PI * 2) * 0.3f + 0.7f;
        
        // Цвета огня
        int darkRed = 0xC0330000;      // Темно-красный фон
        int brightRed = 0xFF660000;    // Ярко-красный
        int orange = 0xFFFF6600;       // Оранжевый
        int yellow = 0xFFFFAA00;       // Желтый
        
        // Основной фон с градиентом
        drawGradientRect(ctx, x, y, x + width, y + height, darkRed, brightRed);
        
        // Внешняя рамка (оранжевая пульсация)
        int outerBorder = blendColors(orange, yellow, pulse);
        ctx.fill(x - 2, y - 2, x + width + 2, y - 1, outerBorder);
        ctx.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, outerBorder);
        ctx.fill(x - 2, y - 1, x - 1, y + height + 1, outerBorder);
        ctx.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, outerBorder);
        
        // Внутренняя рамка (ярко-оранжевая)
        ctx.fill(x - 1, y - 1, x + width + 1, y, 0xFFFF8800);
        ctx.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFFFF8800);
        ctx.fill(x - 1, y, x, y + height, 0xFFFF8800);
        ctx.fill(x + width, y, x + width + 1, y + height, 0xFFFF8800);
        
        // Эффект "искр" - маленькие точки
        int sparkCount = 8;
        for (int i = 0; i < sparkCount; i++) {
            float sparkTime = (time + i / (float) sparkCount) % 1.0f;
            int sparkX = x + 5 + (int) (Math.sin(sparkTime * Math.PI * 2 + i) * (width - 15));
            int sparkY = y + 5 + (int) (sparkTime * (height - 10));
            
            if (sparkY < y + height - 5) {
                int sparkColor = blendColors(yellow, orange, sparkTime);
                ctx.fill(sparkX, sparkY, sparkX + 2, sparkY + 2, sparkColor);
            }
        }
        
        // Декоративные линии (эффект жара)
        for (int i = 0; i < 3; i++) {
            float lineTime = (time + i * 0.33f) % 1.0f;
            int lineY = y + 5 + (int) (lineTime * (height - 10));
            int lineAlpha = (int) ((1.0f - lineTime) * 128);
            int lineColor = (lineAlpha << 24) | 0xFFAA00;
            ctx.fill(x + 5, lineY, x + width - 5, lineY + 1, lineColor);
        }
    }
    
    /**
     * Рендерит UI в стиле ледяного осколка
     * Холодные сине-голубые цвета со снежным эффектом
     */
    private static void renderIceShardStyle(DrawContext ctx, int x, int y, int width, int height, float time, MinecraftClient client) {
        // Анимация мерцания
        float shimmer = (float) Math.sin(time * Math.PI * 4) * 0.2f + 0.8f;
        
        // Цвета льда и снега
        int darkBlue = 0xC0001133;     // Темно-синий фон
        int iceBlue = 0xFF003366;      // Ледяной синий
        int lightBlue = 0xFF4DA3FF;    // Светло-голубой
        int cyan = 0xFF00FFFF;         // Циан
        
        // Основной фон с градиентом
        drawGradientRect(ctx, x, y, x + width, y + height, darkBlue, iceBlue);
        
        // Внешняя рамка (голубое мерцание)
        int outerBorder = blendColors(lightBlue, cyan, shimmer);
        ctx.fill(x - 2, y - 2, x + width + 2, y - 1, outerBorder);
        ctx.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, outerBorder);
        ctx.fill(x - 2, y - 1, x - 1, y + height + 1, outerBorder);
        ctx.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, outerBorder);
        
        // Внутренняя рамка (яркий циан)
        ctx.fill(x - 1, y - 1, x + width + 1, y, 0xFF00CCFF);
        ctx.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFF00CCFF);
        ctx.fill(x - 1, y, x, y + height, 0xFF00CCFF);
        ctx.fill(x + width, y, x + width + 1, y + height, 0xFF00CCFF);
        
        // Эффект "снежинок" - падающие точки
        int snowflakeCount = 12;
        for (int i = 0; i < snowflakeCount; i++) {
            float snowTime = (time + i / (float) snowflakeCount) % 1.0f;
            int snowX = x + 5 + (int) (Math.sin(snowTime * Math.PI * 4 + i * 0.5) * 10) + (i % 3) * 35;
            int snowY = y + 5 + (int) (snowTime * (height - 10));
            
            if (snowY < y + height - 5) {
                // Мерцающие снежинки
                float snowAlpha = (float) Math.sin(snowTime * Math.PI * 2) * 0.5f + 0.5f;
                int alpha = (int) (snowAlpha * 255);
                int snowColor = (alpha << 24) | 0xFFFFFF;
                
                // Рисуем крестик (снежинку)
                ctx.fill(snowX - 1, snowY, snowX + 2, snowY + 1, snowColor);
                ctx.fill(snowX, snowY - 1, snowX + 1, snowY + 2, snowColor);
            }
        }
        
        // Кристаллические линии (эффект льда)
        for (int i = 0; i < 4; i++) {
            float lineTime = (time * 0.5f + i * 0.25f) % 1.0f;
            int lineX = x + 5 + (int) (lineTime * (width - 10));
            int lineAlpha = (int) ((1.0f - Math.abs(lineTime - 0.5f) * 2) * 128);
            int lineColor = (lineAlpha << 24) | 0x00FFFF;
            
            // Диагональные линии
            for (int j = 0; j < height - 10; j += 2) {
                ctx.fill(lineX + j / 2, y + 5 + j, lineX + j / 2 + 1, y + 6 + j, lineColor);
            }
        }
        
        // Ледяные кристаллы в углах
        drawIceCrystal(ctx, x + 5, y + 5, shimmer);
        drawIceCrystal(ctx, x + width - 10, y + 5, shimmer);
        drawIceCrystal(ctx, x + 5, y + height - 10, shimmer);
        drawIceCrystal(ctx, x + width - 10, y + height - 10, shimmer);
    }
    
    /**
     * Рисует маленький ледяной кристалл
     */
    private static void drawIceCrystal(DrawContext ctx, int x, int y, float brightness) {
        int alpha = (int) (brightness * 200);
        int color = (alpha << 24) | 0xAAFFFF;
        
        // Крестик
        ctx.fill(x + 2, y, x + 3, y + 5, color);
        ctx.fill(x, y + 2, x + 5, y + 3, color);
        // Диагонали
        ctx.fill(x + 1, y + 1, x + 2, y + 2, color);
        ctx.fill(x + 3, y + 1, x + 4, y + 2, color);
        ctx.fill(x + 1, y + 3, x + 2, y + 4, color);
        ctx.fill(x + 3, y + 3, x + 4, y + 4, color);
    }
    
    /**
     * Рисует вертикальный градиент
     */
    private static void drawGradientRect(DrawContext ctx, int x1, int y1, int x2, int y2, int colorTop, int colorBottom) {
        int height = y2 - y1;
        for (int i = 0; i < height; i++) {
            float ratio = i / (float) height;
            int color = blendColors(colorTop, colorBottom, ratio);
            ctx.fill(x1, y1 + i, x2, y1 + i + 1, color);
        }
    }
    
    /**
     * Смешивает два цвета
     * 
     * @param color1 Первый цвет (ARGB)
     * @param color2 Второй цвет (ARGB)
     * @param ratio Соотношение (0.0 = color1, 1.0 = color2)
     * @return Смешанный цвет
     */
    private static int blendColors(int color1, int color2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));
        
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
