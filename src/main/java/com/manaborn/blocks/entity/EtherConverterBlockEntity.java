package com.manaborn.blocks.entity;

import com.manaborn.ManabornMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EtherConverterBlockEntity extends BlockEntity {
    public static final int DUST_PER_ETHER = 25;

    private int dustBuffer = 0;
    private final SimpleInventory output = new SimpleInventory(1); // slot 0: Ether

    public EtherConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ManabornMod.ETHER_CONVERTER_BE, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, EtherConverterBlockEntity be) {
        if (world.isClient()) return;
        // No periodic logic needed; conversion happens on interaction
    }

    public boolean tryInsertDust(ItemStack hand) {
        if (hand.isEmpty() || hand.getItem() != ManabornMod.MAGIC_DUST) return false;
        int space = DUST_PER_ETHER - dustBuffer;
        if (space <= 0) {
            // Already ready to convert; try producing before accepting more
            tryProduceEther();
            space = DUST_PER_ETHER - dustBuffer;
            if (space <= 0) return false;
        }
        int toConsume = Math.min(hand.getCount(), space);
        if (toConsume <= 0) return false;
        hand.decrement(toConsume);
        dustBuffer += toConsume;
        if (dustBuffer >= DUST_PER_ETHER) {
            tryProduceEther();
        } else if (world instanceof ServerWorld sw) {
            sw.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.3f, 1.4f);
        }
        markDirty();
        return true;
    }

    public boolean tryGiveEther(PlayerEntity player) {
        ItemStack out = output.getStack(0);
        if (out.isEmpty()) return false;
        ItemStack give = out.copy();
        give.setCount(1);
        boolean added = player.getInventory().insertStack(give);
        if (!added) {
            player.giveItemStack(give);
        }
        out.decrement(1);
        if (out.getCount() <= 0) output.setStack(0, ItemStack.EMPTY);
        player.getEntityWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.3f, 1.0f);
        markDirty();
        return true;
    }

    private void tryProduceEther() {
        if (!(world instanceof ServerWorld sw)) return;
        while (dustBuffer >= DUST_PER_ETHER) {
            dustBuffer -= DUST_PER_ETHER;
            ItemStack out = output.getStack(0);
            if (out.isEmpty()) {
                output.setStack(0, new ItemStack(ManabornMod.ETHER, 1));
            } else if (out.isOf(ManabornMod.ETHER) && out.getCount() < out.getMaxCount()) {
                out.increment(1);
            } else {
                // Output full; drop item into world
                ItemEntity drop = new ItemEntity(sw, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, new ItemStack(ManabornMod.ETHER, 1));
                sw.spawnEntity(drop);
            }
            sw.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.5f, 1.6f);
        }
        markDirty();
    }
}
