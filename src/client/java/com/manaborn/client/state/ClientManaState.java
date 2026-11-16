package com.manaborn.client.state;

public final class ClientManaState {
	private static int mana = 100;
	private static int maxMana = 100;
	@SuppressWarnings("unused")
	private static int regenTickCounter = 0;
	private static int lastSpendDelayTicks = 0;

	private static final int REGEN_DELAY_TICKS = Integer.MAX_VALUE;

	private ClientManaState() {
	}

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
		if (amount <= 0)
			return;
		mana = Math.max(0, mana - amount);
		lastSpendDelayTicks = REGEN_DELAY_TICKS;
		regenTickCounter = 0;
	}

	public static void clientTick() {
		// Клиентская регенерация отключена — мана обновляется только серверными
		// пакетами
		if (lastSpendDelayTicks > 0) {
			lastSpendDelayTicks--;
		}
	}
}
