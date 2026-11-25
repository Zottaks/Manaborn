package com.manaborn.screen;

import com.manaborn.ManabornMod;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.village.Merchant;
import org.jetbrains.annotations.Nullable;

/**
 * Кастомный обработчик экрана торговли мага.
 * Наследует MerchantScreenHandler, но использует наш ScreenHandlerType,
 * чтобы клиент мог привязать свой экран с текстурой mage_merchant.png.
 */
public class MageMerchantScreenHandler extends MerchantScreenHandler {

    public MageMerchantScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(syncId, playerInventory);
    }

    public MageMerchantScreenHandler(int syncId, PlayerInventory playerInventory, Merchant merchant) {
        super(syncId, playerInventory, merchant);
    }

    @Override
    public @Nullable ScreenHandlerType<?> getType() {
        return ManabornMod.MAGE_MERCHANT_SH;
    }
}
