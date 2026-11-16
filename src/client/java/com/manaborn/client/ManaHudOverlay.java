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

		// Позиция: правый нижний угол, с хорошим отступом
		int xRight = screenWidth - 14;
		int yBottom = screenHeight - 40;

		// Параметры ромбов (немного больше для лучшей видимости)
		int crystalWidth = 9;
		int crystalHeight = 13;
		int gap = 2;
		int crystalsCount = 10;

		// Рисуем ромбы справа налево
		for (int i = 0; i < crystalsCount; i++) {
			int x = xRight - i * (crystalWidth + gap);
			int y = yBottom;
			
			// Добавляем эффект встряски при трате маны (более мягкий)
			if (shakeIntensity > 0) {
				x += (Math.random() - 0.5) * shakeIntensity * 1.5;
				y += (Math.random() - 0.5) * shakeIntensity * 1.5;
			}
			
			// Вычисляем заполненность этого ромба
			float fillAmount = calculateCrystalFill(i, mana, max, crystalsCount);
			
			// Рисуем ромб
			drawManaCrystal(drawContext, x, y, crystalWidth, crystalHeight, fillAmount, i, tickCounter);
		}
		
		// Рисуем текст с количеством маны слева от ромбов (опционально)
		drawManaText(drawContext, xRight - crystalsCount * (crystalWidth + gap) - 35, yBottom + 2, mana, max);
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
		
		if (fillAmount > 0) {
			// Вычисляем высоту заполнения
			int fillHeight = (int) (height * fillAmount);
			int fillTopY = bottomY - fillHeight;
			
			// Красивые цвета маны: яркий голубой с более насыщенными оттенками
			int topColor = 0xFF00FFFF;      // Яркий циан
			int midColor = 0xFF00CCFF;      // Голубой
			int bottomColor = 0xFF0099FF;   // Темный голубой
			
			// Интерполяция цветов в зависимости от заполнения
			int displayTopColor = interpolateColor(topColor, bottomColor, fillAmount * 0.5f);
			int displayBottomColor = interpolateColor(midColor, bottomColor, fillAmount * 0.7f);
			
			// Добавляем эффект пульсации при регенерации
			if (pulseAnimation > 0) {
				float pulse = (float) Math.sin(time * 8) * 0.5f + 0.5f;
				float glowIntensity = pulseAnimation * pulse * 0.4f;
				displayTopColor = brightenColor(displayTopColor, glowIntensity);
				displayBottomColor = brightenColor(displayBottomColor, glowIntensity);
			}
			
			// Рисуем красивую заполненную часть кристалла с градиентом
			drawEnhancedGradientCrystal(context, centerX, fillTopY, leftX, rightX, middleY, bottomY, 
			                            displayTopColor, displayBottomColor, fillAmount);
			
			// Добавляем свечение на полных кристаллах
			if (fillAmount >= 0.9f) {
				float glowPulse = (float) Math.sin(time * 2.5f) * 0.5f + 0.5f;
				int glowIntensity = (int) (glowPulse * 80 + 40);
				int glowColor = addAlpha(0x00FFFF, glowIntensity);
				drawEnhancedGlow(context, x - 2, y - 2, width + 4, height + 4, glowColor);
			}
			
			// Добавляем блики на кристалле для эффекта 3D
			if (fillAmount > 0.15f) {
				drawCrystalHighlight(context, centerX - 1, topY + 1, width / 2, height / 3, 
				                    addAlpha(0xFFFFFF, (int) (fillAmount * 100)));
			}
		} else {
			// Пустой кристалл - темный с тонким контуром
			drawEmptyCrystal(context, centerX, topY, leftX, rightX, middleY, bottomY);
		}
		
		// Рисуем контур кристалла поверх всего
		drawCrystalOutlineEnhanced(context, centerX, topY, leftX, rightX, middleY, bottomY, fillAmount);
	}
	
	/**
	 * Рисует улучшенный градиентный кристалл с лучшей формой
	 */
	private static void drawEnhancedGradientCrystal(DrawContext context, int centerX, int topY, 
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
	 * Рисует пустой кристалл
	 */
	private static void drawEmptyCrystal(DrawContext context, int centerX, int topY, 
	                                     int leftX, int rightX, int middleY, int bottomY) {
		// Рисуем темный пустой кристалл с едва заметным свечением
		int height = bottomY - topY;
		for (int dy = 0; dy < height; dy++) {
			int currentY = topY + dy;
			float progress = dy / (float) height;
			int color = interpolateColor(0xFF1A2A3A, 0xFF0F1820, progress);
			
			// Вычисляем ширину на текущей высоте (форма ромба)
			int currentWidth;
			if (currentY < middleY) {
				float upperProgress = (currentY - topY) / (float) (middleY - topY);
				currentWidth = (int) ((rightX - leftX) * upperProgress);
			} else {
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
	 * Рисует улучшенное свечение вокруг кристалла
	 */
	private static void drawEnhancedGlow(DrawContext context, int x, int y, int width, int height, int color) {
		// Мягкое свечение (несколько слоев для эффекта размытия)
		int alpha1 = (color >> 24) & 0xFF;
		int rgb = color & 0x00FFFFFF;
		
		// Внешний слой свечения (самый прозрачный)
		context.fill(x - 1, y - 1, x + width + 1, y, addAlpha(rgb, alpha1 / 3));
		context.fill(x - 1, y + height, x + width + 1, y + height + 1, addAlpha(rgb, alpha1 / 3));
		context.fill(x - 1, y, x, y + height, addAlpha(rgb, alpha1 / 3));
		context.fill(x + width, y, x + width + 1, y + height, addAlpha(rgb, alpha1 / 3));
		
		// Внутренний слой свечения (более видный)
		context.fill(x, y, x + width, y + 1, color);
		context.fill(x, y + height - 1, x + width, y + height, color);
		context.fill(x, y, x + 1, y + height, color);
		context.fill(x + width - 1, y, x + width, y + height, color);
	}
	
	/**
	 * Рисует улучшенную обводку кристалла с градиентом интенсивности
	 */
	private static void drawCrystalOutlineEnhanced(DrawContext context, int centerX, int topY, 
	                                        int leftX, int rightX, int middleY, int bottomY, float fillAmount) {
		// Цвет контура зависит от заполнения
		int outlineColor;
		if (fillAmount >= 0.7f) {
			outlineColor = 0xFF00FFCC; // Яркий голубой
		} else if (fillAmount >= 0.3f) {
			outlineColor = 0xFF0088FF; // Средний голубой
		} else {
			outlineColor = 0xFF003388; // Темный голубой
		}
		
		// Верхняя часть ромба
		drawLineThick(context, centerX, topY, leftX, middleY, outlineColor, 1);
		drawLineThick(context, centerX, topY, rightX, middleY, outlineColor, 1);
		// Нижняя часть ромба
		drawLineThick(context, leftX, middleY, centerX, bottomY, outlineColor, 1);
		drawLineThick(context, rightX, middleY, centerX, bottomY, outlineColor, 1);
	}
	
	/**
	 * Рисует утолщенную линию
	 */
	private static void drawLineThick(DrawContext context, int x1, int y1, int x2, int y2, int color, int thickness) {
		// Используем стандартный метод рисования линии
		drawLine(context, x1, y1, x2, y2, color);
	}

	
	
	/**
	 * Рисует текст с количеством маны
	 */
	private static void drawManaText(DrawContext context, int x, int y, int mana, int maxMana) {
		String text = mana + "/" + maxMana;
		int textColor = 0xFF00FFFF; // Яркий голубой
		
		// Добавляем тень для лучшей читаемости
		context.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, textColor, true);
	}
	
	/**
	 * Рисует блик на кристалле для 3D эффекта
	 */
	private static void drawCrystalHighlight(DrawContext context, int x, int y, int width, int height, int color) {
		context.fill(x, y, x + width, y + height, color);
	}
	
	/**
	 * Рисует линию между двумя точками (Bresenham's line algorithm)
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


