package com.manaborn.client;

import com.manaborn.client.state.ClientSpellLevelsState;
import com.manaborn.client.ui.ManabornGuiStyle;
import com.manaborn.items.MagicStaffItem;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellRegistry;
import com.manaborn.spells.SpellType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Панель выбранного заклинания в правом верхнем углу.
 *
 * Ремейк старого визуала: тематический градиент и двойная пульсирующая
 * рамка в цветах школы магии, фоновая атмосфера школы (искры, снежинки,
 * нотный стан) и у каждого заклинания — свой узнаваемый анимированный
 * штрих (комета, языки пламени, ноты, глитч и т.д.).
 *
 * Панель плавно исчезает через 3 секунды после смены заклинания.
 */
public final class SpellHudOverlay {

    private static SpellType lastSpell = null;
    private static long lastSpellChangeTime = 0;
    private static final long FADE_DELAY_MS = 3000;
    private static final long FADE_DURATION_MS = 500;

    // ── Школы магии: палитры панели ──────────────────────────────────────

    private enum School {
        FIRE(0x330000, 0x660000, 0xFF6600, 0xFFAA00, 0xFF8800),
        ICE(0x001133, 0x003366, 0x4DA3FF, 0x00FFFF, 0x00CCFF),
        DARK(0x0A0A12, 0x1A1426, 0x3A2A5F, 0x5C3B89, 0x8A6AD9),
        SOUND(0x1A1A1A, 0x333333, 0xFFFF00, 0xFFDD00, 0xFFFFFF),
        ENTROPY(0x141414, 0x1E1E1E, 0xD0D0D0, 0x888888, 0x5A5A5A);

        final int bgTop;
        final int bgBottom;
        final int borderA;
        final int borderB;
        final int inner;

        School(int bgTop, int bgBottom, int borderA, int borderB, int inner) {
            this.bgTop = bgTop;
            this.bgBottom = bgBottom;
            this.borderA = borderA;
            this.borderB = borderB;
            this.inner = inner;
        }
    }

    private static School schoolOf(SpellType spell) {
        return switch (spell) {
            case FIREBALL, FIRE_WALL, INNER_HEAT, HEAT_WEAPON -> School.FIRE;
            case ICE_SHARD, ICE_WALL, FROST_AURA, FROST_COFFIN -> School.ICE;
            case ABSORBING_BARRIER, WEAKNESS_CURSE -> School.DARK;
            case SOUND_STRIKE, BATTLE_HYMN, SILENCE_ZONE, CLARIFYING_TONE -> School.SOUND;
            case TRANSLOCATION -> School.ENTROPY;
        };
    }

    // ── Рендер ───────────────────────────────────────────────────────────

