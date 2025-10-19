package com.manaborn.client.state;

public final class ClientManaState {
	private static int mana = 100;
	private static int maxMana = 100;
	private static int regenTickCounter = 0;
	private static int lastSpendDelayTicks = 0;

	// 20 маны за 5 секунд -> 1 мана каждые 5 тиков (20 TPS)
	private static final int TICKS_PER_1_MANA = 5;
	private static final int REGEN_DELAY_TICKS = 40; // 2 секунды задержка после траты

	private ClientManaState() {}

	public static int getMana() {
		return mana;
	}

	public static int getMaxMana() {
		return maxMana;
	}

	public static void set(int current, int max) {
		int oldMana = mana;
		mana = Math.max(0, Math.min(max, current));
		maxMana = Math.max(0, max);
		
		// Если мана уменьшилась (потратили на сервере), сбрасываем задержку регенерации
		if (mana < oldMana) {
			lastSpendDelayTicks = REGEN_DELAY_TICKS;
			regenTickCounter = 0;
		}
	}

	public static void spend(int amount) {
		if (amount <= 0) return;
		mana = Math.max(0, mana - amount);
		lastSpendDelayTicks = REGEN_DELAY_TICKS; // сброс задержки регена
		regenTickCounter = 0;
	}

	public static void clientTick() {
		if (maxMana <= 0) return;
		if (mana >= maxMana) {
			regenTickCounter = 0;
			return;
		}
		if (lastSpendDelayTicks > 0) {
			lastSpendDelayTicks--;
			return;
		}
		regenTickCounter++;
		if (regenTickCounter >= TICKS_PER_1_MANA) {
			regenTickCounter -= TICKS_PER_1_MANA;
			mana = Math.min(maxMana, mana + 1);
		}
	}
}


