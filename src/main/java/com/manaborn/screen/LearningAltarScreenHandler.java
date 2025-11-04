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

        // Slots: 0 - wand, 1 - book
        this.addSlot(new Slot(inventory, 0, 44, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() == ManabornMod.MANA_WAND;
            }
        });
        this.addSlot(new Slot(inventory, 1, 116, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() == ManabornMod.FIREBALL_SPELL_BOOK
                        || stack.getItem() == ManabornMod.ICE_SHARD_SPELL_BOOK
                        || stack.getItem() == ManabornMod.FIRE_WALL_SPELL_BOOK
                        || stack.getItem() == ManabornMod.INNER_HEAT_SPELL_BOOK
                        || stack.getItem() == ManabornMod.FROST_AURA_SPELL_BOOK
                        || stack.getItem() == ManabornMod.ABSORBING_BARRIER_SPELL_BOOK
                        || stack.getItem() == ManabornMod.ICE_WALL_SPELL_BOOK;
            }
        });

        // Player inventory (27) and hotbar (9)
        int m;
        int l;
        for (m = 0; m < 3; ++m) {
            for (l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18));
            }
        }
        for (m = 0; m < 9; ++m) {
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
