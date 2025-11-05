package com.manaborn.blocks.entity;

import com.manaborn.ManabornMod;
import com.manaborn.items.ManaVesselItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import com.manaborn.screen.ManaConverterScreenHandler;

public class ManaConverterBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
    public static final int MAX_MANA = 500;
    public static final int MANA_PER_DUST = 20;
    public static final int TICKS_PER_DUST = 60; // ~3s
    public static final int TRANSFER_PER_TICK = 2;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY); // 0: dust, 1: vessel
    private int manaStored = 0;
    private int progress = 0;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> manaStored;
                case 1 -> MAX_MANA;
                case 2 -> progress;
                case 3 -> TICKS_PER_DUST;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 2)
                progress = value;
        }

        @Override
        public int size() {
            return 4;
        }
    };

    public ManaConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ManabornMod.MANA_CONVERTER_BE, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, ManaConverterBlockEntity be) {
        if (world.isClient())
            return;
        boolean dirty = false;

        // Convert dust -> mana
        ItemStack dust = be.items.get(0);
        if (!dust.isEmpty() && dust.getItem() == ManabornMod.MAGIC_DUST && be.manaStored < MAX_MANA) {
            be.progress++;
            if (be.progress >= TICKS_PER_DUST) {
                be.progress = 0;
                dust.decrement(1);
                be.manaStored = Math.min(MAX_MANA, be.manaStored + MANA_PER_DUST);
                dirty = true;
                world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.4f, 1.4f);
            }
        } else {
            be.progress = 0; // pause progress when no dust or full
        }

        // Transfer to vessel
        ItemStack vessel = be.items.get(1);
        if (!vessel.isEmpty() && vessel.getItem() instanceof ManaVesselItem && be.manaStored > 0) {
            int stored = ManaVesselItem.getStored(vessel);
            int free = ManaVesselItem.CAPACITY - stored;
            if (free > 0) {
                int move = Math.min(TRANSFER_PER_TICK, Math.min(free, be.manaStored));
                if (move > 0) {
                    ManaVesselItem.setStored(vessel, stored + move);
                    be.manaStored -= move;
                    dirty = true;
                    if (world.getTime() % 20 == 0) {
                        world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 0.3f, 1.5f);
                    }
                }
            }
        }

        if (dirty)
            be.markDirty();
    }

    // Inventory
    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items)
            if (!s.isEmpty())
                return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty())
            markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        items.clear();
    }

    // NBT intentionally omitted for 1.21.10 draft, following project pattern

    // UI
    @Override
    public Text getDisplayName() {
        return Text.literal("Конвертор маны");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory inv, PlayerEntity player) {
        return new ManaConverterScreenHandler(syncId, inv, this, properties);
    }
}
