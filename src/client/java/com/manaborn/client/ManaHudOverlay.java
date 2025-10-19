package com.manaborn.client;

import com.manaborn.client.state.ClientManaState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

/**
 * Красивое отображение маны на HUD с анимациями
 * Использует новый HudElementRegistry API вместо устаревшего HudRenderCallback
 */
public final class ManaHudOverlay {
	
	private static int previousMana = 0;
	private static float shakeIntensity = 0.0f;
	private static float pulseAnimation = 0.0f;
	private static long lastUpdateTime = 0;
	
	/**
	 * Метод рендеринга маны на HUD с красивыми анимациями
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

		// Обновление анимаций
		updateAnimations(mana, tickCounter);

		int screenWidth = drawContext.getScaledWindowWidth();
		int screenHeight = drawContext.getScaledWindowHeight();

		// Позиция: правый нижний угол, ближе к низу экрана
		int xRight = screenWidth - 12;
		int yBottom = screenHeight - 39; // Ближе к низу

		// Параметры кристаллов (уменьшены для компактности)
		int crystalWidth = 8;
		int crystalHeight = 12;
		int gap = 2;
		int crystalsCount = 10; // Всегда показываем 10 кристаллов

		// Рисуем кристаллы справа налево
		for (int i = 0; i < crystalsCount; i++) {
			int x = xRight - i * (crystalWidth + gap);
			int y = yBottom;
			
			// Добавляем эффект встряски при трате маны
			if (shakeIntensity > 0) {
				x += (Math.random() - 0.5) * shakeIntensity * 2;
				y += (Math.random() - 0.5) * shakeIntensity * 2;
			}
			
			// Вычисляем заполненность этого кристалла (0.0 - 1.0)
			float fillAmount = calculateCrystalFill(i, mana, max, crystalsCount);
			
			// Рисуем кристалл
			drawManaCrystal(drawContext, x, y, crystalWidth, crystalHeight, fillAmount, i, tickCounter);
		}
		
		// Рисуем текст с количеством маны слева от кристаллов
		drawManaText(drawContext, xRight - crystalsCount * (crystalWidth + gap) - 30, yBottom + 2, mana, max);
	}
	
	/**
	 * Обновление анимаций
	 */
	private static void updateAnimations(int currentMana, RenderTickCounter tickCounter) {
		long currentTime = System.currentTimeMillis();
		float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
		lastUpdateTime = currentTime;
		
		// Анимация встряски при трате маны
		if (currentMana < previousMana) {
			shakeIntensity = 1.0f;
		}
		shakeIntensity = Math.max(0, shakeIntensity - deltaTime * 3.0f);
		
		// Анимация пульсации при регенерации
		if (currentMana > previousMana) {
			pulseAnimation = 1.0f;
		}
		pulseAnimation = Math.max(0, pulseAnimation - deltaTime * 2.0f);
		
		previousMana = currentMana;
	}
	
	/**
	 * Вычисляет заполненность конкретного кристалла
	 */
	private static float calculateCrystalFill(int crystalIndex, int mana, int maxMana, int totalCrystals) {
		float manaPerCrystal = maxMana / (float) totalCrystals;
		float crystalStartMana = crystalIndex * manaPerCrystal;
		float crystalEndMana = (crystalIndex + 1) * manaPerCrystal;
		
		if (mana <= crystalStartMana) {
			return 0.0f;
		} else if (mana >= crystalEndMana) {
			return 1.0f;
		} else {
			return (mana - crystalStartMana) / manaPerCrystal;
		}
	}
	
	/**
	 * Рисует красивый кристалл маны с градиентом и свечением
	 */
	private static void drawManaCrystal(DrawContext context, int x, int y, int width, int height, 
	                                     float fillAmount, int index, RenderTickCounter tickCounter) {
		// Время для анимации
		float time = (System.currentTimeMillis() % 10000) / 1000.0f + index * 0.3f;
		
		// Форма кристалла (ромб/кристалл)
		int centerX = x + width / 2;
		int topY = y;
		int bottomY = y + height;
		int leftX = x;
		int rightX = x + width;
		int middleY = y + height / 2;
		
		// Темная обводка кристалла
		drawCrystalOutline(context, centerX, topY, leftX, rightX, middleY, bottomY, 0xFF0A0A0A);
		
		if (fillAmount > 0) {
			// Вычисляем высоту заполнения
			int fillHeight = (int) (height * fillAmount);
			int fillTopY = bottomY - fillHeight;
			
			// Цвета градиента маны (от светло-голубого к темно-синему)
			int topColor = interpolateColor(0xFF00D4FF, 0xFF0066FF, fillAmount);
			int bottomColor = interpolateColor(0xFF0066FF, 0xFF001AFF, fillAmount);
			
			// Добавляем эффект пульсации при регенерации
			if (pulseAnimation > 0) {
				float pulse = (float) Math.sin(time * 10) * 0.5f + 0.5f;
				float glowIntensity = pulseAnimation * pulse * 0.3f;
				topColor = brightenColor(topColor, glowIntensity);
				bottomColor = brightenColor(bottomColor, glowIntensity);
			}
			
			// Рисуем заполненную часть кристалла с градиентом
			drawGradientCrystal(context, centerX, fillTopY, leftX, rightX, middleY, bottomY, 
			                   topColor, bottomColor, fillAmount);
			
			// Добавляем свечение на полных кристаллах
			if (fillAmount >= 0.95f) {
				float glowPulse = (float) Math.sin(time * 3) * 0.5f + 0.5f;
				int glowColor = addAlpha(0x00D4FF, (int) (glowPulse * 60));
				drawCrystalGlow(context, x - 1, y - 1, width + 2, height + 2, glowColor);
			}
		} else {
			// Пустой кристалл - темный с легким свечением
			drawGradientCrystal(context, centerX, topY, leftX, rightX, middleY, bottomY, 
			                   0xFF1A1A2A, 0xFF0A0A1A, 0.0f);
		}
		
		// Добавляем блики на кристалле
		if (fillAmount > 0.1f) {
			drawCrystalHighlight(context, centerX, topY + 2, width / 3, height / 4, 
			                    addAlpha(0xFFFFFF, (int) (fillAmount * 80)));
		}
	}
	
