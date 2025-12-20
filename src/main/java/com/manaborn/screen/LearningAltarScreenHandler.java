package com.manaborn.screen;

import com.manaborn.ManabornMod;
import com.manaborn.blocks.entity.LearningAltarBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class LearningAltarScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate properties;
    public static final int BUTTON_LEARN = 1;

    // Client-side constructor
    public LearningAltarScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2), new ArrayPropertyDelegate(6));
    }

    // Server-side constructor
    public LearningAltarScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate properties) {
        super(ManabornMod.LEARNING_ALTAR_SH, syncId);
        this.inventory = inventory;
        this.properties = properties;

        inventory.onOpen(playerInventory.player);
        this.addProperties(properties);

        // Slots: 0 - wand (слева), 1 - book (справа)
        // GUI размер: 220x200, слоты на позициях (30, 45) и (170, 45)
        this.addSlot(new Slot(inventory, 0, 30, 45) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() == ManabornMod.MANA_WAND;
            }
        });
        this.addSlot(new Slot(inventory, 1, 170, 45) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof com.manaborn.items.SpellBookItem;
            }
        });

        // Player inventory (27) and hotbar (9)
        // GUI размер 220, инвентарь 162px -> центрируем: (220-162)/2 = 29
        int invX = 29;
        int invY = 118;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, invX + col * 18, invY + 58));
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
                if (!this.insertItem(slotStack, 2, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (this.slots.get(0).canInsert(slotStack)) {
                    if (!this.insertItem(slotStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (this.slots.get(1).canInsert(slotStack)) {
                    if (!this.insertItem(slotStack, 1, 2, false)) return ItemStack.EMPTY;
                } else if (index < 29) {
                    if (!this.insertItem(slotStack, 29, 38, false)) return ItemStack.EMPTY;
                } else if (!this.insertItem(slotStack, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (slotStack.isEmpty()) slot.setStack(ItemStack.EMPTY);
            else slot.markDirty();
        }
        return itemStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    public int getProgress() { return properties.get(0); }
    public int getMaxProgress() { return properties.get(1); }
    public int getRequiredXp() { return properties.get(3); }
    public boolean canStart() { return properties.get(4) != 0; }
    public boolean hasEnoughXp() { return properties.get(5) != 0; }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == BUTTON_LEARN && this.inventory instanceof LearningAltarBlockEntity altar) {
            altar.setLastInteractingPlayer(player);
            altar.tryStartLearning(player);
            return true;
        }
        return super.onButtonClick(player, id);
    }
}
