package com.manaborn.client.ui;

import com.manaborn.screen.LearningAltarScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class LearningAltarScreen extends HandledScreen<LearningAltarScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("manaborn", "textures/gui/learning_altar.png");
    private ButtonWidget learnButton;

    public LearningAltarScreen(LearningAltarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        // Decorated background: dark panel with colored accents
        int bg = 0xCC0F0F13; // deep bluish gray with alpha
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, bg);
        // top/bottom accent lines
        context.fill(x, y, x + backgroundWidth, y + 2, 0xFF2A4A7A);
        context.fill(x, y + backgroundHeight - 2, x + backgroundWidth, y + backgroundHeight, 0xFF2A4A7A);
        // corner accents
        context.fill(x + 4, y + 4, x + 20, y + 5, 0xFF4DA3FF);
        context.fill(x + backgroundWidth - 20, y + 4, x + backgroundWidth - 4, y + 5, 0xFF4DA3FF);
        context.fill(x + 4, y + backgroundHeight - 5, x + 20, y + backgroundHeight - 4, 0xFF4DA3FF);
        context.fill(x + backgroundWidth - 20, y + backgroundHeight - 5, x + backgroundWidth - 4, y + backgroundHeight - 4, 0xFF4DA3FF);

        // Progress bar (between slots): compute gap between wand and book slots
        int progress = this.handler.getProgress();
        int max = this.handler.getMaxProgress();
        int wandSlotX = x + 44;
        int wandSlotY = y + 35;
        int bookSlotX = x + 116;
        int bookSlotY = y + 35;
        int slotSize = 18;
        int gapLeft = wandSlotX + slotSize;
        int gapRight = bookSlotX;
        int available = Math.max(0, gapRight - gapLeft);
        int barWidth = Math.max(32, Math.min(60, available - 4)); // keep small margins and sane limits
        int filled = max > 0 ? (int)((progress / (float)max) * barWidth) : 0;
        int barX = gapLeft + ((available - barWidth) / 2);
        int barY = wandSlotY + (slotSize - 8) / 2; // vertically centered in slot row
        // border
        int bx = barX - 1, by = barY - 1, bw = barWidth + 2, bh = 10;
        context.fill(bx, by, bx + bw, by + 1, 0xFF1F1F1F);
        context.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF1F1F1F);
        context.fill(bx, by, bx + 1, by + bh, 0xFF1F1F1F);
        context.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFF1F1F1F);
        // background and fill
        context.fill(barX, barY, barX + barWidth, barY + 8, 0xFF000000);
        // animated shimmer for filled portion
        int baseFill = 0xFF4DA3FF;
        context.fill(barX, barY, barX + filled, barY + 8, baseFill);
        if (filled > 0) {
            long t = Util.getMeasuringTimeMs();
            float phase = (t % 2000L) / 2000.0f; // 2s loop
            int shimmerWidth = 18;
            int shimmerX = barX + (int)MathHelper.lerp(phase, 0, Math.max(0, filled - shimmerWidth));
            int shimmerColor = 0x66B9E5FF; // soft translucent highlight
            context.fill(shimmerX, barY, shimmerX + shimmerWidth, barY + 8, shimmerColor);
        }

        // Slot outlines and labels
        // Slot positions mirror handler: wand (44,35), book (116,35) relative to GUI origin (8, y baseline like vanilla). We'll draw near those positions.
        // Outline 18x18 squares
        int s = 18;
        int oc = 0xFF444444;
        context.fill(wandSlotX - 1, wandSlotY - 1, wandSlotX + s + 1, wandSlotY, oc);
        context.fill(wandSlotX - 1, wandSlotY + s, wandSlotX + s + 1, wandSlotY + s + 1, oc);
        context.fill(wandSlotX - 1, wandSlotY, wandSlotX, wandSlotY + s, oc);
        context.fill(wandSlotX + s, wandSlotY, wandSlotX + s + 1, wandSlotY + s, oc);
        context.fill(bookSlotX - 1, bookSlotY - 1, bookSlotX + s + 1, bookSlotY, oc);
        context.fill(bookSlotX - 1, bookSlotY + s, bookSlotX + s + 1, bookSlotY + s + 1, oc);
        context.fill(bookSlotX - 1, bookSlotY, bookSlotX, bookSlotY + s, oc);
        context.fill(bookSlotX + s, bookSlotY, bookSlotX + s + 1, bookSlotY + s, oc);
        // Soft glow around active slots (under items)
        try {
            boolean wandFilled = this.handler.getSlot(0).hasStack();
            boolean bookFilled = this.handler.getSlot(1).hasStack();
            if (wandFilled) {
                context.fill(wandSlotX - 3, wandSlotY - 3, wandSlotX + s + 3, wandSlotY, 0x303AA7FF);
                context.fill(wandSlotX - 3, wandSlotY + s, wandSlotX + s + 3, wandSlotY + s + 3, 0x303AA7FF);
                context.fill(wandSlotX - 3, wandSlotY, wandSlotX, wandSlotY + s, 0x303AA7FF);
                context.fill(wandSlotX + s, wandSlotY, wandSlotX + s + 3, wandSlotY + s, 0x303AA7FF);
            }
            if (bookFilled) {
                context.fill(bookSlotX - 3, bookSlotY - 3, bookSlotX + s + 3, bookSlotY, 0x303AA7FF);
                context.fill(bookSlotX - 3, bookSlotY + s, bookSlotX + s + 3, bookSlotY + s + 3, 0x303AA7FF);
                context.fill(bookSlotX - 3, bookSlotY, bookSlotX, bookSlotY + s, 0x303AA7FF);
                context.fill(bookSlotX + s, bookSlotY, bookSlotX + s + 3, bookSlotY + s, 0x303AA7FF);
            }
        } catch (Exception ignored) {}
        // Labels
        context.drawText(this.textRenderer, Text.literal("Палочка").asOrderedText(), wandSlotX - 6, wandSlotY - 12, 0xFFBBBBBB, false);
        context.drawText(this.textRenderer, Text.literal("Книга").asOrderedText(), bookSlotX + 2, bookSlotY - 12, 0xFFBBBBBB, false);

        // Required XP info
        int req = handler.getRequiredXp();
        boolean enough = handler.hasEnoughXp();
        int color = enough ? 0xFFA0FF7A : 0xFFFF6B6B;
        String label = req > 0 ? ("Требуется опыт: " + req) : "";
        if (!label.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(label).asOrderedText(), x + 8, y + 8, color, false);
        }

        // Subtle background particles when learning is in progress
        if (max > 0 && progress > 0) {
            long time = Util.getMeasuringTimeMs();
            int dots = 14;
            for (int i = 0; i < dots; i++) {
                // pseudo-random but time-scrolling positions
                float t = (time % 4000L) / 4000.0f + i * 0.07f;
                float rx = (float)(MathHelper.sin(t * 9.3f + i) * 43758.5453f);
                float ry = (float)(MathHelper.cos(t * 7.1f + i * 0.37f) * 9623.1337f);
                float fx = rx - MathHelper.floor(rx);
                float fy = ry - MathHelper.floor(ry);
                float px = x + 10 + (fx * (backgroundWidth - 20));
                float py = y + 10 + (fy * (backgroundHeight - 20));
                int alpha = 0x20 + (int)(0x20 * (0.5f + 0.5f * MathHelper.sin(t * 6.28f + i)));
                int pcol = (alpha << 24) | 0xB9E5FF; // soft blue
                int px1 = (int)px, py1 = (int)py;
                context.fill(px1, py1, px1 + 1, py1 + 1, pcol);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Suppress the main title by not drawing it; keep the player inventory title only
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - textRenderer.getWidth(this.title)) / 2;
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        learnButton = ButtonWidget.builder(Text.literal("Изучить"), btn -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && mc.interactionManager != null) {
                mc.interactionManager.clickButton(this.handler.syncId, com.manaborn.screen.LearningAltarScreenHandler.BUTTON_LEARN);
            }
        }).dimensions(x + 58, y + 58, 60, 20).build();
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

