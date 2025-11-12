package com.manaborn.screen;

import com.manaborn.ManabornMod;
import com.manaborn.mana.ManaManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * ScreenHandler для магического верстака
 * Работает как обычный крафтинг, но требует 5 маны
 */
public class MagicCraftingTableScreenHandler extends ScreenHandler {
    public static final int MANA_COST = 5;
    
    private final CraftingInventory craftingInventory;
    private final CraftingResultInventory resultInventory;
    private final ScreenHandlerContext context;
    private final PlayerEntity player;

    public MagicCraftingTableScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public MagicCraftingTableScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(ManabornMod.MAGIC_CRAFTING_TABLE_SH, syncId);
        this.craftingInventory = new CraftingInventory(this, 3, 3);
        this.resultInventory = new CraftingResultInventory();
        this.context = context;
        this.player = playerInventory.player;

        // Result slot (0) - с проверкой маны
        this.addSlot(new CraftingResultSlot(playerInventory.player, this.craftingInventory, this.resultInventory, 0, 124, 35) {
            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                // Проверяем и тратим ману на сервере
                if (!player.getEntityWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                    if (!ManaManager.tryConsume(serverPlayer, MANA_COST)) {
                        // Если недостаточно маны - отменяем крафт
                        serverPlayer.sendMessage(
                            net.minecraft.text.Text.literal("Недостаточно маны для крафта! Требуется: " + MANA_COST),
                            true
                        );
                        // Возвращаем предмет
                        stack.setCount(0);
                        return;
                    }
                }
                
                super.onTakeItem(player, stack);
                this.onCrafted(stack);
            }
        });

        // Crafting grid slots (1-9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(this.craftingInventory, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }

        // Player inventory (10-36)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar (37-45)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        this.context.run((world, pos) -> {
            if (!world.isClient()) {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) this.player;
                ItemStack result = ItemStack.EMPTY;

                // Жестко заданные рецепты для магического верстака
                // Шаблоны (индексы: r*3+c):
                // mana_wand:      [2:'A', 4:'S', 6:'S'] с остальными пустыми
                // ether_scepter:  [2:'E', 4:'S', 6:'S'] с остальными пустыми

                ItemStack s0 = this.craftingInventory.getStack(0);
                ItemStack s1 = this.craftingInventory.getStack(1);
                ItemStack s2 = this.craftingInventory.getStack(2);
                ItemStack s3 = this.craftingInventory.getStack(3);
                ItemStack s4 = this.craftingInventory.getStack(4);
                ItemStack s5 = this.craftingInventory.getStack(5);
                ItemStack s6 = this.craftingInventory.getStack(6);
                ItemStack s7 = this.craftingInventory.getStack(7);
                ItemStack s8 = this.craftingInventory.getStack(8);

                boolean othersEmpty = s0.isEmpty() && s1.isEmpty() && s3.isEmpty() && s5.isEmpty() && s7.isEmpty() && s8.isEmpty();

                boolean isStickCenter = !s4.isEmpty() && s4.getItem() == net.minecraft.item.Items.STICK;
                boolean isStickDownLeft = !s6.isEmpty() && s6.getItem() == net.minecraft.item.Items.STICK;

                // manaborn items
                var dustItem = ManabornMod.MAGIC_DUST;
                var etherItem = ManabornMod.ETHER;
                var wandItem = ManabornMod.MANA_WAND;
                var scepterItem = ManabornMod.ETHER_SCEPTER;

                if (othersEmpty && isStickCenter && isStickDownLeft) {
                    // Check top-right slot for catalyst
                    if (!s2.isEmpty() && s2.getItem() == dustItem) {
                        result = new ItemStack(wandItem);
                    } else if (!s2.isEmpty() && s2.getItem() == etherItem) {
                        result = new ItemStack(scepterItem);
                    }
                }

                this.resultInventory.setStack(0, result);
                serverPlayer.networkHandler.sendPacket(
                        new ScreenHandlerSlotUpdateS2CPacket(this.syncId, 0, 0, result));
            }
        });
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.context.run((world, pos) -> {
            this.dropInventory(player, this.craftingInventory);
        });
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(this.context, player, ManabornMod.MAGIC_CRAFTING_TABLE);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);
        
        if (slotObj != null && slotObj.hasStack()) {
            ItemStack originalStack = slotObj.getStack();
            newStack = originalStack.copy();
            
            if (slot == 0) {
                // Result slot
                this.context.run((world, pos) -> {
                    originalStack.getItem().onCraftByPlayer(originalStack, player);
                });
                
                if (!this.insertItem(originalStack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
                
                slotObj.onQuickTransfer(originalStack, newStack);
            } else if (slot >= 10 && slot < 46) {
                // Player inventory to crafting grid
                if (!this.insertItem(originalStack, 1, 10, false)) {
                    if (slot < 37) {
                        if (!this.insertItem(originalStack, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.insertItem(originalStack, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.insertItem(originalStack, 10, 46, false)) {
                return ItemStack.EMPTY;
            }
            
            if (originalStack.isEmpty()) {
                slotObj.setStack(ItemStack.EMPTY);
            } else {
                slotObj.markDirty();
            }
            
            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slotObj.onTakeItem(player, originalStack);
            if (slot == 0) {
                player.dropItem(originalStack, false);
            }
        }
        
        return newStack;
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return slot.inventory != this.resultInventory && super.canInsertIntoSlot(stack, slot);
    }
}
