package com.manaborn.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import com.manaborn.items.MagicStaffItem;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import com.manaborn.client.state.ClientSpellLevelsState;

/**
 * Progress bar for Ice Wall spell charging
 */
public final class IceWallProgressOverlay {

    /**
     * Render the Ice Wall progress bar when charging
     */
    public static void render(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden)
            return;

        PlayerEntity player = client.player;
        ItemStack mainHandStack = player.getMainHandStack();

        // Check if player is holding Magic Staff with Ice Wall selected and is using it
        if (!(mainHandStack.getItem() instanceof MagicStaffItem))
            return;

        SpellType selected = SpellManager.getSelectedSpell(mainHandStack);
        if (selected != SpellType.ICE_WALL)
            return;

        // Check if player is actively charging (right mouse button held)
        if (!player.isUsingItem())
            return;

        // Получаем уровень заклинания Ice Wall
        int spellLevel = ClientSpellLevelsState.getSpellLevel(SpellType.ICE_WALL);
        
        // Максимальное время зарядки зависит от уровня:
        // Уровень 1: до 1.5с (30 тиков) - только малая стена
        // Уровень 2: до 3.0с (60 тиков) - малая + средняя
        // Уровень 3: до 3.0с (60 тиков) - все 3 вариации
        int maxChargeTicks = (spellLevel == 1) ? 30 : 60;
        
        // Calculate progress
        int maxUseTime = ((MagicStaffItem) mainHandStack.getItem()).getMaxUseTime(mainHandStack, player);
        int remainingUseTicks = player.getItemUseTimeLeft();
        int usedTicksRaw = maxUseTime - remainingUseTicks;
        if (usedTicksRaw < 0)
            usedTicksRaw = 0;
        int usedTicks = Math.min(usedTicksRaw, maxChargeTicks);
        float progress = MathHelper.clamp(usedTicks / (float) maxChargeTicks, 0.0f, 1.0f);

        // Get screen dimensions
        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();

        // Position: center of screen, slightly above hotbar
        int barWidth = 200;
        int barHeight = 20;
        int x = (screenWidth - barWidth) / 2;
        int y = screenHeight - 60;

        // Draw progress bar background
        drawProgressBarBackground(drawContext, x, y, barWidth, barHeight);

        // Draw progress bar fill
        drawProgressBarFill(drawContext, x, y, barWidth, barHeight, progress, spellLevel);

