package com.manaborn.client;

import com.manaborn.client.ui.RadialSpellMenuScreen;
import com.manaborn.items.MagicStaffItem;
import com.manaborn.network.SelectSpellC2SPacket;
import com.manaborn.spells.SpellType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/**
 * Обработчик радиального меню выбора заклинаний
 * 
 * Управляет открытием меню и отправкой выбранного заклинания на сервер
 */
public class RadialMenuHandler {

    /**
     * Открывает радиальное меню выбора заклинаний
     * Вызывается при Shift + ПКМ с посохом в руке
     */
    public static void openRadialMenu() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        // Проверяем, что в руке посох
        ItemStack mainHandStack = client.player.getMainHandStack();
        ItemStack offHandStack = client.player.getOffHandStack();

        boolean hasStaff = mainHandStack.getItem() instanceof MagicStaffItem ||
                offHandStack.getItem() instanceof MagicStaffItem;

        if (!hasStaff) {
            return;
        }

        // Открываем радиальное меню
        client.setScreen(new RadialSpellMenuScreen(spell -> {
            // Callback при выборе заклинания
            onSpellSelected(spell);
        }));
    }

    /**
     * Обработка выбора заклинания в радиальном меню
     * Отправляет пакет на сервер для установки выбранного заклинания
     */
    private static void onSpellSelected(SpellType spell) {
        if (spell == null) {
            return;
        }

        // Отправляем пакет на сервер
        ClientPlayNetworking.send(new SelectSpellC2SPacket(spell.getId()));
    }
}
