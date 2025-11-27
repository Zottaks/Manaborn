package com.manaborn.client;

import com.manaborn.items.MagicStaffItem;
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
 * 
 * HUD исчезает через 3 секунды после смены заклинания
 */
public final class SpellHudOverlay {
    
    private static SpellType lastSpell = null;
    private static long lastSpellChangeTime = 0;
    private static final long FADE_DELAY_MS = 3000; // 3 секунды
    private static final long FADE_DURATION_MS = 500; // 0.5 секунды на исчезновение
    
    /**
     * Метод рендеринга информации о заклинании на HUD
     * 
     * @param drawContext Контекст для рисования
     * @param tickCounter Счетчик тиков для анимаций
     */
    public static void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) return;

        // Проверяем, держит ли игрок волшебную палочку (MagicStaffItem или EtherScepterItem)
        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();
        
        ItemStack staff = null;
        if (mainHand.getItem() instanceof MagicStaffItem) {
            staff = mainHand;
        } else if (offHand.getItem() instanceof MagicStaffItem) {
            staff = offHand;
        }
        if (staff == null) {
            lastSpell = null;
            return;
        }

        SpellType selectedSpell = SpellManager.getSelectedSpell(staff);
        if (selectedSpell == null) {
            // Нечего отображать, если заклинание ещё не выбрано/не записано в NBT
            lastSpell = null;
            return;
        }
        
        // Обновляем время при смене заклинания
        if (lastSpell != selectedSpell) {
            lastSpell = selectedSpell;
            lastSpellChangeTime = System.currentTimeMillis();
        }
        
        // Вычисляем прозрачность (fade out)
        long currentTime = System.currentTimeMillis();
        long timeSinceChange = currentTime - lastSpellChangeTime;
        
        float alpha = 1.0f;
        if (timeSinceChange > FADE_DELAY_MS) {
            long fadeTime = timeSinceChange - FADE_DELAY_MS;
            if (fadeTime >= FADE_DURATION_MS) {
                return; // Полностью исчезло
            }
            alpha = 1.0f - (fadeTime / (float) FADE_DURATION_MS);
        }
        
        int screenWidth = drawContext.getScaledWindowWidth();

        // Позиция в правом верхнем углу
        int x = screenWidth - 140;
        int y = 20;
        int width = 130;
        int height = 50;
        
        // Получаем время для анимации
        float time = (System.currentTimeMillis() % 2000) / 2000.0f;
        
        // Рисуем в зависимости от типа заклинания с прозрачностью
        switch (selectedSpell) {
            case FIREBALL:
                renderFireballStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case ICE_SHARD:
                renderIceShardStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case FIRE_WALL:
                renderFireWallStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case INNER_HEAT:
                // Внутренний жар: используем огненный стиль
                renderFireballStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case HEAT_WEAPON:
                // Нагрев оружия: используем огненный стиль
                renderFireballStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case FROST_AURA:
                // Аура стужи: используем ледяной стиль
                renderIceShardStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case ABSORBING_BARRIER:
                renderDarkBarrierStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case WEAKNESS_CURSE:
                // Проклятие Слабости: темный стиль как у барьера
                renderDarkBarrierStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case ICE_WALL:
                // Стена льда: используем ледяной стиль
                renderIceShardStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case SOUND_STRIKE:
                // Звуковой удар: бело-жёлтый звуковой стиль
                renderSoundStrikeStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case BATTLE_HYMN:
                // Боевой гимн: тот же звуковой стиль
                renderSoundStrikeStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
            case SILENCE_ZONE:
                // Область тишины: серый/тёмный стиль
                renderDarkBarrierStyle(drawContext, x, y, width, height, time, client, alpha);
                break;
        }
        
        // Применяем прозрачность к тексту
        int textAlpha = (int) (alpha * 255) << 24;
        
        // Текст с названием заклинания
        Text spellText = Text.literal(selectedSpell.getDisplayName());
        drawContext.drawText(client.textRenderer, spellText, x + 10, y + 10, textAlpha | 0xFFFFFF, true);
        
        // Текст со стоимостью маны
        Text costText = Text.literal("Мана: " + selectedSpell.getManaCost());
        drawContext.drawText(client.textRenderer, costText, x + 10, y + 25, textAlpha | 0xFFFFFF, true);
        
        // Подсказка
        Text hintText = Text.literal("ПКМ - использовать");
        drawContext.drawText(client.textRenderer, hintText, x + 10, y + 37, textAlpha | 0xAAAAAA, false);
    }
    
    /**
     * Рендерит UI в стиле огненного шара
     * Красно-оранжевые цвета с эффектом пульсации лавы
     */
    private static void renderFireballStyle(DrawContext ctx, int x, int y, int width, int height, float time, MinecraftClient client, float alpha) {
        // Анимация пульсации
        float pulse = (float) Math.sin(time * Math.PI * 2) * 0.3f + 0.7f;
        
        // Цвета огня с прозрачностью
        int alphaValue = (int) (alpha * 192); // 0xC0 = 192
        int darkRed = (alphaValue << 24) | 0x330000;      // Темно-красный фон
        int brightRed = ((int)(alpha * 255) << 24) | 0x660000;    // Ярко-красный
        int orange = ((int)(alpha * 255) << 24) | 0xFF6600;       // Оранжевый
        int yellow = ((int)(alpha * 255) << 24) | 0xFFAA00;       // Желтый
        
        // Основной фон с градиентом
        drawGradientRect(ctx, x, y, x + width, y + height, darkRed, brightRed);
        
        // Внешняя рамка (оранжевая пульсация)
        int outerBorder = blendColors(orange, yellow, pulse);
        ctx.fill(x - 2, y - 2, x + width + 2, y - 1, outerBorder);
        ctx.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, outerBorder);
        ctx.fill(x - 2, y - 1, x - 1, y + height + 1, outerBorder);
        ctx.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, outerBorder);
        
        // Внутренняя рамка (ярко-оранжевая)
        ctx.fill(x - 1, y - 1, x + width + 1, y, ((int)(alpha * 255) << 24) | 0xFF8800);
        ctx.fill(x - 1, y + height, x + width + 1, y + height + 1, ((int)(alpha * 255) << 24) | 0xFF8800);
        ctx.fill(x - 1, y, x, y + height, ((int)(alpha * 255) << 24) | 0xFF8800);
        ctx.fill(x + width, y, x + width + 1, y + height, ((int)(alpha * 255) << 24) | 0xFF8800);
        
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
            int lineAlpha = (int) ((1.0f - lineTime) * 128 * alpha);
            int lineColor = (lineAlpha << 24) | 0xFFAA00;
            ctx.fill(x + 5, lineY, x + width - 5, lineY + 1, lineColor);
        }
    }
    
    /**
     * Рендерит UI в стиле ледяного осколка
     * Холодные сине-голубые цвета со снежным эффектом
     */
    private static void renderIceShardStyle(DrawContext ctx, int x, int y, int width, int height, float time, MinecraftClient client, float alpha) {
        // Анимация мерцания
        float shimmer = (float) Math.sin(time * Math.PI * 4) * 0.2f + 0.8f;
        
        // Цвета льда и снега с прозрачностью
        int alphaValue = (int) (alpha * 192); // 0xC0 = 192
        int darkBlue = (alphaValue << 24) | 0x001133;     // Темно-синий фон
        int iceBlue = ((int)(alpha * 255) << 24) | 0x003366;      // Ледяной синий
        int lightBlue = ((int)(alpha * 255) << 24) | 0x4DA3FF;    // Светло-голубой
        int cyan = ((int)(alpha * 255) << 24) | 0x00FFFF;         // Циан
        
        // Основной фон с градиентом
        drawGradientRect(ctx, x, y, x + width, y + height, darkBlue, iceBlue);
        
        // Внешняя рамка (голубое мерцание)
        int outerBorder = blendColors(lightBlue, cyan, shimmer);
        ctx.fill(x - 2, y - 2, x + width + 2, y - 1, outerBorder);
        ctx.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, outerBorder);
        ctx.fill(x - 2, y - 1, x - 1, y + height + 1, outerBorder);
        ctx.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, outerBorder);
        
        // Внутренняя рамка (яркий циан)
        ctx.fill(x - 1, y - 1, x + width + 1, y, ((int)(alpha * 255) << 24) | 0x00CCFF);
        ctx.fill(x - 1, y + height, x + width + 1, y + height + 1, ((int)(alpha * 255) << 24) | 0x00CCFF);
        ctx.fill(x - 1, y, x, y + height, ((int)(alpha * 255) << 24) | 0x00CCFF);
        ctx.fill(x + width, y, x + width + 1, y + height, ((int)(alpha * 255) << 24) | 0x00CCFF);
        
        // Эффект "снежинок" - падающие точки
        int snowflakeCount = 12;
        for (int i = 0; i < snowflakeCount; i++) {
            float snowTime = (time + i / (float) snowflakeCount) % 1.0f;
            int snowX = x + 5 + (int) (Math.sin(snowTime * Math.PI * 4 + i * 0.5) * 10) + (i % 3) * 35;
            int snowY = y + 5 + (int) (snowTime * (height - 10));
            
            if (snowY < y + height - 5) {
                // Мерцающие снежинки
                float snowAlpha = (float) Math.sin(snowTime * Math.PI * 2) * 0.5f + 0.5f;
                int snowAlphaValue = (int) (snowAlpha * 255 * alpha);
                int snowColor = (snowAlphaValue << 24) | 0xFFFFFF;
                
                // Рисуем крестик (снежинку)
                ctx.fill(snowX - 1, snowY, snowX + 2, snowY + 1, snowColor);
                ctx.fill(snowX, snowY - 1, snowX + 1, snowY + 2, snowColor);
            }
        }
        
        // Кристаллические линии (эффект льда)
        for (int i = 0; i < 4; i++) {
            float lineTime = (time * 0.5f + i * 0.25f) % 1.0f;
            int lineX = x + 5 + (int) (lineTime * (width - 10));
            int lineAlpha = (int) ((1.0f - Math.abs(lineTime - 0.5f) * 2) * 128 * alpha);
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
    
    private static void renderDarkBarrierStyle(DrawContext ctx, int x, int y, int width, int height, float time, MinecraftClient client, float alpha) {
        float pulse = (float) Math.sin(time * Math.PI * 2) * 0.25f + 0.75f;
        int baseA = (int) (alpha * 200);
        int bgTop = (baseA << 24) | 0x0A0A12;
        int bgBottom = ((int)(alpha * 255) << 24) | 0x141424;
        drawGradientRect(ctx, x, y, x + width, y + height, bgTop, bgBottom);

        int ring1 = ((int)(alpha * 255) << 24) | 0x3A2A5F;
        int ring2 = ((int)(alpha * 255) << 24) | 0x5C3B89;
        int ringBlend = blendColors(ring1, ring2, pulse);
        ctx.fill(x - 2, y - 2, x + width + 2, y - 1, ringBlend);
        ctx.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, ringBlend);
        ctx.fill(x - 2, y - 1, x - 1, y + height + 1, ringBlend);
        ctx.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, ringBlend);

        int inner = ((int)(alpha * 255) << 24) | 0x221833;
        ctx.fill(x - 1, y - 1, x + width + 1, y, inner);
        ctx.fill(x - 1, y + height, x + width + 1, y + height + 1, inner);
        ctx.fill(x - 1, y, x, y + height, inner);
        ctx.fill(x + width, y, x + width + 1, y + height, inner);

        int specks = 10;
        for (int i = 0; i < specks; i++) {
            float t = (time + i * 0.07f) % 1.0f;
            int sx = x + 6 + (int)((i * 11 + 7 * t) % (width - 12));
            int sy = y + 6 + (int)(t * (height - 12));
            int a = (int)(alpha * (120 + 100 * Math.sin((t + i) * Math.PI * 2)));
            int c = ((a & 0xFF) << 24) | 0xA060FF;
            ctx.fill(sx, sy, sx + 2, sy + 2, c);
        }

        for (int i = 0; i < 3; i++) {
            float t = (time * 0.8f + i * 0.33f) % 1.0f;
            int lineY = y + 6 + (int)(t * (height - 12));
            int la = (int)((1.0f - Math.abs(t - 0.5f) * 2) * 100 * alpha);
            int lc = (la << 24) | 0x8A6AD9;
            ctx.fill(x + 6, lineY, x + width - 6, lineY + 1, lc);
        }
    }
    
    /**
     * Рендерит UI в стиле стены огня
     * Золотисто-оранжевые цвета с эффектом вертикальных языков пламени
     */
    private static void renderFireWallStyle(DrawContext ctx, int x, int y, int width, int height, float time, MinecraftClient client, float alpha) {
        // Анимация пульсации
        float pulse = (float) Math.sin(time * Math.PI * 2) * 0.3f + 0.7f;
        
        // Цвета огненной стены с прозрачностью
        int alphaValue = (int) (alpha * 192);
        int darkOrange = (alphaValue << 24) | 0x442200;    // Темно-оранжевый фон
        int orange = ((int)(alpha * 255) << 24) | 0xFF6600; // Оранжевый
        int gold = ((int)(alpha * 255) << 24) | 0xFFAA00;   // Золотой
        int brightGold = ((int)(alpha * 255) << 24) | 0xFFDD00; // Яркое золото
        
        // Основной фон с градиентом
        drawGradientRect(ctx, x, y, x + width, y + height, darkOrange, orange);
        
        // Внешняя рамка (золотая пульсация)
        int outerBorder = blendColors(gold, brightGold, pulse);
        ctx.fill(x - 2, y - 2, x + width + 2, y - 1, outerBorder);
        ctx.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, outerBorder);
        ctx.fill(x - 2, y - 1, x - 1, y + height + 1, outerBorder);
        ctx.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, outerBorder);
        
        // Внутренняя рамка (оранжевая)
        ctx.fill(x - 1, y - 1, x + width + 1, y, ((int)(alpha * 255) << 24) | 0xFF8800);
        ctx.fill(x - 1, y + height, x + width + 1, y + height + 1, ((int)(alpha * 255) << 24) | 0xFF8800);
        ctx.fill(x - 1, y, x, y + height, ((int)(alpha * 255) << 24) | 0xFF8800);
        ctx.fill(x + width, y, x + width + 1, y + height, ((int)(alpha * 255) << 24) | 0xFF8800);
        
        // Эффект "языков пламени" - вертикальные полосы
        int flameCount = 6;
        for (int i = 0; i < flameCount; i++) {
            float flameTime = (time + i / (float) flameCount) % 1.0f;
            int flameX = x + 10 + i * ((width - 20) / (flameCount - 1));
            int flameHeight = (int) ((Math.sin(flameTime * Math.PI * 2 + i) * 0.5 + 0.5) * (height - 20));
            int flameY = y + height - 10 - flameHeight;
            
            int flameColor = blendColors(orange, brightGold, flameTime);
            ctx.fill(flameX, flameY, flameX + 3, y + height - 10, flameColor);
        }
    }
    
    /**
     * Рендерит UI в стиле звуковой магии
     * Бело-жёлтые цвета с музыкальными нотами
     */
    private static void renderSoundStrikeStyle(DrawContext ctx, int x, int y, int width, int height, float time, MinecraftClient client, float alpha) {
        // Цвета звуковой магии с прозрачностью (БЕЗ пульсаций)
        int alphaValue = (int) (alpha * 192);
        int darkGray = (alphaValue << 24) | 0x1A1A1A;      // Темно-серый фон
        int lightGray = ((int)(alpha * 255) << 24) | 0x333333;    // Светло-серый
        int white = ((int)(alpha * 255) << 24) | 0xFFFFFF;        // Белый
        int yellow = ((int)(alpha * 255) << 24) | 0xFFFF00;       // Жёлтый
        int gold = ((int)(alpha * 255) << 24) | 0xFFDD00;         // Золотой
        
        // Основной фон с градиентом
        drawGradientRect(ctx, x, y, x + width, y + height, darkGray, lightGray);
        
        // Внешняя рамка (статичная жёлтая)
        ctx.fill(x - 2, y - 2, x + width + 2, y - 1, yellow);
        ctx.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, yellow);
        ctx.fill(x - 2, y - 1, x - 1, y + height + 1, yellow);
        ctx.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, yellow);
        
        // Внутренняя рамка (белая)
        ctx.fill(x - 1, y - 1, x + width + 1, y, white);
        ctx.fill(x - 1, y + height, x + width + 1, y + height + 1, white);
        ctx.fill(x - 1, y, x, y + height, white);
        ctx.fill(x + width, y, x + width + 1, y + height, white);
        
        // Рисуем музыкальные ноты (меньше, чтобы не мешали глазу)
        // Статичные ноты в углах
        drawMusicNote(ctx, x + 8, y + 8, white, alpha * 0.5f);
        drawMusicNote(ctx, x + width - 14, y + height - 14, yellow, alpha * 0.5f);
        
        // Анимированные плывущие ноты (МЕНЬШЕ - только 5)
        int noteCount = 5;
        for (int i = 0; i < noteCount; i++) {
            float noteTime = (time * 0.5f + i / (float) noteCount) % 1.0f; // Медленнее
            int noteX = x + 15 + (i * 22);
            int noteY = y + 12 + (int) (noteTime * (height - 24));
            
            if (noteY < y + height - 12) {
                // Цвета чередуются
                int noteColor = (i % 2 == 0) ? white : yellow;
                
                // Затухание в конце пути
                float noteAlphaFactor = 1.0f - (noteTime * 0.6f);
                drawMusicNote(ctx, noteX, noteY, noteColor, alpha * noteAlphaFactor * 0.8f);
            }
        }
        
        // Горизонтальные "нотные линии" (как на нотном стане)
        int lineCount = 3;
        int lineSpacing = height / (lineCount + 1);
        for (int i = 1; i <= lineCount; i++) {
            int lineY = y + lineSpacing * i;
            int lineAlphaValue = (int) (alpha * 60);
            int lineColor = (lineAlphaValue << 24) | 0xFFFFFF;
            ctx.fill(x + 5, lineY, x + width - 5, lineY + 1, lineColor);
        }
    }
    
    /**
     * Рисует музыкальную ноту ♪
     */
    private static void drawMusicNote(DrawContext ctx, int x, int y, int baseColor, float alpha) {
        int alphaValue = (int) (alpha * 255);
        int color = (alphaValue << 24) | (baseColor & 0xFFFFFF);
        
        // Головка ноты (круг)
        ctx.fill(x, y + 4, x + 4, y + 8, color);
        
        // Штиль ноты (вертикальная линия)
        ctx.fill(x + 3, y, x + 5, y + 5, color);
        
        // Флажок (маленький)
        ctx.fill(x + 4, y, x + 7, y + 3, color);
    }
}
