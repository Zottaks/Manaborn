package com.manaborn.client;

import com.manaborn.client.config.SpellMenuMode;
import com.manaborn.client.ui.RadialSpellMenuScreen;
import com.manaborn.items.MagicStaffItem;
import com.manaborn.network.SelectSpellC2SPacket;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

/**
 * Обработчик радиального меню выбора заклинаний
 * 
 * Управляет открытием меню и отправкой выбранного заклинания на сервер
 */
public class RadialMenuHandler {

    /**
     * true, если меню открыто через Shift+ПКМ в режиме удержания (Hold) —
     * тогда меню закроется с выбором при отпускании Shift.
     * Отслеживается в MagicStaffClientHandler.
     */
    public static boolean holdMenuOpen = false;

    /**
     * Регистрирует перехват Shift + ПКМ с посохом в руке для открытия меню.
     * Возврат CONSUME на клиенте отменяет отправку пакета на сервер,
     * поэтому каст при открытии меню не происходит.
     */
    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient()) {
                return ActionResult.PASS;
            }
            if (!player.isSneaking()) {
                return ActionResult.PASS;
            }
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof MagicStaffItem)) {
                return ActionResult.PASS;
            }
            // Shift + взгляд вниз с Транслокацией — это самокаст на себя, а не меню
            SpellType selected = SpellManager.getSelectedSpell(stack);
            if (selected == SpellType.TRANSLOCATION && player.getPitch() > 60.0f) {
                return ActionResult.PASS;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            // Меню уже открыто — не переоткрываем, иначе при удержании ПКМ
            // оно будет мерцать (событие использования повторяется)
            if (client != null && client.currentScreen instanceof RadialSpellMenuScreen) {
                return ActionResult.CONSUME;
            }

            // В режиме удержания запоминаем, что меню должно закрыться при отпускании Shift
            holdMenuOpen = !SpellMenuMode.getInstance().isPressMode;

            openRadialMenu();
            return ActionResult.CONSUME;
        });
    }

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
