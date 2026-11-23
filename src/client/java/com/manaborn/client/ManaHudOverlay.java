package com.manaborn.client;

import com.manaborn.client.state.ClientManaState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/**
 * Отображение маны на HUD — мягкие манакристаллы.
 *
 * Сохранена классическая компоновка (кристаллы в правом нижнем углу),
 * но визуал переработан:
 *  - плавная интерполяция значения маны (без дёрганий)
 *  - мягкий градиент «глубокий сапфир → ледяная лазурь»
 *  - бегущая волна света вдоль ряда кристаллов
 *  - дышащее свечение вокруг заполненных кристаллов
 *  - редкие искорки-мерцания на вершинах кристаллов
 */
public final class ManaHudOverlay {

    // ── Палитра (мягкая, сапфирово-лазурная) ─────────────────────────────
    private static final int CRYSTAL_TOP = 0xBFE0FF;    // бледная лазурь
    private static final int CRYSTAL_MID = 0x5E9FE8;    // лазурь
    private static final int CRYSTAL_BOTTOM = 0x2A56B0; // сапфир
    private static final int CRYSTAL_CORE = 0xEAF5FF;   // почти белый блик
    private static final int EMPTY_TOP = 0x1A2338;
    private static final int EMPTY_BOTTOM = 0x0E1424;
    private static final int OUTLINE_FULL = 0x9CC8FF;
    private static final int OUTLINE_EMPTY = 0xFF2E4166;

