package com.manaborn.client.ui;

import com.manaborn.screen.EtherConverterScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Преобразователь пыли в эфир — визуал в едином стиле Manaborn.
 */
public class EtherConverterScreen extends HandledScreen<EtherConverterScreenHandler> {
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 200;

    // Акценты
    private static final int ACCENT_DUST = 0x5E9FE8;
    private static final int ACCENT_ETHER = 0xC58CFF;

    public EtherConverterScreen(EtherConverterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        ManabornGuiStyle.drawFrame(ctx, x, y, GUI_WIDTH, GUI_HEIGHT);

        // === ПАНЕЛЬ ПРЕОБРАЗОВАНИЯ ===
        int panelX = x + 10;
        int panelY = y + 28;
        int panelW = GUI_WIDTH - 20;
        int panelH = 70;
        ManabornGuiStyle.drawPanel(ctx, panelX, panelY, panelW, panelH);

        // Слот пыли (слева)
        int dustX = x + 40;
        int dustY = y + 50;
        ManabornGuiStyle.drawSlot(ctx, dustX, dustY, 16, ACCENT_DUST, slotHasStack(0));

        // Слот эфира (справа)
        int etherX = x + 160;
        int etherY = y + 50;
        ManabornGuiStyle.drawSlot(ctx, etherX, etherY, 16, ACCENT_ETHER, slotHasStack(1));

        // === ЭФИРНЫЙ ВИХРЬ С КОЛЬЦОМ ПРОГРЕССА ===
        int progress = handler.getProgress();
        int maxProgress = handler.getMaxProgress();
        float prog = maxProgress > 0 ? progress / (float) maxProgress : 0.0f;

        int vortexCx = x + 110;
        int vortexCy = y + 58;
        ManabornGuiStyle.drawVortex(ctx, vortexCx, vortexCy, ACCENT_ETHER);
        ManabornGuiStyle.drawProgressRing(ctx, vortexCx, vortexCy, 17, prog, ACCENT_ETHER);

        // Потоки от слотов к вихрю
        ManabornGuiStyle.drawFlowLine(ctx, dustX + 19, dustY + 8, vortexCx - 20, vortexCy, ACCENT_DUST, progress > 0);
        ManabornGuiStyle.drawFlowLine(ctx, vortexCx + 20, vortexCy, etherX - 2, etherY + 8, ACCENT_ETHER, progress > 0);

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
        ManabornGuiStyle.drawTitle(ctx, this.textRenderer, "Преобразователь", GUI_WIDTH, 8);

        // Конверсия: 25 пыли = 1 эфир
        String convText = "25 пыли → 1 эфир";
        int convWidth = this.textRenderer.getWidth(convText);
        ctx.drawText(this.textRenderer, convText, (GUI_WIDTH - convWidth) / 2, 75, ManabornGuiStyle.COLOR_TEXT_DIM, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
