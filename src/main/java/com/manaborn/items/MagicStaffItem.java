package com.manaborn.items;

import com.manaborn.mana.ManaManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.stat.Stats;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class MagicStaffItem extends Item {
    public MagicStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public net.minecraft.util.ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
		if (!world.isClient()) {
			ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;
			if (!ManaManager.tryConsume(serverPlayer, 20)) {
				return net.minecraft.util.ActionResult.FAIL;
			}
            SmallFireballEntity fireball = new SmallFireballEntity(world, user, user.getRotationVector());
            fireball.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());
            world.spawnEntity(fireball);
            world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        user.getItemCooldownManager().set(stack, 100);
        return net.minecraft.util.ActionResult.SUCCESS;
    }
}