package com.manaborn.blocks.entity;

import com.manaborn.ManabornMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ManaMagnetBlockEntity extends BlockEntity {
    
    private static final int PRODUCTION_INTERVAL = 1200; // 60 секунд (1200 тиков)
    private static final int MAX_STORAGE = 64; // Максимум хранимой пыли
    
    private int timer = 0;
    private final SimpleInventory inventory = new SimpleInventory(1);

    public ManaMagnetBlockEntity(BlockPos pos, BlockState state) {
        super(ManabornMod.MANA_MAGNET_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ManaMagnetBlockEntity blockEntity) {
        if (world.isClient()) return;
        
        blockEntity.timer++;
        
        // Каждые 60 секунд производим пыль
        if (blockEntity.timer >= PRODUCTION_INTERVAL) {
            blockEntity.timer = 0;
            blockEntity.produceDust((ServerWorld) world, pos);
        }
        
        blockEntity.markDirty();
    }

    private void produceDust(ServerWorld world, BlockPos pos) {
        ItemStack currentStack = inventory.getStack(0);
        
        // Проверяем, есть ли место для новой пыли
        if (currentStack.isEmpty()) {
            inventory.setStack(0, new ItemStack(ManabornMod.MAGIC_DUST, 1));
        } else if (currentStack.isOf(ManabornMod.MAGIC_DUST) && currentStack.getCount() < MAX_STORAGE) {
            currentStack.increment(1);
        } else {
            // Хранилище полно - не производим
            return;
        }
        
        // Звуковой эффект производства
        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 
                SoundCategory.BLOCKS, 0.5f, 1.2f);
        
        markDirty();
    }

    public boolean collectDust(PlayerEntity player) {
        ItemStack dustStack = inventory.getStack(0);
        if (!dustStack.isEmpty()) {
            // Отдаем всю накопленную пыль игроку
            player.giveItemStack(dustStack.copy());
            inventory.setStack(0, ItemStack.EMPTY);
            
            // Звук сбора
            player.getEntityWorld().playSound(null, player.getBlockPos(), 
                    SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 
                    0.3f, 1.0f);
            
            markDirty();
            return true;
        }
        return false;
    }

    public SimpleInventory getInventory() {
        return inventory;
    }

    // NBT intentionally omitted for 1.21.10 draft, following project pattern
    // Timer and inventory will reset on world reload - acceptable for now
}