	/**
	 * Рисует обводку кристалла
	 */
	private static void drawCrystalOutline(DrawContext context, int centerX, int topY, 
	                                        int leftX, int rightX, int middleY, int bottomY, int color) {
		// Верхняя часть ромба
		drawLine(context, centerX, topY, leftX, middleY, color);
		drawLine(context, centerX, topY, rightX, middleY, color);
		// Нижняя часть ромба
		drawLine(context, leftX, middleY, centerX, bottomY, color);
		drawLine(context, rightX, middleY, centerX, bottomY, color);
	}
	
	/**
	 * Рисует заполненный кристалл с градиентом
	 */
	private static void drawGradientCrystal(DrawContext context, int centerX, int topY, 
	                                        int leftX, int rightX, int middleY, int bottomY,
	                                        int topColor, int bottomColor, float fillAmount) {
		// Рисуем кристалл построчно для создания градиента
		int height = bottomY - topY;
		for (int dy = 0; dy < height; dy++) {
			int currentY = topY + dy;
			float progress = dy / (float) height;
			int color = interpolateColor(topColor, bottomColor, progress);
			
			// Вычисляем ширину на текущей высоте (форма ромба)
			int currentWidth;
			if (currentY < middleY) {
				// Верхняя часть - расширяется
				float upperProgress = (currentY - topY) / (float) (middleY - topY);
				currentWidth = (int) ((rightX - leftX) * upperProgress);
			} else {
				// Нижняя часть - сужается
				float lowerProgress = (bottomY - currentY) / (float) (bottomY - middleY);
				currentWidth = (int) ((rightX - leftX) * lowerProgress);
			}
			
			int x1 = centerX - currentWidth / 2;
			int x2 = centerX + currentWidth / 2;
			
			if (x2 > x1) {
				context.fill(x1, currentY, x2, currentY + 1, color);
			}
		}
	}
	
	/**
	 * Рисует свечение вокруг кристалла
	 */
	private static void drawCrystalGlow(DrawContext context, int x, int y, int width, int height, int color) {
		// Внешнее свечение
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width - 1, y, x + width, y + height, color);
	}
	
	/**
	 * Рисует блик на кристалле
	 */
	private static void drawCrystalHighlight(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + height, color);
	}
	
	/**
	 * Рисует линию между двумя точками
	 */
	private static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);
		int sx = x1 < x2 ? 1 : -1;
		int sy = y1 < y2 ? 1 : -1;
		int err = dx - dy;
		
		while (true) {
			context.fill(x1, y1, x1 + 1, y1 + 1, color);
			
			if (x1 == x2 && y1 == y2) break;
			
			int e2 = 2 * err;
			if (e2 > -dy) {
				err -= dy;
				x1 += sx;
			}
			if (e2 < dx) {
				err += dx;
				y1 += sy;
			}
		}
	}
	
	/**
	 * Рисует текст с количеством маны
	 */
	private static void drawManaText(DrawContext context, int x, int y, int mana, int maxMana) {
		String text = mana + "/" + maxMana;
		int textColor = 0xFF00D4FF;
		
		// Добавляем тень для читаемости
		context.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, textColor, true);
	}
	
	/**
	 * Интерполяция между двумя цветами
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
	 * Добавляет альфа-канал к цвету
	 */
	private static int addAlpha(int rgb, int alpha) {
		return (alpha << 24) | (rgb & 0x00FFFFFF);
	}
	
	/**
	 * Осветляет цвет
	 */
	private static int brightenColor(int color, float amount) {
		int a = (color >> 24) & 0xFF;
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		
		r = Math.min(255, (int) (r + (255 - r) * amount));
		g = Math.min(255, (int) (g + (255 - g) * amount));
		b = Math.min(255, (int) (b + (255 - b) * amount));
		
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}


