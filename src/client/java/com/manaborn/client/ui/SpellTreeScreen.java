package com.manaborn.client.ui;

import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.client.state.ClientSpellLevelsState;
import com.manaborn.client.state.ClientPassivesState;
import com.manaborn.passives.PassiveType;
import com.manaborn.network.PassiveSlotsUpdateC2SPacket;
import com.manaborn.spells.SpellType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.Arrays;
import java.util.List;

public class SpellTreeScreen extends Screen {
    public SpellTreeScreen() {
        super(Text.literal("Магия Manaborn"));
    }

    private void renderPassiveDescription(DrawContext context) {
        int bg = 0xBF1E1E23;
        int stroke = 0xFF3A3A3A;

        context.fill(passiveDescX, passiveDescY, passiveDescX + passiveDescW, passiveDescY + passiveDescH, bg);
        drawBorder(context, passiveDescX, passiveDescY, passiveDescW, passiveDescH, stroke);

        String header = "Описание";
        context.drawTextWithShadow(this.textRenderer, header, passiveDescX + 8, passiveDescY + 6, 0xFFFFFFFF);

        PassiveType[] active = ClientPassivesState.getActive();
        PassiveType s0 = active.length > 0 ? active[0] : null;
        PassiveType s1 = active.length > 1 ? active[1] : null;

        if (s0 == null && s1 == null) {
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("Выберите пассивки в слоты").formatted(net.minecraft.util.Formatting.GRAY),
                    passiveDescX + 8, passiveDescY + 22, 0xFFFFFFFF);
            passiveDescMaxScroll = 0;
            if (passiveDescScrollOffset > 0) passiveDescScrollOffset = 0;
            return;
        }

        int textX = passiveDescX + 8;
        int textY = passiveDescY + 22;
        int textW = passiveDescW - 16;
        int textH = passiveDescH - 30;

        List<OrderedText> lines = buildActivePassivesDescriptionLines(s0, s1, textW);
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

