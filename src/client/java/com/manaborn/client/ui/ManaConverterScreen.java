package com.manaborn.client.ui;

import com.manaborn.screen.ManaConverterScreenHandler;
import com.manaborn.items.ManaVesselItem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class ManaConverterScreen extends HandledScreen<ManaConverterScreenHandler> {
    public ManaConverterScreen(ManaConverterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Background panel
        int bg = 0xCC0F0F13;
        ctx.fill(x, y, x + backgroundWidth, y + backgroundHeight, bg);
        ctx.fill(x, y, x + backgroundWidth, y + 2, 0xFF4DA3FF);
        ctx.fill(x, y + backgroundHeight - 2, x + backgroundWidth, y + backgroundHeight, 0xFF4DA3FF);

        // Slots: 0 dust on left, 1 vessel on right
        int s = 18;
        int dustX = x + 31, dustY = y + 35;
        int vesselX = x + 127, vesselY = y + 35;
        int oc = 0xFF444444;
        // borders
        drawSlotBorder(ctx, dustX, dustY, s, oc);
        drawSlotBorder(ctx, vesselX, vesselY, s, oc);

        // Centered mana tank
        int tankW = 14, tankH = 54;
        int tankX = x + (backgroundWidth - tankW) / 2;
        int tankY = y + 16;
        drawTank(ctx, tankX, tankY, tankW, tankH, handler.getMana(), handler.getMaxMana());

        // Labels
        ctx.drawText(this.textRenderer, Text.literal("Пыль").asOrderedText(), dustX, dustY - 12, 0xFFBBBBBB, false);
        ctx.drawText(this.textRenderer, Text.literal("Хранилище").asOrderedText(), tankX - 6, tankY - 10, 0xFFBBD7FF, false);
        ctx.drawText(this.textRenderer, Text.literal("Сосуд").asOrderedText(), vesselX, vesselY - 12, 0xFFBBBBBB, false);

        // Left flow: dust -> mana (progress-based)
        int progress = handler.getProgress();
        int maxProg = handler.getMaxProgress();
        int leftGapStart = dustX + s + 3;
        int leftGapEnd = tankX - 6;
        int leftWidth = Math.max(16, leftGapEnd - leftGapStart);
        int leftFilled = (maxProg > 0) ? Math.max(0, Math.min(leftWidth, (int) (leftWidth * (progress / (float) maxProg)))) : 0;
        int leftBarY = dustY + (s - 6) / 2;
        drawBar(ctx, leftGapStart, leftBarY, leftWidth, 6, leftFilled, 0xFF4DA3FF);

        // Right flow: mana -> vessel (animate only when vessel present and can accept mana)
        int mana = handler.getMana();
        int rightGapStart = tankX + tankW + 6;
        int rightGapEnd = vesselX - 3;
        int rightWidth = Math.max(16, rightGapEnd - rightGapStart);
        int rightBarY = vesselY + (s - 6) / 2;
        int animFilled = 0;
        boolean hasVessel = false;
        boolean vesselCanAccept = false;
        if (this.handler.getSlot(1) != null && this.handler.getSlot(1).hasStack()) {
            var stack = this.handler.getSlot(1).getStack();
            hasVessel = stack.getItem() instanceof ManaVesselItem;
            if (hasVessel) {
                int stored = ManaVesselItem.getStored(stack);
                vesselCanAccept = stored < ManaVesselItem.CAPACITY;
            }
        }
        if (mana > 0 && hasVessel && vesselCanAccept) {
            long t = Util.getMeasuringTimeMs();
            float phase = (float) ((t % 1000L) / 1000.0);
            animFilled = (int) (rightWidth * phase);
        }
        drawBar(ctx, rightGapStart, rightBarY, rightWidth, 6, animFilled, 0xFF7ED957);

        // Title subdued
        ctx.drawText(this.textRenderer, Text.literal("Конвертор маны").asOrderedText(), x + 8, y + 6, 0xFFBBD7FF,
                false);

        // Subtle particles
        long time = Util.getMeasuringTimeMs();
        int dots = 10;
        for (int i = 0; i < dots; i++) {
            float t = (time % 4000L) / 4000.0f + i * 0.11f;
            float fx = (float) (MathHelper.sin(t * 8.1f + i) * 0.5f + 0.5f);
            float fy = (float) (MathHelper.cos(t * 6.7f + i * 0.31f) * 0.5f + 0.5f);
            int px = x + 10 + (int) (fx * (backgroundWidth - 20));
            int py = y + 12 + (int) (fy * (backgroundHeight - 24));
            int alpha = 0x20 + (int) (0x20 * (0.5f + 0.5f * MathHelper.sin(t * 6.28f + i)));
            int pcol = (alpha << 24) | 0xB9E5FF;
            ctx.fill(px, py, px + 1, py + 1, pcol);
        }
    }

    private void drawSlotBorder(DrawContext ctx, int sx, int sy, int size, int color) {
        ctx.fill(sx - 1, sy - 1, sx + size + 1, sy, color);
        ctx.fill(sx - 1, sy + size, sx + size + 1, sy + size + 1, color);
        ctx.fill(sx - 1, sy, sx, sy + size, color);
        ctx.fill(sx + size, sy, sx + size + 1, sy + size, color);
    }

    private void drawBar(DrawContext ctx, int x, int y, int w, int h, int filled, int color) {
        // border
        int bx = x - 1, by = y - 1, bw = w + 2, bh = h + 2;
        int border = 0xFF1F1F1F;
        ctx.fill(bx, by, bx + bw, by + 1, border);
        ctx.fill(bx, by + bh - 1, bx + bw, by + bh, border);
        ctx.fill(bx, by, bx + 1, by + bh, border);
        ctx.fill(bx + bw - 1, by, bx + bw, by + bh, border);
        // bg
        ctx.fill(x, y, x + w, y + h, 0xFF000000);
        // fill
        if (filled > 0)
            ctx.fill(x, y, x + filled, y + h, color);
    }

    private void drawTank(DrawContext ctx, int x, int y, int w, int h, int mana, int max) {
        // border
        int b = 0xFF1F1F1F;
        ctx.fill(x - 1, y - 1, x + w + 1, y, b);
        ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, b);
        ctx.fill(x - 1, y, x, y + h, b);
        ctx.fill(x + w, y, x + w + 1, y + h, b);
        // bg
        ctx.fill(x, y, x + w, y + h, 0xFF000000);
        // fill from bottom
        if (max > 0 && mana > 0) {
            int fillH = Math.max(1, (int) (h * (mana / (float) max)));
            int fy = y + h - fillH;
            ctx.fill(x + 1, fy + 1, x + w - 1, y + h - 1, 0xFF4DA3FF);
            // shimmer
            long t = Util.getMeasuringTimeMs();
            int shimmer = (int) ((t / 120) % (w - 2));
            int sx = x + 1 + shimmer;
            ctx.fill(sx, fy + 1, sx + 1, y + h - 1, 0x66B9E5FF);
        }
        // ticks and label
        ctx.drawText(this.textRenderer, Text.literal(mana + "/" + max), x - 8, y + h + 4, 0xFFBBD7FF, false);
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