        // Draw progress text
        drawProgressText(drawContext, x, y, barWidth, barHeight, progress, usedTicks, spellLevel);
    }

    /**
     * Draw progress bar background with ice theme
     */
    private static void drawProgressBarBackground(DrawContext context, int x, int y, int width, int height) {
        // Outer border (dark ice blue)
        context.fill(x - 1, y - 1, x + width + 1, y, 0xFF1A3A52); // top
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, 0xFF1A3A52); // bottom
        context.fill(x - 1, y - 1, x, y + height + 1, 0xFF1A3A52); // left
        context.fill(x + width, y - 1, x + width + 1, y + height + 1, 0xFF1A3A52); // right

        // Background (dark ice)
        context.fill(x, y, x + width, y + height, 0xFF0D1929);

        // Inner border highlight
        context.fill(x, y, x + width, y + 1, 0xFF2A4A6A); // top highlight
        context.fill(x, y + height - 1, x + width, y + height, 0xFF2A4A6A); // bottom highlight
        context.fill(x, y, x + 1, y + height, 0xFF2A4A6A); // left highlight
        context.fill(x + width - 1, y, x + width, y + height, 0xFF2A4A6A); // right highlight
    }

    /**
     * Draw progress bar fill with gradient based on charge level
     */
    private static void drawProgressBarFill(DrawContext context, int x, int y, int width, int height, float progress, int spellLevel) {
        if (progress <= 0)
            return;

        int fillWidth = (int) (width * progress);

        // Determine color based on charge level and spell level
        int fillColor;
        if (spellLevel >= 3 && progress >= 1.0f) {
            // Level 3: Large wall - bright cyan
            fillColor = 0xFF00FFFF;
        } else if (spellLevel >= 2 && progress >= 0.5f) {
            // Level 2+: Medium wall - light blue
            fillColor = 0xFF00AAFF;
        } else {
            // Level 1 or low charge: Small wall - ice blue
            fillColor = 0xFF0088DD;
        }

        // Draw gradient fill
        for (int i = 0; i < fillWidth; i++) {
            float gradientProgress = i / (float) width;
            int color = interpolateColor(fillColor, 0xFF004488, gradientProgress * 0.5f);
            context.fill(x + i, y, x + i + 1, y + height, color);
        }

        // Add glow effect for high charge (только для уровня 3)
        if (spellLevel >= 3 && progress >= 1.0f) {
            float glowPulse = (float) Math.sin(System.currentTimeMillis() * 0.005) * 0.3f + 0.7f;
            int glowAlpha = (int) (glowPulse * 100);
            int glowColor = addAlpha(0x00FFFF, glowAlpha);
            context.fill(x, y - 1, x + fillWidth, y, glowColor);
            context.fill(x, y + height, x + fillWidth, y + height + 1, glowColor);
        }
    }

    /**
     * Draw progress text showing wall size and charge time
     */
    private static void drawProgressText(DrawContext context, int x, int y, int width, int height, float progress,
            int usedTicks, int spellLevel) {
        float usedSeconds = usedTicks / 20.0f;
        String wallType;
        String chargeInfo;

        // Максимальное время для текущего уровня
        float maxSeconds = (spellLevel == 1) ? 1.5f : 3.0f;
        float shownSeconds = Math.min(usedSeconds, maxSeconds);
        
        // Определяем тип стены в зависимости от уровня заклинания
        if (spellLevel >= 3 && usedSeconds >= 3.0f) {
            wallType = "Highground";
            chargeInfo = String.format("%.1f/3.0с", shownSeconds);
        } else if (spellLevel >= 2 && usedSeconds >= 1.5f) {
            wallType = "Средняя стена 3x4";
            chargeInfo = String.format("%.1f/3.0с", shownSeconds);
        } else {
            wallType = "Малая стена 2x2";
            // Для уровня 1 показываем максимум 1.5с
            String maxTime = (spellLevel == 1) ? "1.5с" : "3.0с";
            chargeInfo = String.format("%.1f/%s", shownSeconds, maxTime);
        }

        int textX = x + width / 2;
        int textY = y + height / 2 - 4;

        // Draw wall type
        String wallText = wallType;
        // Цвет зависит от уровня и прогресса
        int wallColor;
        if (spellLevel >= 3 && progress >= 1.0f) {
            wallColor = 0xFF00FFFF; // Cyan для Highground
        } else if (spellLevel >= 2 && progress >= 0.5f) {
            wallColor = 0xFF00AAFF; // Light blue для средней
        } else {
            wallColor = 0xFF0088DD; // Ice blue для малой
        }
        drawCenteredText(context, wallText, textX, textY, wallColor);

        // Draw charge info
        String chargeText = chargeInfo;
        int chargeColor = 0xFFCCCCCC;
        drawCenteredText(context, chargeText, textX, textY + 10, chargeColor);
    }

    /**
     * Draw centered text
     */
    private static void drawCenteredText(DrawContext context, String text, int x, int y, int color) {
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
        int textX = x - textWidth / 2;
        context.drawText(MinecraftClient.getInstance().textRenderer, text, textX, y, color, true);
    }

    /**
     * Interpolate between two colors
     */
    private static int interpolateColor(int color1, int color2, float t) {
        t = MathHelper.clamp(t, 0.0f, 1.0f);

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Add alpha channel to RGB color
     */
    private static int addAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