    private List<OrderedText> buildActivePassivesDescriptionLines(PassiveType slot0, PassiveType slot1, int width) {
        List<OrderedText> out = new java.util.ArrayList<>();

        if (slot0 != null) {
            Text header = Text.literal("Слот 1: ").formatted(net.minecraft.util.Formatting.GRAY)
                    .append(Text.literal(slot0.getDisplayName()).formatted(slot0.getColor()));
            out.addAll(this.textRenderer.wrapLines(header, width));
            for (String line : getPassiveDescription(slot0).split("\n")) {
                out.addAll(this.textRenderer.wrapLines(Text.literal(line).formatted(net.minecraft.util.Formatting.GRAY), width));
            }
            out.add(OrderedText.EMPTY);
        }

        if (slot1 != null) {
            Text header = Text.literal("Слот 2: ").formatted(net.minecraft.util.Formatting.GRAY)
                    .append(Text.literal(slot1.getDisplayName()).formatted(slot1.getColor()));
            out.addAll(this.textRenderer.wrapLines(header, width));
            for (String line : getPassiveDescription(slot1).split("\n")) {
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

    private int scrollOffset = 0;
    private int maxScroll = 0;

    private PassiveType draggingPassive = null;
    private boolean dragging = false;

    private int passivesListScrollOffset = 0;
    private int passivesListMaxScroll = 0;

    private int passiveDescScrollOffset = 0;
    private int passiveDescMaxScroll = 0;

    private int passiveDescX;
    private int passiveDescY;
    private int passiveDescW;
    private int passiveDescH;

    private int passiveSlotX;
    private int passiveSlot0Y;
    private int passiveSlot1Y;
    private int passiveSlotSize;

    private int passivesButtonX;
    private int passivesButtonY;
    private int passivesButtonSize;

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

        passivesButtonSize = 16;
        int passivesTabX = tabsStartX + (tabWidth + gap) * 4;
        passivesButtonX = passivesTabX + (tabWidth - passivesButtonSize) / 2;
        passivesButtonY = tabBarY + (tabHeight - passivesButtonSize) / 2;

        if (selectedTab != Tab.PASSIVES) {
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

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal(""), b -> {
                    if (selectedTab == Tab.PASSIVES) {
                        selectedTab = Tab.FIRE;
                    } else {
                        selectedTab = Tab.PASSIVES;
                    }
                    scrollOffset = 0;
                    this.init();
                })
                        .dimensions(passivesButtonX, passivesButtonY, passivesButtonSize, passivesButtonSize)
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

        // Вкладки магии скрываем, когда открыты пассивки
        if (selectedTab != Tab.PASSIVES) {
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
        }

        // Рисуем иконку кнопки пассивок (квадрат на том же месте)
        int btnBg = selectedTab == Tab.PASSIVES ? 0xFF2A2A2A : 0xFF1E1E23;
        context.fill(passivesButtonX, passivesButtonY, passivesButtonX + passivesButtonSize, passivesButtonY + passivesButtonSize, btnBg);
        drawBorder(context, passivesButtonX, passivesButtonY, passivesButtonSize, passivesButtonSize, 0xFF4A4A4A);

        int listStartY = tabBarY + tabHeight + 8;
        int listX = boxLeft + 16;

        List<SpellType> fireSpells = Arrays.asList(SpellType.FIREBALL, SpellType.FIRE_WALL, SpellType.INNER_HEAT, SpellType.HEAT_WEAPON);
        List<SpellType> iceSpells = Arrays.asList(SpellType.ICE_SHARD, SpellType.ICE_WALL);
        List<SpellType> darkSpells = Arrays.asList(SpellType.ABSORBING_BARRIER, SpellType.WEAKNESS_CURSE);
        List<SpellType> soundSpells = Arrays.asList(SpellType.SOUND_STRIKE, SpellType.BATTLE_HYMN, SpellType.SILENCE_ZONE, SpellType.CLARIFYING_TONE);

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
                res = renderBranch(context, fireSpells, listX, listStartY, boxTop, boxBottom);
            } else if (selectedTab == Tab.ICE) {
                res = renderBranch(context, iceSpells, listX, listStartY, boxTop, boxBottom);
            } else if (selectedTab == Tab.DARK) {
                res = renderBranch(context, darkSpells, listX, listStartY, boxTop, boxBottom);
            } else {
                res = renderBranch(context, soundSpells, listX, listStartY, boxTop, boxBottom);
            }
            context.disableScissor();

            maxScroll = Math.max(0, res.contentHeight - (boxBottom - listStartY - 10));
            if (scrollOffset > maxScroll) {
                scrollOffset = maxScroll;
            }

            if (!res.any) {
                String empty = "Нет изученных заклинаний";
                String tip = "Используйте книги заклинаний, чтобы изучать их";
                int ew = this.textRenderer.getWidth(empty);
                int tw = this.textRenderer.getWidth(tip);
                int cy = (h / 2) - 10;
                context.drawTextWithShadow(this.textRenderer, empty, (w - ew) / 2, cy, 0xFFFFFFFF);
                context.drawTextWithShadow(this.textRenderer, tip, (w - tw) / 2, cy + 12, 0xFFAAAAAA);
            }
        }

        if (dragging && draggingPassive != null) {
            String t = draggingPassive.getDisplayName();
            int tw = this.textRenderer.getWidth(t);
            context.fill(mouseX + 8, mouseY + 8, mouseX + 12 + tw, mouseY + 20, 0xC0000000);
            ItemStack icon = getPassiveIcon(draggingPassive);
            context.drawItem(icon, mouseX + 10, mouseY + 10);
            context.drawTextWithShadow(this.textRenderer, Text.literal(t).formatted(draggingPassive.getColor()), mouseX + 28, mouseY + 10, 0xFFFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);

        // Поверх виджетов: иконка на кнопке пассивок
        context.drawItem(new ItemStack(Items.ENCHANTED_BOOK), passivesButtonX, passivesButtonY);
    }

    private record BranchRenderResult(boolean any, int contentHeight) {}

    private BranchRenderResult renderBranch(DrawContext context, List<SpellType> spells, int x, int startY, int boxTop, int boxBottom) {
        int y = startY - scrollOffset;
        int yStart = y;
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
            } else if (spell == SpellType.SOUND_STRIKE || spell == SpellType.BATTLE_HYMN || spell == SpellType.SILENCE_ZONE || spell == SpellType.CLARIFYING_TONE) {
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

        int contentHeight = Math.max(0, y - yStart);
        return new BranchRenderResult(any, contentHeight);
    }

    private void renderPassives(DrawContext context, int mouseX, int mouseY, int boxLeft, int boxRight, int listStartY, int boxBottom) {
        int contentLeft = boxLeft + 16;
        int contentRight = boxRight - 16;
        int contentTop = listStartY;

        int contentW = contentRight - contentLeft;

        int listW = Math.max(180, (int) (contentW * 0.42));
        int listX = contentLeft;
        int rightX = listX + listW + 10;
        int rightW = contentRight - rightX;

        int y = contentTop - passivesListScrollOffset;
        int cardH = 34;
        int gap = 6;

        List<PassiveType> learned = new java.util.ArrayList<>(ClientPassivesState.getLearned());
        learned.sort(java.util.Comparator.comparing(PassiveType::getId));

        int yStart = y;
        for (PassiveType type : learned) {
            int bg = 0xBF1E1E23;
            int stroke = 0xFF3A3A3A;
            int accent = 0xFF6A5A00;
            if (type == PassiveType.SHADOW_SHELTER) accent = 0xFF8A6AD9;
            if (type == PassiveType.COAL_HEAT) accent = 0xFFFFA64D;
            if (type == PassiveType.FROST_SKIN || type == PassiveType.REACTIVE_FROST) accent = 0xFF66D0FF;

            context.fill(listX, y, listX + listW, y + cardH, bg);
            drawBorder(context, listX, y, listW, cardH, stroke);
            context.fill(listX + 1, y + 1, listX + 5, y + cardH - 1, accent);
            ItemStack icon = getPassiveIcon(type);
            context.drawItem(icon, listX + 10, y + 9);
            context.drawTextWithShadow(this.textRenderer, Text.literal(type.getDisplayName()).formatted(type.getColor()), listX + 30, y + 10, 0xFFFFFFFF);

            y += cardH + gap;
        }

        int contentHeight = Math.max(0, y - yStart);
        passivesListMaxScroll = Math.max(0, contentHeight - (boxBottom - contentTop - 10));
        if (passivesListScrollOffset > passivesListMaxScroll) passivesListScrollOffset = passivesListMaxScroll;

        PassiveType[] active = ClientPassivesState.getActive();

        int slotSize = 44;
        int slotGap = 10;
        int slot0Y = contentTop + 10;
        int slot1Y = slot0Y + slotSize + slotGap;

        int slotsX = rightX;
        passiveSlotX = slotsX;
        passiveSlot0Y = slot0Y;
        passiveSlot1Y = slot1Y;
        passiveSlotSize = slotSize;
        renderSlot(context, slotsX, slot0Y, slotSize, "Слот 1", active.length > 0 ? active[0] : null);
        renderSlot(context, slotsX, slot1Y, slotSize, "Слот 2", active.length > 1 ? active[1] : null);

        // Панель описания справа/ниже слотов (чуть выше, чтобы не "уезжала" вниз)
        int descTop = slot1Y + slotSize + 6;
        int bottomMargin = 16;
        passiveDescX = rightX;
        passiveDescW = Math.max(140, rightW);
        int desiredH = 90;
        passiveDescY = descTop;
        int availableH = boxBottom - bottomMargin - passiveDescY;
        passiveDescH = Math.max(0, Math.min(desiredH, availableH));

        renderPassiveDescription(context);
    }

    private void renderSlot(DrawContext context, int x, int y, int size, String title, PassiveType current) {
        int bg = 0xBF1E1E23;
        int stroke = 0xFF3A3A3A;
        context.fill(x, y, x + size, y + size, bg);
        drawBorder(context, x, y, size, size, stroke);
        context.drawTextWithShadow(this.textRenderer, title, x + size + 8, y + 4, 0xFFCCCCCC);
        if (current != null) {
            ItemStack icon = getPassiveIcon(current);
            context.drawItem(icon, x + (size - 16) / 2, y + (size - 16) / 2);
            context.drawTextWithShadow(this.textRenderer, Text.literal(current.getDisplayName()).formatted(current.getColor()), x + size + 8, y + 18, 0xFFFFFFFF);
        } else {
            context.drawTextWithShadow(this.textRenderer, Text.literal("Пусто").formatted(net.minecraft.util.Formatting.DARK_GRAY), x + size + 8, y + 18, 0xFFFFFFFF);
        }
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedTab == Tab.PASSIVES) {
            boolean inDesc = mouseX >= passiveDescX && mouseX <= passiveDescX + passiveDescW && mouseY >= passiveDescY && mouseY <= passiveDescY + passiveDescH;
            if (inDesc && passiveDescMaxScroll > 0) {
                passiveDescScrollOffset -= (int) (verticalAmount * 18);
                if (passiveDescScrollOffset < 0) passiveDescScrollOffset = 0;
                if (passiveDescScrollOffset > passiveDescMaxScroll) passiveDescScrollOffset = passiveDescMaxScroll;
                return true;
            }

            if (passivesListMaxScroll > 0) {
                passivesListScrollOffset -= (int) (verticalAmount * 18);
                if (passivesListScrollOffset < 0) passivesListScrollOffset = 0;
                if (passivesListScrollOffset > passivesListMaxScroll) passivesListScrollOffset = passivesListMaxScroll;
                return true;
            }

            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        scrollOffset -= (int) (verticalAmount * 18);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() == 0 && selectedTab == Tab.PASSIVES) {
            PassiveType hit = hitTestPassive((int) click.x(), (int) click.y());
            if (hit != null) {
                passiveDescScrollOffset = 0;
                draggingPassive = hit;
                dragging = true;
                return true;
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0 && selectedTab == Tab.PASSIVES && dragging) {
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
                passiveDescScrollOffset = 0;
                ClientPlayNetworking.send(new PassiveSlotsUpdateC2SPacket(
                        s0 != null ? s0.getId() : "",
                        s1 != null ? s1.getId() : ""
                ));
            }
            dragging = false;
            draggingPassive = null;
            return true;
        }
        return super.mouseReleased(click);
    }

