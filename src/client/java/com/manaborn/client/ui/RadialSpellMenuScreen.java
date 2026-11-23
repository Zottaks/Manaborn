package com.manaborn.client.ui;

import com.manaborn.client.config.SpellMenuMode;
import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.client.state.ClientSpellLevelsState;
import com.manaborn.items.MagicStaffItem;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellRegistry;
import com.manaborn.spells.SpellType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Радиальное меню выбора заклинаний в едином стиле Manaborn.
 *
 * Особенности:
 * - Отображает только изученные заклинания
 * - Каждое заклинание — медальон с процедурной иконкой стихии
 * - Наведение — движением мыши, выбор — ЛКМ (или отпускание Shift в режиме Hold)
 * - Текущее заклинание на посохе отмечено ромбиком
 */
public class RadialSpellMenuScreen extends Screen {
    private static final int MENU_RADIUS = 100; // Радиус меню
    private static final int NODE_RADIUS = 17; // Радиус медальона заклинания
    private static final int CENTER_CIRCLE_RADIUS = 30; // Радиус центрального круга

    private final List<SpellType> learnedSpells;
    private SpellType hoveredSpell = null;
    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;

    // Callback для выбора заклинания
    private final SpellSelectionCallback callback;

    @FunctionalInterface
    public interface SpellSelectionCallback {
        void onSpellSelected(SpellType spell);
    }

    // ── Стихии ───────────────────────────────────────────────────────────

    private enum Element {
        FIRE(0xFF7A2E, "Стихия огня"),
        ICE(0x66D0FF, "Стихия льда"),
        DARK(0x8A6AD9, "Стихия тьмы"),
        SOUND(0xFFD94A, "Магия звука"),
        ENTROPY(0xD8D8E0, "Энтропия");

        final int rgb;
        final String label;

        Element(int rgb, String label) {
            this.rgb = rgb;
            this.label = label;
        }
    }

    private static Element elementOf(SpellType spell) {
        return switch (spell) {
            case FIREBALL, FIRE_WALL, INNER_HEAT, HEAT_WEAPON -> Element.FIRE;
            case ICE_SHARD, ICE_WALL, FROST_AURA, FROST_COFFIN -> Element.ICE;
            case ABSORBING_BARRIER, WEAKNESS_CURSE -> Element.DARK;
            case SOUND_STRIKE, BATTLE_HYMN, SILENCE_ZONE, CLARIFYING_TONE -> Element.SOUND;
            case TRANSLOCATION -> Element.ENTROPY;
        };
    }

    public RadialSpellMenuScreen(SpellSelectionCallback callback) {
        super(Text.literal("Выбор заклинания"));
        this.callback = callback;
        this.learnedSpells = new ArrayList<>(ClientLearnedSpellsState.getLearnedSpells());
    }

    @Override
    protected void init() {
        super.init();
        if (this.client != null) {
            // Захватываем курсор в центр экрана
            double centerX = this.client.getWindow().getScaledWidth() / 2.0;
            double centerY = this.client.getWindow().getScaledHeight() / 2.0;
            GLFW.glfwSetCursorPos(
                    this.client.getWindow().getHandle(),
                    centerX * this.client.getWindow().getScaleFactor(),
                    centerY * this.client.getWindow().getScaleFactor());

            // Запоминаем текущее состояние кнопок мыши: меню открывается по
            // Shift+ПКМ, и зажатая ПКМ не должна мгновенно закрыть его
            this.wasLeftPressed = GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(),
                    GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            this.wasRightPressed = GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(),
                    GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        }
    }

