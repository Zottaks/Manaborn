package com.manaborn.client.ui;

import com.manaborn.screen.EtherConverterScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class EtherConverterScreen extends HandledScreen<EtherConverterScreenHandler> {
    public EtherConverterScreen(EtherConverterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Background
        int bg = 0xCC0F0F13;
        ctx.fill(x, y, x + backgroundWidth, y + backgroundHeight, bg);
        ctx.fill(x, y, x + backgroundWidth, y + 2, 0xFFB97CFF);
        ctx.fill(x, y + backgroundHeight - 2, x + backgroundWidth, y + backgroundHeight, 0xFFB97CFF);

        // Slots
        int s = 18;
        int inX = x + 44, inY = y + 35;    // input dust
        int outX = x + 116, outY = y + 35; // output ether
        int oc = 0xFF444444;
        drawSlotBorder(ctx, inX, inY, s, oc);
        drawSlotBorder(ctx, outX, outY, s, oc);

        // Progress bar between slots
        int progress = handler.getProgress();
        int maxProgress = handler.getMaxProgress();
        int barX = inX + s + 4;
        int barY = inY + (s - 6) / 2;
        int barWidth = outX - barX - 4;
        int barHeight = 6;
        int filledWidth = (maxProgress > 0) ? (int) (barWidth * (progress / (float) maxProgress)) : 0;
        drawProgressBar(ctx, barX, barY, barWidth, barHeight, filledWidth);

        // Labels
        ctx.drawText(this.textRenderer, Text.literal("Пыль").asOrderedText(), inX, inY - 12, 0xFFBBBBBB, false);
        ctx.drawText(this.textRenderer, Text.literal("Эфир").asOrderedText(), outX, outY - 12, 0xFFBBBBBB, false);

        // Title
        ctx.drawText(this.textRenderer, Text.literal("Преобразователь").asOrderedText(), x + 8, y + 6, 0xFFE9D8FF, false);
        
        // === ВЫДЕЛЕНИЕ ИНВЕНТАРЯ ИГРОКА ===
        // Основной инвентарь 9x3 (x=8, y=84)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 8 + col * 18;
                int slotY = y + 84 + row * 18;
                drawSlot(ctx, slotX, slotY, 0xFF8B8B8B);
            }
        }
        
        // Хотбар 9x1 (x=8, y=142)
        for (int col = 0; col < 9; col++) {
            int slotX = x + 8 + col * 18;
            int slotY = y + 142;
            drawSlot(ctx, slotX, slotY, 0xFF9A9A9A);
        }
    }
    
    /**
     * Рисует рамку слота
     */
    private void drawSlot(DrawContext context, int x, int y, int color) {
        // Внешняя рамка
        context.fill(x - 1, y - 1, x + 17, y, color);        // Верх
        context.fill(x - 1, y + 16, x + 17, y + 17, color);  // Низ
        context.fill(x - 1, y - 1, x, y + 17, color);        // Лево
        context.fill(x + 16, y - 1, x + 17, y + 17, color);  // Право
        
        // Внутренний темный фон
        context.fill(x, y, x + 16, y + 16, 0xAA000000);
    }

    private void drawSlotBorder(DrawContext ctx, int sx, int sy, int size, int color) {
        ctx.fill(sx - 1, sy - 1, sx + size + 1, sy, color);
        ctx.fill(sx - 1, sy + size, sx + size + 1, sy + size + 1, color);
        ctx.fill(sx - 1, sy, sx, sy + size, color);
        ctx.fill(sx + size, sy, sx + size + 1, sy + size, color);
    }

    private void drawProgressBar(DrawContext ctx, int x, int y, int w, int h, int filled) {
        // Border
        int border = 0xFF1F1F1F;
        ctx.fill(x - 1, y - 1, x + w + 1, y, border);
        ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, border);
        ctx.fill(x - 1, y, x, y + h, border);
        ctx.fill(x + w, y, x + w + 1, y + h, border);
        // Background
        ctx.fill(x, y, x + w, y + h, 0xFF000000);
        // Filled portion (purple/ethereal color)
        if (filled > 0) {
            ctx.fill(x, y, x + filled, y + h, 0xFFB97CFF);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);
        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX,
                this.playerInventoryTitleY, 4210752, false);
    }
}
