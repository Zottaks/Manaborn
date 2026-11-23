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

    // ── Энтропийный глитч-стиль ──────────────────────────────────────────

    private static long chaosHash(long seed) {
        seed = (seed * 6364136223846793005L + 1442695040888963407L);
        seed ^= seed >> 29;
        seed *= 0xBF58476D1CE4E5B9L;
        seed ^= seed >> 32;
        return seed & 0x7FFFFFFFFFFFFFFFL;
    }

    private static int chaosColor(long seed) {
        return switch ((int) (seed % 8)) {
            case 0 -> 0xFFFFFF;
            case 1 -> 0xE86AF0;
            case 2 -> 0x5AF0D8;
            default -> 0xC7D0DD;
        };
    }

    private static void drawBackground(DrawContext context, int x, int y, int width, int height) {
        long frame = System.currentTimeMillis() / 160;

        context.fill(x, y, x + width, y + height, 0xF014141C);

        // Рамка с глитч-сегментами: отдельные куски вспыхивают хаотичными цветами
        for (int i = 0; i <= width; i += 2) {
            long seed = chaosHash(frame * 13 + i * 7);
            int top = ((seed % 5) == 0) ? chaosColor(seed >> 8) : 0x666666;
            int bottom = (((seed >> 4) % 7) == 0) ? chaosColor(seed >> 12) : 0x666666;
            context.fill(x - 1 + i, y - 1, x - 1 + i + 2, y, 0xFF000000 | top);
            context.fill(x - 1 + i, y + height, x - 1 + i + 2, y + height + 1, 0xFF000000 | bottom);
        }
        context.fill(x - 1, y - 1, x, y + height + 1, 0xFF666666);
        context.fill(x + width, y - 1, x + width + 1, y + height + 1, 0xFF666666);

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

        long frame = System.currentTimeMillis() / 120;

        // Глитч-осколки внутри заливки — пространство «трещит» по мере зарядки
        int shards = 2 + (int) (progress * 8);
        for (int s = 0; s < shards; s++) {
            long seed = chaosHash(frame * 31 + s * 977);
            int sx = x + (int) (seed % Math.max(1, fillWidth));
            int sy = y + 1 + (int) ((seed >> 8) % (height - 2));
            context.fill(sx, sy, sx + 1 + (int) (seed >> 16) % 2, sy + 1, 0xFF000000 | chaosColor(seed >> 20));
        }

        // Случайные строки заливки «сдвигаются» на пару пикселей — разлом
        if (fillWidth > 8 && (chaosHash(frame * 53) % 3) == 0) {
            long seed = chaosHash(frame * 97);
            int rowY = y + 2 + (int) (seed % (height - 4));
            int shift = 1 + (int) ((seed >> 6) % 2);
            int segX = x + 2 + (int) ((seed >> 10) % (fillWidth - 10));
            context.fill(segX, rowY, segX + 6, rowY + 1, 0xFF000000);
            context.fill(segX + 6 + shift, rowY, segX + 12 + shift, rowY + 1, 0xFFFFFFFF);
        }

        // Яркий фронт зарядки
        if (fillWidth > 2) {
            context.fill(x + fillWidth - 2, y, x + fillWidth, y + height, 0xFFFFFFFF);
        }
    }

    private static void drawThresholds(DrawContext context, int x, int y, int width, int height, int maxChargeTicks) {
        int[] thresholds = new int[] {60, 200, 400};
        long frame = System.currentTimeMillis() / 200;
        int i = 0;
        for (int threshold : thresholds) {
            if (threshold > maxChargeTicks) {
                continue;
            }
            int markerX = x + Math.min(width - 1, Math.round(width * (threshold / (float) maxChargeTicks)));
            // Маркеры мерцают хаотичными вспышками
            int color = ((chaosHash(frame * 7 + i * 131) % 4) == 0) ? 0xFFFFFFFF : 0xFFBBBBBB;
            context.fill(markerX, y + 2, markerX + 1, y + height - 2, color);
            i++;
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
