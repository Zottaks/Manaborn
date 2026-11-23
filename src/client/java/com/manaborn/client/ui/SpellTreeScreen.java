package com.manaborn.client.ui;

import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.client.state.ClientSpellLevelsState;
import com.manaborn.client.state.ClientPassivesState;
import com.manaborn.passives.PassiveType;
import com.manaborn.network.PassiveSlotsUpdateC2SPacket;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellRegistry;
import com.manaborn.spells.SpellType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Дерево магии Manaborn (клавиша I) — единый визуальный стиль.
 *
 * Вкладки стихий + вкладка пассивок.
 * Пассивки: ЛКМ по перку — описание, перетаскивание — в активный слот.
 */
public class SpellTreeScreen extends Screen {
    public SpellTreeScreen() {
        super(Text.literal("Магия Manaborn"));
    }

    // ── Акценты стихий ───────────────────────────────────────────────────
    private static final int ACCENT_FIRE = 0xFF8C00;
    private static final int ACCENT_ICE = 0x4FC3F7;
    private static final int ACCENT_DARK = 0x9B7BFF;
    private static final int ACCENT_SOUND = 0xFFD700;
    private static final int ACCENT_ENTROPY = 0xC7D0DD;
    private static final int ACCENT_PASSIVE = 0xBFA8FF;

    // ── Состояние ────────────────────────────────────────────────────────
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private PassiveType draggingPassive = null;
    private boolean dragging = false;
    private PassiveType potentialDrag = null; // клик начат, но перетаскивание ещё нет

    private PassiveType selectedPassive = null; // перк, чьё описание показано

    private int passivesListScrollOffset = 0;
    private int passivesListMaxScroll = 0;

    private int passiveDescScrollOffset = 0;
    private int passiveDescMaxScroll = 0;

    // Координаты элементов (обновляются при рендере)
    private int passiveDescX, passiveDescY, passiveDescW, passiveDescH;
    private int passiveSlotX, passiveSlot0Y, passiveSlot1Y, passiveSlotSize;
    private int passivesButtonX, passivesButtonY, passivesButtonSize;
    private int passiveListX, passiveListY, passiveListW, passiveCardH = 34, passiveCardGap = 6;

    private int tabBarY, tabWidth = 70, tabHeight = 16, tabGap = 6, tabsStartX;

    private enum Tab { FIRE, ICE, DARK, SOUND, ENTROPY, PASSIVES }
    private Tab selectedTab = Tab.FIRE;

    // ── Рендер ───────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int w = this.width;
        int h = this.height;

        // Затемнение мира с фиолетовым подтоном
        context.fill(0, 0, w, h, 0xB0080612);

        int pad = 10;
        int boxTop = 32;
        int boxLeft = pad;
        int boxRight = w - pad;
        int boxBottom = h - pad;

        // Рамка окна в едином стиле
        ManabornGuiStyle.drawFrame(context, boxLeft, boxTop, boxRight - boxLeft, boxBottom - boxTop);

        // Заголовок
        ManabornGuiStyle.drawTitle(context, this.textRenderer, "Дерево магии", w, 10);

        // Подсказки по краям
        context.drawTextWithShadow(this.textRenderer, "Esc — закрыть", 8, 8, ManabornGuiStyle.COLOR_TEXT_DIM);
        int learnedCount = ClientLearnedSpellsState.getLearnedSpells().size();
        String dbg = "Изучено заклинаний: " + learnedCount;
        int dbgW = this.textRenderer.getWidth(dbg);
        context.drawTextWithShadow(this.textRenderer, dbg, w - 8 - dbgW, 8, ManabornGuiStyle.COLOR_TEXT_DIM);

        // Геометрия вкладок
        tabBarY = boxTop + 8;
        tabsStartX = (w - (tabWidth * 5 + tabGap * 4)) / 2;

        passivesButtonSize = 16;
        int passivesTabX = tabsStartX + (tabWidth + tabGap) * 5;
        passivesButtonX = passivesTabX + (tabWidth - passivesButtonSize) / 2;
        passivesButtonY = tabBarY + (tabHeight - passivesButtonSize) / 2;

        // Вкладки стихий (скрыты на вкладке пассивок)
        if (selectedTab != Tab.PASSIVES) {
            Tab[] schoolTabs = {Tab.FIRE, Tab.ICE, Tab.DARK, Tab.SOUND, Tab.ENTROPY};
            String[] labels = {"Огонь", "Лёд", "Тьма", "Звук", "Энтропия"};
            int[] accents = {ACCENT_FIRE, ACCENT_ICE, ACCENT_DARK, ACCENT_SOUND, ACCENT_ENTROPY};
            for (int i = 0; i < schoolTabs.length; i++) {
                int tx = tabsStartX + (tabWidth + tabGap) * i;
                boolean hovered = mouseX >= tx && mouseX <= tx + tabWidth
                        && mouseY >= tabBarY && mouseY <= tabBarY + tabHeight;
                drawElementTab(context, tx, tabBarY, tabWidth, tabHeight,
                        labels[i], accents[i], schoolTabs[i], selectedTab == schoolTabs[i], hovered);
            }
        }

        // Кнопка-иконка пассивок
        ManabornGuiStyle.drawSlot(context, passivesButtonX, passivesButtonY, passivesButtonSize,
                ACCENT_PASSIVE, selectedTab == Tab.PASSIVES);
        context.drawItem(new ItemStack(Items.ENCHANTED_BOOK), passivesButtonX, passivesButtonY);

        int listStartY = tabBarY + tabHeight + 8;
        int listX = boxLeft + 16;

        List<SpellType> fireSpells = Arrays.asList(SpellType.FIREBALL, SpellType.FIRE_WALL, SpellType.INNER_HEAT, SpellType.HEAT_WEAPON);
        List<SpellType> iceSpells = Arrays.asList(SpellType.ICE_SHARD, SpellType.ICE_WALL);
        List<SpellType> darkSpells = Arrays.asList(SpellType.ABSORBING_BARRIER, SpellType.WEAKNESS_CURSE);
        List<SpellType> soundSpells = Arrays.asList(SpellType.SOUND_STRIKE, SpellType.BATTLE_HYMN, SpellType.SILENCE_ZONE, SpellType.CLARIFYING_TONE);
        List<SpellType> entropySpells = Arrays.asList(SpellType.TRANSLOCATION);

