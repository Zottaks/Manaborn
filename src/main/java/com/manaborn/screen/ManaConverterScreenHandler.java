package com.manaborn.screen;

import com.manaborn.ManabornMod;
import com.manaborn.items.ManaVesselItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ManaConverterScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate properties;

    // Client-side
    public ManaConverterScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2), new ArrayPropertyDelegate(4));
    }

    // Server-side
    public ManaConverterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
            PropertyDelegate properties) {
        super(ManabornMod.MANA_CONVERTER_SH, syncId);
        this.inventory = inventory;
        this.properties = properties;

        inventory.onOpen(playerInventory.player);
        this.addProperties(properties);

        // Slots: 0 - magic dust, 1 - mana vessel
        this.addSlot(new Slot(inventory, 0, 31, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() == ManabornMod.MAGIC_DUST;
            }
        });
        this.addSlot(new Slot(inventory, 1, 127, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof ManaVesselItem;
            }
        });

        // Player inventory (27) and hotbar (9)
        for (int m = 0; m < 3; ++m) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        for (int m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 142));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            itemStack = slotStack.copy();
            if (index < 2) {
                if (!this.insertItem(slotStack, 2, this.slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (this.slots.get(0).canInsert(slotStack)) {
                    if (!this.insertItem(slotStack, 0, 1, false))
                        return ItemStack.EMPTY;
                } else if (this.slots.get(1).canInsert(slotStack)) {
                    if (!this.insertItem(slotStack, 1, 2, false))
                        return ItemStack.EMPTY;
                } else if (index < 29) {
                    if (!this.insertItem(slotStack, 29, 38, false))
                        return ItemStack.EMPTY;
                } else if (!this.insertItem(slotStack, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.isEmpty())
                slot.setStack(ItemStack.EMPTY);
            else
                slot.markDirty();
        }
        return itemStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    public int getMana() {
        return properties.get(0);
    }

    public int getMaxMana() {
        return properties.get(1);
    }

    public int getProgress() {
        return properties.get(2);
    }

    public int getMaxProgress() {
        return properties.get(3);
    }
}
