package com.manaborn.client.ui;

import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.client.state.ClientSpellLevelsState;
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
        // Создаем кнопки вкладок (Огонь, Лед, Тьма, Звук)
        int w = this.width;
        int boxTop = 32;
        int tabBarY = boxTop + 8;
        int tabWidth = 70;
        int tabHeight = 14;
        int gap = 6;
        int tabsStartX = (w - (tabWidth * 4 + gap * 3)) / 2;
        int fireTabX = tabsStartX;
        int iceTabX = tabsStartX + tabWidth + gap;
        int darkTabX = tabsStartX + (tabWidth + gap) * 2;
        int soundTabX = tabsStartX + (tabWidth + gap) * 3;

        this.clearChildren();
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Огонь"), b -> selectedTab = Tab.FIRE)
                        .dimensions(fireTabX, tabBarY, tabWidth, tabHeight)
                        .build());
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Лед"), b -> selectedTab = Tab.ICE)
                        .dimensions(iceTabX, tabBarY, tabWidth, tabHeight)
                        .build());
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Тьма"), b -> selectedTab = Tab.DARK)
                        .dimensions(darkTabX, tabBarY, tabWidth, tabHeight)
                        .build());
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Звук"), b -> selectedTab = Tab.SOUND)
                        .dimensions(soundTabX, tabBarY, tabWidth, tabHeight)
                        .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int w = this.width;
        int h = this.height;

        context.fill(0, 0, w, h, 0xB0000000);

        int pad = 10;
        int boxTop = 32;
        int boxLeft = pad;
        int boxRight = w - pad;
        int boxBottom = h - pad;

        int bgTop = 0xA0151518;
        int bgMid = 0xA01E1E23;
        int bgBot = 0xA0151518;
        int boxHeight = boxBottom - boxTop;
        int thirds = Math.max(1, boxHeight / 3);
        context.fill(boxLeft, boxTop, boxRight, boxTop + thirds, bgTop);
        context.fill(boxLeft, boxTop + thirds, boxRight, boxTop + thirds * 2, bgMid);
        context.fill(boxLeft, boxTop + thirds * 2, boxRight, boxBottom, bgBot);

        int borderColor = 0xFF3A3A3A;
        context.fill(boxLeft, boxTop, boxRight, boxTop + 1, borderColor);
        context.fill(boxLeft, boxBottom - 1, boxRight, boxBottom, borderColor);
        context.fill(boxLeft, boxTop, boxLeft + 1, boxBottom, borderColor);
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
        int tabWidth = 70;
        int tabHeight = 14;
        int gap = 6;
        int tabsStartX = (w - (tabWidth * 4 + gap * 3)) / 2;
        int fireTabX = tabsStartX;
        int iceTabX = tabsStartX + tabWidth + gap;
        int darkTabX = tabsStartX + (tabWidth + gap) * 2;
        int soundTabX = tabsStartX + (tabWidth + gap) * 3;

        int fireTabBg = selectedTab == Tab.FIRE ? 0xFF7A3A00 : 0xFF2A2A2A;
        int iceTabBg = selectedTab == Tab.ICE ? 0xFF004A6A : 0xFF2A2A2A;
        int darkTabBg = selectedTab == Tab.DARK ? 0xFF2A1B46 : 0xFF2A2A2A;
        int soundTabBg = selectedTab == Tab.SOUND ? 0xFF6A5A00 : 0xFF2A2A2A;
        int selBorder = 0xFFFFFFFF;
        int unselBorder = 0xFF4A4A4A;

        context.fill(fireTabX, tabBarY, fireTabX + tabWidth, tabBarY + tabHeight, fireTabBg);
        context.fill(iceTabX, tabBarY, iceTabX + tabWidth, tabBarY + tabHeight, iceTabBg);
        context.fill(darkTabX, tabBarY, darkTabX + tabWidth, tabBarY + tabHeight, darkTabBg);
        context.fill(soundTabX, tabBarY, soundTabX + tabWidth, tabBarY + tabHeight, soundTabBg);
        int fb = selectedTab == Tab.FIRE ? selBorder : unselBorder;
        int ib = selectedTab == Tab.ICE ? selBorder : unselBorder;
        int db = selectedTab == Tab.DARK ? selBorder : unselBorder;
        int sb = selectedTab == Tab.SOUND ? selBorder : unselBorder;
        drawBorder(context, fireTabX, tabBarY, tabWidth, tabHeight, fb);
        drawBorder(context, iceTabX, tabBarY, tabWidth, tabHeight, ib);
        drawBorder(context, darkTabX, tabBarY, tabWidth, tabHeight, db);
        drawBorder(context, soundTabX, tabBarY, tabWidth, tabHeight, sb);
        if (selectedTab == Tab.FIRE) {
            context.fill(fireTabX, tabBarY + tabHeight, fireTabX + tabWidth, tabBarY + tabHeight + 2, 0xFFFFA64D);
        } else if (selectedTab == Tab.ICE) {
            context.fill(iceTabX, tabBarY + tabHeight, iceTabX + tabWidth, tabBarY + tabHeight + 2, 0xFF66D0FF);
        } else if (selectedTab == Tab.DARK) {
            context.fill(darkTabX, tabBarY + tabHeight, darkTabX + tabWidth, tabBarY + tabHeight + 2, 0xFF8A6AD9);
        } else if (selectedTab == Tab.SOUND) {
            context.fill(soundTabX, tabBarY + tabHeight, soundTabX + tabWidth, tabBarY + tabHeight + 2, 0xFFFFDD00);
        }

        String fireTitle = "Огонь";
        String iceTitle = "Лед";
        String darkTitle = "Тьма";
        String soundTitle = "Звук";
        int fireTitleW = this.textRenderer.getWidth(fireTitle);
        int iceTitleW = this.textRenderer.getWidth(iceTitle);
        int darkTitleW = this.textRenderer.getWidth(darkTitle);
        int soundTitleW = this.textRenderer.getWidth(soundTitle);
        context.drawTextWithShadow(this.textRenderer, fireTitle, fireTabX + (tabWidth - fireTitleW) / 2, tabBarY + 3,
                0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, iceTitle, iceTabX + (tabWidth - iceTitleW) / 2, tabBarY + 3,
                0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, darkTitle, darkTabX + (tabWidth - darkTitleW) / 2, tabBarY + 3,
                0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, soundTitle, soundTabX + (tabWidth - soundTitleW) / 2, tabBarY + 3,
                0xFFFFFFFF);

        int listStartY = tabBarY + tabHeight + 8;
        int listX = boxLeft + 16;

        List<SpellType> fireSpells = Arrays.asList(SpellType.FIREBALL, SpellType.FIRE_WALL, SpellType.INNER_HEAT, SpellType.HEAT_WEAPON);
        List<SpellType> iceSpells = Arrays.asList(SpellType.ICE_SHARD, SpellType.ICE_WALL);
        List<SpellType> darkSpells = Arrays.asList(SpellType.ABSORBING_BARRIER, SpellType.WEAKNESS_CURSE);
        List<SpellType> soundSpells = Arrays.asList(SpellType.SOUND_STRIKE);

        if (selectedTab == Tab.FIRE) {
            int bannerTop = listStartY;
            int bannerHeight = 26;
            int bannerLeft = boxLeft + 8;
            int bannerRight = boxRight - 8;
            context.fill(bannerLeft, bannerTop, bannerRight, bannerTop + bannerHeight, 0x40201500);
            context.fill(bannerLeft, bannerTop, bannerRight, bannerTop + 1, 0xFF7A3A00);
            context.fill(bannerLeft, bannerTop + bannerHeight - 1, bannerRight, bannerTop + bannerHeight, 0xFF7A3A00);
            int sw = this.textRenderer.getWidth("Магия огня");
            context.drawTextWithShadow(this.textRenderer, "Магия огня", bannerLeft + 8, bannerTop + 9, 0xFFFFE0A0);
            int fx = bannerLeft + 8 + sw + 8;
            int fy = bannerTop + 6;
            long t = System.currentTimeMillis() / 120;
            for (int i = 0; i < 8; i++) {
                int px = fx + i * 10;
                int ph = 4 + (int) ((i + t) % 6);
                int py = fy + 8 - ph;
                int col = 0xFFCC6A1A;
                context.fill(px, py, px + 4, py + ph, col);
            }
            listStartY = bannerTop + bannerHeight + 6;
        } else if (selectedTab == Tab.ICE) {
            int bannerTop = listStartY;
            int bannerHeight = 26;
            int bannerLeft = boxLeft + 8;
            int bannerRight = boxRight - 8;
            context.fill(bannerLeft, bannerTop, bannerRight, bannerTop + bannerHeight, 0x40203246);
            context.fill(bannerLeft, bannerTop, bannerRight, bannerTop + 1, 0xFF1A6A8C);
            context.fill(bannerLeft, bannerTop + bannerHeight - 1, bannerRight, bannerTop + bannerHeight, 0xFF1A6A8C);
            context.drawTextWithShadow(this.textRenderer, "Магия льда", bannerLeft + 8, bannerTop + 9, 0xFFAEEBFF);
            listStartY = bannerTop + bannerHeight + 6;
        } else if (selectedTab == Tab.DARK) {
            int bannerTop = listStartY;
            int bannerHeight = 26;
            int bannerLeft = boxLeft + 8;
            int bannerRight = boxRight - 8;
            context.fill(bannerLeft, bannerTop, bannerRight, bannerTop + bannerHeight, 0x4020162A);
            context.fill(bannerLeft, bannerTop, bannerRight, bannerTop + 1, 0xFF3A2A5F);
            context.fill(bannerLeft, bannerTop + bannerHeight - 1, bannerRight, bannerTop + bannerHeight, 0xFF3A2A5F);
            context.drawTextWithShadow(this.textRenderer, "Магия тьмы", bannerLeft + 8, bannerTop + 9, 0xFFE6D8FF);
            listStartY = bannerTop + bannerHeight + 6;
        } else if (selectedTab == Tab.SOUND) {
            int bannerTop = listStartY;
            int bannerHeight = 26;
            int bannerLeft = boxLeft + 8;
            int bannerRight = boxRight - 8;
            context.fill(bannerLeft, bannerTop, bannerRight, bannerTop + bannerHeight, 0x40332600);
            context.fill(bannerLeft, bannerTop, bannerRight, bannerTop + 1, 0xFF6A5A00);
            context.fill(bannerLeft, bannerTop + bannerHeight - 1, bannerRight, bannerTop + bannerHeight, 0xFF6A5A00);
            int sw = this.textRenderer.getWidth("♪ Магия звука");
            context.drawTextWithShadow(this.textRenderer, "♪ Магия звука", bannerLeft + 8, bannerTop + 9, 0xFFFFEE99);
            // Рисуем звуковые волны
            int fx = bannerLeft + 8 + sw + 8;
            int fy = bannerTop + 6;
            long t = System.currentTimeMillis() / 150;
            for (int i = 0; i < 5; i++) {
                int px = fx + i * 12;
                int waveOffset = (int) ((i + t) % 4);
                int py1 = fy + 7 - waveOffset;
                int py2 = fy + 7 + waveOffset;
                int col = 0xFFDDCC00;
                context.fill(px, py1, px + 2, py1 + 1, col);
                context.fill(px, py2, px + 2, py2 + 1, col);
            }
            listStartY = bannerTop + bannerHeight + 6;
        }

        boolean any;
        if (selectedTab == Tab.FIRE) {
            any = renderBranch(context, fireSpells, listX, listStartY);
        } else if (selectedTab == Tab.ICE) {
            any = renderBranch(context, iceSpells, listX, listStartY);
        } else if (selectedTab == Tab.DARK) {
            any = renderBranch(context, darkSpells, listX, listStartY);
        } else {
            any = renderBranch(context, soundSpells, listX, listStartY);
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
        int cardWidth = Math.max(200, context.getScaledWindowWidth() - (x + 32));
        for (SpellType spell : spells) {
            boolean learned = ClientLearnedSpellsState.hasLearned(spell);
            if (!learned) {
                continue;
            }
            any = true;

            // Получаем описание и считаем количество строк для адаптивной высоты
            String desc = getDescription(spell);
            String[] lines = desc.split("\n");
            int descriptionLines = lines.length;
            
            // Базовая высота + дополнительное пространство для каждой строки описания
            int cardHeight = 46 + (descriptionLines - 1) * 10; // +10 пикселей на каждую дополнительную строку
            
            int bg = 0xBF1E1E23;
            int stroke = 0xFF3A3A3A;
            int accent;
            if (spell == SpellType.ICE_SHARD || spell == SpellType.ICE_WALL) {
                accent = 0xFF66D0FF;
            } else if (spell == SpellType.ABSORBING_BARRIER || spell == SpellType.WEAKNESS_CURSE) {
                accent = 0xFF8A6AD9;
            } else if (spell == SpellType.SOUND_STRIKE) {
                accent = 0xFFFFDD00;
            } else {
                accent = 0xFFFFA64D;
            }
            context.fill(x, y, x + cardWidth, y + cardHeight, bg);
            drawBorder(context, x, y, cardWidth, cardHeight, stroke);
            context.fill(x + 1, y + 1, x + 5, y + cardHeight - 1, accent);

            // Получаем уровень заклинания
            int spellLevel = ClientSpellLevelsState.getSpellLevel(spell);
            String levelRoman = switch (spellLevel) {
                case 2 -> "II";
                case 3 -> "III";
                default -> "I";
            };
            
            Text name = Text.literal(spell.getDisplayName() + " " + levelRoman).formatted(spell.getColor());
            context.drawTextWithShadow(this.textRenderer, name, x + 10, y + 6, 0xFFFFFFFF);

            String cost = "Стоимость: " + spell.getManaCost() + " маны";
            context.drawText(this.textRenderer, Text.literal(cost), x + 10, y + 20, 0xFFCCCCCC, false);

            // Поддержка многострочного описания через \n
            int lineY = y + 31;
            for (String line : lines) {
                context.drawText(this.textRenderer, Text.literal(line).formatted(net.minecraft.util.Formatting.GRAY),
                        x + 10, lineY, 0xFFC0C0C0, false);
                lineY += 10; // Отступ между строками
            }

            y += cardHeight + 6;
        }
        return any;
    }

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
            default:
                return "Заклинание.";
        }
    }

    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum Tab {
        FIRE, ICE, DARK, SOUND
    }

    private Tab selectedTab = Tab.FIRE;

}
