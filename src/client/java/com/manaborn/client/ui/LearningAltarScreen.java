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
 * Алтарь Изучения - улучшенный визуал
 */
public class LearningAltarScreen extends HandledScreen<LearningAltarScreenHandler> {
    // UI Constants
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 200;
    
    // Colors
    private static final int COLOR_BG = 0xF0101020;
    private static final int COLOR_BORDER_INNER = 0xFF3D2A5C;
    private static final int COLOR_PANEL = 0xE0181830;
    private static final int COLOR_TITLE = 0xFFE0B0FF;
    private static final int COLOR_TEXT = 0xFFCCCCCC;
    private static final int COLOR_HIGHLIGHT = 0xFF4DA3FF;
    
    // Animation
    private int glowPulse = 0;
    
    private ButtonWidget learnButton;

    public LearningAltarScreen(LearningAltarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        glowPulse = (glowPulse + 1) % 60;
        
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;
        
        // Основной фон
        context.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, COLOR_BG);
        
        // Внешняя рамка с пульсацией
        int pulseOffset = (int)(Math.sin(glowPulse * 0.1) * 20);
        int borderR = Math.min(255, 107 + pulseOffset);
        int borderColor = 0xFF000000 | (borderR << 16) | (75 << 8) | 163;
        drawBorder(context, x, y, GUI_WIDTH, GUI_HEIGHT, borderColor, 2);
        
        // Внутренняя рамка
        drawBorder(context, x + 3, y + 3, GUI_WIDTH - 6, GUI_HEIGHT - 6, COLOR_BORDER_INNER, 1);
        
        // === ПАНЕЛЬ ИЗУЧЕНИЯ ===
        int panelX = x + 10;
        int panelY = y + 28;
        int panelW = GUI_WIDTH - 20;
        int panelH = 60;
        
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL);
        drawBorder(context, panelX, panelY, panelW, panelH, COLOR_BORDER_INNER, 1);
        
        // Слоты: палочка слева (30, 45), книга справа (170, 45)
        int wandSlotX = x + 30;
        int wandSlotY = y + 45;
        int bookSlotX = x + 170;
        int bookSlotY = y + 45;
        int slotSize = 16;
        
        // Свечение активных слотов
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
        
        int wandGlow = wandFilled ? 0xFF4DA3FF : 0xFF444444;
        int bookGlow = getSpellElementColor(bookSpell, bookFilled);
        
        // Рамки слотов
        drawSlotBorder(context, wandSlotX, wandSlotY, slotSize, wandGlow);
        drawSlotBorder(context, bookSlotX, bookSlotY, slotSize, bookGlow);
        
        // === ПРОГРЕСС БАР ===
        int progress = this.handler.getProgress();
        int max = this.handler.getMaxProgress();
        
        int barX = x + 60;
        int barY = y + 50;
        int barW = 100;
        int barH = 8;
        int filled = max > 0 ? (int)((progress / (float)max) * barW) : 0;
        
        // Фон бара
        context.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF1F1F1F);
        context.fill(barX, barY, barX + barW, barY + barH, 0xFF000000);
        
        // Заполнение с анимацией
        if (filled > 0) {
            context.fill(barX, barY, barX + filled, barY + barH, COLOR_HIGHLIGHT);
            // Shimmer
            long t = Util.getMeasuringTimeMs();
            float phase = (t % 1500L) / 1500.0f;
            int shimmerX = barX + (int)(phase * filled);
            int shimmerW = Math.min(12, filled);
            if (shimmerX + shimmerW <= barX + filled) {
                context.fill(shimmerX, barY, shimmerX + shimmerW, barY + barH, 0x66FFFFFF);
            }
        }
        
        
        // === ИНВЕНТАРЬ ИГРОКА (центрировано) ===
        int invWidth = 9 * 18; // 162
        int invX = x + (GUI_WIDTH - invWidth) / 2; // Центрируем
        int invY = y + 118;
        int invHeight = 4 * 18 + 4;
        
        context.fill(invX - 4, invY - 4, invX + invWidth + 4, invY + invHeight, 0x80000000);
        drawBorder(context, invX - 4, invY - 4, invWidth + 8, invHeight + 4, COLOR_BORDER_INNER, 1);
        
        
        // Магические частицы при изучении
        if (max > 0 && progress > 0) {
            long time = Util.getMeasuringTimeMs();
            for (int i = 0; i < 8; i++) {
                float pt = (time % 3000L) / 3000.0f + i * 0.125f;
                float px = x + 60 + (float)(Math.sin(pt * Math.PI * 2 + i) * 50);
                float py = y + 50 + (float)(Math.cos(pt * Math.PI * 2 + i * 0.7) * 15);
                int alpha = (int)(0x40 * (0.5f + 0.5f * Math.sin(pt * Math.PI * 4)));
                context.fill((int)px, (int)py, (int)px + 2, (int)py + 2, (alpha << 24) | 0xB9E5FF);
            }
        }
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
    
    /**
     * Возвращает цвет рамки слота книги в зависимости от элемента заклинания
     * Огонь - оранжевый, Лёд - голубой, Звук - жёлтый, Тьма - тёмно-фиолетовый
     */
    private int getSpellElementColor(SpellType spell, boolean hasFilled) {
        if (!hasFilled || spell == null) {
            return 0xFF444444; // Серый - пустой слот
        }
        
        return switch (spell) {
            // Огненные заклинания - оранжевый
            case FIREBALL, FIRE_WALL, INNER_HEAT, HEAT_WEAPON -> 0xFFFF8C00;
            // Ледяные заклинания - голубой
            case ICE_SHARD, FROST_AURA, ICE_WALL -> 0xFF00BFFF;
            // Звуковые заклинания - жёлтый
            case SOUND_STRIKE, BATTLE_HYMN, CLARIFYING_TONE -> 0xFFFFD700;
            // Тёмные заклинания - тёмно-фиолетовый
            case ABSORBING_BARRIER, WEAKNESS_CURSE -> 0xFF8B008B;
            // Прочее - серый
            default -> 0xFF888888;
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Заголовок
        String title = "✧ Алтарь Изучения ✧";
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, (GUI_WIDTH - titleWidth) / 2, 8, COLOR_TITLE, true);
        
        // Лейблы слотов
        context.drawText(this.textRenderer, "Палочка", 18, 35, COLOR_TEXT, false);
        context.drawText(this.textRenderer, "Книга", 162, 35, COLOR_TEXT, false);
        
        // Требуемый опыт
        int req = handler.getRequiredXp();
        boolean enough = handler.hasEnoughXp();
        if (req > 0) {
            int color = enough ? 0xFF88FF88 : 0xFFFF6666;
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

