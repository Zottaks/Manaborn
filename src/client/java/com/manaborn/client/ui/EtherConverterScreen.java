package com.manaborn.client.ui;

import com.manaborn.screen.EtherConverterScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Преобразователь пыли в эфир - улучшенный визуал
 */
public class EtherConverterScreen extends HandledScreen<EtherConverterScreenHandler> {
    // UI Constants
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 200;
    
    // Colors
    private static final int COLOR_BG = 0xF0101020;
    private static final int COLOR_BORDER_INNER = 0xFF3D2A5C;
    private static final int COLOR_PANEL = 0xE0181830;
    private static final int COLOR_TITLE = 0xFFE0B0FF;
    private static final int COLOR_ETHER = 0xFFB97CFF;
    
    // Animation
    private int glowPulse = 0;
    
    public EtherConverterScreen(EtherConverterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        glowPulse = (glowPulse + 1) % 60;
        
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        // Основной фон
        ctx.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, COLOR_BG);
        
        // Внешняя рамка с пульсацией (как у алтарей)
        int pulseOffset = (int)(Math.sin(glowPulse * 0.1) * 20);
        int borderR = Math.min(255, 107 + pulseOffset);
        int borderColor = 0xFF000000 | (borderR << 16) | (75 << 8) | 163;
        drawBorder(ctx, x, y, GUI_WIDTH, GUI_HEIGHT, borderColor, 2);
        
        // Внутренняя рамка
        drawBorder(ctx, x + 3, y + 3, GUI_WIDTH - 6, GUI_HEIGHT - 6, COLOR_BORDER_INNER, 1);
        
        // === ПАНЕЛЬ ПРЕОБРАЗОВАНИЯ ===
        int panelX = x + 10;
        int panelY = y + 28;
        int panelW = GUI_WIDTH - 20;
        int panelH = 70;
        
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL);
        drawBorder(ctx, panelX, panelY, panelW, panelH, COLOR_BORDER_INNER, 1);
        
        // Слот пыли (слева)
        int dustX = x + 40;
        int dustY = y + 50;
        drawSlotBorder(ctx, dustX, dustY, 16, 0xFF4DA3FF);
        
        // Слот эфира (справа)
        int etherX = x + 160;
        int etherY = y + 50;
        drawSlotBorder(ctx, etherX, etherY, 16, COLOR_ETHER);
        
        // === ПРОГРЕСС БАР ===
        int progress = handler.getProgress();
        int maxProgress = handler.getMaxProgress();
        
        int barX = dustX + 25;
        int barY = dustY + 5;
        int barW = etherX - barX - 10;
        int filledWidth = (maxProgress > 0) ? (int)(barW * (progress / (float)maxProgress)) : 0;
        drawProgressBar(ctx, barX, barY, barW, 6, filledWidth, COLOR_ETHER);
        
        // === ИНВЕНТАРЬ ИГРОКА (центрировано) ===
        int invWidth = 9 * 18;
        int invX = x + (GUI_WIDTH - invWidth) / 2;
        int invY = y + 118;
        int invHeight = 4 * 18 + 4;
        
        ctx.fill(invX - 4, invY - 4, invX + invWidth + 4, invY + invHeight, 0x80000000);
        drawBorder(ctx, invX - 4, invY - 4, invWidth + 8, invHeight + 4, COLOR_BORDER_INNER, 1);
    }
    
    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // Заголовок
        String title = "✧ Преобразователь ✧";
        int titleWidth = this.textRenderer.getWidth(title);
        ctx.drawText(this.textRenderer, title, (GUI_WIDTH - titleWidth) / 2, 8, COLOR_TITLE, true);
        
        // Конверсия: 25 пыли = 1 эфир
        String convText = "25 пыли → 1 эфир";
        int convWidth = this.textRenderer.getWidth(convText);
        ctx.drawText(this.textRenderer, convText, (GUI_WIDTH - convWidth) / 2, 75, 0xFF888888, false);
    }
    
    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color, int thickness) {
        ctx.fill(x, y, x + w, y + thickness, color);
        ctx.fill(x, y + h - thickness, x + w, y + h, color);
        ctx.fill(x, y, x + thickness, y + h, color);
        ctx.fill(x + w - thickness, y, x + w, y + h, color);
    }
    
    private void drawSlotBorder(DrawContext ctx, int sx, int sy, int size, int color) {
        ctx.fill(sx - 2, sy - 2, sx + size + 2, sy - 1, color);
        ctx.fill(sx - 2, sy + size + 1, sx + size + 2, sy + size + 2, color);
        ctx.fill(sx - 2, sy - 1, sx - 1, sy + size + 1, color);
        ctx.fill(sx + size + 1, sy - 1, sx + size + 2, sy + size + 1, color);
    }
    
    private void drawProgressBar(DrawContext ctx, int x, int y, int w, int h, int filled, int color) {
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF1F1F1F);
        ctx.fill(x, y, x + w, y + h, 0xFF000000);
        if (filled > 0) {
            ctx.fill(x, y, x + filled, y + h, color);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