        // Баннер стихии
        if (selectedTab == Tab.FIRE) {
            listStartY = renderBanner(context, boxLeft, boxRight, listStartY, "Магия огня", 0xFFFFE0A0, ACCENT_FIRE);
            renderBannerGarnish(context, Tab.FIRE, boxLeft, boxRight, listStartY - 32, "Магия огня", ACCENT_FIRE);
        } else if (selectedTab == Tab.ICE) {
            listStartY = renderBanner(context, boxLeft, boxRight, listStartY, "Магия льда", 0xFFAEEBFF, ACCENT_ICE);
            renderBannerGarnish(context, Tab.ICE, boxLeft, boxRight, listStartY - 32, "Магия льда", ACCENT_ICE);
        } else if (selectedTab == Tab.DARK) {
            listStartY = renderBanner(context, boxLeft, boxRight, listStartY, "Магия тьмы", 0xFFE6D8FF, ACCENT_DARK);
            renderBannerGarnish(context, Tab.DARK, boxLeft, boxRight, listStartY - 32, "Магия тьмы", ACCENT_DARK);
        } else if (selectedTab == Tab.SOUND) {
            listStartY = renderBanner(context, boxLeft, boxRight, listStartY, "♪ Магия звука", 0xFFFFEE99, ACCENT_SOUND);
            renderBannerGarnish(context, Tab.SOUND, boxLeft, boxRight, listStartY - 32, "♪ Магия звука", ACCENT_SOUND);
        } else if (selectedTab == Tab.ENTROPY) {
            listStartY = renderBanner(context, boxLeft, boxRight, listStartY, "Магия энтропии", 0xFFEDF2FA, ACCENT_ENTROPY);
            renderBannerGarnish(context, Tab.ENTROPY, boxLeft, boxRight, listStartY - 32, "Магия энтропии", ACCENT_ENTROPY);
        }

        if (selectedTab == Tab.PASSIVES) {
            renderPassives(context, mouseX, mouseY, boxLeft, boxRight, listStartY, boxBottom);
        } else {
            BranchRenderResult res;
            int clipLeft = boxLeft + 8;
            int clipRight = boxRight - 8;
            int clipTop = listStartY;
            int clipBottom = boxBottom - 10;
            context.enableScissor(clipLeft, clipTop, clipRight, clipBottom);
            if (selectedTab == Tab.FIRE) {
                res = renderBranch(context, fireSpells, listX, listStartY);
            } else if (selectedTab == Tab.ICE) {
                res = renderBranch(context, iceSpells, listX, listStartY);
            } else if (selectedTab == Tab.DARK) {
                res = renderBranch(context, darkSpells, listX, listStartY);
            } else if (selectedTab == Tab.ENTROPY) {
                res = renderBranch(context, entropySpells, listX, listStartY);
            } else {
                res = renderBranch(context, soundSpells, listX, listStartY);
            }
            context.disableScissor();

            maxScroll = Math.max(0, res.contentHeight - (boxBottom - listStartY - 10));
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;

            if (!res.any) {
                String empty = "Нет изученных заклинаний";
                String tip = "Используйте книги заклинаний, чтобы изучать их";
                int ew = this.textRenderer.getWidth(empty);
                int tw = this.textRenderer.getWidth(tip);
                int cy = (h / 2) - 10;
                context.drawTextWithShadow(this.textRenderer, empty, (w - ew) / 2, cy, 0xFFFFFFFF);
                context.drawTextWithShadow(this.textRenderer, tip, (w - tw) / 2, cy + 12, ManabornGuiStyle.COLOR_TEXT_DIM);
            }
        }

