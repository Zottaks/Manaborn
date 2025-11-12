package com.manaborn.client.ui;

import com.manaborn.screen.MagicCraftingTableScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Screen для магического верстака
 */
public class MagicCraftingTableScreen extends HandledScreen<MagicCraftingTableScreenHandler> {

    public MagicCraftingTableScreen(MagicCraftingTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = 29;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;
        
        // Фон
        int bg = 0xCC0F0F13;
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, bg);
        
        // Магическая рамка
        context.fill(x, y, x + backgroundWidth, y + 2, 0xFFAA00FF);  // Верх - фиолетовый
        context.fill(x, y + backgroundHeight - 2, x + backgroundWidth, y + backgroundHeight, 0xFFAA00FF);  // Низ
        context.fill(x, y, x + 2, y + backgroundHeight, 0xFFAA00FF);  // Лево
        context.fill(x + backgroundWidth - 2, y, x + backgroundWidth, y + backgroundHeight, 0xFFAA00FF);  // Право
        
        // === ЗОНА КРАФТА ===
        // Фон для зоны крафта
        context.fill(x + 6, y + 14, x + 150, y + 74, 0x99000000);  // Полупрозрачный черный
        
        // Рамка зоны крафта (серая)
        context.fill(x + 6, y + 14, x + 150, y + 15, 0xFF6B6B6B);  // Верх
        context.fill(x + 6, y + 73, x + 150, y + 74, 0xFF6B6B6B);  // Низ
        context.fill(x + 6, y + 14, x + 7, y + 74, 0xFF6B6B6B);    // Лево
        context.fill(x + 149, y + 14, x + 150, y + 74, 0xFF6B6B6B); // Право
        
        // Слоты крафтинга 3x3 (x=30, y=17)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = x + 30 + col * 18;
                int slotY = y + 17 + row * 18;
                drawSlot(context, slotX, slotY, 0xFF8B8B8B);  // Серые слоты
            }
        }
        
        // Стрелка "→" между крафтом и результатом
        context.fill(x + 90, y + 38, x + 110, y + 40, 0xFF999999);   // Горизонтальная линия
        context.fill(x + 107, y + 35, x + 110, y + 38, 0xFF999999);  // Верхняя стрелка
        context.fill(x + 107, y + 40, x + 110, y + 43, 0xFF999999);  // Нижняя стрелка
        
        // Слот результата (x=124, y=35)
        drawSlot(context, x + 124, y + 35, 0xFFAAAAAA);  // Светло-серый результат
        
        // === РАЗДЕЛИТЕЛЬ ===
        int separatorY = y + 78;
        context.fill(x + 4, separatorY, x + backgroundWidth - 4, separatorY + 1, 0xFF666666);  // Линия
        context.fill(x + 4, separatorY + 1, x + backgroundWidth - 4, separatorY + 2, 0x88888888);  // Подсветка
        
        // === ИНВЕНТАРЬ ИГРОКА ===
        // Фон для инвентаря
        context.fill(x + 6, y + 81, x + backgroundWidth - 6, y + 160, 0x66000000);  // Темный фон
        
        // Основной инвентарь 9x3 (x=8, y=84)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 8 + col * 18;
                int slotY = y + 84 + row * 18;
                drawSlot(context, slotX, slotY, 0xFF555555);  // Серые слоты
            }
        }
        
        // Разделитель перед хотбаром
        context.fill(x + 8, y + 138, x + backgroundWidth - 8, y + 139, 0xFF444444);
        
        // Хотбар 9x1 (x=8, y=142)
        for (int col = 0; col < 9; col++) {
            int slotX = x + 8 + col * 18;
            int slotY = y + 142;
            drawSlot(context, slotX, slotY, 0xFF777777);  // Светло-серые слоты хотбара
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
}
