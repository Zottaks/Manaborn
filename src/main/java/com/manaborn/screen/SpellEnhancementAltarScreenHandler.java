package com.manaborn.screen;

import com.manaborn.ManabornMod;
import com.manaborn.blocks.entity.SpellEnhancementAltarBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Screen Handler для Алтаря Улучшения Заклинаний
 * Содержит 1 слот для ресурсов улучшения
 */
public class SpellEnhancementAltarScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    // Client-side constructor
    public SpellEnhancementAltarScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(1)); // 1 слот для ресурсов
    }

    // Server-side constructor
    public SpellEnhancementAltarScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ManabornMod.SPELL_ENHANCEMENT_ALTAR_SH, syncId);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        // ОДИН слот для ресурсов - справа вверху (где был квадрат уровня)
        int slotX = 140;  // Правый верхний угол
        int slotY = 18;   // Верхняя часть
        
        this.addSlot(new Slot(inventory, 0, slotX, slotY));

        // Инвентарь игрока (начинается с Y=84)
        int playerInvY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                    8 + col * 18, playerInvY + row * 18));
            }
        }

        // Хотбар
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInvY + 58));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot clickedSlot = this.slots.get(slot);
        
        if (clickedSlot != null && clickedSlot.hasStack()) {
            ItemStack originalStack = clickedSlot.getStack();
            newStack = originalStack.copy();
            
            if (slot < 1) {
                // From altar to player inventory
                if (!this.insertItem(originalStack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory to altar
                if (!this.insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (originalStack.isEmpty()) {
                clickedSlot.setStack(ItemStack.EMPTY);
            } else {
                clickedSlot.markDirty();
            }
        }
        
        return newStack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Получить BlockEntity (если доступен)
     */
    public SpellEnhancementAltarBlockEntity getBlockEntity() {
        if (inventory instanceof SpellEnhancementAltarBlockEntity be) {
            return be;
        }
        return null;
    }
}