    private PassiveType hitTestPassive(int mouseX, int mouseY) {
        int boxTop = 32;
        int pad = 10;
        int boxLeft = pad;
        int boxRight = this.width - pad;
        int tabBarY = boxTop + 8;
        int tabHeight = 14;
        int listStartY = tabBarY + tabHeight + 8;

        int contentLeft = boxLeft + 16;
        int contentRight = boxRight - 16;
        int mid = contentLeft + (contentRight - contentLeft) / 2;

        int listX = contentLeft;
        int listW = mid - contentLeft - 10;
        int y = listStartY - passivesListScrollOffset;
        int cardH = 34;
        int gap = 6;

        List<PassiveType> learned = new java.util.ArrayList<>(ClientPassivesState.getLearned());
        learned.sort(java.util.Comparator.comparing(PassiveType::getId));

        for (PassiveType type : learned) {
            if (mouseX >= listX && mouseX <= listX + listW && mouseY >= y && mouseY <= y + cardH) {
                return type;
            }
            y += cardH + gap;
        }
        return null;
    }

    private int hitTestSlot(int mouseX, int mouseY) {
        int slotsX = passiveSlotX;
        int slotSize = passiveSlotSize;
        int slot0Y = passiveSlot0Y;
        int slot1Y = passiveSlot1Y;

        if (mouseX >= slotsX && mouseX <= slotsX + slotSize && mouseY >= slot0Y && mouseY <= slot0Y + slotSize) {
            return 0;
        }
        if (mouseX >= slotsX && mouseX <= slotsX + slotSize && mouseY >= slot1Y && mouseY <= slot1Y + slotSize) {
            return 1;
        }
        return -1;
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
        FIRE, ICE, DARK, SOUND, PASSIVES
    }

    private Tab selectedTab = Tab.FIRE;

}