    // ── Состояние анимаций ───────────────────────────────────────────────
    private static float displayedMana = -1;      // сглаженное значение маны
    private static int previousMana = 0;
    private static float shakeIntensity = 0.0f;
    private static float regenPulse = 0.0f;
    private static long lastUpdateTime = 0;

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) return;

        int mana = ClientManaState.getMana();
        int max = ClientManaState.getMaxMana();
        if (max <= 0) return;

        updateAnimations(mana);

        int screenWidth = ctx.getScaledWindowWidth();
        int screenHeight = ctx.getScaledWindowHeight();

        // Геометрия кристаллов
        int crystalW = 9;
        int crystalH = 13;
        int gap = 2;
        int count = 10;

        int xRight = screenWidth - 14;
        // На уровне хотбара: центр кристаллов совпадает с центром хотбара
        int yBottom = screenHeight - 19;

        int totalW = count * (crystalW + gap) - gap;

        // Мягкая подложка-пластина под всей полоской
        drawBackdropPlate(ctx, xRight - totalW - 6, yBottom - 4, totalW + 12, crystalH + 9);

        // Кристаллы рисуются справа налево
        for (int i = 0; i < count; i++) {
            int x = xRight - i * (crystalW + gap) - crystalW;
            int y = yBottom;

            // Мягкая встряска при трате маны
            if (shakeIntensity > 0) {
                x += (int) ((Math.random() - 0.5) * shakeIntensity * 1.2);
                y += (int) ((Math.random() - 0.5) * shakeIntensity * 1.2);
            }

            // Заполненность кристалла от сглаженного значения
            // (индекс развёрнут: кристалл 0 — самый левый/самый полный)
            int crystalIndex = count - 1 - i;
            float fill = crystalFill(crystalIndex, displayedMana, max, count);

            drawManaCrystal(ctx, x, y, crystalW, crystalH, fill, crystalIndex);
        }

        // Текст с количеством маны — над полоской, прижат к правому краю,
        // чтобы не пересекаться с хотбаром
        MinecraftClient mc = MinecraftClient.getInstance();
        String manaLabel = mana + "/" + max;
        int manaLabelW = mc.textRenderer.getWidth(manaLabel);
        drawManaText(ctx, xRight - manaLabelW - 1, yBottom - 12, mana, max);
    }

    // ── Анимации ─────────────────────────────────────────────────────────

    private static void updateAnimations(int targetMana) {
        long now = Util.getMeasuringTimeMs();
        float dt = lastUpdateTime == 0 ? 0.016f : (now - lastUpdateTime) / 1000.0f;
        dt = Math.min(dt, 0.1f);
        lastUpdateTime = now;

        if (displayedMana < 0) displayedMana = targetMana;

        // Плавное сглаживание: экспоненциальное приближение к цели
        float ease = 1.0f - (float) Math.exp(-dt * 9.0f);
        displayedMana += (targetMana - displayedMana) * ease;
        if (Math.abs(displayedMana - targetMana) < 0.05f) displayedMana = targetMana;

        if (targetMana < previousMana) shakeIntensity = 0.8f;
        shakeIntensity = Math.max(0, shakeIntensity - dt * 2.5f);

        if (targetMana > previousMana) regenPulse = 1.0f;
        regenPulse = Math.max(0, regenPulse - dt * 1.6f);

        previousMana = targetMana;
    }

    private static float crystalFill(int index, float mana, int maxMana, int total) {
        float perCrystal = maxMana / (float) total;
        float start = index * perCrystal;
        return MathHelper.clamp((mana - start) / perCrystal, 0.0f, 1.0f);
    }

    // ── Подложка ─────────────────────────────────────────────────────────

    /**
     * Тёмная полупрозрачная пластина со скошенными углами и едва заметной
     * дышащей кромкой — визуально «собирает» кристаллы в единый элемент.
     */
    private static void drawBackdropPlate(DrawContext ctx, int x, int y, int w, int h) {
        long t = Util.getMeasuringTimeMs();
        float breath = 0.5f + 0.5f * MathHelper.sin(t * 0.0013f);

        // Тело пластины (углы срезаны на 1px для смягчения)
        ctx.fill(x + 1, y, x + w - 1, y + h, 0x38070B18);
        ctx.fill(x, y + 1, x + w, y + h - 1, 0x38070B18);

        // Тонкая кромка
        int edge = (28 + (int) (18 * breath)) << 24 | 0x7FA8E0;
        ctx.fill(x + 1, y, x + w - 1, y + 1, edge);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, edge);
        ctx.fill(x, y + 1, x + 1, y + h - 1, edge);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
    }

    // ── Кристалл ─────────────────────────────────────────────────────────

    private static void drawManaCrystal(DrawContext ctx, int x, int y, int w, int h,
                                        float fill, int index) {
        long t = Util.getMeasuringTimeMs();
        float time = t / 1000.0f;

        int cx = x + w / 2;
        int topY = y;
        int bottomY = y + h;
        int midY = y + h / 2;
        int halfW = w / 2;

        if (fill > 0.001f) {
            // Бегущая волна света: позиция волны вдоль ряда кристаллов
            float wavePos = ((time * 0.55f) % 1.6f) * 12.0f - 1.0f; // -1..11 в «единицах кристалла»
            float waveBoost = (float) Math.exp(-Math.pow(index - wavePos, 2) / 1.4);

            // Дыхание каждого кристалла (со сдвигом фазы)
            float breath = 0.5f + 0.5f * MathHelper.sin(time * 1.4f + index * 0.55f);

            // Цвета с учётом волны и дыхания
            float glowAmt = Math.min(1.0f, waveBoost * 0.55f + breath * 0.10f + regenPulse * 0.25f);
            int cTop = brighten(CRYSTAL_TOP, glowAmt * 0.8f);
            int cMid = brighten(CRYSTAL_MID, glowAmt * 0.6f);
            int cBot = brighten(CRYSTAL_BOTTOM, glowAmt * 0.5f);

            // Свечение за кристаллом (мягкое, многослойное)
            float glowStrength = 0.25f + fill * 0.5f + waveBoost * 0.35f + regenPulse * 0.3f;
            drawSoftGlow(ctx, cx, midY, halfW, h / 2, 0x5E9FE8, glowStrength);

            // Тело кристалла: заполненная снизу часть
            int fillH = Math.max(1, (int) (h * fill));
            int fillTop = bottomY - fillH;
            drawDiamondGradient(ctx, cx, fillTop, bottomY, midY, halfW, cTop, cMid, cBot);

            // Внутреннее светлое ядро (мягкий свет из центра кристалла)
            if (fill > 0.35f) {
                int coreH = (int) (fillH * 0.55f);
                int coreTop = bottomY - coreH;
                int coreHalf = Math.max(1, halfW / 2);
                int coreAlpha = (int) (60 + 90 * breath * fill);
                drawDiamondCore(ctx, cx, coreTop, bottomY - 1, coreHalf, (coreAlpha << 24) | CRYSTAL_CORE);
            }

            // Искорка на вершине — редко и нежно
            float sparkleSeed = MathHelper.sin(time * 0.9f + index * 2.13f);
            if (fill >= 0.95f && sparkleWindow(time, index)) {
                float sp = sparklePhase(time, index);
                int alpha = (int) (Math.sin(sp * Math.PI) * 220);
                if (alpha > 0) drawSparkle(ctx, cx, topY + 1, (alpha << 24) | 0xF2FAFF);
            }
        } else {
            // Пустой кристалл — приглушённый силуэт
            drawDiamondGradient(ctx, cx, topY, bottomY, midY, halfW,
                    brighten(EMPTY_TOP, 0), EMPTY_TOP, EMPTY_BOTTOM);
        }

        // Контур
        int outline = fill > 0.5f
                ? withAlpha(OUTLINE_FULL, 120 + (int) (60 * fill))
                : OUTLINE_EMPTY;
        drawDiamondOutline(ctx, cx, topY, bottomY, midY, halfW, outline);
    }

    // ── Примитивы кристалла ──────────────────────────────────────────────

    /**
     * Ромб с вертикальным градиентом в три стадии (верх/середина/низ).
     */
    private static void drawDiamondGradient(DrawContext ctx, int cx, int topY, int bottomY,
                                            int midY, int halfW, int cTop, int cMid, int cBot) {
        int height = bottomY - topY;
        for (int dy = 0; dy < height; dy++) {
            int curY = topY + dy;
            float progress = dy / (float) height;

            int color;
            if (curY < midY) {
                color = blend(argb(cTop), argb(cMid), progress * 2.0f);
            } else {
                color = blend(argb(cMid), argb(cBot), (progress - 0.5f) * 2.0f);
            }

            int rowHalf;
            if (curY < midY) {
                rowHalf = (int) (halfW * ((curY - topY) / (float) Math.max(1, midY - topY)));
            } else {
                rowHalf = (int) (halfW * ((bottomY - curY) / (float) Math.max(1, bottomY - midY)));
            }

            // Кончики ромба: каждый ряд закрашен хотя бы центральным пикселем —
            // вершины не пустые, но ширина не выходит за контур ромба
            if (rowHalf >= 0) {
                ctx.fill(cx - rowHalf, curY, cx + rowHalf + 1, curY + 1, color);
            }
        }
    }

    /**
     * Маленький светлый ромб-ядро внутри кристалла.
     */
    private static void drawDiamondCore(DrawContext ctx, int cx, int topY, int bottomY, int halfW, int argb) {
        int height = bottomY - topY;
        int midY = topY + height / 2;
        for (int dy = 0; dy < height; dy++) {
            int curY = topY + dy;
            int rowHalf;
            if (curY < midY) {
                rowHalf = (int) (halfW * ((curY - topY) / (float) Math.max(1, midY - topY)));
            } else {
                rowHalf = (int) (halfW * ((bottomY - curY) / (float) Math.max(1, bottomY - midY)));
            }
            if (rowHalf >= 0) {
                ctx.fill(cx - rowHalf, curY, cx + rowHalf + 1, curY + 1, argb);
            }
        }
    }

    private static void drawDiamondOutline(DrawContext ctx, int cx, int topY, int bottomY,
                                           int midY, int halfW, int argb) {
        line(ctx, cx, topY, cx - halfW, midY, argb);
        line(ctx, cx, topY, cx + halfW, midY, argb);
        line(ctx, cx - halfW, midY, cx, bottomY, argb);
        line(ctx, cx + halfW, midY, cx, bottomY, argb);
    }

    /**
     * Многослойное мягкое свечение вокруг точки.
     */
    private static void drawSoftGlow(DrawContext ctx, int cx, int cy, int rx, int ry, int rgb, float strength) {
        strength = MathHelper.clamp(strength, 0.0f, 1.0f);
        int a1 = (int) (26 * strength);
        int a2 = (int) (14 * strength);

        // Внутренний слой
        ctx.fill(cx - rx - 1, cy - ry - 1, cx + rx + 2, cy + ry + 2, withAlpha(rgb, a1));
        // Внешний, шире и прозрачнее
        ctx.fill(cx - rx - 3, cy - ry - 2, cx + rx + 4, cy + ry + 3, withAlpha(rgb, a2));
    }

    /**
     * Четырёхлучевая искорка.
     */
    private static void drawSparkle(DrawContext ctx, int cx, int cy, int argb) {
        ctx.fill(cx, cy - 2, cx + 1, cy + 3, argb);
        ctx.fill(cx - 2, cy, cx + 3, cy + 1, argb);
        int soft = (argb & 0x00FFFFFF) | (((argb >> 24) & 0xFF) / 2 << 24);
        ctx.fill(cx - 1, cy - 1, cx, cy, soft);
        ctx.fill(cx + 1, cy - 1, cx + 2, cy, soft);
        ctx.fill(cx - 1, cy + 1, cx, cy + 2, soft);
        ctx.fill(cx + 1, cy + 1, cx + 2, cy + 2, soft);
    }

    // ── Искорки: расписание ──────────────────────────────────────────────

    /** Окно активности искорки для кристалла (каждые ~7 секунд, короткая вспышка) */
    private static boolean sparkleWindow(float time, int index) {
        float cycle = 7.0f + index * 0.9f;
        float phase = (time + index * 2.7f) % cycle;
        return phase < 0.6f;
    }

    private static float sparklePhase(float time, int index) {
        float cycle = 7.0f + index * 0.9f;
        return ((time + index * 2.7f) % cycle) / 0.6f;
    }

    // ── Текст ────────────────────────────────────────────────────────────

    private static void drawManaText(DrawContext ctx, int x, int y, int mana, int maxMana) {
        long t = Util.getMeasuringTimeMs();
        MinecraftClient client = MinecraftClient.getInstance();

        String text = mana + "/" + maxMana;

        // Мягкий цвет: бледная лазурь, чуть тускнеет при низкой мане
        float fullness = maxMana > 0 ? mana / (float) maxMana : 0;
        int rgb = blendRgb(0x8FB8E8, 0xCFE6FF, fullness);

        // Нежное свечение текста при полной мане
        if (fullness >= 0.999f) {
            float pulse = 0.5f + 0.5f * MathHelper.sin(t * 0.0025f);
            rgb = blendRgb(rgb, 0xFFFFFF, pulse * 0.35f);
        }

        ctx.drawText(client.textRenderer, text, x, y, 0xFF000000 | rgb, true);
    }

    // ── Утилиты ──────────────────────────────────────────────────────────

    private static void line(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            ctx.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    private static int argb(int rgb) {
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int blend(int c1, int c2, float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);
        int a = (int) (((c1 >> 24) & 0xFF) + (((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF)) * t);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blendRgb(int rgb1, int rgb2, float t) {
        return blend(argb(rgb1), argb(rgb2), t) & 0x00FFFFFF;
    }

    private static int brighten(int rgb, float amount) {
        amount = MathHelper.clamp(amount, 0.0f, 1.0f);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = Math.min(255, (int) (r + (255 - r) * amount));
        g = Math.min(255, (int) (g + (255 - g) * amount));
        b = Math.min(255, (int) (b + (255 - b) * amount));
        return (r << 16) | (g << 8) | b;
    }
}
