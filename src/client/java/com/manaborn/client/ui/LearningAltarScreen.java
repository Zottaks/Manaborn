package com.manaborn.client.ui;

import com.manaborn.items.SpellBookItem;
import com.manaborn.screen.LearningAltarScreenHandler;
import com.manaborn.spells.SpellType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

/**
 * Алтарь Изучения — визуал в едином стиле Manaborn.
 */
public class LearningAltarScreen extends HandledScreen<LearningAltarScreenHandler> {
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 200;

    // Акценты
    private static final int ACCENT_WAND = 0x4DA3FF;
    private static final int ACCENT_PROGRESS = 0x9B7BFF;

    private ButtonWidget learnButton;

    public LearningAltarScreen(LearningAltarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        // Рамка окна в едином стиле
        ManabornGuiStyle.drawFrame(context, x, y, GUI_WIDTH, GUI_HEIGHT);

        // === ПАНЕЛЬ ИЗУЧЕНИЯ ===
        int panelX = x + 10;
        int panelY = y + 28;
        int panelW = GUI_WIDTH - 20;
        int panelH = 60;
        ManabornGuiStyle.drawPanel(context, panelX, panelY, panelW, panelH);

        // Слоты: палочка слева, книга справа
        int wandSlotX = x + 30;
        int wandSlotY = y + 45;
        int bookSlotX = x + 170;
        int bookSlotY = y + 45;

        boolean wandFilled = false;
        boolean bookFilled = false;
        SpellType bookSpell = null;
        try {
            wandFilled = this.handler.getSlot(0).hasStack();
            if (this.handler.getSlot(1).hasStack()) {
                bookFilled = true;
                ItemStack bookStack = this.handler.getSlot(1).getStack();
                if (bookStack.getItem() instanceof SpellBookItem spellBook) {
                    bookSpell = spellBook.getSpell();
                }
            }
        } catch (Exception ignored) {}

        ManabornGuiStyle.drawSlot(context, wandSlotX, wandSlotY, 16, ACCENT_WAND, wandFilled);
        ManabornGuiStyle.drawSlot(context, bookSlotX, bookSlotY, 16, getSpellElementRgb(bookSpell), bookFilled);

        // === МАГИЧЕСКИЙ КРУГ ИЗУЧЕНИЯ (вокруг центра панели, под прогресс-баром) ===
        int circleCx = x + 110;
        int circleCy = y + 58;
        ManabornGuiStyle.drawDottedCircle(context, circleCx, circleCy, 20, 0x9B7BFF, 40);
        ManabornGuiStyle.drawDottedCircle(context, circleCx, circleCy, 13, 0xBFA8FF, 55);
        ManabornGuiStyle.drawOrbitRunes(context, this.textRenderer, circleCx, circleCy, 20, 0xBFA8FF);

        // Потоки знаний: от слотов к кругу
        boolean learningActive = this.handler.getProgress() > 0;
        ManabornGuiStyle.drawFlowLine(context, wandSlotX + 19, wandSlotY + 8, circleCx - 22, circleCy, 0x4DA3FF, learningActive);
        ManabornGuiStyle.drawFlowLine(context, circleCx + 22, circleCy, bookSlotX - 2, bookSlotY + 8, 0xBFA8FF, learningActive);

        // === ПРОГРЕСС ИЗУЧЕНИЯ ===
        int progress = this.handler.getProgress();
        int max = this.handler.getMaxProgress();

        int barX = x + 60;
        int barY = y + 50;
        int barW = 100;
        int barH = 8;
        float prog = max > 0 ? progress / (float) max : 0.0f;
        ManabornGuiStyle.drawProgressBar(context, barX, barY, barW, barH, prog, ACCENT_PROGRESS);

        // === ИНВЕНТАРЬ ИГРОКА ===
        int invWidth = 9 * 18;
        int invX = x + (GUI_WIDTH - invWidth) / 2;
        int invY = y + 118;
        int invHeight = 4 * 18 + 4;
        ManabornGuiStyle.drawPanel(context, invX - 4, invY - 4, invWidth + 8, invHeight + 8);
        ManabornGuiStyle.drawInventoryGrid(context, invX, invY);

        // Парящие магические частицы во время изучения
        if (max > 0 && progress > 0) {
            long time = Util.getMeasuringTimeMs();
            for (int i = 0; i < 8; i++) {
                float pt = (time % 3000L) / 3000.0f + i * 0.125f;
                float px = x + 110 + (float) (Math.sin(pt * Math.PI * 2 + i) * 45);
                float py = y + 54 + (float) (Math.cos(pt * Math.PI * 2 + i * 0.7) * 14);
                int alpha = (int) (0x50 * (0.5f + 0.5f * Math.sin(pt * Math.PI * 4)));
                context.fill((int) px, (int) py, (int) px + 2, (int) py + 2,
                        (alpha << 24) | ManabornGuiStyle.brightenRgb(0x9B7BFF, 0.4f));
            }
        }
    }

    /**
     * RGB-акцент стихии заклинания для рамки слота книги.
     */
    private int getSpellElementRgb(SpellType spell) {
        if (spell == null) return 0x8E86A8;
        return switch (spell) {
            case FIREBALL, FIRE_WALL, INNER_HEAT, HEAT_WEAPON -> 0xFF8C00;
            case ICE_SHARD, FROST_AURA, ICE_WALL, FROST_COFFIN -> 0x4FC3F7;
            case SOUND_STRIKE, BATTLE_HYMN, CLARIFYING_TONE -> 0xFFD700;
            case ABSORBING_BARRIER, WEAKNESS_CURSE -> 0xB45FD0;
            default -> 0x8E86A8;
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        ManabornGuiStyle.drawTitle(context, this.textRenderer, "Алтарь Изучения", GUI_WIDTH, 8);

        // Лейблы слотов
        context.drawText(this.textRenderer, "Палочка", 18, 35, ManabornGuiStyle.COLOR_TEXT_DIM, false);
        context.drawText(this.textRenderer, "Книга", 168, 35, ManabornGuiStyle.COLOR_TEXT_DIM, false);

        // Требуемый опыт
        int req = handler.getRequiredXp();
        boolean enough = handler.hasEnoughXp();
        if (req > 0) {
            int color = enough ? 0xFF9BE89B : 0xFFE88A8A;
            String xpText = "Опыт: " + req + " уровней";
            context.drawText(this.textRenderer, xpText, 70, 70, color, false);
        }
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        learnButton = ButtonWidget.builder(
            Text.literal("✦ ИЗУЧИТЬ ✦").formatted(Formatting.BOLD),
            btn -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null && mc.interactionManager != null) {
                    mc.interactionManager.clickButton(this.handler.syncId, com.manaborn.screen.LearningAltarScreenHandler.BUTTON_LEARN);
                }
            }
        ).dimensions(x + 70, y + 90, 80, 20).build();
        this.addDrawableChild(learnButton);
        updateButtonState();
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        updateButtonState();
    }

    private void updateButtonState() {
        boolean active = handler.canStart() && handler.hasEnoughXp() && handler.getProgress() == 0;
        if (learnButton != null) learnButton.active = active;
    }
}
