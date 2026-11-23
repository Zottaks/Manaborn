package com.manaborn.client.ui;

import com.manaborn.screen.ManaConverterScreenHandler;
import com.manaborn.items.ManaVesselItem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Преобразователь маны — визуал в едином стиле Manaborn.
 */
public class ManaConverterScreen extends HandledScreen<ManaConverterScreenHandler> {
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 200;

    // Акценты
    private static final int ACCENT_MANA = 0x5E9FE8;
    private static final int ACCENT_VESSEL = 0x7ED957;

    public ManaConverterScreen(ManaConverterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        ManabornGuiStyle.drawFrame(ctx, x, y, GUI_WIDTH, GUI_HEIGHT);

        // === ПАНЕЛЬ КОНВЕРТАЦИИ ===
        int panelX = x + 10;
        int panelY = y + 28;
        int panelW = GUI_WIDTH - 20;
        int panelH = 70;
        ManabornGuiStyle.drawPanel(ctx, panelX, panelY, panelW, panelH);

        // Слот пыли (слева)
        int dustX = x + 30;
        int dustY = y + 50;
        boolean dustFilled = slotHasStack(0);
        ManabornGuiStyle.drawSlot(ctx, dustX, dustY, 16, ACCENT_MANA, dustFilled);

        // Слот сосуда (справа)
        int vesselX = x + 170;
        int vesselY = y + 50;
        boolean vesselFilled = slotHasStack(1);
        ManabornGuiStyle.drawSlot(ctx, vesselX, vesselY, 16, ACCENT_VESSEL, vesselFilled);

        // === ЦЕНТРАЛЬНЫЙ ТАНК МАНЫ ===
        int tankW = 24;
        int tankH = 50;
        int tankX = x + (GUI_WIDTH - tankW) / 2;
        int tankY = y + 35;
        ManabornGuiStyle.drawTank(ctx, tankX, tankY, tankW, tankH, handler.getMana(), handler.getMaxMana(), ACCENT_MANA);

        // Искры над танком
        ManabornGuiStyle.drawOrbitSparkles(ctx, tankX + tankW / 2, tankY - 4, 12, 4, ACCENT_MANA);

        // Деления-метки на танке (25/50/75%)
        for (int i = 1; i <= 3; i++) {
            int tickY = tankY + tankH - (tankH * i / 4);
            ctx.fill(tankX - 5, tickY, tankX - 2, tickY + 1, ManabornGuiStyle.withAlpha(0x9B8BD8, 90));
            ctx.fill(tankX + tankW + 2, tickY, tankX + tankW + 5, tickY + 1, ManabornGuiStyle.withAlpha(0x9B8BD8, 90));
        }

        // === ЭНЕРГЕТИЧЕСКИЕ ТРУБЫ ===
        int progress = handler.getProgress();
        int maxProg = handler.getMaxProgress();
        int mana = handler.getMana();

        int pipeY = y + 59;

        // Левая труба: пыль -> танк
        float leftFill = maxProg > 0 ? progress / (float) maxProg : 0.0f;
        ManabornGuiStyle.drawPipe(ctx, dustX + 19, tankX - 2, pipeY, ACCENT_MANA, leftFill, progress > 0);

        // Правая труба: танк -> сосуд
        boolean vesselCanAccept = false;
        int stored = 0;
        try {
            if (this.handler.getSlot(1).hasStack()) {
                var stack = this.handler.getSlot(1).getStack();
                if (stack.getItem() instanceof ManaVesselItem) {
                    stored = ManaVesselItem.getStored(stack);
                    vesselCanAccept = stored < ManaVesselItem.CAPACITY;
                }
            }
        } catch (Exception ignored) {}

        boolean transferring = mana > 0 && vesselCanAccept;
        float rightFill = ManaVesselItem.CAPACITY > 0 ? stored / (float) ManaVesselItem.CAPACITY : 0.0f;
        ManabornGuiStyle.drawPipe(ctx, tankX + tankW + 2, vesselX - 2, pipeY, ACCENT_VESSEL, rightFill, transferring);

        // === ИНВЕНТАРЬ ИГРОКА ===
        int invWidth = 9 * 18;
        int invX = x + (GUI_WIDTH - invWidth) / 2;
        int invY = y + 118;
        int invHeight = 4 * 18 + 4;
        ManabornGuiStyle.drawPanel(ctx, invX - 4, invY - 4, invWidth + 8, invHeight + 8);
        ManabornGuiStyle.drawInventoryGrid(ctx, invX, invY);
    }

    private boolean slotHasStack(int idx) {
        try {
            return this.handler.getSlot(idx).hasStack();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ManabornGuiStyle.drawTitle(ctx, this.textRenderer, "Конвертер Маны", GUI_WIDTH, 8);

        // Мана в танке
        int mana = handler.getMana();
        int maxMana = handler.getMaxMana();
        String manaText = mana + "/" + maxMana;
        int manaWidth = this.textRenderer.getWidth(manaText);
        ctx.drawText(this.textRenderer, manaText, (GUI_WIDTH - manaWidth) / 2, 88,
                ManabornGuiStyle.withAlpha(ManabornGuiStyle.brightenRgb(0x5E9FE8, 0.3f), 255), false);

        // Подпись инвентаря
        String invLabel = "Инвентарь";
        int invLabelX = (GUI_WIDTH - this.textRenderer.getWidth(invLabel)) / 2;
        ctx.drawText(this.textRenderer, invLabel, invLabelX, 108, ManabornGuiStyle.COLOR_TEXT_DIM, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
