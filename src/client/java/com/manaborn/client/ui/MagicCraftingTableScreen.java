package com.manaborn.client.ui;

import com.manaborn.screen.MagicCraftingTableScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Магический верстак — визуал в едином стиле Manaborn.
 */
public class MagicCraftingTableScreen extends HandledScreen<MagicCraftingTableScreenHandler> {
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 195;

    // Акценты
    private static final int ACCENT_GRID = 0x9B7BFF;
    private static final int ACCENT_RESULT = 0xFFCF5E;

    public MagicCraftingTableScreen(MagicCraftingTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        long t = Util.getMeasuringTimeMs();

        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        ManabornGuiStyle.drawFrame(context, x, y, GUI_WIDTH, GUI_HEIGHT);

        // === ПАНЕЛЬ КРАФТА ===
        int craftPanelX = x + 10;
        int craftPanelY = y + 25;
        int craftPanelW = GUI_WIDTH - 20;
        int craftPanelH = 70;
        ManabornGuiStyle.drawPanel(context, craftPanelX, craftPanelY, craftPanelW, craftPanelH);

        // Слоты крафтинга 3x3
        int gridStartX = x + 25;
        int gridStartY = y + 35;
        int slotIdx = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = gridStartX + col * 18;
                int slotY = gridStartY + row * 18;
                ManabornGuiStyle.drawSlot(context, slotX, slotY, 16, ACCENT_GRID, slotHasStack(slotIdx));
                slotIdx++;
            }
        }

        // Рунические знаки по углам сетки крафта
        long tw = t / 350L;
        String[] runes = {"✦", "✧", "◆", "✦"};
        int[][] runePos = {{x + 17, y + 29}, {x + 79, y + 29}, {x + 17, y + 88}, {x + 79, y + 88}};
        for (int i = 0; i < 4; i++) {
            float twinkle = 0.5f + 0.5f * (float) Math.sin(t * 0.003 + i * 1.7);
            int runeColor = ManabornGuiStyle.withAlpha(
                    ManabornGuiStyle.brightenRgb(ACCENT_GRID, 0.2f * twinkle), (int) (120 + 90 * twinkle));
            context.drawText(this.textRenderer, runes[(int) ((tw + i) % 4)], runePos[i][0], runePos[i][1], runeColor, false);
        }

        // Поток энергии: сетка -> результат (активен, когда есть результат)
        boolean hasResult = slotHasStack(9);
        ManabornGuiStyle.drawFlowLine(context, x + 81, y + 55, x + 116, y + 55, ACCENT_RESULT, hasResult);

        // Стрелка к результату с мягкой пульсацией
        float arrowPulse = 0.5f + 0.5f * (float) Math.sin(t * 0.004);
        int arrowColor = ManabornGuiStyle.withAlpha(
                ManabornGuiStyle.blendRgb(0x8E86A8, 0xE9D9FF, arrowPulse * 0.6f), 255);
        context.drawText(this.textRenderer, "➜", x + 92, y + 52, arrowColor, false);

        // Слот результата: скобки + кружащиеся искры
        ManabornGuiStyle.drawSlot(context, x + 120, y + 47, 16, ACCENT_RESULT, hasResult);
        ManabornGuiStyle.drawCornerBrackets(context, x + 116, y + 43, 24, 24, ACCENT_RESULT);
        ManabornGuiStyle.drawOrbitSparkles(context, x + 128, y + 55, 16, 3, ACCENT_RESULT);

        // === ИНВЕНТАРЬ ИГРОКА ===
        int invWidth = 9 * 18;
        int invX = x + (GUI_WIDTH - invWidth) / 2;
        int invY = y + 113;
        int invHeight = 4 * 18 + 4;
        ManabornGuiStyle.drawPanel(context, invX - 4, invY - 4, invWidth + 8, invHeight + 8);
        ManabornGuiStyle.drawInventoryGrid(context, invX, invY);
    }

    private boolean slotHasStack(int idx) {
        try {
            return this.handler.getSlot(idx).hasStack();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        ManabornGuiStyle.drawTitle(context, this.textRenderer, "Магический Крафт", GUI_WIDTH, 8);

        String invLabel = "Инвентарь";
        int invLabelX = (GUI_WIDTH - this.textRenderer.getWidth(invLabel)) / 2;
        context.drawText(this.textRenderer, invLabel, invLabelX, 103, ManabornGuiStyle.COLOR_TEXT_DIM, false);
    }
}
