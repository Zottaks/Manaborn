package com.manaborn.spells.impl;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.SpellLevelManager;

/**
 * Inner Heat spell implementation with level-based scaling
 * 
 * Level 1 (слабый): Speed I, 5с, кд 35с, мана 25
 * Level 2 (средний): Speed I, 7.5с, кд 32.5с, мана 27
 * Level 3 (ОРИГИНАЛ): Speed II, 10с, кд 30с, мана 30
 */
public class InnerHeatSpell implements LeveledSpell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            return true;
        }

        int level = SpellLevelManager.getSpellLevel(user.getUuid(), SpellType.INNER_HEAT);
        
        // Level-based duration and amplifier
        int duration = switch (level) {
            case 1 -> 100;  // 5 seconds
            case 2 -> 150;  // 7.5 seconds
            default -> 200; // 10 seconds (original)
        };
        
        int amplifier = switch (level) {
            case 1 -> 0; // Speed I
            case 2 -> 0; // Speed I
            default -> 1; // Speed II (original)
        };
        
        serverPlayer
                .addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, amplifier, false, true, true));

        // Visual effects: fire particles around player
        if (world instanceof ServerWorld serverWorld) {
            double baseX = user.getX();
            double baseY = user.getY();
            double baseZ = user.getZ();
            for (int i = 0; i < 30; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
                double offsetY = world.random.nextDouble() * 1.8;
                double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;
                serverWorld.spawnParticles(ParticleTypes.FLAME,
                        baseX + offsetX,
                        baseY + offsetY,
                        baseZ + offsetZ,
                        1,
                        0.0, 0.01, 0.0,
                        0.01);
            }
        }

        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);

        return true;
    }

    @Override
    public int getManaCost() {
        return 25; // Level 1 cost
    }
    
    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 25;
            case 2 -> 27;
            default -> 30; // level 3 (original)
        };
    }

    @Override
    public int getCooldown() {
        return 700; // Level 1: 35 seconds
    }
    
    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 700;  // 35 seconds
            case 2 -> 650;  // 32.5 seconds
            default -> 600; // 30 seconds (level 3, original)
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.INNER_HEAT;
    }
}
