package com.manaborn.client.ui;

import com.manaborn.screen.LearningAltarScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

        // Progress bar (simple): width 100, height 8
        int progress = this.handler.getProgress();
        int max = this.handler.getMaxProgress();
        int barWidth = 100;
        int filled = max > 0 ? (int)((progress / (float)max) * barWidth) : 0;
        int barX = x + 38;
        int barY = y + 34;
        // border
        int bx = barX - 1, by = barY - 1, bw = barWidth + 2, bh = 10;
        context.fill(bx, by, bx + bw, by + 1, 0xFF1F1F1F);
        context.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF1F1F1F);
        context.fill(bx, by, bx + 1, by + bh, 0xFF1F1F1F);
        context.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFF1F1F1F);
        // background and fill
        context.fill(barX, barY, barX + barWidth, barY + 8, 0xFF000000);
        context.fill(barX, barY, barX + filled, barY + 8, 0xFF4DA3FF);

        // Slot outlines and labels
        // Slot positions mirror handler: wand (44,35), book (116,35) relative to GUI origin (8, y baseline like vanilla). We'll draw near those positions.
        int wandSlotX = x + 44;
        int wandSlotY = y + 35;
        int bookSlotX = x + 116;
        int bookSlotY = y + 35;
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
        // Labels
        context.drawText(this.textRenderer, Text.literal("Палочка").asOrderedText(), wandSlotX - 6, wandSlotY - 12, 0xFFBBBBBB, false);
        context.drawText(this.textRenderer, Text.literal("Книга").asOrderedText(), bookSlotX + 2, bookSlotY - 12, 0xFFBBBBBB, false);

        // Required XP info
        int req = handler.getRequiredXp();
        boolean enough = handler.hasEnoughXp();
        int color = enough ? 0xFFA0FF7A : 0xFFFF6B6B;
        String label = req > 0 ? ("Требуется опыт: " + req) : "";
        if (!label.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(label).asOrderedText(), x + 8, y + 16, color, false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
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
        }).dimensions(x + 58, y + 54, 60, 20).build();
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