    /**
     * Подтверждение выбора (режим удержания): выбирает наведённое заклинание
     * и закрывает меню. Если ничего не наведено — просто закрывает.
     */
    public void confirmSelection() {
        if (hoveredSpell != null && callback != null) {
            callback.onSpellSelected(hoveredSpell);
        }
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long t = Util.getMeasuringTimeMs();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Тёмный фон с мягкой виньеткой сверху и снизу
        context.fill(0, 0, this.width, this.height, 0xB0060916);
        ManabornGuiStyle.fillGradientV(context, 0, 0, this.width, 42, 0x90120A24, 0x00120A24);
        ManabornGuiStyle.fillGradientV(context, 0, this.height - 42, this.width, this.height, 0x00120A24, 0x90120A24);

        // Тонкое созвездие-подложка
        ManabornGuiStyle.drawConstellation(context, centerX - 160, centerY - 150, 320, 300, 0x4A5A90);

        // Окружности, задающие ритм меню
        ManabornGuiStyle.drawDottedCircle(context, centerX, centerY, MENU_RADIUS, 0x8FA8E8, 30);
        ManabornGuiStyle.drawDottedCircle(context, centerX, centerY, MENU_RADIUS + 24, 0x6A7AB8, 14);
        ManabornGuiStyle.drawOrbitSparkles(context, centerX, centerY, MENU_RADIUS, 5, 0x9FB8FF);

        // Вычисляем, какое заклинание под курсором
        updateHoveredSpell(mouseX, mouseY, centerX, centerY);

        // Медальоны заклинаний
        drawSpells(context, centerX, centerY, t);

        // Центральный круг
        drawCenter(context, centerX, centerY);

        // Информация о наведённом заклинании или подсказка управления
        if (hoveredSpell != null) {
            drawSpellInfo(context, hoveredSpell);
        } else {
            drawHint(context);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    public boolean handleKeyPress(int keyCode) {
        // ESC - закрыть меню
        if (keyCode == 256) { // GLFW.GLFW_KEY_ESCAPE
            this.close();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        // Проверяем клики мыши и клавиши через GLFW
        if (this.client != null) {
            // Обработка ESC
            boolean escPressed = GLFW.glfwGetKey(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
            if (escPressed) {
                this.close();
                return;
            }

            boolean leftClick = GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rightClick = GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

            // Обработка кликов с edge detection
            if (leftClick && !this.wasLeftPressed) {
                // ЛКМ — выбрать наведённое заклинание и закрыть
                confirmSelection();
            } else if (rightClick && !this.wasRightPressed) {
                // ПКМ — закрыть без выбора
                this.close();
            }

            this.wasLeftPressed = leftClick;
            this.wasRightPressed = rightClick;
        }
    }

    private void updateHoveredSpell(int mouseX, int mouseY, int centerX, int centerY) {
        if (learnedSpells.isEmpty()) {
            hoveredSpell = null;
            return;
        }

        // Вычисляем расстояние от центра
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Если курсор в центральном круге - ничего не выбрано
        if (distance < CENTER_CIRCLE_RADIUS) {
            hoveredSpell = null;
            return;
        }

        // Вычисляем угол курсора
        double angle = Math.atan2(dy, dx);

        // Нормализуем угол к диапазону [0, 2π]
        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        // Вычисляем угол для каждого заклинания
        double angleStep = 2 * Math.PI / learnedSpells.size();
        double startAngle = -Math.PI / 2; // Начинаем сверху

        // Находим ближайшее заклинание
        int closestIndex = 0;
        double minAngleDiff = Double.MAX_VALUE;

        for (int i = 0; i < learnedSpells.size(); i++) {
            double spellAngle = startAngle + angleStep * i;

            // Нормализуем угол заклинания
            while (spellAngle < 0)
                spellAngle += 2 * Math.PI;
            while (spellAngle >= 2 * Math.PI)
                spellAngle -= 2 * Math.PI;

            // Вычисляем разницу углов (с учетом цикличности)
            double diff = Math.abs(angle - spellAngle);
            if (diff > Math.PI) {
                diff = 2 * Math.PI - diff;
            }

            if (diff < minAngleDiff) {
                minAngleDiff = diff;
                closestIndex = i;
            }
        }

        hoveredSpell = learnedSpells.get(closestIndex);
    }

    // ── Центр ────────────────────────────────────────────────────────────

    private void drawCenter(DrawContext context, int cx, int cy) {
        // Тело центрального круга с дышащей кромкой
        long t = Util.getMeasuringTimeMs();
        float breath = 0.5f + 0.5f * MathHelper.sin(t * 0.002f);

        drawCircle(context, cx, cy, CENTER_CIRCLE_RADIUS, 0xE0100A20);
        drawCircleOutline(context, cx, cy, CENTER_CIRCLE_RADIUS,
                ManabornGuiStyle.withAlpha(0x9A86C8, 140 + (int) (60 * breath)));

        // Орбитальные руны вокруг центра
        ManabornGuiStyle.drawOrbitRunes(context, this.textRenderer, cx, cy, CENTER_CIRCLE_RADIUS + 9, 0x9A86C8);

        if (learnedSpells.isEmpty()) {
            drawCenteredText(context, "Нет изученных", cx, cy - 8, ManabornGuiStyle.COLOR_TEXT_DIM);
            drawCenteredText(context, "заклинаний", cx, cy + 2, ManabornGuiStyle.COLOR_TEXT_DIM);
        } else {
            drawCenteredText(context, "Выберите", cx, cy - 8, ManabornGuiStyle.COLOR_TITLE);
            drawCenteredText(context, "заклинание", cx, cy + 2, ManabornGuiStyle.COLOR_TEXT_DIM);
        }
    }

    private void drawCenteredText(DrawContext context, String text, int cx, int y, int color) {
        context.drawText(this.textRenderer, text, cx - this.textRenderer.getWidth(text) / 2, y, color, true);
    }

    // ── Медальоны заклинаний ─────────────────────────────────────────────

    private void drawSpells(DrawContext context, int centerX, int centerY, long t) {
        if (learnedSpells.isEmpty()) {
            return;
        }

        SpellType current = getStaffSelection();
        double angleStep = 2 * Math.PI / learnedSpells.size();
        double startAngle = -Math.PI / 2; // Начинаем сверху

        // Линия связи с наведённым заклинанием — под медальонами
        if (hoveredSpell != null) {
            int idx = learnedSpells.indexOf(hoveredSpell);
            double a = startAngle + angleStep * idx;
            int sx = centerX + (int) (Math.cos(a) * MENU_RADIUS);
            int sy = centerY + (int) (Math.sin(a) * MENU_RADIUS);
            ManabornGuiStyle.drawFlowLine(context, centerX, centerY, sx, sy, elementOf(hoveredSpell).rgb, true);
        }

        for (int i = 0; i < learnedSpells.size(); i++) {
            SpellType spell = learnedSpells.get(i);
            double angle = startAngle + angleStep * i;

            int spellX = centerX + (int) (Math.cos(angle) * MENU_RADIUS);
            int spellY = centerY + (int) (Math.sin(angle) * MENU_RADIUS);

            drawSpellNode(context, spellX, spellY, spell, spell == hoveredSpell, spell == current, t);
        }
    }

    private void drawSpellNode(DrawContext context, int x, int y, SpellType spell,
                               boolean hovered, boolean isCurrent, long t) {
        Element el = elementOf(spell);
        int rgb = el.rgb;
        float pulse = 0.5f + 0.5f * MathHelper.sin(t * 0.004f);

        // Свечение под медальоном при наведении
        if (hovered) {
            int glow = ManabornGuiStyle.withAlpha(ManabornGuiStyle.brightenRgb(rgb, 0.2f), 36 + (int) (28 * pulse));
            drawCircle(context, x, y, NODE_RADIUS + 6, glow);
        }

        // Тело медальона + лёгкий элементный отлив (отлив — только при наведении: экономим кадр)
        drawCircle(context, x, y, NODE_RADIUS, 0xE00C081A);
        if (hovered) {
            drawCircle(context, x, y, NODE_RADIUS - 3,
                    ManabornGuiStyle.withAlpha(ManabornGuiStyle.dimRgb(rgb, 0.45f), 70));
        }

        // Кольцо медальона (один проход — тонкое кольцо)
        int ringColor = ManabornGuiStyle.withAlpha(
                hovered ? ManabornGuiStyle.brightenRgb(rgb, 0.35f) : rgb,
                hovered ? 255 : 150);
        drawCircleBorder(context, x, y, NODE_RADIUS, ringColor);

        // Маркер-ромбик над медальоном, если заклинание сейчас выбрано на посохе
        if (isCurrent) {
            int m = 0xFF000000 | ManabornGuiStyle.brightenRgb(rgb, 0.45f);
            context.fill(x, y - NODE_RADIUS - 9, x + 1, y - NODE_RADIUS - 8, m);
            context.fill(x - 1, y - NODE_RADIUS - 8, x + 2, y - NODE_RADIUS - 7, m);
            context.fill(x, y - NODE_RADIUS - 7, x + 1, y - NODE_RADIUS - 6, m);
        }

        // Иконка стихии в центре медальона
        drawElementIcon(context, x, y, el, hovered);

        // Название под медальоном
        String name = spell.getDisplayName();
        int nameColor = hovered
                ? 0xFF000000 | ManabornGuiStyle.brightenRgb(rgb, 0.45f)
                : 0xFFCFC8E8;
        context.drawText(this.textRenderer, name,
                x - this.textRenderer.getWidth(name) / 2, y + NODE_RADIUS + 5, nameColor, true);

        // Звёзды уровня под названием
        int lvl = ClientSpellLevelsState.getSpellLevel(spell);
        for (int s = 0; s < 3; s++) {
            int starColor = s < lvl ? 0xFFFFD700 : 0xFF44445A;
            context.drawText(this.textRenderer, "★", x - 15 + s * 10, y + NODE_RADIUS + 16, starColor, false);
        }
    }

    // ── Информация о наведённом заклинании ───────────────────────────────

    private void drawSpellInfo(DrawContext context, SpellType spell) {
        Element el = elementOf(spell);
        int lvl = ClientSpellLevelsState.getSpellLevel(spell);
        String roman = switch (lvl) {
            case 2 -> "II";
            case 3 -> "III";
            default -> "I";
        };

        // Стоимость и кулдаун зависят от уровня, как при реальном касте
        Spell impl = SpellRegistry.getSpell(spell);
        int cost = (impl instanceof LeveledSpell leveled) ? leveled.getManaCost(lvl) : spell.getManaCost();
        int cd = (impl instanceof LeveledSpell leveled) ? leveled.getCooldown(lvl)
                : (impl != null ? impl.getCooldown() : 0);
        String cdText = String.format(java.util.Locale.ROOT, "%.1f", cd / 20.0f);

        String nameLine = spell.getDisplayName() + " " + roman;
        String statsLine = "Стоимость: " + cost + " маны  •  Кулдаун: " + cdText + " с";

        int w = Math.max(this.textRenderer.getWidth(nameLine), this.textRenderer.getWidth(statsLine)) + 62;
        w = Math.max(w, 180);
        int h = 48;
        int x = (this.width - w) / 2;
        int y = this.height - h - 26;

        ManabornGuiStyle.drawCard(context, x, y, w, h, el.rgb, true);

        // Иконка стихии слева в карточке
        drawElementIcon(context, x + 20, y + h / 2, el, true);

        int nameColor = 0xFF000000 | ManabornGuiStyle.brightenRgb(el.rgb, 0.4f);
        context.drawText(this.textRenderer, nameLine, x + 38, y + 7, nameColor, true);
        context.drawText(this.textRenderer, statsLine, x + 38, y + 20, ManabornGuiStyle.COLOR_TEXT, false);
        context.drawText(this.textRenderer, el.label, x + 38, y + 33, ManabornGuiStyle.COLOR_TEXT_DIM, false);
    }

    private void drawHint(DrawContext context) {
        boolean pressMode = SpellMenuMode.getInstance().isPressMode;
        String hint = pressMode
                ? "ЛКМ — выбрать   •   ПКМ — закрыть"
                : "Отпустите Shift — выбрать заклинание";
        drawCenteredText(context, hint, this.width / 2, this.height - 40, ManabornGuiStyle.COLOR_TEXT_DIM);
    }

    // ── Текущее заклинание на посохе ─────────────────────────────────────

    private SpellType getStaffSelection() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return null;
        }
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        if (main.getItem() instanceof MagicStaffItem) {
            return SpellManager.getSelectedSpell(main);
        }
        if (off.getItem() instanceof MagicStaffItem) {
            return SpellManager.getSelectedSpell(off);
        }
        return null;
    }

    // ── Процедурные иконки стихий ────────────────────────────────────────

    private void drawElementIcon(DrawContext ctx, int cx, int cy, Element el, boolean bright) {
        int argb = 0xFF000000 | (bright ? ManabornGuiStyle.brightenRgb(el.rgb, 0.25f) : el.rgb);
        switch (el) {
            case FIRE -> drawIconFlame(ctx, cx, cy, argb);
            case ICE -> drawIconSnowflake(ctx, cx, cy, argb);
            case DARK -> drawIconCrescent(ctx, cx, cy, argb);
            case SOUND -> {
                String note = "♪";
                ctx.drawText(this.textRenderer, note, cx - this.textRenderer.getWidth(note) / 2, cy - 4, argb, true);
            }
            case ENTROPY -> drawIconEntropy(ctx, cx, cy, argb);
        }
    }

    /** Пламя: пиксельный силуэт с ярким ядром у основания */
    private void drawIconFlame(DrawContext ctx, int cx, int cy, int argb) {
        String[] map = {
                "...#...",
                "...##..",
                "..###..",
                "..###..",
                ".#####.",
                ".#####.",
                "..####.",
                "..###..",
        };
        drawBitmap(ctx, map, cx, cy, argb);
        // Яркое ядро у основания пламени
        ctx.fill(cx - 1, cy, cx + 2, cy + 3, 0xFFFFD977);
    }

    /** Снежинка: вертикаль, горизонталь и две диагонали */
    private void drawIconSnowflake(DrawContext ctx, int cx, int cy, int argb) {
        ctx.fill(cx, cy - 5, cx + 1, cy + 6, argb);
        ctx.fill(cx - 5, cy, cx + 6, cy + 1, argb);
        for (int i = -3; i <= 3; i++) {
            ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, argb);
            ctx.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, argb);
        }
    }

