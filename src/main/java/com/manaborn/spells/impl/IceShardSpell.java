package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import com.manaborn.entities.IceShardEntity;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellLevelManager;

/**
 * Ice Shard spell implementation with level-based scaling
 * 
 * Level 1 (слабый): Slowness I, 10с, кд 10с, мана 30
 * Level 2 (средний): Slowness II, 7.5с, кд 8.5с, мана 27
 * Level 3 (ОРИГИНАЛ): Slowness III, 5с, кд 7с, мана 25
 */
public class IceShardSpell implements LeveledSpell {

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
        return 30; // Level 1 cost
    }
    
    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 30;
            case 2 -> 27;
            default -> 25; // level 3 (original)
        };
    }

    @Override
    public int getCooldown() {
        return 200; // Level 1: 10 seconds
    }
    
    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 200; // 10 seconds
            case 2 -> 170; // 8.5 seconds
            default -> 140; // 7 seconds (level 3, original)
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.ICE_SHARD;
    }
}