    public static void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) return;

        // Посох в руках (MagicStaffItem или EtherScepterItem)
        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();
        ItemStack staff = null;
        if (mainHand.getItem() instanceof MagicStaffItem) staff = mainHand;
        else if (offHand.getItem() instanceof MagicStaffItem) staff = offHand;
        if (staff == null) {
            lastSpell = null;
            return;
        }

        SpellType selectedSpell = SpellManager.getSelectedSpell(staff);
        if (selectedSpell == null) {
            lastSpell = null;
            return;
        }

        // Обновляем время при смене заклинания
        if (lastSpell != selectedSpell) {
            lastSpell = selectedSpell;
            lastSpellChangeTime = System.currentTimeMillis();
        }

        // Прозрачность (fade out)
        long timeSinceChange = System.currentTimeMillis() - lastSpellChangeTime;
        float alpha = 1.0f;
        if (timeSinceChange > FADE_DELAY_MS) {
            long fadeTime = timeSinceChange - FADE_DELAY_MS;
            if (fadeTime >= FADE_DURATION_MS) return;
            alpha = 1.0f - (fadeTime / (float) FADE_DURATION_MS);
        }

        int screenWidth = drawContext.getScaledWindowWidth();
        int width = 150;
        int height = 50;
        int x = screenWidth - width - 10;
        int y = 20;

        long t = Util.getMeasuringTimeMs();
        float time = (t % 2000) / 2000.0f;

        School school = schoolOf(selectedSpell);

        drawPanelBase(drawContext, x, y, width, height, school, time, alpha);
        drawAmbient(drawContext, school, x, y, width, height, time, alpha);
        drawSignature(drawContext, selectedSpell, x, y, width, height, t, alpha);
        drawTexts(drawContext, client, selectedSpell, staff, x, y, width, alpha);
    }

    // ── Основа панели: градиент школы + двойная пульсирующая рамка ──────

    private static void drawPanelBase(DrawContext ctx, int x, int y, int w, int h,
                                      School s, float time, float alpha) {
        float pulse = (float) Math.sin(time * Math.PI * 2) * 0.3f + 0.7f;
        int aTop = (int) (alpha * 192) << 24;
        int aFull = (int) (alpha * 255) << 24;

        // Основной фон с градиентом школы
        ManabornGuiStyle.fillGradientV(ctx, x, y, x + w, y + h, aTop | s.bgTop, aFull | s.bgBottom);

        // Внешняя рамка (пульсация между двумя цветами школы)
        int outer = ManabornGuiStyle.blendArgb(aFull | s.borderA, aFull | s.borderB, pulse);
        ctx.fill(x - 2, y - 2, x + w + 2, y - 1, outer);
        ctx.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, outer);
        ctx.fill(x - 2, y - 1, x - 1, y + h + 1, outer);
        ctx.fill(x + w + 1, y - 1, x + w + 2, y + h + 1, outer);

        // Внутренняя рамка
        int inner = aFull | s.inner;
        ctx.fill(x - 1, y - 1, x + w + 1, y, inner);
        ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, inner);
        ctx.fill(x - 1, y, x, y + h, inner);
        ctx.fill(x + w, y, x + w + 1, y + h, inner);

        // Тонкий блик по верхней кромке внутри панели
        int sheen = ((int) (alpha * 24) << 24) | 0xFFFFFF;
        ctx.fill(x + 2, y + 2, x + w - 2, y + 3, sheen);
    }

    // ── Атмосфера школы: мягкий фоновый эффект ───────────────────────────

    private static void drawAmbient(DrawContext ctx, School school, int x, int y, int w, int h,
                                    float time, float alpha) {
        switch (school) {
            case FIRE -> {
                // Редкие поднимающиеся искры
                for (int i = 0; i < 4; i++) {
                    float sparkTime = (time + i / 4.0f) % 1.0f;
                    int sx = x + 8 + (i * 31) % (w - 16);
                    int sy = y + 6 + (int) (sparkTime * (h - 14));
                    int a = (int) ((1.0f - sparkTime) * 140 * alpha);
                    if (a > 0) ctx.fill(sx, sy, sx + 1, sy + 2, (a << 24) | 0xFFAA33);
                }
            }
            case ICE -> {
                // Падающие мерцающие снежинки
                for (int i = 0; i < 5; i++) {
                    float snowTime = (time + i / 5.0f) % 1.0f;
                    int sx = x + 8 + (i * 27) % (w - 16);
                    int sy = y + 5 + (int) (snowTime * (h - 12));
                    int a = (int) ((0.5f + 0.5f * (float) Math.sin(snowTime * Math.PI * 2)) * 150 * alpha);
                    if (a > 0) {
                        int col = (a << 24) | 0xFFFFFF;
                        ctx.fill(sx - 1, sy, sx + 2, sy + 1, col);
                        ctx.fill(sx, sy - 1, sx + 1, sy + 2, col);
                    }
                }
            }
            case DARK -> {
                // Медленно тонущие фиолетовые крупинки
                for (int i = 0; i < 5; i++) {
                    float dt = (time + i * 0.19f) % 1.0f;
                    int sx = x + 8 + (i * 25) % (w - 16);
                    int sy = y + 6 + (int) (dt * (h - 14));
                    int a = (int) ((120 + 100 * Math.sin((dt + i) * Math.PI * 2)) * alpha);
                    if (a > 0) ctx.fill(sx, sy, sx + 2, sy + 2, (a << 24) | 0xA060FF);
                }
            }
            case SOUND -> {
                // Едва заметные линии нотного стана
                int a = (int) (alpha * 50);
                for (int i = 1; i <= 3; i++) {
                    int lineY = y + (h / 4) * i;
                    ctx.fill(x + 5, lineY, x + w - 5, lineY + 1, (a << 24) | 0xFFFFFF);
                }
            }
            case ENTROPY -> {
                // Хаотичные глитч-пиксели, вспыхивающие в случайных местах рывками
                int frame = (int) (time * 12.999f);
                for (int i = 0; i < 6; i++) {
                    long seed = (frame * 31L + i * 977L) * 6364136223846793005L;
                    seed ^= seed >> 29;
                    seed &= 0x7FFFFFFFFFFFFFFFL;
                    int rx = x + 6 + (int) (seed % (w - 14));
                    int ry = y + 4 + (int) ((seed >> 12) % (h - 9));
                    int rgb = switch ((int) ((seed >> 6) % 6)) {
                        case 0 -> 0xFFFFFF;
                        case 1 -> 0xE86AF0;
                        case 2 -> 0x5AF0D8;
                        default -> 0xD8D8E0;
                    };
                    int a = (int) (alpha * (90 + (seed >> 20) % 120));
                    ctx.fill(rx, ry, rx + 2, ry + 1, (a << 24) | rgb);
                }
            }
        }
    }

    // ── Фирменный штрих каждого заклинания ───────────────────────────────

    private static void drawSignature(DrawContext ctx, SpellType spell, int x, int y, int w, int h,
                                      long t, float alpha) {
        int tx = x + 10; // внутренняя область панели
        int cy = y + h / 2;

        switch (spell) {
            case FIREBALL -> {
                // Комета, пролетающая вдоль нижней кромки с хвостом
                float p = (t % 2400) / 2400.0f;
                int cx = tx + (int) (p * (w - 24));
                int a = (int) (alpha * (p < 0.85f ? 255 : (1 - (p - 0.85f) / 0.15f) * 255));
                int yy = y + h - 8;
                ctx.fill(cx, yy, cx + 2, yy + 2, (a << 24) | 0xFFAA33);
                ctx.fill(cx - 3, yy, cx, yy + 1, ((a * 2 / 3) << 24) | 0xFF6600);
                ctx.fill(cx - 5, yy, cx - 3, yy + 1, ((a / 3) << 24) | 0xCC3300);
            }
            case FIRE_WALL -> {
                // Языки пламени вдоль нижней кромки
                float time = (t % 2000) / 2000.0f;
                for (int i = 0; i < 5; i++) {
                    float ft = (time + i / 5.0f) % 1.0f;
                    int fx = tx + 4 + i * ((w - 28) / 4);
                    int fh = (int) ((Math.sin(ft * Math.PI * 2 + i) * 0.5 + 0.5) * 9);
                    ctx.fill(fx, y + h - 6 - fh, fx + 2, y + h - 6, blendGlow(0xFF6600, 0xFFDD00, ft, alpha));
                }
            }
            case INNER_HEAT -> {
                // Тёплое кольцо, пульсирующее от центра панели
                float p = (t % 1600) / 1600.0f;
                int rr = 6 + (int) (p * 14);
                int a = (int) (alpha * (1 - p) * 120);
                if (a > 0) ring(ctx, x + w / 2, cy, rr, (a << 24) | 0xFF9955);
            }
            case HEAT_WEAPON -> {
                // Диагональные проблески жара
                for (int i = 0; i < 2; i++) {
                    float p = ((t * 0.0012f) + i * 0.5f) % 1.0f;
                    int sx = tx + (int) (p * (w - 24));
                    int a = (int) (Math.sin(p * Math.PI) * 90 * alpha);
                    if (a > 0) {
                        for (int j = 0; j < 8; j++) {
                            ctx.fill(sx + j / 2, y + h - 8 - j, sx + j / 2 + 1, y + h - 7 - j, (a << 24) | 0xFFCC88);
                        }
                    }
                }
            }
            case ICE_SHARD -> {
                // Быстрый осколок, проносящийся вдоль нижней кромки
                float p = (t % 1800) / 1800.0f;
                int sx = tx + (int) (p * (w - 24));
                int a = (int) (alpha * (p < 0.85f ? 230 : (1 - (p - 0.85f) / 0.15f) * 230));
                int yy = y + h - 8;
                ctx.fill(sx, yy, sx + 3, yy + 1, (a << 24) | 0xBFE8FF);
                ctx.fill(sx + 1, yy - 1, sx + 2, yy + 2, (a << 24) | 0xBFE8FF);
                ctx.fill(sx - 3, yy, sx, yy + 1, ((a / 2) << 24) | 0x4DA3FF);
            }
            case ICE_WALL -> {
                // Сосульки под верхней кромкой панели
                for (int i = 0; i < 4; i++) {
                    int ix = tx + 6 + i * ((w - 32) / 3);
                    int len = 3 + (i % 2) * 2;
                    ctx.fill(ix, y + 1, ix + 1, y + 1 + len, ((int) (alpha * 170) << 24) | 0xBFE8FF);
                }
            }
            case FROST_AURA -> {
                // Кольца ауры, расходящиеся от центра панели
                for (int i = 0; i < 2; i++) {
                    float p = ((t * 0.0008f) + i * 0.5f) % 1.0f;
                    int rr = 6 + (int) (p * 16);
                    int a = (int) (alpha * (1 - p) * 100);
                    if (a > 0) ring(ctx, x + w / 2, cy, rr, (a << 24) | 0x66D0FF);
                }
            }
            case FROST_COFFIN -> {
                // Ледяные кристаллы в правых углах панели
                drawIceCrystal(ctx, x + w - 10, y + 5, alpha);
                drawIceCrystal(ctx, x + w - 10, y + h - 9, alpha);
            }
            case ABSORBING_BARRIER -> {
                // Блик, пробегающий по панели слева направо (барьерный отблеск)
                float p = (t % 3000) / 3000.0f;
                int sx = tx + (int) (p * (w - 24));
                int a = (int) (Math.sin(p * Math.PI) * 45 * alpha);
                if (a > 0) ctx.fill(sx, y + 4, sx + 4, y + h - 4, (a << 24) | 0xC8A8FF);
            }
            case WEAKNESS_CURSE -> {
                // Тёмные капли, стекающие с верхней кромки
                for (int i = 0; i < 3; i++) {
                    float p = ((t * 0.0007f) + i * 0.33f) % 1.0f;
                    int dx = tx + 8 + i * ((w - 36) / 2);
                    int dy = y + 2 + (int) (p * (h - 10));
                    int a = (int) ((1 - p) * 160 * alpha);
                    if (a > 0) ctx.fill(dx, dy, dx + 1, dy + 3, (a << 24) | 0x5C3B89);
                }
            }
            case SOUND_STRIKE -> {
                // Волновые дуги, расходящиеся вправо от левого края
                for (int i = 0; i < 3; i++) {
                    float p = ((t * 0.0009f) + i * 0.33f) % 1.0f;
                    int wx = x + 12 + (int) (p * 14);
                    int a = (int) ((1 - p) * 200 * alpha);
                    if (a > 0) {
                        int col = (a << 24) | (i % 2 == 0 ? 0xFFFF00 : 0xFFFFFF);
                        ctx.fill(wx, cy - 3, wx + 1, cy + 4, col);
                        ctx.fill(wx + 1, cy - 2, wx + 2, cy + 3, col);
                    }
                }
            }
            case BATTLE_HYMN -> {
                // Пара нот, всплывающих у правого края
                for (int i = 0; i < 2; i++) {
                    float p = ((t * 0.0006f) + i * 0.5f) % 1.0f;
                    int nx = x + w - 20 - i * 12;
                    int ny = y + h - 14 - (int) (p * (h - 24));
                    drawMusicNote(ctx, nx, ny, i == 0 ? 0xFFFF00 : 0xFFFFFF, alpha * (1.0f - p * 0.7f));
                }
            }
            case SILENCE_ZONE -> {
                // Волновые дуги, которые «глохнут» на полпути — тишина поглощает звук
                for (int i = 0; i < 2; i++) {
                    float p = ((t * 0.0009f) + i * 0.5f) % 1.0f;
                    float fade = Math.max(0.0f, 1.0f - p * 2.2f);
                    int a = (int) (fade * 180 * alpha);
                    if (a > 0) {
                        int wx = x + 12 + (int) (p * 10);
                        int col = (a << 24) | 0xFFFF00;
                        ctx.fill(wx, cy - 3, wx + 1, cy + 4, col);
                        ctx.fill(wx + 1, cy - 2, wx + 2, cy + 3, col);
                    }
                }
            }
            case CLARIFYING_TONE -> {
                // Чистые блёстки, поднимающиеся вверх
                for (int i = 0; i < 3; i++) {
                    float p = ((t * 0.0011f) + i * 0.37f) % 1.0f;
                    int sx = tx + 6 + i * ((w - 32) / 2);
                    int sy = y + h - 6 - (int) (p * (h - 14));
                    int a = (int) (Math.sin(p * Math.PI) * 200 * alpha);
                    if (a > 0) {
                        ctx.fill(sx, sy - 1, sx + 1, sy + 2, (a << 24) | 0xFFFFFF);
                        ctx.fill(sx - 1, sy, sx + 2, sy + 1, (a << 24) | 0xFFFFFF);
                    }
                }
            }
            case TRANSLOCATION -> {
                // Глитч-пиксель, вспыхивающий в случайном месте
                long frame = t / 500;
                if (frame % 5 == 0) {
                    int gx = tx + (int) ((frame * 37) % (w - 24));
                    ctx.fill(gx, y + 6, gx + 4, y + 7, ((int) (alpha * 160) << 24) | 0xFFFFFF);
                }
            }
        }
    }

    // ── Тексты ───────────────────────────────────────────────────────────

    private static void drawTexts(DrawContext ctx, MinecraftClient client, SpellType selectedSpell,
                                  ItemStack staff, int x, int y, int width, float alpha) {
        int textAlpha = (int) (alpha * 255) << 24;
        int tx = x + 10;

        int spellLevel = ClientSpellLevelsState.getSpellLevel(selectedSpell);
        String levelRoman = switch (spellLevel) {
            case 1 -> "I";
            case 2 -> "II";
            default -> "III";
        };

        // Название с уровнем — в ярком цвете школы магии
        int nameColor = textAlpha | ManabornGuiStyle.brightenRgb(
                ManabornGuiStyle.elementRgb(selectedSpell), 0.35f);
        Text spellText = Text.literal(selectedSpell.getDisplayName() + " " + levelRoman);
        ctx.drawText(client.textRenderer, spellText, tx, y + 7, nameColor, true);

        // Стоимость маны — зависит от уровня, как при реальном касте
        Spell spellImpl = SpellRegistry.getSpell(selectedSpell);
        int manaCost = (spellImpl instanceof LeveledSpell leveled)
                ? leveled.getManaCost(spellLevel)
                : selectedSpell.getManaCost();
        if (staff.getItem() instanceof com.manaborn.items.EtherScepterItem) {
            manaCost = Math.max(0, manaCost - com.manaborn.items.EtherScepterItem.MANA_DISCOUNT);
        }
        ctx.drawText(client.textRenderer, Text.literal("Мана: " + manaCost), tx, y + 20, textAlpha | 0xFFFFFF, true);

        // Подсказка
        ctx.drawText(client.textRenderer, Text.literal("ПКМ - использовать"), tx, y + 34, textAlpha | 0xAAAAAA, false);
    }

    // ── Примитивы ────────────────────────────────────────────────────────

    /** Смешать два RGB и применить прозрачность HUD */
    private static int blendGlow(int rgb1, int rgb2, float t, float alpha) {
        return ((int) (alpha * 255) << 24) | ManabornGuiStyle.blendRgb(rgb1, rgb2, t);
    }

    /** Контур круга толщиной 1px */
    private static void ring(DrawContext ctx, int cx, int cy, int radius, int argb) {
        int r2 = radius * radius;
        int inner = (radius - 1) * (radius - 1);
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int d2 = dx * dx + dy * dy;
                if (d2 <= r2 && d2 >= inner) {
                    ctx.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, argb);
                }
            }
        }
    }

    /** Маленький ледяной кристалл-крестик */
    private static void drawIceCrystal(DrawContext ctx, int x, int y, float alpha) {
        int a = (int) (alpha * 200);
        int color = (a << 24) | 0xAAFFFF;
        ctx.fill(x + 2, y, x + 3, y + 5, color);
        ctx.fill(x, y + 2, x + 5, y + 3, color);
        ctx.fill(x + 1, y + 1, x + 2, y + 2, color);
        ctx.fill(x + 3, y + 1, x + 4, y + 2, color);
        ctx.fill(x + 1, y + 3, x + 2, y + 4, color);
        ctx.fill(x + 3, y + 3, x + 4, y + 4, color);
    }

    /** Музыкальная нота ♪ */
    private static void drawMusicNote(DrawContext ctx, int x, int y, int baseColor, float alpha) {
        int a = (int) (alpha * 255);
        if (a <= 0) return;
        int color = (a << 24) | (baseColor & 0xFFFFFF);
        ctx.fill(x, y + 4, x + 4, y + 8, color);
        ctx.fill(x + 3, y, x + 5, y + 5, color);
        ctx.fill(x + 4, y, x + 7, y + 3, color);
    }
}
