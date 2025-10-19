package com.manaborn.client;

import com.manaborn.client.state.ClientManaState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Отображение маны на HUD
 * Использует новый HudElementRegistry API вместо устаревшего HudRenderCallback
 */
public final class ManaHudOverlay {
	
	/**
	 * Метод рендеринга маны на HUD
	 * 
	 * @param drawContext Контекст для рисования
	 * @param tickCounter Счетчик тиков для анимаций
	 */
	public static void render(DrawContext drawContext, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client == null || client.player == null || client.options.hudHidden) return;

		int mana = ClientManaState.getMana();
		int max = ClientManaState.getMaxMana();
		if (max <= 0) return;

		int bubbles = max / 10; // 10 маны на пузырёк
		int filled = (int) Math.round((mana / (double) max) * bubbles);

		int screenWidth = drawContext.getScaledWindowWidth();
		int screenHeight = drawContext.getScaledWindowHeight();

		int xRight = screenWidth - 10;
		int yBottom = screenHeight - 32;

		int bubbleSize = 9;
		int gap = 1;
		int outline = 0xFF1F1F1F;
		int fillBlue = 0xFF4DA3FF;
		int emptyFill = 0xFF000000;

		for (int i = 0; i < bubbles; i++) {
			int x1 = xRight - i * (bubbleSize + gap) - bubbleSize;
			int y1 = yBottom;
			int x2 = x1 + bubbleSize;
			int y2 = y1 + bubbleSize;
			// outline
			drawContext.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, outline);
			// background
			drawContext.fill(x1, y1, x2, y2, i < filled ? fillBlue : emptyFill);
		}
	}
}


