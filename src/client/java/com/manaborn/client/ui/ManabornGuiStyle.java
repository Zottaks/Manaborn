package com.manaborn.client.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/**
 * Единый визуальный стиль интерфейсов Manaborn.
 *
 * Мягкая «арканная» тема: глубокий фиолетово-индиго фон с градиентом,
 * мерцающие звёздочки, дышащая рамка с угловыми самоцветами,
 * градиентные прогресс-бары с бегущим бликом.
 *
 * Все цвета-параметры — RGB (0xRRGGBB), альфа добавляется внутри.
 */
public final class ManabornGuiStyle {

    private ManabornGuiStyle() {}

    // ── Палитра ──────────────────────────────────────────────────────────
    public static final int COLOR_TITLE = 0xFFE9D9FF;   // бледный лавандовый
    public static final int COLOR_TEXT = 0xFFCCC4E0;    // мягкий серо-фиолетовый
    public static final int COLOR_TEXT_DIM = 0xFF8E86A8;

    private static final int BG_TOP = 0xF81A1332;       // глубокий индиго
    private static final int BG_BOTTOM = 0xF80C0918;    // почти чёрный фиолетовый
    private static final int PANEL_TOP = 0xE6241C44;
    private static final int PANEL_BOTTOM = 0xE6141028;
    private static final int SLOT_BG = 0xFF100C1E;

    // ── Рамка окна ───────────────────────────────────────────────────────

    /**
     * Полный фон окна: тень, градиент, звёздочки, дышащая рамка, угловые самоцветы.
     */
    public static void drawFrame(DrawContext ctx, int x, int y, int w, int h) {
        long t = Util.getMeasuringTimeMs();
        float pulse = 0.5f + 0.5f * MathHelper.sin(t * 0.0011f);

        // Мягкая тень под окном
        ctx.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x55000000);

        // Основной фон с вертикальным градиентом
        fillGradientV(ctx, x, y, x + w, y + h, BG_TOP, BG_BOTTOM);

        // Мерцающие звёздочки внутри окна
        drawStarfield(ctx, x + 5, y + 5, w - 10, h - 10, 14, t);

        // Внешнее мягкое свечение
        int glowA = 24 + (int) (22 * pulse);
        drawBorder(ctx, x - 1, y - 1, w + 2, h + 2, withAlpha(0x8B6FD8, glowA), 1);

        // Основная рамка (плавно дышит)
        int border = withAlpha(blendRgb(0x7A5CCF, 0xA98BFF, pulse), 255);
        drawBorder(ctx, x, y, w, h, border, 1);

        // Внутренняя тонкая рамка
        drawBorder(ctx, x + 2, y + 2, w - 4, h - 4, 0xFF3A2C5E, 1);