        // Перетаскиваемый перк следует за курсором
        if (dragging && draggingPassive != null) {
            String t = draggingPassive.getDisplayName();
            int tw = this.textRenderer.getWidth(t);
            ManabornGuiStyle.drawPanel(context, mouseX + 8, mouseY + 8, tw + 24, 22);
            context.drawItem(getPassiveIcon(draggingPassive), mouseX + 12, mouseY + 11);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(t).formatted(draggingPassive.getColor()), mouseX + 32, mouseY + 15, 0xFFFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    // ── Баннер стихии ────────────────────────────────────────────────────

    private int renderBanner(DrawContext context, int boxLeft, int boxRight, int top, String label, int textColor, int accentRgb) {
        int bannerHeight = 26;
        int bannerLeft = boxLeft + 8;
        int bannerRight = boxRight - 8;

        // Градиентная подложка в цвет стихии (приглушённая)
        ManabornGuiStyle.fillGradientV(context, bannerLeft, top, bannerRight, top + bannerHeight,
                ManabornGuiStyle.withAlpha(ManabornGuiStyle.dimRgb(accentRgb, 0.18f), 70),
                ManabornGuiStyle.withAlpha(ManabornGuiStyle.dimRgb(accentRgb, 0.08f), 70));
        // Акцентные кромки
        context.fill(bannerLeft, top, bannerRight, top + 1, ManabornGuiStyle.withAlpha(accentRgb, 150));
        context.fill(bannerLeft, top + bannerHeight - 1, bannerRight, top + bannerHeight, ManabornGuiStyle.withAlpha(accentRgb, 150));

        context.drawTextWithShadow(this.textRenderer, label, bannerLeft + 8, top + 9, textColor);
        return top + bannerHeight + 6;
    }

    /**
     * Декоративная полоса справа от названия школы в баннере.
     * У каждой школы — своя живая анимация.
     */
    private void renderBannerGarnish(DrawContext context, Tab tab, int boxLeft, int boxRight,
                                     int bannerTop, String label, int accentRgb) {
        long t = Util.getMeasuringTimeMs();
        int stripX = boxLeft + 8 + this.textRenderer.getWidth(label) + 16;
        int stripRight = boxRight - 16;
        if (stripRight - stripX < 60) return; // слишком узкое окно — не рисуем
        int cy = bannerTop + 13; // середина баннера (высота 26)

        switch (tab) {
            case FIRE -> {
                // Переливающиеся столбы пламени с жаркими ядрами и угольками
                int i = 0;
                for (int px = stripX + 4; px < stripRight - 4; px += 14, i++) {
                    float wave = 0.5f + 0.5f * MathHelper.sin(t * 0.004f + i * 1.3f);
                    int flameH = 4 + (int) (wave * 7);
                    int baseY = bannerTop + 24;
                    for (int dy = 0; dy < flameH; dy++) {
                        float f = dy / (float) Math.max(1, flameH);
                        int rgb = ManabornGuiStyle.blendRgb(0x8C2E00, 0xFFCC66, f);
                        context.fill(px, baseY - dy, px + 3, baseY - dy + 1, ManabornGuiStyle.withAlpha(rgb, 200));
                    }
                    // Жаркое ядро
                    context.fill(px + 1, baseY - flameH + 2, px + 2, baseY,
                            ManabornGuiStyle.withAlpha(0xFFE9B0, 160 + (int) (60 * wave)));
                    // Уголёк, поднимающийся над пламенем
                    float rise = ((t * 0.0011f) + i * 0.29f) % 1.0f;
                    int ey = baseY - flameH - 1 - (int) (rise * 6);
                    int alpha = (int) ((1.0f - rise) * 180);
                    if (alpha > 0) {
                        context.fill(px + 1, ey, px + 2, ey + 1, ManabornGuiStyle.withAlpha(0xFFAA33, alpha));
                    }
                }
            }
            case ICE -> {
                // Сосульки под верхней кромкой баннера
                int i = 0;
                for (int px = stripX + 6; px < stripRight - 6; px += 12, i++) {
                    int len = 2 + (i % 3);
                    context.fill(px, bannerTop + 1, px + 1, bannerTop + 1 + len,
                            ManabornGuiStyle.withAlpha(0xBFE8FF, 170));
                    if (len > 2) {
                        context.fill(px, bannerTop + 1 + len, px + 1, bannerTop + 2 + len,
                                ManabornGuiStyle.withAlpha(0xBFE8FF, 90));
                    }
                }
                // Дрейфующие снежинки
                for (int s = 0; s < 4; s++) {
                    float p = ((t * 0.00035f) + s * 0.27f) % 1.0f;
                    int sx = stripX + 4 + (int) (p * (stripRight - stripX - 8));
                    int sy = bannerTop + 12 + (int) (Math.sin((p + s * 0.31f) * Math.PI * 2) * 5);
                    int alpha = (int) (Math.sin(p * Math.PI) * 200);
                    if (alpha > 0) {
                        drawMiniDiamond(context, sx, sy, ManabornGuiStyle.withAlpha(0xDDF2FF, alpha));
                    }
                }
                // Блик, пробегающий по нижней кромке
                float glint = (t * 0.0007f) % 1.0f;
                int gx = stripX + (int) (glint * (stripRight - stripX - 2));
                int ga = (int) (Math.sin(glint * Math.PI) * 160);
                if (ga > 0) {
                    context.fill(gx, bannerTop + 24, gx + 2, bannerTop + 25, ManabornGuiStyle.withAlpha(0xFFFFFF, ga));
                }
            }
            case DARK -> {
                // Дрейфующие теневые огоньки с мягким свечением
                for (int s = 0; s < 5; s++) {
                    float p = ((t * 0.0005f) + s * 0.21f) % 1.0f;
                    int sx = stripX + 6 + (int) (p * (stripRight - stripX - 12));
                    int sy = cy + (int) (Math.sin((p * 3 + s) * Math.PI * 2) * 6);
                    int alpha = (int) (Math.sin(p * Math.PI) * 190);
                    if (alpha > 0) {
                        context.fill(sx - 1, sy - 1, sx + 2, sy + 2, ManabornGuiStyle.withAlpha(accentRgb, alpha / 3));
                        context.fill(sx, sy, sx + 1, sy + 1,
                                ManabornGuiStyle.withAlpha(ManabornGuiStyle.brightenRgb(accentRgb, 0.3f), alpha));
                    }
                }
                // Тонкая дымка вдоль нижней кромки
                for (int px = stripX; px < stripRight - 2; px += 5) {
                    float wisp = 0.5f + 0.5f * MathHelper.sin(t * 0.002f + px * 0.35f);
                    context.fill(px, bannerTop + 24, px + 3, bannerTop + 25,
                            ManabornGuiStyle.withAlpha(accentRgb, 20 + (int) (30 * wisp)));
                }
            }
            case SOUND -> {
                // Мини-эквалайзер с плавными столбиками
                int i = 0;
                for (int px = stripX + 2; px < stripRight - 2; px += 5, i++) {
                    float wave = 0.5f + 0.5f * MathHelper.sin(t * 0.005f + i * 0.9f);
                    float wave2 = 0.5f + 0.5f * MathHelper.sin(t * 0.0023f + i * 1.7f);
                    int barH = 2 + (int) ((wave * 0.6f + wave2 * 0.4f) * 9);
                    int rgb = ManabornGuiStyle.blendRgb(accentRgb, 0xFFFFFF, wave * 0.35f);
                    context.fill(px, bannerTop + 24 - barH, px + 2, bannerTop + 24, ManabornGuiStyle.withAlpha(rgb, 190));
                }
                // Плывущая нота
                float p = (t * 0.0006f) % 1.0f;
                int nx = stripX + (int) (p * (stripRight - stripX - 8));
                int ny = bannerTop + 6 + (int) (Math.sin(p * Math.PI * 4) * 3);
                int na = (int) (Math.sin(p * Math.PI) * 220);
                if (na > 0) {
                    context.drawText(this.textRenderer, "♪", nx, ny, ManabornGuiStyle.withAlpha(0xFFEE99, na), true);
                }
            }
            case ENTROPY -> {
                // Хаос и непредсказуемость: разорванная линия и глитч-осколки,
                // перескакивающие на новые места рывками
                long frame = t / 160;

                // Разорванная пунктирная линия — разрывы прыгают каждый «кадр»
                for (int px = stripX; px < stripRight - 3; px += 6) {
                    long seed = chaosHash(frame * 13 + px * 7);
                    if ((seed % 4) == 0) continue; // случайный разрыв
                    int alpha = 60 + (int) (seed >> 8) % 120;
                    context.fill(px, cy, px + 3, cy + 1, ManabornGuiStyle.withAlpha(accentRgb, alpha));
                }

                // Глитч-осколки: случайные прямоугольники случайных цветов
                for (int i = 0; i < 9; i++) {
                    long seed = chaosHash(frame * 31 + i * 977);
                    int gw = 1 + (int) (seed % 3);
                    int gh = 1 + (int) ((seed >> 4) % 2);
                    int gx = stripX + 2 + (int) ((seed >> 8) % Math.max(1, stripRight - stripX - 6));
                    int gy = bannerTop + 3 + (int) ((seed >> 16) % 20);
                    int rgb = chaosColor(seed >> 20, accentRgb);
                    int alpha = 100 + (int) ((seed >> 24) % 140);
                    context.fill(gx, gy, gx + gw, gy + gh, ManabornGuiStyle.withAlpha(rgb, alpha));
                }

                // Редкая крупная вспышка — блок «сдвига пространства»
                long seed = chaosHash(frame * 53);
                if ((seed % 5) == 0) {
                    int fx = stripX + 4 + (int) ((seed >> 8) % Math.max(1, stripRight - stripX - 14));
                    context.fill(fx, bannerTop + 6, fx + 8, bannerTop + 10,
                            ManabornGuiStyle.withAlpha(0xFFFFFF, 60));
                    context.fill(fx + 2, bannerTop + 11, fx + 10, bannerTop + 13,
                            ManabornGuiStyle.withAlpha(0xE86AF0, 80));
                }
            }
            default -> { }
        }
    }

    // ── Ветка заклинаний ─────────────────────────────────────────────────

    private record BranchRenderResult(boolean any, int contentHeight) {}

    private BranchRenderResult renderBranch(DrawContext context, List<SpellType> spells, int x, int startY) {
        int y = startY - scrollOffset;
        int yStart = y;
        boolean any = false;
        int cardWidth = Math.max(200, context.getScaledWindowWidth() - (x + 32));

        for (SpellType spell : spells) {
            if (!ClientLearnedSpellsState.hasLearned(spell)) continue;
            any = true;

            String desc = getDescription(spell);
            String[] lines = desc.split("\n");
            int cardHeight = 46 + (lines.length - 1) * 10;

            int accent = spellAccent(spell);
            ManabornGuiStyle.drawCard(context, x, y, cardWidth, cardHeight, accent, false);

            int spellLevel = ClientSpellLevelsState.getSpellLevel(spell);
            String levelRoman = switch (spellLevel) {
                case 2 -> "II";
                case 3 -> "III";
                default -> "I";
            };

            Text name = Text.literal(spell.getDisplayName() + " " + levelRoman).formatted(spell.getColor());
            context.drawTextWithShadow(this.textRenderer, name, x + 10, y + 6, 0xFFFFFFFF);

            // Звёзды уровня справа от названия
            int starX = x + cardWidth - 34;
            for (int s = 0; s < 3; s++) {
                int starColor = s < spellLevel ? 0xFFFFD700 : 0xFF444444;
                context.drawText(this.textRenderer, "★", starX + s * 10, y + 6, starColor, false);
            }

            // Стоимость и кулдаун зависят от текущего уровня заклинания
            Spell spellImpl = SpellRegistry.getSpell(spell);
            int manaCost = (spellImpl instanceof LeveledSpell leveled)
                    ? leveled.getManaCost(spellLevel)
                    : spell.getManaCost();
            int cooldownTicks = (spellImpl instanceof LeveledSpell leveled)
                    ? leveled.getCooldown(spellLevel)
                    : (spellImpl != null ? spellImpl.getCooldown() : 0);
            String cost = "Стоимость: " + manaCost + " маны  •  Кулдаун: " + String.format(java.util.Locale.ROOT, "%.1f", cooldownTicks / 20.0f) + " с";
            context.drawText(this.textRenderer, Text.literal(cost), x + 10, y + 20, ManabornGuiStyle.COLOR_TEXT, false);

            int lineY = y + 31;
            for (String line : lines) {
                context.drawText(this.textRenderer,
                        Text.literal(line).formatted(net.minecraft.util.Formatting.GRAY), x + 10, lineY, 0xFFC0C0C0, false);
                lineY += 10;
            }

            y += cardHeight + 6;
        }

        return new BranchRenderResult(any, Math.max(0, y - yStart));
    }

    private int spellAccent(SpellType spell) {
        return switch (spell) {
            case ICE_SHARD, ICE_WALL, FROST_AURA, FROST_COFFIN -> ACCENT_ICE;
            case ABSORBING_BARRIER, WEAKNESS_CURSE -> ACCENT_DARK;
            case SOUND_STRIKE, BATTLE_HYMN, SILENCE_ZONE, CLARIFYING_TONE -> ACCENT_SOUND;
            case TRANSLOCATION -> ACCENT_ENTROPY;
            default -> ACCENT_FIRE;
        };
    }

    // ── Вкладки стихий ───────────────────────────────────────────────────

    /**
     * Проработанная вкладка школы магии: градиент в цвете стихии, иконка,
     * подсветка при наведении и живая анимация-«изюминка» на активной вкладке.
     */
    private void drawElementTab(DrawContext ctx, int x, int y, int w, int h,
                                String label, int accentRgb, Tab tab, boolean active, boolean hovered) {
        long t = Util.getMeasuringTimeMs();

        // Тело вкладки
        if (active) {
            ManabornGuiStyle.fillGradientV(ctx, x, y, x + w, y + h,
                    ManabornGuiStyle.withAlpha(ManabornGuiStyle.dimRgb(accentRgb, 0.62f), 255),
                    ManabornGuiStyle.withAlpha(ManabornGuiStyle.dimRgb(accentRgb, 0.28f), 255));
        } else {
            ManabornGuiStyle.fillGradientV(ctx, x, y, x + w, y + h, 0xE61A1428, 0xE6120E1E);
        }

        // Рамка: ярче при выборе или наведении
        int borderAlpha = active ? 235 : (hovered ? 170 : 80);
        int borderRgb = (hovered && !active) ? ManabornGuiStyle.brightenRgb(accentRgb, 0.2f) : accentRgb;
        ManabornGuiStyle.drawBorder(ctx, x, y, w, h, ManabornGuiStyle.withAlpha(borderRgb, borderAlpha), 1);

        // Светящаяся кромка под активной вкладкой
        if (active) {
            ctx.fill(x + 1, y + h, x + w - 1, y + h + 2,
                    ManabornGuiStyle.withAlpha(ManabornGuiStyle.brightenRgb(accentRgb, 0.3f), 200));
        }

        // Иконка школы слева
        int iconColor = 0xFF000000 | (active || hovered
                ? ManabornGuiStyle.brightenRgb(accentRgb, 0.25f)
                : ManabornGuiStyle.dimRgb(accentRgb, 0.9f));
        drawSchoolIcon(ctx, tab, x + 9, y + h / 2, iconColor);

        // Подпись
        int textColor = active ? 0xFFFFFFFF : (hovered ? 0xFFE8E2F8 : ManabornGuiStyle.COLOR_TEXT_DIM);
        int tw = this.textRenderer.getWidth(label);
        ctx.drawText(this.textRenderer, label, x + 17 + (w - 17 - tw) / 2, y + (h - 8) / 2, textColor, true);

        // Изюминка школы — живая анимация на активной вкладке
        if (active) {
            drawSchoolGarnish(ctx, tab, x, y, w, h, accentRgb, t);
        }
    }

    /** Мини-иконка школы магии (~7px), центр в (cx, cy) */
    private void drawSchoolIcon(DrawContext ctx, Tab tab, int cx, int cy, int argb) {
        switch (tab) {
            case FIRE -> {
                String[] flame = {
                        "..#..",
                        "..##.",
                        ".###.",
                        ".###.",
                        "#####",
                        ".###.",
                };
                int h = flame.length;
                int w = flame[0].length();
                for (int r = 0; r < h; r++) {
                    for (int c = 0; c < w; c++) {
                        if (flame[r].charAt(c) == '#') {
                            ctx.fill(cx - w / 2 + c, cy - h / 2 + r,
                                    cx - w / 2 + c + 1, cy - h / 2 + r + 1, argb);
                        }
                    }
                }
                ctx.fill(cx, cy + 1, cx + 1, cy + 2, 0xFFFFD977); // ядро
            }
            case ICE -> {
                ctx.fill(cx, cy - 3, cx + 1, cy + 4, argb);
                ctx.fill(cx - 3, cy, cx + 4, cy + 1, argb);
                for (int i = -2; i <= 2; i++) {
                    ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, argb);
                    ctx.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, argb);
                }
            }
            case DARK -> {
                // Полумесяц
                String[] crescent = {
                        ".##.",
                        "##..",
                        "#...",
                        "##..",
                        ".##.",
                };
                int h = crescent.length;
                int w = crescent[0].length();
                for (int r = 0; r < h; r++) {
                    for (int c = 0; c < w; c++) {
                        if (crescent[r].charAt(c) == '#') {
                            ctx.fill(cx - w / 2 + c, cy - h / 2 + r,
                                    cx - w / 2 + c + 1, cy - h / 2 + r + 1, argb);
                        }
                    }
                }
            }
            case SOUND -> {
                String note = "♪";
                ctx.drawText(this.textRenderer, note, cx - this.textRenderer.getWidth(note) / 2, cy - 4, argb, true);
            }
            case ENTROPY -> {
                // Звезда хаоса: восемь лучей из пустого центра
                String[] star = {
                        "#.#.#",
                        "..#..",
                        "##.##",
                        "..#..",
                        "#.#.#",
                };
                int h = star.length;
                int w = star[0].length();
                for (int r = 0; r < h; r++) {
                    for (int c = 0; c < w; c++) {
                        if (star[r].charAt(c) == '#') {
                            ctx.fill(cx - w / 2 + c, cy - h / 2 + r,
                                    cx - w / 2 + c + 1, cy - h / 2 + r + 1, argb);
                        }
                    }
                }
            }
            default -> { }
        }
    }

    /** Анимированная «изюминка» школы на активной вкладке */
    private void drawSchoolGarnish(DrawContext ctx, Tab tab, int x, int y, int w, int h, int accentRgb, long t) {
        switch (tab) {
            case FIRE -> {
                // Угольки, поднимающиеся над вкладкой
                for (int i = 0; i < 3; i++) {
                    float phase = ((t * 0.0016f) + i * 0.37f) % 1.0f;
                    int ex = x + 10 + i * (w - 20) / 2;
                    int ey = y - 2 - (int) (phase * 6);
                    int alpha = (int) ((1.0f - phase) * 200);
                    if (alpha > 0) {
                        ctx.fill(ex, ey, ex + 1, ey + 1,
                                ManabornGuiStyle.withAlpha(i % 2 == 0 ? 0xFFAA33 : accentRgb, alpha));
                    }
                }
            }
            case ICE -> {
                // Кристаллики на верхних углах
                int crystal = ManabornGuiStyle.withAlpha(ManabornGuiStyle.brightenRgb(accentRgb, 0.3f), 220);
                drawMiniDiamond(ctx, x + 3, y - 2, crystal);
                drawMiniDiamond(ctx, x + w - 4, y - 2, crystal);
                // Блик, пробегающий по верхней кромке
                float p = (t * 0.0009f) % 1.0f;
                int gx = x + 2 + (int) (p * (w - 6));
                int alpha = (int) (Math.sin(p * Math.PI) * 200);
                if (alpha > 0) {
                    ctx.fill(gx, y - 1, gx + 1, y, ManabornGuiStyle.withAlpha(0xFFFFFF, alpha));
                }
            }
            case DARK -> {
                // Фиолетовые огоньки-дымки, дрейфующие под вкладкой
                for (int i = 0; i < 2; i++) {
                    float p = ((t * 0.0012f) + i * 0.5f) % 1.0f;
                    int wx = x + 4 + (int) (p * (w - 10));
                    int wy = y + h + 3 + (int) (Math.sin((p + i) * Math.PI * 2) * 1.5f);
                    int alpha = (int) (Math.sin(p * Math.PI) * 160);
                    if (alpha > 0) {
                        ctx.fill(wx, wy, wx + 2, wy + 1, ManabornGuiStyle.withAlpha(accentRgb, alpha));
                    }
                }
            }
            case SOUND -> {
                // Пульсирующие «столбики звука» над правым углом
                float pulse = 0.5f + 0.5f * MathHelper.sin(t * 0.006f);
                int a1 = (int) (120 + 100 * pulse);
                int a2 = (int) (120 + 100 * (1 - pulse));
                ctx.fill(x + w - 5, y - 2, x + w - 4, y, ManabornGuiStyle.withAlpha(accentRgb, a1));
                ctx.fill(x + w - 3, y - 3, x + w - 2, y, ManabornGuiStyle.withAlpha(accentRgb, a2));
            }
            case ENTROPY -> {
                // Глитч: случайные пиксели-вспышки вокруг вкладки, меняются рывками
                long frame = t / 140;
                for (int i = 0; i < 3; i++) {
                    long seed = chaosHash(frame * 31 + i * 977);
                    int gx = x + 2 + (int) (seed % (w - 4));
                    int gy = y - 2 + (int) ((seed >> 8) % (h + 4));
                    int rgb = chaosColor(seed >> 16, accentRgb);
                    ctx.fill(gx, gy, gx + 2, gy + 1, ManabornGuiStyle.withAlpha(rgb, 150 + (int) (seed >> 20) % 90));
                }
                // Случайный сдвиг нижней светящейся кромки — «выкушенный» кусок
                long seed = chaosHash(frame * 17);
                int gap = x + 2 + (int) (seed % (w - 10));
                ctx.fill(gap, y + h, gap + 5, y + h + 2, 0xFF14101F);
            }
            default -> { }
        }
    }

    /** Детерминированный «случайный» хеш для глитч-эффектов */
    private static long chaosHash(long seed) {
        seed = (seed * 6364136223846793005L + 1442695040888963407L);
        seed ^= seed >> 29;
        seed *= 0xBF58476D1CE4E5B9L;
        seed ^= seed >> 32;
        return seed & 0x7FFFFFFFFFFFFFFFL;
    }

    /** Случайный цвет глитча: серебро / белый / маджента / бирюза */
    private static int chaosColor(long seed, int baseRgb) {
        return switch ((int) (seed % 6)) {
            case 0 -> 0xFFFFFF;
            case 1 -> 0xE86AF0;
            case 2 -> 0x5AF0D8;
            default -> baseRgb;
        };
    }

    private static void drawMiniDiamond(DrawContext ctx, int cx, int cy, int argb) {
        ctx.fill(cx, cy - 1, cx + 1, cy, argb);
        ctx.fill(cx - 1, cy, cx + 2, cy + 1, argb);
        ctx.fill(cx, cy + 1, cx + 1, cy + 2, argb);
    }

    // ── Вкладка пассивок ─────────────────────────────────────────────────

    private void renderPassives(DrawContext context, int mouseX, int mouseY, int boxLeft, int boxRight, int listStartY, int boxBottom) {
        int contentLeft = boxLeft + 16;
        int contentRight = boxRight - 16;
        int contentTop = listStartY;
        int contentW = contentRight - contentLeft;

        int listW = Math.max(180, (int) (contentW * 0.42));
        int listX = contentLeft;
        int rightX = listX + listW + 10;
        int rightW = contentRight - rightX;

        // Сохраняем для хит-теста
        passiveListX = listX;
        passiveListY = contentTop;
        passiveListW = listW;

        int y = contentTop - passivesListScrollOffset;
        int yStart = y;

        List<PassiveType> learned = new java.util.ArrayList<>(ClientPassivesState.getLearned());
        learned.sort(java.util.Comparator.comparing(PassiveType::getId));

        PassiveType[] active = ClientPassivesState.getActive();

        for (PassiveType type : learned) {
            boolean isSelected = type == selectedPassive;
            boolean isActive = (active.length > 0 && active[0] == type) || (active.length > 1 && active[1] == type);

            ManabornGuiStyle.drawCard(context, listX, y, listW, passiveCardH, passiveAccent(type), isSelected);
            context.drawItem(getPassiveIcon(type), listX + 10, y + 9);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(type.getDisplayName()).formatted(type.getColor()), listX + 32, y + 10, 0xFFFFFFFF);

            // Метка активности
            if (isActive) {
                context.drawText(this.textRenderer, "●", listX + listW - 14, y + 10,
                        ManabornGuiStyle.withAlpha(0x9BE89B, 230), false);
            }

            y += passiveCardH + passiveCardGap;
        }

        int contentHeight = Math.max(0, y - yStart);
        passivesListMaxScroll = Math.max(0, contentHeight - (boxBottom - contentTop - 10));
        if (passivesListScrollOffset > passivesListMaxScroll) passivesListScrollOffset = passivesListMaxScroll;

        // Слоты активных перков
        int slotSize = 44;
        int slotGap = 10;
        int slot0Y = contentTop + 10;
        int slot1Y = slot0Y + slotSize + slotGap;

        passiveSlotX = rightX;
        passiveSlot0Y = slot0Y;
        passiveSlot1Y = slot1Y;
        passiveSlotSize = slotSize;

        renderPassiveSlot(context, rightX, slot0Y, slotSize, "Слот 1", active.length > 0 ? active[0] : null);
        renderPassiveSlot(context, rightX, slot1Y, slotSize, "Слот 2", active.length > 1 ? active[1] : null);

        // Панель описания
        int descTop = slot1Y + slotSize + 6;
        int bottomMargin = 16;
        passiveDescX = rightX;
        passiveDescW = Math.max(140, rightW);
        passiveDescY = descTop;
        int availableH = boxBottom - bottomMargin - passiveDescY;
        passiveDescH = Math.max(0, Math.min(90, availableH));

        renderPassiveDescription(context);
    }

    private int passiveAccent(PassiveType type) {
        return switch (type) {
            case SHADOW_SHELTER -> ACCENT_DARK;
            case COAL_HEAT -> ACCENT_FIRE;
            case FROST_SKIN, REACTIVE_FROST -> ACCENT_ICE;
        };
    }

    private void renderPassiveSlot(DrawContext context, int x, int y, int size, String title, PassiveType current) {
        int accent = current != null ? passiveAccent(current) : 0x6E6486;
        ManabornGuiStyle.drawPanel(context, x, y, size, size);
        ManabornGuiStyle.drawCornerBrackets(context, x - 1, y - 1, size + 2, size + 2, accent);
        context.drawTextWithShadow(this.textRenderer, title, x + size + 8, y + 4, ManabornGuiStyle.COLOR_TEXT);

        if (current != null) {
            context.drawItem(getPassiveIcon(current), x + (size - 16) / 2, y + (size - 16) / 2);
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal(current.getDisplayName()).formatted(current.getColor()), x + size + 8, y + 18, 0xFFFFFFFF);
        } else {
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("Пусто").formatted(net.minecraft.util.Formatting.DARK_GRAY), x + size + 8, y + 18, 0xFFFFFFFF);
        }
    }

    private void renderPassiveDescription(DrawContext context) {
        if (passiveDescH <= 0) return;

        ManabornGuiStyle.drawPanel(context, passiveDescX, passiveDescY, passiveDescW, passiveDescH);

        String header = "Описание";
        context.drawTextWithShadow(this.textRenderer, header, passiveDescX + 8, passiveDescY + 6, ManabornGuiStyle.COLOR_TITLE);

        int textX = passiveDescX + 8;
        int textY = passiveDescY + 20;
        int textW = passiveDescW - 16;
        int textH = passiveDescH - 28;

        List<OrderedText> lines = buildPassiveDescriptionLines(textW);
        int lineH = this.textRenderer.fontHeight + 2;
        int totalH = lines.size() * lineH;
        passiveDescMaxScroll = Math.max(0, totalH - textH);
        if (passiveDescScrollOffset > passiveDescMaxScroll) passiveDescScrollOffset = passiveDescMaxScroll;

        context.enableScissor(textX, textY, textX + textW, textY + textH);
        int y = textY - passiveDescScrollOffset;
        for (OrderedText line : lines) {
            context.drawText(this.textRenderer, line, textX, y, 0xFFC0C0C0, false);
            y += lineH;
        }
        context.disableScissor();
    }

    private List<OrderedText> buildPassiveDescriptionLines(int width) {
        List<OrderedText> out = new java.util.ArrayList<>();

        // Приоритет: выбранный кликом перк
        if (selectedPassive != null) {
            out.addAll(this.textRenderer.wrapLines(
                    Text.literal(selectedPassive.getDisplayName()).formatted(selectedPassive.getColor()), width));
            for (String line : getPassiveDescription(selectedPassive).split("\n")) {
                out.addAll(this.textRenderer.wrapLines(
                        Text.literal(line).formatted(net.minecraft.util.Formatting.GRAY), width));
            }
            out.add(OrderedText.EMPTY);
            out.addAll(this.textRenderer.wrapLines(
                    Text.literal("Перетащите перк в слот выше, чтобы активировать")
                            .formatted(net.minecraft.util.Formatting.DARK_GRAY, net.minecraft.util.Formatting.ITALIC), width));
            return out;
        }

        // Иначе — активные перки
        PassiveType[] active = ClientPassivesState.getActive();
        PassiveType s0 = active.length > 0 ? active[0] : null;
        PassiveType s1 = active.length > 1 ? active[1] : null;

        if (s0 == null && s1 == null) {
            out.addAll(this.textRenderer.wrapLines(
                    Text.literal("Нажмите ЛКМ на перк, чтобы увидеть описание.")
                            .formatted(net.minecraft.util.Formatting.GRAY), width));
            out.add(OrderedText.EMPTY);
            out.addAll(this.textRenderer.wrapLines(
                    Text.literal("Перетащите перк в слот, чтобы активировать его.")
                            .formatted(net.minecraft.util.Formatting.DARK_GRAY), width));
            return out;
        }

        if (s0 != null) {
            Text header = Text.literal("Слот 1: ").formatted(net.minecraft.util.Formatting.GRAY)
                    .append(Text.literal(s0.getDisplayName()).formatted(s0.getColor()));
            out.addAll(this.textRenderer.wrapLines(header, width));
            for (String line : getPassiveDescription(s0).split("\n")) {
                out.addAll(this.textRenderer.wrapLines(Text.literal(line).formatted(net.minecraft.util.Formatting.GRAY), width));
            }
            out.add(OrderedText.EMPTY);
        }
        if (s1 != null) {
            Text header = Text.literal("Слот 2: ").formatted(net.minecraft.util.Formatting.GRAY)
                    .append(Text.literal(s1.getDisplayName()).formatted(s1.getColor()));
            out.addAll(this.textRenderer.wrapLines(header, width));
            for (String line : getPassiveDescription(s1).split("\n")) {
                out.addAll(this.textRenderer.wrapLines(Text.literal(line).formatted(net.minecraft.util.Formatting.GRAY), width));
            }
        }

        while (!out.isEmpty() && out.get(out.size() - 1) == OrderedText.EMPTY) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    private String getPassiveDescription(PassiveType type) {
        return switch (type) {
            case SHADOW_SHELTER -> "Магия тьмы\n" +
                    "При уровне освещения ≤ 7:\n" +
                    "- +5% к скорости передвижения\n" +
                    "- Мобы агрятся с меньшей дистанции";
            case COAL_HEAT -> "Магия огня\n" +
                    "- Огонь и лава наносят на 15% меньше урона\n" +
                    "- При получении урона в ближнем бою: ~10% шанс поджечь атакующего на 2 сек";
            case FROST_SKIN -> "Магия льда\n" +
                    "- Замедление и Обморожение длятся на 25% меньше\n" +
                    "- В снежных биомах: +1 к броне";
            case REACTIVE_FROST -> "Магия льда\n" +
                    "При получении урона:\n" +
                    "- 15% шанс наложить Замедление I на атакующего на 2 сек\n" +
                    "- КД ~10 сек";
        };
    }

    private ItemStack getPassiveIcon(PassiveType type) {
        if (type == null) {
            return new ItemStack(Items.BARRIER);
        }
        return switch (type) {
            case COAL_HEAT -> new ItemStack(Items.FIRE_CHARGE);
            case FROST_SKIN -> new ItemStack(Items.PACKED_ICE);
            case REACTIVE_FROST -> new ItemStack(Items.SNOWBALL);
            case SHADOW_SHELTER -> new ItemStack(Items.ENDER_PEARL);
        };
    }

    // ── Ввод ─────────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedTab == Tab.PASSIVES) {
            boolean inDesc = mouseX >= passiveDescX && mouseX <= passiveDescX + passiveDescW
                    && mouseY >= passiveDescY && mouseY <= passiveDescY + passiveDescH;
            if (inDesc && passiveDescMaxScroll > 0) {
                passiveDescScrollOffset -= (int) (verticalAmount * 18);
                passiveDescScrollOffset = Math.max(0, Math.min(passiveDescMaxScroll, passiveDescScrollOffset));
                return true;
            }

            if (passivesListMaxScroll > 0) {
                passivesListScrollOffset -= (int) (verticalAmount * 18);
                passivesListScrollOffset = Math.max(0, Math.min(passivesListMaxScroll, passivesListScrollOffset));
                return true;
            }

            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        scrollOffset -= (int) (verticalAmount * 18);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() == 0) {
            int mx = (int) click.x();
            int my = (int) click.y();

            // Кнопка-иконка пассивок
            if (mx >= passivesButtonX && mx <= passivesButtonX + passivesButtonSize
                    && my >= passivesButtonY && my <= passivesButtonY + passivesButtonSize) {
                selectedTab = (selectedTab == Tab.PASSIVES) ? Tab.FIRE : Tab.PASSIVES;
                scrollOffset = 0;
                return true;
            }

            if (selectedTab != Tab.PASSIVES) {
                // Вкладки стихий
                Tab[] tabs = {Tab.FIRE, Tab.ICE, Tab.DARK, Tab.SOUND, Tab.ENTROPY};
                for (int i = 0; i < tabs.length; i++) {
                    int tx = tabsStartX + (tabWidth + tabGap) * i;
                    if (mx >= tx && mx <= tx + tabWidth && my >= tabBarY && my <= tabBarY + tabHeight) {
                        selectedTab = tabs[i];
                        scrollOffset = 0;
                        return true;
                    }
                }
            } else {
                // Перк из списка: клик (описание) или начало перетаскивания
                PassiveType hit = hitTestPassive(mx, my);
                if (hit != null) {
                    potentialDrag = hit;
                    passiveDescScrollOffset = 0;
                    return true;
                }

                // Клик по слоту — показать описание перка в нём
                int slot = hitTestSlot(mx, my);
                if (slot >= 0) {
                    PassiveType[] active = ClientPassivesState.getActive();
                    PassiveType p = slot == 0
                            ? (active.length > 0 ? active[0] : null)
                            : (active.length > 1 ? active[1] : null);
                    if (p != null) {
                        selectedPassive = p;
                        passiveDescScrollOffset = 0;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        // Перетаскивание начинается только при движении мыши с зажатой ЛКМ
        if (click.button() == 0 && potentialDrag != null && !dragging) {
            dragging = true;
            draggingPassive = potentialDrag;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && selectedTab == Tab.PASSIVES && potentialDrag != null) {
            if (dragging) {
                // Завершение перетаскивания: назначение в слот
                int slot = hitTestSlot((int) click.x(), (int) click.y());
                if (slot >= 0 && draggingPassive != null) {
                    PassiveType[] active = ClientPassivesState.getActive();
                    PassiveType s0 = active.length > 0 ? active[0] : null;
                    PassiveType s1 = active.length > 1 ? active[1] : null;
                    if (slot == 0) {
                        s0 = draggingPassive;
                    } else {
                        s1 = draggingPassive;
                    }
                    ClientPlayNetworking.send(new PassiveSlotsUpdateC2SPacket(
                            s0 != null ? s0.getId() : "",
                            s1 != null ? s1.getId() : ""
                    ));
                }
            } else {
                // Просто клик — показать описание
                selectedPassive = potentialDrag;
                passiveDescScrollOffset = 0;
            }
            dragging = false;
            draggingPassive = null;
            potentialDrag = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    private PassiveType hitTestPassive(int mouseX, int mouseY) {
        int y = passiveListY - passivesListScrollOffset;

        List<PassiveType> learned = new java.util.ArrayList<>(ClientPassivesState.getLearned());
        learned.sort(java.util.Comparator.comparing(PassiveType::getId));

        for (PassiveType type : learned) {
            if (mouseX >= passiveListX && mouseX <= passiveListX + passiveListW
                    && mouseY >= y && mouseY <= y + passiveCardH) {
                return type;
            }
            y += passiveCardH + passiveCardGap;
        }
        return null;
    }

    private int hitTestSlot(int mouseX, int mouseY) {
        if (mouseX >= passiveSlotX && mouseX <= passiveSlotX + passiveSlotSize
                && mouseY >= passiveSlot0Y && mouseY <= passiveSlot0Y + passiveSlotSize) {
            return 0;
        }
        if (mouseX >= passiveSlotX && mouseX <= passiveSlotX + passiveSlotSize
                && mouseY >= passiveSlot1Y && mouseY <= passiveSlot1Y + passiveSlotSize) {
            return 1;
        }
        return -1;
    }

    // ── Описания заклинаний ──────────────────────────────────────────────

    private String getDescription(SpellType spell) {
        int level = ClientSpellLevelsState.getSpellLevel(spell);

        switch (spell) {
            case FIREBALL:
                float fbDamage = switch (level) {
                    case 1 -> 0.25f;
                    case 2 -> 1.25f;
                    default -> 2.0f;
                };
                return "Запускает огненный шар, взрываясь при попадании.\nУрон взрыва: " + fbDamage + " ❤";
            case ICE_SHARD:
                float isDamage = switch (level) {
                    case 1 -> 2.0f;
                    case 2 -> 3.0f;
                    default -> 4.0f;
                };
                return "Быстрый ледяной осколок. Урон: " + (isDamage / 2.0f) + " ❤\nЗамедляет цель.";
            case ICE_WALL:
                return "Создает стену из льда. Удерживайте ПКМ для увеличения размера.";
            case FIRE_WALL:
                return "Создает стену огня, наносящую урон при соприкосновении.";
            case INNER_HEAT:
                return "Наполняет вас огненной энергией, временно увеличивая скорость.";
            case HEAT_WEAPON:
                return "Нагревает ваши атаки, поджигая цели.\n10-я атака вызывает взрыв без урона владельцу.";
            case ABSORBING_BARRIER:
                return "Темный барьер отражает вражеские снаряды и восстанавливает ману.";
            case WEAKNESS_CURSE:
                return "Проклинает цель, значительно снижая наносимый ею урон.";
            case SOUND_STRIKE:
                float ssDamage = switch (level) {
                    case 1 -> 1.0f;
                    case 2 -> 3.0f;
                    default -> 4.0f;
                };
                return "Точный звуковой удар. Урон: " + (ssDamage / 2.0f) + " ❤\nОглушает цель. Требует неподвижности кастующего.";
            case BATTLE_HYMN:
                int channelTime = switch (level) {
                    case 1 -> 7;
                    case 2 -> 6;
                    default -> 5;
                };
                int radius = switch (level) {
                    case 1 -> 3;
                    case 2 -> 4;
                    default -> 5;
                };
                String strength = level >= 3 ? "II" : "I";
                int duration = switch (level) {
                    case 1 -> 10;
                    case 2 -> 20;
                    default -> 15;
                };
                return "Каналируемое заклинание. Проигрывается " + channelTime + "с.\nДаёт игрокам в радиусе " + radius + " блоков Силу " + strength + " на " + duration + "с.\nТребует неподвижности. Уязвим во время каста.";
            case SILENCE_ZONE:
                int szDuration = switch (level) {
                    case 1 -> 7;
                    case 2 -> 8;
                    default -> 10;
                };
                int szRadius = switch (level) {
                    case 1 -> 5;
                    case 2 -> 6;
                    default -> 7;
                };
                return "Создаёт зону тишины радиусом " + szRadius + " блоков.\nДлительность: " + szDuration + " сек. Блокирует ВСЕ заклинания\nвнутри зоны (включая ваши).";
            case CLARIFYING_TONE:
                int ctRadius = switch (level) {
                    case 1 -> 2;
                    case 2 -> 3;
                    default -> 5;
                };
                String levelDesc = switch (level) {
                    case 1 -> "снимает лёгкие дебаффы (Слабость, Медлительность I, Голод, слабый яд)";
                    case 2 -> "снимает лёгкие и средние дебаффы (до Медлительности II, Слепота, Усталость, усиленный яд)";
                    default -> "снимает почти все отрицательные эффекты, кроме Обморожения";
                };
                return "Проясняющий звуковой тон.\nОдиночная цель по взгляду или вы сами.\nShift+ПКМ — область " + ctRadius + " блоков вокруг вас.\n" + levelDesc + ".";
            default:
                return "Заклинание.";
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
