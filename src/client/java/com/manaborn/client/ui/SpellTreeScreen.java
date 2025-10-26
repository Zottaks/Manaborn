package com.manaborn.client.ui;

import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.spells.SpellType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.Arrays;
import java.util.List;

public class SpellTreeScreen extends Screen {
    public SpellTreeScreen() {
        super(Text.literal("Магия Manaborn"));
    }

    @Override
    protected void init() {
        super.init();
        // Создаем кнопки вкладок
        int w = this.width;
        int pad = 10;
        int boxTop = 32;
        int tabBarY = boxTop + 8;
        int tabWidth = 80;
        int tabHeight = 14;
        int tabsStartX = (w - (tabWidth * 2 + 8)) / 2;
        int fireTabX = tabsStartX;
        int iceTabX = tabsStartX + tabWidth + 8;

        this.clearChildren();
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Огонь"), b -> selectedTab = Tab.FIRE)
                .dimensions(fireTabX, tabBarY, tabWidth, tabHeight)
                .build()
        );
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Лед"), b -> selectedTab = Tab.ICE)
                .dimensions(iceTabX, tabBarY, tabWidth, tabHeight)
                .build()
        );
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

        int tabBarY = boxTop + 8;
        int tabWidth = 80;
        int tabHeight = 14;
        int tabsStartX = (w - (tabWidth * 2 + 8)) / 2;
        int fireTabX = tabsStartX;
        int iceTabX = tabsStartX + tabWidth + 8;

        int fireTabBg = selectedTab == Tab.FIRE ? 0xFF5A2A00 : 0xFF2A2A2A;
        int iceTabBg = selectedTab == Tab.ICE ? 0xFF00334D : 0xFF2A2A2A;

        context.fill(fireTabX, tabBarY, fireTabX + tabWidth, tabBarY + tabHeight, fireTabBg);
        context.fill(iceTabX, tabBarY, iceTabX + tabWidth, tabBarY + tabHeight, iceTabBg);

        String fireTitle = "Огонь";
        String iceTitle = "Лед";
        int fireTitleW = this.textRenderer.getWidth(fireTitle);
        int iceTitleW = this.textRenderer.getWidth(iceTitle);
        context.drawTextWithShadow(this.textRenderer, fireTitle, fireTabX + (tabWidth - fireTitleW) / 2, tabBarY + 3, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, iceTitle, iceTabX + (tabWidth - iceTitleW) / 2, tabBarY + 3, 0xFFFFFFFF);

        int listStartY = tabBarY + tabHeight + 8;
        int listX = boxLeft + 16;

        List<SpellType> fireSpells = Arrays.asList(SpellType.FIREBALL, SpellType.FIRE_WALL, SpellType.INNER_HEAT);
        List<SpellType> iceSpells = Arrays.asList(SpellType.ICE_SHARD);

        boolean any;
        if (selectedTab == Tab.FIRE) {
            any = renderBranch(context, fireSpells, listX, listStartY);
        } else {
            any = renderBranch(context, iceSpells, listX, listStartY);
        }

        if (!any) {
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
            if (!learned) {
                continue; // Показываем только изученные
            }
            any = true;

            int titleColor = 0xFFFFFFFF;
            Text name = Text.literal("- ")
                    .append(Text.literal(spell.getDisplayName()).formatted(spell.getColor()));
            context.drawTextWithShadow(this.textRenderer, name, x, y, titleColor);
            y += 12; // больше отступа под заголовок

            String desc = getDescription(spell);
            int metaColor = 0xFFA0A0A0;
            context.drawText(this.textRenderer, Text.literal("Стоимость: " + spell.getManaCost() + " маны"), x + 12, y, metaColor, false);
            y += 11;
            context.drawText(this.textRenderer, Text.literal(desc).formatted(net.minecraft.util.Formatting.GRAY), x + 12, y, 0xFFC0C0C0, false);
            y += 14; // увеличенный нижний отступ, чтобы точно избежать наложений
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
            case INNER_HEAT:
                return "Наполняет вас огненной энергией, временно увеличивая скорость.";
            default:
                return "Заклинание.";
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum Tab { FIRE, ICE }
    private Tab selectedTab = Tab.FIRE;

}
