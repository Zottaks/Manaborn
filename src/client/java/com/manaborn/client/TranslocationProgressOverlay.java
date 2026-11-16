package com.manaborn.client;

import com.manaborn.items.MagicStaffItem;
import com.manaborn.client.state.ClientSpellLevelsState;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/**
 * Progress bar for Translocation spell charging (entropy magic).
 */
public final class TranslocationProgressOverlay {

    private static final java.util.Map<java.util.UUID, Integer> LAST_STAGE = new java.util.HashMap<>();

    public static void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }

        PlayerEntity player = client.player;
        ItemStack mainHandStack = player.getMainHandStack();

        if (!(mainHandStack.getItem() instanceof MagicStaffItem)) {
            return;
        }

        SpellType selected = SpellManager.getSelectedSpell(mainHandStack);
        if (selected != SpellType.TRANSLOCATION) {
            return;
        }

        if (!player.isUsingItem()) {
            LAST_STAGE.remove(player.getUuid());
            return;
        }

        int spellLevel = ClientSpellLevelsState.getSpellLevel(SpellType.TRANSLOCATION);
        int maxChargeTicks = getMaxChargeTicks(spellLevel);

        int maxUseTime = ((MagicStaffItem) mainHandStack.getItem()).getMaxUseTime(mainHandStack, player);
        int remainingUseTicks = player.getItemUseTimeLeft();
        int usedTicksRaw = maxUseTime - remainingUseTicks;
        if (usedTicksRaw < 0) {
            usedTicksRaw = 0;
        }
        int usedTicks = Math.min(usedTicksRaw, maxChargeTicks);
        float progress = MathHelper.clamp(usedTicks / (float) maxChargeTicks, 0.0f, 1.0f);

        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();

        int barWidth = 200;
        int barHeight = 16;
        int x = (screenWidth - barWidth) / 2;
        int y = screenHeight - 58;

        drawBackground(drawContext, x, y, barWidth, barHeight);
        drawThresholds(drawContext, x, y, barWidth, barHeight, maxChargeTicks);
        drawFill(drawContext, x, y, barWidth, barHeight, progress);
        drawText(drawContext, x, y, barWidth, barHeight, usedTicks, maxChargeTicks);
        handleStageSound(player, usedTicks, maxChargeTicks);
    }

    private static int getMaxChargeTicks(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 60;   // 3 seconds
            case 2 -> 200;  // 10 seconds
            default -> 400; // 20 seconds
        };
    }

    private static void handleStageSound(PlayerEntity player, int usedTicks, int maxChargeTicks) {
        int maxStage = maxChargeTicks >= 400 ? 3 : (maxChargeTicks >= 200 ? 2 : 1);
        int stageFromTicks = usedTicks >= 400 ? 3 : (usedTicks >= 200 ? 2 : (usedTicks >= 60 ? 1 : 0));
        int stage = Math.min(stageFromTicks, maxStage);
        int lastStage = LAST_STAGE.getOrDefault(player.getUuid(), 0);
        if (stage > lastStage) {
            float pitch = switch (stage) {
                case 1 -> 1.1f;
                case 2 -> 1.25f;
                default -> 1.4f;
            };
            player.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.6f, pitch);
            LAST_STAGE.put(player.getUuid(), stage);
        } else if (stage < lastStage) {
            LAST_STAGE.put(player.getUuid(), stage);
        }
    }

    private static void drawBackground(DrawContext context, int x, int y, int width, int height) {
        context.fill(x - 1, y - 1, x + width + 1, y, 0xFF666666);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFF666666);
        context.fill(x - 1, y - 1, x, y + height + 1, 0xFF666666);
        context.fill(x + width, y - 1, x + width + 1, y + height + 1, 0xFF666666);

        context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        context.fill(x, y, x + width, y + 1, 0xFF888888);
        context.fill(x, y + height - 1, x + width, y + height, 0xFF888888);
    }

    private static void drawFill(DrawContext context, int x, int y, int width, int height, float progress) {
        if (progress <= 0) {
            return;
        }
        int fillWidth = (int) (width * progress);
        for (int i = 0; i < fillWidth; i++) {
            float t = i / (float) width;
            int shade = (int) (220 + (35 * t));
            int color = 0xFF000000 | (shade << 16) | (shade << 8) | shade;
            context.fill(x + i, y, x + i + 1, y + height, color);
        }
    }

    private static void drawThresholds(DrawContext context, int x, int y, int width, int height, int maxChargeTicks) {
        int[] thresholds = new int[] {60, 200, 400};
        int color = 0xFFBBBBBB;
        for (int threshold : thresholds) {
            if (threshold > maxChargeTicks) {
                continue;
            }
            int markerX = x + Math.min(width - 1, Math.round(width * (threshold / (float) maxChargeTicks)));
            context.fill(markerX, y + 2, markerX + 1, y + height - 2, color);
        }
    }

    private static void drawText(DrawContext context, int x, int y, int width, int height, int usedTicks, int maxChargeTicks) {
        float usedSeconds = usedTicks / 20.0f;
        float maxSeconds = maxChargeTicks / 20.0f;
        String text = String.format("%.1f/%.1fс", Math.min(usedSeconds, maxSeconds), maxSeconds);
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, text, textX, textY, 0xFFFFFFFF, true);
    }
}
