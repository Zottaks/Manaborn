package com.manaborn.client.ui;

import com.manaborn.screen.MagicCraftingTableScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Магический верстак - улучшенный визуал
 */
public class MagicCraftingTableScreen extends HandledScreen<MagicCraftingTableScreenHandler> {
    // UI Constants
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 195;
    
    // Colors
    private static final int COLOR_BG = 0xF0101020;
    private static final int COLOR_BORDER_INNER = 0xFF3D2A5C;
    private static final int COLOR_PANEL = 0xE0181830;
    private static final int COLOR_TITLE = 0xFFE0B0FF;
    
    // Animation
    private int glowPulse = 0;

    public MagicCraftingTableScreen(MagicCraftingTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        glowPulse = (glowPulse + 1) % 60;
        
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;
        
        // Основной фон
        context.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, COLOR_BG);
        
        // Внешняя рамка с пульсацией (как у алтарей)
        int pulseOffset = (int)(Math.sin(glowPulse * 0.1) * 20);
        int borderR = Math.min(255, 107 + pulseOffset);
        int borderColor = 0xFF000000 | (borderR << 16) | (75 << 8) | 163;
        drawBorder(context, x, y, GUI_WIDTH, GUI_HEIGHT, borderColor, 2);
        
        // Внутренняя рамка
        drawBorder(context, x + 3, y + 3, GUI_WIDTH - 6, GUI_HEIGHT - 6, COLOR_BORDER_INNER, 1);
        
        // === ПАНЕЛЬ КРАФТА ===
        int craftPanelX = x + 10;
        int craftPanelY = y + 25;
        int craftPanelW = GUI_WIDTH - 20;
        int craftPanelH = 70;
        
        context.fill(craftPanelX, craftPanelY, craftPanelX + craftPanelW, craftPanelY + craftPanelH, COLOR_PANEL);
        drawBorder(context, craftPanelX, craftPanelY, craftPanelW, craftPanelH, COLOR_BORDER_INNER, 1);
        
        // Слоты крафтинга 3x3 (центрировано)
        int gridStartX = x + 25;
        int gridStartY = y + 35;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = gridStartX + col * 18;
                int slotY = gridStartY + row * 18;
                drawSlotBorder(context, slotX, slotY, 16, 0xFF555555);
            }
        }
        
        // Стрелка → (статичная)
        context.drawText(this.textRenderer, "➜", x + 90, y + 52, 0xFFAAAAAA, false);
        
        // Слот результата с золотой рамкой (статичная)
        drawSlotBorder(context, x + 120, y + 47, 16, 0xFFDDAA00);
        
        // === ИНВЕНТАРЬ ИГРОКА (центрировано) ===
        int invWidth = 9 * 18;
        int invX = x + (GUI_WIDTH - invWidth) / 2;
        int invY = y + 113;
        int invHeight = 4 * 18 + 4;
        
        context.fill(invX - 4, invY - 4, invX + invWidth + 4, invY + invHeight, 0x80000000);
        drawBorder(context, invX - 4, invY - 4, invWidth + 8, invHeight + 4, COLOR_BORDER_INNER, 1);
    }
    
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Заголовок
        String title = "✧ Магический Крафт ✧";
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, (GUI_WIDTH - titleWidth) / 2, 8, COLOR_TITLE, true);
        
        // Подпись инвентаря (центрировано)
        String invLabel = "Инвентарь";
        int invLabelX = (GUI_WIDTH - this.textRenderer.getWidth(invLabel)) / 2;
        context.drawText(this.textRenderer, invLabel, invLabelX, 103, 0xFF666666, false);
    }
    
    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color, int thickness) {
        context.fill(x, y, x + w, y + thickness, color);
        context.fill(x, y + h - thickness, x + w, y + h, color);
        context.fill(x, y, x + thickness, y + h, color);
        context.fill(x + w - thickness, y, x + w, y + h, color);
    }
    
    private void drawSlotBorder(DrawContext context, int sx, int sy, int size, int color) {
        context.fill(sx - 2, sy - 2, sx + size + 2, sy - 1, color);
        context.fill(sx - 2, sy + size + 1, sx + size + 2, sy + size + 2, color);
        context.fill(sx - 2, sy - 1, sx - 1, sy + size + 1, color);
        context.fill(sx + size + 1, sy - 1, sx + size + 2, sy + size + 1, color);
    }
}
