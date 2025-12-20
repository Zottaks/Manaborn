package com.manaborn.client.ui;

import com.manaborn.screen.ManaConverterScreenHandler;
import com.manaborn.items.ManaVesselItem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/**
 * Преобразователь маны - улучшенный визуал
 */
public class ManaConverterScreen extends HandledScreen<ManaConverterScreenHandler> {
    // UI Constants
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 200;
    
    // Colors
    private static final int COLOR_BG = 0xF0101020;
    private static final int COLOR_BORDER_INNER = 0xFF3D2A5C;
    private static final int COLOR_PANEL = 0xE0181830;
    private static final int COLOR_TITLE = 0xFFE0B0FF;
    private static final int COLOR_MANA = 0xFF4DA3FF;
    
    // Animation
    private int glowPulse = 0;

    public ManaConverterScreen(ManaConverterScreenHandler handler, PlayerInventory inventory, Text title) {
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
        
        // === ПАНЕЛЬ КОНВЕРТАЦИИ ===
        int panelX = x + 10;
        int panelY = y + 28;
        int panelW = GUI_WIDTH - 20;
        int panelH = 70;
        
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL);
        drawBorder(ctx, panelX, panelY, panelW, panelH, COLOR_BORDER_INNER, 1);
        
        // Слот пыли (слева)
        int dustX = x + 30;
        int dustY = y + 50;
        drawSlotBorder(ctx, dustX, dustY, 16, 0xFF4DA3FF);
        
        // Слот сосуда (справа)
        int vesselX = x + 170;
        int vesselY = y + 50;
        drawSlotBorder(ctx, vesselX, vesselY, 16, 0xFF7ED957);
        
        // === ЦЕНТРАЛЬНЫЙ ТАНК МАНЫ ===
        int tankW = 24;
        int tankH = 50;
        int tankX = x + (GUI_WIDTH - tankW) / 2;
        int tankY = y + 35;
        drawTank(ctx, tankX, tankY, tankW, tankH, handler.getMana(), handler.getMaxMana());
        
        // === ПРОГРЕСС БАРЫ ===
        int progress = handler.getProgress();
        int maxProg = handler.getMaxProgress();
        int mana = handler.getMana();
        
        // Левый бар: пыль -> танк
        int leftBarX = dustX + 22;
        int leftBarY = dustY + 5;
        int leftBarW = tankX - leftBarX - 8;
        int leftFilled = (maxProg > 0) ? (int)(leftBarW * (progress / (float)maxProg)) : 0;
        drawProgressBar(ctx, leftBarX, leftBarY, leftBarW, 6, leftFilled, COLOR_MANA);
        
        // Правый бар: танк -> сосуд (анимация только при активной передаче)
        int rightBarX = tankX + tankW + 8;
        int rightBarY = vesselY + 5;
        int rightBarW = vesselX - rightBarX - 6;
        int rightFilled = 0;
        
        boolean hasVessel = false;
        boolean vesselCanAccept = false;
        try {
            if (this.handler.getSlot(1).hasStack()) {
                var stack = this.handler.getSlot(1).getStack();
                hasVessel = stack.getItem() instanceof ManaVesselItem;
                if (hasVessel) {
                    int stored = ManaVesselItem.getStored(stack);
                    vesselCanAccept = stored < ManaVesselItem.CAPACITY;
                }
            }
        } catch (Exception ignored) {}
        
        if (mana > 0 && hasVessel && vesselCanAccept) {
            long t = Util.getMeasuringTimeMs();
            float phase = (float)((t % 1000L) / 1000.0);
            rightFilled = (int)(rightBarW * phase);
        }
        drawProgressBar(ctx, rightBarX, rightBarY, rightBarW, 6, rightFilled, 0xFF7ED957);
        
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
        String title = "✧ Преобразователь Маны ✧";
        int titleWidth = this.textRenderer.getWidth(title);
        ctx.drawText(this.textRenderer, title, (GUI_WIDTH - titleWidth) / 2, 8, COLOR_TITLE, true);
        
        // Мана в танке
        int mana = handler.getMana();
        int maxMana = handler.getMaxMana();
        String manaText = mana + "/" + maxMana;
        int manaWidth = this.textRenderer.getWidth(manaText);
        ctx.drawText(this.textRenderer, manaText, (GUI_WIDTH - manaWidth) / 2, 88, COLOR_MANA, false);
        
        // Подпись инвентаря (центрировано)
        String invLabel = "Инвентарь";
        int invLabelX = (GUI_WIDTH - this.textRenderer.getWidth(invLabel)) / 2;
        ctx.drawText(this.textRenderer, invLabel, invLabelX, 108, 0xFF666666, false);
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
    
    private void drawTank(DrawContext ctx, int x, int y, int w, int h, int mana, int max) {
        // Рамка танка (статичная)
        drawBorder(ctx, x - 1, y - 1, w + 2, h + 2, 0xFF555555, 1);
        
        // Фон танка
        ctx.fill(x, y, x + w, y + h, 0xFF0A0A15);
        
        // Заполнение снизу
        if (max > 0 && mana > 0) {
            int fillH = Math.max(1, (int)(h * (mana / (float)max)));
            int fy = y + h - fillH;
            ctx.fill(x + 1, fy, x + w - 1, y + h, COLOR_MANA);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
