package com.manaborn.blocks.entity;

import com.manaborn.ManabornMod;
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

public class EtherConverterBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
    public static final int DUST_PER_ETHER = 25;
    public static final int TICKS_PER_CONVERSION = 100; // 5 seconds

    // Slot 0: Magic Dust input, Slot 1: Ether output
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int progress = 0;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> TICKS_PER_CONVERSION;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }

        @Override
        public int size() {
            return 2;
        }
    };

    public EtherConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ManabornMod.ETHER_CONVERTER_BE, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, EtherConverterBlockEntity be) {
        if (world.isClient()) return;

        boolean dirty = false;

        // Convert 25 dust from slot 0 into 1 ether in slot 1 with progress
        ItemStack dust = be.items.get(0);
        if (!dust.isEmpty() && dust.getItem() == ManabornMod.MAGIC_DUST && dust.getCount() >= DUST_PER_ETHER) {
            ItemStack out = be.items.get(1);
            boolean canOutput = out.isEmpty() || (out.isOf(ManabornMod.ETHER) && out.getCount() < out.getMaxCount());
            if (canOutput) {
                be.progress++;
                dirty = true;
                
                if (be.progress >= TICKS_PER_CONVERSION) {
                    be.progress = 0;
                    dust.decrement(DUST_PER_ETHER);
                    if (out.isEmpty()) {
                        be.items.set(1, new ItemStack(ManabornMod.ETHER, 1));
                    } else {
                        out.increment(1);
                    }
                    world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.5f, 1.6f);
                } else if (be.progress % 20 == 0) {
                    // Sound every second during conversion
                    world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.2f, 1.2f);
                }
            } else {
                be.progress = 0; // Reset if output is full
            }
        } else {
            be.progress = 0; // Reset if not enough dust
        }

        if (dirty) be.markDirty();
    }

    // Inventory implementation
    @Override
    public int size() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
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
    public boolean canPlayerUse(PlayerEntity player) { return true; }

    @Override
    public void clear() { items.clear(); }

    // UI
    @Override
    public Text getDisplayName() {
        return Text.literal("Преобразователь");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory inv, PlayerEntity player) {
        return new com.manaborn.screen.EtherConverterScreenHandler(syncId, inv, this, properties);
    }
}