    /** Полумесяц: круг с вырезом цвета фона медальона */
    private void drawIconCrescent(DrawContext ctx, int cx, int cy, int argb) {
        drawCircle(ctx, cx - 1, cy, 5, argb);
        drawCircle(ctx, cx + 2, cy - 2, 4, 0xFF0C081A);
    }

    /** Энтропия: звезда хаоса — восемь лучей, разлетающихся из пустого центра */
    private void drawIconEntropy(DrawContext ctx, int cx, int cy, int argb) {
        String[] star = {
                "#..#..#",
                ".##.##.",
                "..#.#..",
                "##...##",
                "..#.#..",
                ".##.##.",
                "#..#..#",
        };
        drawBitmap(ctx, star, cx, cy, argb);
    }

    // ── Графические примитивы ────────────────────────────────────────────

    private void drawBitmap(DrawContext ctx, String[] rows, int cx, int cy, int argb) {
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

    private void drawCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        // Оптимизированное рисование круга через горизонтальные линии
        for (int y = -radius; y <= radius; y++) {
            int width = (int) Math.sqrt(radius * radius - y * y);
            if (width > 0) {
                context.fill(centerX - width, centerY + y, centerX + width, centerY + y + 1, color);
            }
        }
    }

    private void drawCircleOutline(DrawContext context, int centerX, int centerY, int radius, int color) {
        int thickness = 2;
        for (int r = radius - thickness; r <= radius + thickness; r++) {
            drawCircleBorder(context, centerX, centerY, r, color);
        }
    }

