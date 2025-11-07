package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;

/**
 * Fireball spell implementation
 */
public class FireballSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return true;
        }

        SmallFireballEntity fireball = new SmallFireballEntity(world, user, user.getRotationVector());
        fireball.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());
        world.spawnEntity(fireball);
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        return true;
    }

    @Override
    public int getManaCost() {
        return 20;
    }

    @Override
    public int getCooldown() {
        return 100; // 5 seconds
    }

    @Override
    public SpellType getType() {
        return SpellType.FIREBALL;
    }
}
