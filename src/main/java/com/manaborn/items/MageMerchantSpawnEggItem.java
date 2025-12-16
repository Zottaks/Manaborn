package com.manaborn.items;

import com.manaborn.ManabornMod;
import com.manaborn.entities.MageMerchantEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Кастомное яйцо призыва для маг-торговца.
 * Обходит проблемы с SpawnEggItem в 1.21+
 */
public class MageMerchantSpawnEggItem extends Item {

    public MageMerchantSpawnEggItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        ItemStack stack = context.getStack();
        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide();
        
        // Позиция спавна - на блоке, на который кликнули, или рядом
        BlockPos spawnPos = pos.offset(side);
        
        // Создаём и спавним маг-торговца
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Используем метод spawn который автоматически размещает сущность
        MageMerchantEntity merchant = ManabornMod.MAGE_MERCHANT_ENTITY.spawn(
                serverWorld,
                spawnPos,
                SpawnReason.SPAWN_ITEM_USE);
        
        if (merchant != null) {
            merchant.setPersistent();
            
            // Уменьшаем стак (если не в креативе)
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().creativeMode) {
                stack.decrement(1);
            }
            
            return ActionResult.SUCCESS_SERVER;
        }
        
        return ActionResult.FAIL;
    }
}
