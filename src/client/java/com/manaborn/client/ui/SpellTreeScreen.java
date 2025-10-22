package com.manaborn.client.ui;

import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.spells.SpellType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;

public class SpellTreeScreen extends Screen {
    public SpellTreeScreen() {
        super(Text.literal("Магия Manaborn"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int w = this.width;
        int h = this.height;

        // Полупрозрачная подложка на весь экран для контраста
        context.fill(0, 0, w, h, 0xB0000000);

        // Область подложки
        int pad = 10;
        int boxTop = 32;
        int boxLeft = pad;
        int boxRight = w - pad;
        int boxBottom = h - pad;
        context.fill(boxLeft, boxTop, boxRight, boxBottom, 0x80000000);
        int borderColor = 0xFF3A3A3A;
        // Top
        context.fill(boxLeft, boxTop, boxRight, boxTop + 1, borderColor);
        // Bottom
        context.fill(boxLeft, boxBottom - 1, boxRight, boxBottom, borderColor);
        // Left
        context.fill(boxLeft, boxTop, boxLeft + 1, boxBottom, borderColor);
        // Right
        context.fill(boxRight - 1, boxTop, boxRight, boxBottom, borderColor);

        // Заголовок и подсказка поверх подложки
        String title = "Дерево магии";
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawTextWithShadow(this.textRenderer, title, (w - titleWidth) / 2, 12, 0xFFFFFFFF);

        String hint = "Esc — закрыть";
        context.drawTextWithShadow(this.textRenderer, hint, 8, 8, 0xFF808080);

        // Debug: количество изученных
        int learnedCount = ClientLearnedSpellsState.getLearnedSpells().size();
        String dbg = "Изучено: " + learnedCount;
        int dbgW = this.textRenderer.getWidth(dbg);
        context.drawTextWithShadow(this.textRenderer, dbg, w - 8 - dbgW, 8, 0xFFAAAAAA);

        // Две школы: Огонь и Лед
        int columnGap = 12;
        int columnWidth = (boxRight - boxLeft - columnGap) / 2;
        int leftColX = boxLeft + 8;
        int rightColX = boxLeft + columnWidth + columnGap + 8;
        int startY = boxTop + 12;

        // Заголовки школ
        context.drawTextWithShadow(this.textRenderer, "Огонь", leftColX, startY, 0xFFFFA54D);
        context.drawTextWithShadow(this.textRenderer, "Лед", rightColX, startY, 0xFF7FDBFF);

        int yFire = startY + 14;
        int yIce = startY + 14;

        // Ветки: показываем только изученные заклинания
        List<SpellType> fireSpells = Arrays.asList(SpellType.FIREBALL, SpellType.FIRE_WALL);
        List<SpellType> iceSpells = Arrays.asList(SpellType.ICE_SHARD);

        boolean hasFire = renderBranch(context, fireSpells, leftColX, yFire);
        boolean hasIce = renderBranch(context, iceSpells, rightColX, yIce);

        // Пустое состояние: ни одного изученного
        if (!hasFire && !hasIce) {
            String empty = "Нет изученных заклинаний";
            String tip = "Используйте книги заклинаний, чтобы изучать их";
            int ew = this.textRenderer.getWidth(empty);
            int tw = this.textRenderer.getWidth(tip);
            int cy = (h / 2) - 10;
            context.drawTextWithShadow(this.textRenderer, empty, (w - ew) / 2, cy, 0xFFFFFFFF);
            context.drawTextWithShadow(this.textRenderer, tip, (w - tw) / 2, cy + 12, 0xFFAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private boolean renderBranch(DrawContext context, List<SpellType> spells, int x, int startY) {
        int y = startY;
        boolean any = false;
        for (SpellType spell : spells) {
            boolean learned = ClientLearnedSpellsState.hasLearned(spell);
            if (learned) any = true;

            int titleColor = learned ? 0xFFFFFFFF : 0xFFAAAAAA;
            Text name = Text.literal("- ")
                    .append(Text.literal(spell.getDisplayName()).formatted(spell.getColor()))
                    .append(learned ? Text.empty() : Text.literal("  (не изучено)"));
            context.drawTextWithShadow(this.textRenderer, name, x, y, titleColor);
            y += 10;

            String desc = getDescription(spell);
            int metaColor = learned ? 0xFFA0A0A0 : 0xFF808080;
            context.drawText(this.textRenderer, Text.literal("Стоимость: " + spell.getManaCost() + " маны"), x + 12, y, metaColor, false);
            y += 9;
            context.drawText(this.textRenderer, Text.literal(desc).formatted(net.minecraft.util.Formatting.GRAY), x + 12, y, learned ? 0xFFC0C0C0 : 0xFF808080, false);
            y += 12;
        }
        return any;
    }

    private String getDescription(SpellType spell) {
        switch (spell) {
            case FIREBALL:
                return "Запускает огненный шар, нанося урон при попадании.";
            case ICE_SHARD:
                return "Быстрый ледяной осколок, поражающий цель на расстоянии.";
            case FIRE_WALL:
                return "Создает стену огня, наносящую урон при соприкосновении.";
            default:
                return "Заклинание.";
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