        // Угловые самоцветы
        int gem = withAlpha(blendRgb(0xBFA8FF, 0xE9D9FF, pulse), 255);
        cornerGem(ctx, x, y, gem);
        cornerGem(ctx, x + w - 1, y, gem);
        cornerGem(ctx, x, y + h - 1, gem);
        cornerGem(ctx, x + w - 1, y + h - 1, gem);
    }

    // ── Панель ───────────────────────────────────────────────────────────

    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        fillGradientV(ctx, x, y, x + w, y + h, PANEL_TOP, PANEL_BOTTOM);
        drawBorder(ctx, x, y, w, h, 0xFF453468, 1);
        // Верхний блик панели
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, 0x26FFFFFF);
    }

    // ── Слот ─────────────────────────────────────────────────────────────

    /**
     * Красивый слот: тёмная подложка с внутренней тенью, рамка в цвет акцента.
     * Заполненный слот мягко светится.
     */
    public static void drawSlot(DrawContext ctx, int x, int y, int size, int accentRgb, boolean filled) {
        long t = Util.getMeasuringTimeMs();

        // Подложка и внутренняя тень
        ctx.fill(x - 1, y - 1, x + size + 1, y + size + 1, SLOT_BG);
        ctx.fill(x, y, x + size, y + 1, 0x50000000);
        ctx.fill(x, y, x + 1, y + size, 0x50000000);

        // Рамка
        int border;
        if (filled) {
            float pulse = 0.5f + 0.5f * MathHelper.sin(t * 0.004f);
            border = withAlpha(blendRgb(dimRgb(accentRgb, 0.65f), brightenRgb(accentRgb, 0.4f), pulse), 255);
            // Мягкое свечение вокруг заполненного слота
            int glowA = 18 + (int) (16 * pulse);
            drawBorder(ctx, x - 3, y - 3, size + 6, size + 6, withAlpha(accentRgb, glowA), 1);
        } else {
            border = withAlpha(dimRgb(accentRgb, 0.4f), 200);
        }
        drawBorder(ctx, x - 2, y - 2, size + 4, size + 4, border, 1);

        // Уголки слота для аккуратности
        int corner = withAlpha(brightenRgb(accentRgb, filled ? 0.5f : 0.15f), filled ? 255 : 160);
        ctx.fill(x - 2, y - 2, x - 1, y - 1, corner);
        ctx.fill(x + size + 1, y - 2, x + size + 2, y - 1, corner);
        ctx.fill(x - 2, y + size + 1, x - 1, y + size + 2, corner);
        ctx.fill(x + size + 1, y + size + 1, x + size + 2, y + size + 2, corner);
    }

    // ── Прогресс-бар ─────────────────────────────────────────────────────

    /**
     * Градиентный прогресс-бар с бегущим бликом по заполненной части.
     */
    public static void drawProgressBar(DrawContext ctx, int x, int y, int w, int h, float progress, int rgb) {
        long t = Util.getMeasuringTimeMs();
        progress = MathHelper.clamp(progress, 0.0f, 1.0f);

        // Ложе
        drawBorder(ctx, x - 1, y - 1, w + 2, h + 2, 0xFF2A2144, 1);
        fillGradientV(ctx, x, y, x + w, y + h, 0xFF0D0A18, 0xFF141024);

        int filled = (int) (w * progress);
        if (filled > 0) {
            // Градиентная заливка
            int cTop = withAlpha(brightenRgb(rgb, 0.35f), 255);
            int cBot = withAlpha(dimRgb(rgb, 0.25f), 255);
            fillGradientV(ctx, x, y, x + filled, y + h, cTop, cBot);

            // Бегущий блик
            float phase = (t % 1600L) / 1600.0f;
            int bandW = 6;
            int sx = x + (int) (phase * (filled + bandW * 2)) - bandW;
            for (int i = 0; i < bandW; i++) {
                int bx = sx + i;
                if (bx < x || bx >= x + filled) continue;
                float falloff = 1.0f - Math.abs(i - bandW / 2.0f) / (bandW / 2.0f);
                ctx.fill(bx, y, bx + 1, y + h, withAlpha(0xFFFFFF, (int) (70 * falloff)));
            }

            // Яркая кромка на конце заполнения
            ctx.fill(x + filled - 1, y, x + filled, y + h, withAlpha(brightenRgb(rgb, 0.6f), 220));
        }
    }

    // ── Танк жидкости ────────────────────────────────────────────────────

    /**
     * Вертикальный танк с градиентной «жидкостью», волной на поверхности
     * и поднимающимися пузырьками.
     */
    public static void drawTank(DrawContext ctx, int x, int y, int w, int h, int value, int max, int rgb) {
        long t = Util.getMeasuringTimeMs();

        // Рамка и фон
        drawBorder(ctx, x - 2, y - 2, w + 4, h + 4, 0xFF453468, 1);
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, SLOT_BG);
        ctx.fill(x, y, x + w, y + h, 0xFF0A0812);

        if (max > 0 && value > 0) {
            int fillH = Math.max(1, (int) (h * (value / (float) max)));
            int fy = y + h - fillH;

            // Жидкость с градиентом (сверху светлее)
            int cTop = withAlpha(brightenRgb(rgb, 0.3f), 235);
            int cBot = withAlpha(dimRgb(rgb, 0.3f), 235);
            fillGradientV(ctx, x, fy, x + w, y + h, cTop, cBot);

            // Волна на поверхности
            for (int col = 0; col < w; col++) {
                float wave = MathHelper.sin(t * 0.005f + col * 0.8f);
                int wy = fy + (wave > 0.3f ? 1 : 0);
                ctx.fill(x + col, fy, x + col + 1, wy + 1, withAlpha(brightenRgb(rgb, 0.55f), 200));
            }

            // Пузырьки, поднимающиеся вверх
            for (int i = 0; i < 3; i++) {
                int travel = fillH - 4;
                if (travel <= 0) break;
                int by = fy + 2 + (int) ((travel - ((t / 45L + i * 37L) % travel)));
                int bx = x + 2 + (i * 7) % Math.max(1, w - 4);
                float fade = 1.0f - (by - fy) / (float) Math.max(1, fillH);
                ctx.fill(bx, by, bx + 2, by + 2, withAlpha(0xFFFFFF, (int) (50 + 60 * fade)));
            }
        }
    }

    // ── Заголовок ────────────────────────────────────────────────────────

    /**
     * Центрированный заголовок с боковыми орнаментами и линией, тающей к краям.
     */
    public static void drawTitle(DrawContext ctx, TextRenderer tr, String title, int guiWidth, int y) {
        long t = Util.getMeasuringTimeMs();
        float pulse = 0.5f + 0.5f * MathHelper.sin(t * 0.0016f);

        int tw = tr.getWidth(title);
        int tx = (guiWidth - tw) / 2;
        ctx.drawText(tr, title, tx, y, COLOR_TITLE, true);

        // Орнаменты по бокам
        int orn = withAlpha(blendRgb(0x9B7BFF, 0xE9D9FF, pulse), 230);
        ctx.drawText(tr, "✦", tx - 12, y, orn, false);
        ctx.drawText(tr, "✦", tx + tw + 4, y, orn, false);

        // Линия под заголовком, тающая к краям
        int lineW = tw + 28;
        int lx = (guiWidth - lineW) / 2;
        int ly = y + 11;
        for (int i = 0; i < lineW; i++) {
            float edge = 1.0f - Math.abs(i - lineW / 2.0f) / (lineW / 2.0f);
            ctx.fill(lx + i, ly, lx + i + 1, ly + 1, withAlpha(0x9B7BFF, (int) (110 * edge)));
        }
    }

    // ── Цветовые утилиты ─────────────────────────────────────────────────

    public static int withAlpha(int rgb, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (rgb & 0x00FFFFFF);
    }

    public static int blendRgb(int rgb1, int rgb2, float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);
        int r = (int) ((((rgb1 >> 16) & 0xFF) + (((rgb2 >> 16) & 0xFF) - ((rgb1 >> 16) & 0xFF)) * t));
        int g = (int) ((((rgb1 >> 8) & 0xFF) + (((rgb2 >> 8) & 0xFF) - ((rgb1 >> 8) & 0xFF)) * t));
        int b = (int) (((rgb1 & 0xFF) + ((rgb2 & 0xFF) - (rgb1 & 0xFF)) * t));
        return (r << 16) | (g << 8) | b;
    }

    public static int brightenRgb(int rgb, float amount) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r = Math.min(255, (int) (r + (255 - r) * amount));
        g = Math.min(255, (int) (g + (255 - g) * amount));
        b = Math.min(255, (int) (b + (255 - b) * amount));
        return (r << 16) | (g << 8) | b;
    }

    public static int dimRgb(int rgb, float factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    // ── Низкоуровневые примитивы ─────────────────────────────────────────

    public static void fillGradientV(DrawContext ctx, int x1, int y1, int x2, int y2, int argbTop, int argbBottom) {
        int height = y2 - y1;
        for (int i = 0; i < height; i++) {
            float ratio = i / (float) height;
            ctx.fill(x1, y1 + i, x2, y1 + i + 1, blendArgb(argbTop, argbBottom, ratio));
        }
    }

    public static int blendArgb(int c1, int c2, float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);
        int a = (int) (((c1 >> 24) & 0xFF) + (((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF)) * t);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color, int thickness) {
        ctx.fill(x, y, x + w, y + thickness, color);
        ctx.fill(x, y + h - thickness, x + w, y + h, color);
        ctx.fill(x, y, x + thickness, y + h, color);
        ctx.fill(x + w - thickness, y, x + w, y + h, color);
    }

    // ── Внутренние декорации ─────────────────────────────────────────────

    private static void cornerGem(DrawContext ctx, int cx, int cy, int color) {
        // Ромбовидный самоцвет 5x5 на углу рамки
        ctx.fill(cx, cy - 2, cx + 1, cy + 3, color);
        ctx.fill(cx - 2, cy, cx + 3, cy + 1, color);
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, withAlpha(0xFFFFFF, 120));
    }

    private static void drawStarfield(DrawContext ctx, int x, int y, int w, int h, int count, long time) {
        if (w <= 0 || h <= 0) return;
        for (int i = 0; i < count; i++) {
            // Детерминированная позиция из хэша
            int hash = (int) ((i * 2654435761L) ^ (w * 31L + h));
            hash = (hash ^ (hash >>> 13)) * 1274126177;
            hash = hash ^ (hash >>> 16);
            int sx = x + Math.floorMod(hash, w);
            int sy = y + Math.floorMod(hash >> 8, h);

            float twinkle = 0.5f + 0.5f * MathHelper.sin(time * 0.0022f + i * 1.7f);
            int alpha = (int) (twinkle * 85);
            if (alpha < 8) continue;
            ctx.fill(sx, sy, sx + 1, sy + 1, withAlpha(0xCBB8FF, alpha));
        }
    }

    // ── Сетка инвентаря ──────────────────────────────────────────────────

    /**
     * Слегка видимые границы слотов инвентаря (9x3 + хотбар).
     * invX/invY — координаты первого слота (как в ScreenHandler).
     */
    public static void drawInventoryGrid(DrawContext ctx, int invX, int invY) {
        int line = withAlpha(0x9B8BD8, 34);
        int hotbarLine = withAlpha(0x9B8BD8, 58);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawBorder(ctx, invX + col * 18 - 1, invY + row * 18 - 1, 18, 18, line, 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawBorder(ctx, invX + col * 18 - 1, invY + 58 - 1, 18, 18, hotbarLine, 1);
        }
    }

    // ── Труба с потоком энергии ──────────────────────────────────────────

    /**
     * Горизонтальная труба: тёмное русло, градиентное заполнение
     * и бегущие капли энергии при активности.
     */
    public static void drawPipe(DrawContext ctx, int x1, int x2, int y, int rgb, float fill, boolean active) {
        long t = Util.getMeasuringTimeMs();

        // Стенки трубы и русло
        ctx.fill(x1, y - 3, x2, y - 2, withAlpha(rgb, 60));
        ctx.fill(x1, y + 3, x2, y + 4, withAlpha(rgb, 60));
        ctx.fill(x1, y - 2, x2, y + 3, 0xFF0D0A18);

        // Заполнение
        int fw = (int) ((x2 - x1) * MathHelper.clamp(fill, 0.0f, 1.0f));
        if (fw > 0) {
            fillGradientV(ctx, x1, y - 2, x1 + fw, y + 3,
                    withAlpha(brightenRgb(rgb, 0.25f), 220), withAlpha(dimRgb(rgb, 0.2f), 220));
        }

        // Бегущие капли энергии
        if (active) {
            for (int i = 0; i < 3; i++) {
                float f = ((t / 900.0f) + i / 3.0f) % 1.0f;
                int px = x1 + (int) (f * (x2 - x1 - 2));
                ctx.fill(px, y - 1, px + 2, y + 2, withAlpha(brightenRgb(rgb, 0.55f), 220));
                ctx.fill(px, y, px + 2, y + 1, withAlpha(0xFFFFFF, 120));
            }
        }
    }

    // ── Магический круг ──────────────────────────────────────────────────

    /** Пунктирная окружность. */
    public static void drawDottedCircle(DrawContext ctx, int cx, int cy, int radius, int rgb, int alpha) {
        int dots = Math.max(16, radius * 3);
        for (int i = 0; i < dots; i += 2) {
            float ang = (float) (i / (float) dots * Math.PI * 2);
            int px = cx + (int) (Math.cos(ang) * radius);
            int py = cy + (int) (Math.sin(ang) * radius);
            ctx.fill(px, py, px + 1, py + 1, withAlpha(rgb, alpha));
        }
    }

    /** Медленно вращающиеся руны по окружности. */
    public static void drawOrbitRunes(DrawContext ctx, TextRenderer tr, int cx, int cy, int radius, int rgb) {
        long t = Util.getMeasuringTimeMs();
        String[] runes = {"✦", "✧", "◆", "✦", "✧", "◆"};
        for (int i = 0; i < runes.length; i++) {
            float ang = t * 0.00055f + i * (float) (Math.PI * 2 / runes.length);
            int rx = cx + (int) (Math.cos(ang) * radius) - 2;
            int ry = cy + (int) (Math.sin(ang) * radius) - 3;
            int alpha = 60 + (int) (45 * Math.sin(t * 0.002f + i * 1.3f));
            ctx.drawText(tr, runes[i], rx, ry, withAlpha(rgb, Math.max(20, alpha)), false);
        }
    }

    // ── Вихрь ────────────────────────────────────────────────────────────

    /** Спиральный вихрь из частиц с пульсирующим ядром. */
    public static void drawVortex(DrawContext ctx, int cx, int cy, int rgb) {
        long t = Util.getMeasuringTimeMs();
        for (int arm = 0; arm < 3; arm++) {
            for (int i = 0; i < 14; i++) {
                float f = i / 14.0f;
                float ang = t * 0.0018f + arm * (float) (Math.PI * 2 / 3) + f * 2.2f;
                float r = 2 + f * 12;
                int px = cx + (int) (Math.cos(ang) * r);
                int py = cy + (int) (Math.sin(ang) * r * 0.7f);
                int alpha = (int) ((1 - f) * 140 + 30);
                ctx.fill(px, py, px + 1, py + 1, withAlpha(brightenRgb(rgb, 0.3f * (1 - f)), alpha));
            }
        }
        float pulse = 0.5f + 0.5f * MathHelper.sin(t * 0.003f);
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, withAlpha(0xFFFFFF, (int) (110 + 80 * pulse)));
    }

    /** Кольцо прогресса из точек вокруг центра. */
    public static void drawProgressRing(DrawContext ctx, int cx, int cy, int radius, float progress, int rgb) {
        long t = Util.getMeasuringTimeMs();
        progress = MathHelper.clamp(progress, 0.0f, 1.0f);
        int dots = 28;
        for (int i = 0; i < dots; i++) {
            float ang = (float) (i / (float) dots * Math.PI * 2) - (float) Math.PI / 2;
            int px = cx + (int) (Math.cos(ang) * radius);
            int py = cy + (int) (Math.sin(ang) * radius);
            boolean lit = i < dots * progress;
            int c = lit ? brightenRgb(rgb, 0.3f) : rgb;
            ctx.fill(px, py, px + 2, py + 2, withAlpha(c, lit ? 210 : 45));
        }
        // Маркер на переднем крае заполнения + короткий шлейф позади него
        if (progress > 0.01f) {
            for (int k = 0; k < 3; k++) {
                float p = progress - k * (1.5f / dots);
                if (p <= 0) break;
                float ang = (float) (p * Math.PI * 2) - (float) Math.PI / 2;
                int px = cx + (int) (Math.cos(ang) * radius);
                int py = cy + (int) (Math.sin(ang) * radius);
                int alpha = 180 - k * 55;
                if (k == 0) {
                    ctx.fill(px - 1, py - 1, px + 3, py + 3, withAlpha(0xFFFFFF, alpha));
                } else {
                    ctx.fill(px, py, px + 2, py + 2, withAlpha(0xFFFFFF, alpha));
                }
            }
        }
    }

    // ── Прочие декорации ─────────────────────────────────────────────────

    /** Искры, кружащиеся вокруг точки (чередуют направление). */
    public static void drawOrbitSparkles(DrawContext ctx, int cx, int cy, int radius, int count, int rgb) {
        long t = Util.getMeasuringTimeMs();
        for (int i = 0; i < count; i++) {
            float ang = t * 0.0012f * (i % 2 == 0 ? 1 : -1) + i * (float) (Math.PI * 2 / count);
            int px = cx + (int) (Math.cos(ang) * radius);
            int py = cy + (int) (Math.sin(ang) * radius);
            float tw = 0.5f + 0.5f * MathHelper.sin(t * 0.004f + i * 2.0f);
            ctx.fill(px, py, px + 2, py + 2, withAlpha(brightenRgb(rgb, 0.4f), (int) (70 + 100 * tw)));
        }
    }

    /** Угловые скобки по периметру прямоугольника. */
    public static void drawCornerBrackets(DrawContext ctx, int x, int y, int w, int h, int rgb) {
        int c = withAlpha(rgb, 230);
        int l = 5;
        ctx.fill(x, y, x + l, y + 1, c);
        ctx.fill(x, y, x + 1, y + l, c);
        ctx.fill(x + w - l, y, x + w, y + 1, c);
        ctx.fill(x + w - 1, y, x + w, y + l, c);
        ctx.fill(x, y + h - 1, x + l, y + h, c);
        ctx.fill(x, y + h - l, x + 1, y + h, c);
        ctx.fill(x + w - l, y + h - 1, x + w, y + h, c);
        ctx.fill(x + w - 1, y + h - l, x + w, y + h, c);
    }

    /** Пунктирная направляющая линия с движущимися частицами. */
    public static void drawFlowLine(DrawContext ctx, int x1, int y1, int x2, int y2, int rgb, boolean active) {
        long t = Util.getMeasuringTimeMs();
        int len = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (len == 0) return;
        for (int i = 0; i <= len; i += 4) {
            float f = i / (float) len;
            int px = (int) (x1 + (x2 - x1) * f);
            int py = (int) (y1 + (y2 - y1) * f);
            ctx.fill(px, py, px + 1, py + 1, withAlpha(rgb, 40));
        }
        if (active) {
            for (int i = 0; i < 2; i++) {
                float f = ((t / 700.0f) + i * 0.5f) % 1.0f;
                int px = (int) (x1 + (x2 - x1) * f);
                int py = (int) (y1 + (y2 - y1) * f);
                ctx.fill(px - 1, py - 1, px + 2, py + 2, withAlpha(brightenRgb(rgb, 0.5f), 200));
                ctx.fill(px, py, px + 1, py + 1, withAlpha(0xFFFFFF, 160));
            }
        }
    }

    /** Мерцающая созвездная россыпь с тонкими соединительными линиями. */
    public static void drawConstellation(DrawContext ctx, int x, int y, int w, int h, int rgb) {
        drawSpellConstellation(ctx, x, y, w, h, 1234567L, 5, rgb);
    }

    /**
     * Уникальное «созвездие» для конкретного заклинания.
     * Рисунок детерминированно зависит от seed (id заклинания),
     * количество звёзд равно points — с прокачкой появляются новые звёзды.
     * Последняя добавленная звезда светится ярче остальных.
     */
    public static void drawSpellConstellation(DrawContext ctx, int x, int y, int w, int h, long seed, int points, int rgb) {
        long t = Util.getMeasuringTimeMs();
        points = MathHelper.clamp(points, 2, 8);

        int[][] pts = new int[points][2];
        long s = seed * 2862933555777941757L + 3037000493L;
        for (int i = 0; i < points; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            pts[i][0] = x + 10 + (int) Math.floorMod(s >> 17, Math.max(1, w - 20));
            s = s * 6364136223846793005L + 1442695040888963407L;
            pts[i][1] = y + 10 + (int) Math.floorMod(s >> 17, Math.max(1, h - 20));
        }

        // Соединительные линии
        for (int i = 0; i < points - 1; i++) {
            faintLine(ctx, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], withAlpha(rgb, 24));
        }

        // Звёзды
        for (int i = 0; i < points; i++) {
            float tw = 0.5f + 0.5f * MathHelper.sin(t * 0.002f + i * 1.9f);
            boolean newest = (i == points - 1);
            int starRgb = newest ? brightenRgb(rgb, 0.6f) : brightenRgb(rgb, 0.35f);
            int alpha = newest ? (int) (150 + 90 * tw) : (int) (50 + 70 * tw);
            int size = newest ? 3 : 2;
            ctx.fill(pts[i][0], pts[i][1], pts[i][0] + size, pts[i][1] + size, withAlpha(starRgb, alpha));
        }
    }

    // ── Карточка (списки заклинаний/перков) ──────────────────────────────

    /**
     * Карточка списка: градиентная подложка, акцентная полоска слева,
     * мягкая рамка (ярче при подсветке).
     */
    public static void drawCard(DrawContext ctx, int x, int y, int w, int h, int accentRgb, boolean highlighted) {
        fillGradientV(ctx, x, y, x + w, y + h, 0xC0221B38, 0xC0120E20);
        drawBorder(ctx, x, y, w, h, withAlpha(accentRgb, highlighted ? 170 : 55), 1);
        fillGradientV(ctx, x + 1, y + 1, x + 4, y + h - 1,
                withAlpha(brightenRgb(accentRgb, 0.25f), 230), withAlpha(dimRgb(accentRgb, 0.35f), 230));
        if (highlighted) {
            // Верхний блик
            ctx.fill(x + 4, y + 1, x + w - 1, y + 2, withAlpha(0xFFFFFF, 24));
        }
    }

    // ── Иконки и цвета стихий ────────────────────────────────────────────

    /** Цвет стихии заклинания */
    public static int elementRgb(com.manaborn.spells.SpellType spell) {
        return switch (spell) {
            case FIREBALL, FIRE_WALL, INNER_HEAT, HEAT_WEAPON -> 0xFF7A2E;
            case ICE_SHARD, ICE_WALL, FROST_AURA, FROST_COFFIN -> 0x66D0FF;
            case ABSORBING_BARRIER, WEAKNESS_CURSE -> 0x8A6AD9;
            case SOUND_STRIKE, BATTLE_HYMN, SILENCE_ZONE, CLARIFYING_TONE -> 0xFFD94A;
            case TRANSLOCATION -> 0xD8D8E0;
        };
    }

    /**
     * Процедурная иконка стихии заклинания (~11px), центр в (cx, cy).
     * Не зависит от наличия символов в шрифте — всё рисуется заливками.
     */
    public static void drawSpellElementIcon(DrawContext ctx, TextRenderer tr, com.manaborn.spells.SpellType spell,
                                            int cx, int cy, boolean bright) {
        int argb = 0xFF000000 | (bright ? brightenRgb(elementRgb(spell), 0.25f) : elementRgb(spell));
        switch (spell) {
            case FIREBALL, FIRE_WALL, INNER_HEAT, HEAT_WEAPON -> {
                String[] flame = {
                        "...#...",
                        "...##..",
                        "..###..",
                        "..###..",
                        ".#####.",
                        ".#####.",
                        "..####.",
                        "..###..",
                };
                drawMiniBitmap(ctx, flame, cx, cy, argb);
                ctx.fill(cx - 1, cy, cx + 2, cy + 3, 0xFFFFD977); // яркое ядро
            }
            case ICE_SHARD, ICE_WALL, FROST_AURA, FROST_COFFIN -> {
                ctx.fill(cx, cy - 5, cx + 1, cy + 6, argb);
                ctx.fill(cx - 5, cy, cx + 6, cy + 1, argb);
                for (int i = -3; i <= 3; i++) {
                    ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, argb);
                    ctx.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, argb);
                }
            }
            case ABSORBING_BARRIER, WEAKNESS_CURSE -> {
                fillDisc(ctx, cx - 1, cy, 5, argb);
                fillDisc(ctx, cx + 2, cy - 2, 4, 0xFF0C081A); // вырез — полумесяц
            }
            case SOUND_STRIKE, BATTLE_HYMN, SILENCE_ZONE, CLARIFYING_TONE -> {
                String note = "♪";
                ctx.drawText(tr, note, cx - tr.getWidth(note) / 2, cy - 4, argb, true);
            }
            case TRANSLOCATION -> {
                // Звезда хаоса: восемь лучей из пустого центра
                String[] star = {
                        "#..#..#",
                        ".##.##.",
                        "..#.#..",
                        "##...##",
                        "..#.#..",
                        ".##.##.",
                        "#..#..#",
                };
                drawMiniBitmap(ctx, star, cx, cy, argb);
            }
        }
    }


    private static void drawMiniBitmap(DrawContext ctx, String[] rows, int cx, int cy, int argb) {
        int h = rows.length;
        int w = rows[0].length();
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                if (rows[r].charAt(c) == '#') {
                    ctx.fill(cx - w / 2 + c, cy - h / 2 + r,
                            cx - w / 2 + c + 1, cy - h / 2 + r + 1, argb);
                }
            }
        }
    }

    private static void fillDisc(DrawContext ctx, int cx, int cy, int radius, int argb) {
        for (int dy = -radius; dy <= radius; dy++) {
            int hw = (int) Math.sqrt(radius * radius - dy * dy);
            if (hw >= 0) {
                ctx.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, argb);
            }
        }
    }

    // ── Вкладка ──────────────────────────────────────────────────────────

    /**
     * Вкладка с акцентным цветом: выбранная — градиент акцента и яркая рамка
     * со светящейся нижней кромкой, невыбранная — приглушённая.
     */
    public static void drawTabButton(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h,
                                     String label, int accentRgb, boolean selected) {
        if (selected) {
            fillGradientV(ctx, x, y, x + w, y + h,
                    withAlpha(dimRgb(accentRgb, 0.55f), 255), withAlpha(dimRgb(accentRgb, 0.25f), 255));
        } else {
            fillGradientV(ctx, x, y, x + w, y + h, 0xE61A1428, 0xE6120E1E);
        }
        drawBorder(ctx, x, y, w, h, withAlpha(accentRgb, selected ? 220 : 70), 1);
        if (selected) {
            ctx.fill(x + 1, y + h, x + w - 1, y + h + 2, withAlpha(brightenRgb(accentRgb, 0.3f), 200));
        }
        int tw = tr.getWidth(label);
        ctx.drawText(tr, label, x + (w - tw) / 2, y + (h - 8) / 2, selected ? 0xFFFFFFFF : COLOR_TEXT_DIM, true);
    }

    private static void faintLine(DrawContext ctx, int x1, int y1, int x2, int y2, int argb) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            ctx.fill(x1, y1, x1 + 1, y1 + 1, argb);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }
}