    private void drawCircleBorder(DrawContext context, int centerX, int centerY, int radius, int color) {
        // Алгоритм средней точки (Bresenham для окружности) - очень быстрый
        int x = 0;
        int y = radius;
        int d = 1 - radius;

        while (x <= y) {
            // Рисуем 8 симметричных точек
            context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
            context.fill(centerX + x, centerY - y, centerX + x + 1, centerY - y + 1, color);
            context.fill(centerX - x, centerY + y, centerX - x + 1, centerY + y + 1, color);
            context.fill(centerX - x, centerY - y, centerX - x + 1, centerY - y + 1, color);
            context.fill(centerX + y, centerY + x, centerX + y + 1, centerY + x + 1, color);
            context.fill(centerX + y, centerY - x, centerX + y + 1, centerY - x + 1, color);
            context.fill(centerX - y, centerY + x, centerX - y + 1, centerY + x + 1, color);
            context.fill(centerX - y, centerY - x, centerX - y + 1, centerY - x + 1, color);

            x++;
            if (d < 0) {
                d += 2 * x + 1;
            } else {
                y--;
                d += 2 * (x - y) + 1;
            }
        }
    }

    @Override
    public boolean shouldPause() {
        // Не ставим игру на паузу
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Закрываем по ESC
        return true;
    }
}
