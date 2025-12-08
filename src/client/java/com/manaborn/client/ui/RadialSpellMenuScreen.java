package com.manaborn.client.ui;

import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.spells.SpellType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Радиальное меню выбора заклинаний в стиле GTA V
 * 
 * Особенности:
 * - Отображает только изученные заклинания
 * - Выбор происходит движением мыши
 * - Закрывается при отпускании ПКМ
 */
public class RadialSpellMenuScreen extends Screen {
    private static final int MENU_RADIUS = 100; // Радиус меню
    private static final int ICON_SIZE = 32; // Размер иконки заклинания
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

    public RadialSpellMenuScreen(SpellSelectionCallback callback) {
        super(Text.literal("Выбор заклинания"));
        this.callback = callback;
        this.learnedSpells = new ArrayList<>(ClientLearnedSpellsState.getLearnedSpells());
    }

    @Override
    protected void init() {
        super.init();
        // Захватываем курсор в центр экрана
        if (this.client != null) {
            double centerX = this.client.getWindow().getScaledWidth() / 2.0;
            double centerY = this.client.getWindow().getScaledHeight() / 2.0;
            GLFW.glfwSetCursorPos(
                    this.client.getWindow().getHandle(),
                    centerX * this.client.getWindow().getScaleFactor(),
                    centerY * this.client.getWindow().getScaleFactor());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Полупрозрачный фон
        context.fill(0, 0, this.width, this.height, 0x80000000);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Вычисляем, какое заклинание под курсором
        updateHoveredSpell(mouseX, mouseY, centerX, centerY);

        // Рисуем центральный круг
        drawCenterCircle(context, centerX, centerY);

        // Рисуем заклинания по кругу
        drawSpells(context, centerX, centerY);

        // Рисуем информацию о выбранном заклинании
        if (hoveredSpell != null) {
            drawSpellInfo(context, hoveredSpell);
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
                // ЛКМ — выбрать заклинание и закрыть
                if (hoveredSpell != null && callback != null) {
                    callback.onSpellSelected(hoveredSpell);
                }
                this.close();
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

    private void drawCenterCircle(DrawContext context, int centerX, int centerY) {
        // Рисуем центральный круг
        int color = 0x80404040; // Полупрозрачный серый
        drawCircle(context, centerX, centerY, CENTER_CIRCLE_RADIUS, color);

        // Рисуем обводку
        drawCircleOutline(context, centerX, centerY, CENTER_CIRCLE_RADIUS, 0xFFFFFFFF);

        // Текст в центре
        Text text = Text.literal("Выберите");
        int textWidth = this.textRenderer.getWidth(text);
        context.drawText(this.textRenderer, text, centerX - textWidth / 2, centerY - 4, 0xFFFFFFFF, true);
    }

    private void drawSpells(DrawContext context, int centerX, int centerY) {
        if (learnedSpells.isEmpty()) {
            return;
        }

        double angleStep = 2 * Math.PI / learnedSpells.size();
        double startAngle = -Math.PI / 2; // Начинаем сверху

        for (int i = 0; i < learnedSpells.size(); i++) {
            SpellType spell = learnedSpells.get(i);
            double angle = startAngle + angleStep * i;

            // Вычисляем позицию заклинания
            int spellX = centerX + (int) (Math.cos(angle) * MENU_RADIUS);
            int spellY = centerY + (int) (Math.sin(angle) * MENU_RADIUS);

            // Определяем цвет (подсвечиваем выбранное)
            boolean isHovered = spell == hoveredSpell;
            int bgColor = isHovered ? 0xC0606060 : 0x80404040;
            int borderColor = isHovered ? spell.getColor().getColorValue() : 0xFFAAAAAA;

            // Рисуем круг для заклинания
            drawCircle(context, spellX, spellY, ICON_SIZE / 2, bgColor);
            drawCircleOutline(context, spellX, spellY, ICON_SIZE / 2, borderColor);

            // Рисуем название заклинания
            Text spellName = Text.literal(spell.getDisplayName());
            int textWidth = this.textRenderer.getWidth(spellName);
            int textX = spellX - textWidth / 2;
            int textY = spellY - 4;

            // Если заклинание выбрано - рисуем текст цветом заклинания
            int textColor = isHovered ? (spell.getColor().getColorValue() | 0xFF000000) : 0xFFFFFFFF;
            context.drawText(this.textRenderer, spellName, textX, textY, textColor, true);
            
            // Рисуем мини-иконки категорий над заклинаниями
            Text categoryIcon = null;
            int iconColor = 0xFFFFFFFF;
            
            if (spell == SpellType.SOUND_STRIKE || spell == SpellType.BATTLE_HYMN || spell == SpellType.SILENCE_ZONE || spell == SpellType.CLARIFYING_TONE) {
                categoryIcon = Text.literal("♪"); // Нота для звуковой магии
                iconColor = 0xFFFFFF00;
            } else if (spell == SpellType.FIREBALL || spell == SpellType.FIRE_WALL || 
                       spell == SpellType.INNER_HEAT || spell == SpellType.HEAT_WEAPON) {
                categoryIcon = Text.literal("🔥"); // Огонь
                iconColor = 0xFFFF6600;
            } else if (spell == SpellType.ICE_SHARD || spell == SpellType.ICE_WALL || 
                       spell == SpellType.FROST_AURA) {
                categoryIcon = Text.literal("❄"); // Снежинка для льда
                iconColor = 0xFF66D0FF;
            } else if (spell == SpellType.ABSORBING_BARRIER || spell == SpellType.WEAKNESS_CURSE) {
                categoryIcon = Text.literal("◆"); // Ромб для тьмы
                iconColor = 0xFF8A6AD9;
            }
            
            if (categoryIcon != null) {
                int iconWidth = this.textRenderer.getWidth(categoryIcon);
                context.drawText(this.textRenderer, categoryIcon, spellX - iconWidth / 2, spellY - 16, iconColor, true);
            }

            // Рисуем линию от центра к заклинанию (если выбрано)
            if (isHovered) {
                drawLine(context, centerX, centerY, spellX, spellY, borderColor);
            }
        }
    }

    private void drawSpellInfo(DrawContext context, SpellType spell) {
        // Рисуем информацию о заклинании внизу экрана
        int infoY = this.height - 70;

        Text nameText = Text.literal(spell.getDisplayName()).formatted(spell.getColor());
        Text costText = Text.literal("Стоимость: " + spell.getManaCost() + " маны");
        
        // Добавляем категорию для магии звука
        Text categoryText = null;
        if (spell == SpellType.SOUND_STRIKE || spell == SpellType.BATTLE_HYMN || spell == SpellType.SILENCE_ZONE || spell == SpellType.CLARIFYING_TONE) {
            categoryText = Text.literal("♪ Магия звука").formatted(spell.getColor());
        }

        int nameWidth = this.textRenderer.getWidth(nameText);
        int costWidth = this.textRenderer.getWidth(costText);
        int categoryWidth = categoryText != null ? this.textRenderer.getWidth(categoryText) : 0;
        
        int maxWidth = Math.max(nameWidth, Math.max(costWidth, categoryWidth));

        int nameX = (this.width - nameWidth) / 2;
        int costX = (this.width - costWidth) / 2;
        int categoryX = categoryText != null ? (this.width - categoryWidth) / 2 : 0;

        // Фон для текста (расширен если есть категория)
        int bgHeight = categoryText != null ? 38 : 25;
        context.fill(
            (this.width - maxWidth) / 2 - 5, 
            infoY - 5, 
            (this.width + maxWidth) / 2 + 5, 
            infoY + bgHeight, 
            0xC0000000
        );

        // Рисуем категорию сверху (если есть)
        int currentY = infoY;
        if (categoryText != null) {
            context.drawText(this.textRenderer, categoryText, categoryX, currentY, 0xFFFFFF00, true);
            currentY += 12;
        }
        
        context.drawText(this.textRenderer, nameText, nameX, currentY, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, costText, costX, currentY + 12, 0xFFAAAAAA, true);
    }

    private void drawCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        // Оптимизированное рисование круга через горизонтальные линии
        // Вместо попиксельного рендеринга используем fill для каждой строки
        for (int y = -radius; y <= radius; y++) {
            int width = (int) Math.sqrt(radius * radius - y * y);
            if (width > 0) {
                context.fill(centerX - width, centerY + y, centerX + width, centerY + y + 1, color);
            }
        }
    }

    private void drawCircleOutline(DrawContext context, int centerX, int centerY, int radius, int color) {
        // Упрощённая обводка круга - рисуем несколько окружностей разного радиуса
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

    private void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        // Оптимизированная линия - рисуем толстую линию через прямоугольники
        int thickness = 2;
        int dx = x2 - x1;
        int dy = y2 - y1;
        int length = (int) Math.sqrt(dx * dx + dy * dy);

        if (length == 0)
            return;

        // Рисуем линию как серию маленьких прямоугольников
        for (int i = 0; i <= length; i += 3) { // Шаг 3 для оптимизации
            int x = x1 + (dx * i) / length;
            int y = y1 + (dy * i) / length;
            context.fill(x - thickness / 2, y - thickness / 2, x + thickness / 2 + 1, y + thickness / 2 + 1, color);
        }
    }

    // Убираем логику закрытия по отпусканию ПКМ — меню остаётся открытым,
    // пока пользователь явно не нажмёт ЛКМ (выбор) или ПКМ (закрыть)

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
