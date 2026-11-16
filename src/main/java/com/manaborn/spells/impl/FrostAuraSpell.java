package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.ActiveEffectManager;
import com.manaborn.spells.SpellLevelManager;

/**
 * Frost Aura spell implementation with level-based scaling
 * 
 * Level 1 (слабый): 10с, кд 40с, мана 30
 * Level 2 (средний): 15с, кд 45с, мана 40
 * Level 3 (ОРИГИНАЛ): 20с, кд 50с, мана 50
 */
public class FrostAuraSpell implements LeveledSpell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }

        int level = SpellLevelManager.getSpellLevel(user.getUuid(), SpellType.FROST_AURA);
        
        // Level-based duration
        int duration = switch (level) {
            case 1 -> 200;  // 10 seconds
            case 2 -> 300;  // 15 seconds
            default -> 400; // 20 seconds (original)
        };
        
        ActiveEffectManager.addFrostAura(serverPlayer.getUuid(), duration);
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_SNOW_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            spawnFrostParticles(serverWorld, user.getX(), user.getY() + 1.0, user.getZ(), 10);
        }

        return true;
    }

    private void spawnFrostParticles(net.minecraft.server.world.ServerWorld world, double x, double y, double z,
            int count) {
        for (int i = 0; i < count; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 1.5 + world.random.nextDouble() * 1.5;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double py = y + world.random.nextDouble() * 1.0;
            world.spawnParticles(net.minecraft.particle.ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0.0, 0.01, 0.0, 0.0);
        }
    }

    @Override
    public int getManaCost() {
        return 30; // Level 1 cost
    }
    
    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 30;
            case 2 -> 40;
            default -> 50; // level 3 (original)
        };
    }

    @Override
    public int getCooldown() {
        return 800; // Level 1: 40 seconds
    }
    
    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 800;  // 40 seconds
            case 2 -> 900;  // 45 seconds
            default -> 1000; // 50 seconds (level 3, original)
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.FROST_AURA;
    }

    @Override
    public boolean hasActiveEffects() {
        return true;
    }
}
