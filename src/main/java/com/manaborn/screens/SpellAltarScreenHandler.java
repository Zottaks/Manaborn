package com.manaborn.screens;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class SpellAltarScreenHandler extends ScreenHandler {
    public SpellAltarScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.SPELL_ALTAR, syncId);
        // Player inventory (3 rows x 9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        // No special shift-click logic yet
        Slot slot = this.getSlot(index);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (index < 36) { // player inventory to other (no other slots yet)
            if (!this.insertItem(stack, 36, this.slots.size(), false)) return ItemStack.EMPTY;
        } else {
            if (!this.insertItem(stack, 0, 36, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        return copy;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
