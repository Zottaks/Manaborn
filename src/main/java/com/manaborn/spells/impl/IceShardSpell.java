package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import com.manaborn.entities.IceShardEntity;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;

/**
 * Ice Shard spell implementation
 */
public class IceShardSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return true;
        }

        IceShardEntity iceShard = new IceShardEntity(world, user);
        iceShard.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 2.0f, 1.0f);
        world.spawnEntity(iceShard);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.PLAYERS, 1.0f,
                1.2f);

        return true;
    }

    @Override
    public int getManaCost() {
        return 25;
    }

    @Override
    public int getCooldown() {
        return 140; // 7 seconds
    }

    @Override
    public SpellType getType() {
        return SpellType.ICE_SHARD;
    }
}
