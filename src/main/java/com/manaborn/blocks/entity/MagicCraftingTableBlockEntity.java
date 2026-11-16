package com.manaborn.blocks.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/**
 * BlockEntity для магического верстака
 * Простая реализация без дополнительных данных
 */
public class MagicCraftingTableBlockEntity extends BlockEntity {
    
    public MagicCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(com.manaborn.ManabornMod.MAGIC_CRAFTING_TABLE_BE, pos, state);
    }
}
