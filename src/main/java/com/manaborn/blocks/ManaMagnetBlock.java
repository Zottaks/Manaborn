package com.manaborn.blocks;

import com.manaborn.blocks.entity.ManaMagnetBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ManaMagnetBlock extends BlockWithEntity {
    
    public static final MapCodec<ManaMagnetBlock> CODEC = createCodec(ManaMagnetBlock::new);

    public ManaMagnetBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ManaMagnetBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, com.manaborn.ManabornMod.MANA_MAGNET_ENTITY,
                (world1, pos, state1, blockEntity) -> ManaMagnetBlockEntity.tick(world1, pos, state1, blockEntity));
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ManaMagnetBlockEntity manaMagnet) {
                // Собрать накопленную пыль
                if (manaMagnet.collectDust(player)) {
                    return ActionResult.SUCCESS;
                } else {
                    // Нет пыли - тихий звук
                    world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
                            net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 1.1f);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    // onStateReplaced omitted - items will drop via standard mechanism
}
