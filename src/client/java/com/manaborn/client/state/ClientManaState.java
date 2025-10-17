package com.manaborn.client.state;

public final class ClientManaState {
	private static int mana = 100;
	private static int maxMana = 100;

	private ClientManaState() {}

	public static int getMana() {
		return mana;
	}

	public static int getMaxMana() {
		return maxMana;
	}

	public static void set(int current, int max) {
		mana = Math.max(0, Math.min(max, current));
		maxMana = Math.max(0, max);
	}
}


