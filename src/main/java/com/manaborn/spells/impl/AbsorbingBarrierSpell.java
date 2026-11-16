package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.ActiveEffectManager;
import com.manaborn.spells.SpellLevelManager;

/**
 * Absorbing Barrier spell implementation with level-based scaling
 * 
 * Level 1 (слабый): 5с, кд 55с, мана 50, БЕЗ восстановления маны
 * Level 2 (средний): 7.5с, кд 65с, мана 55, восстановление 50% урона
 * Level 3 (ОРИГИНАЛ): 10с, кд 75с, мана 60, восстановление 100% урона
 * 
 * ВАЖНО: Восстановление маны обрабатывается в ActiveEffectManager
 */
public class AbsorbingBarrierSpell implements LeveledSpell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }

        int level = SpellLevelManager.getSpellLevel(user.getUuid(), SpellType.ABSORBING_BARRIER);
        
        // Level-based duration
        int duration = switch (level) {
            case 1 -> 100;  // 5 seconds
            case 2 -> 150;  // 7.5 seconds
            default -> 200; // 10 seconds (original)
        };
        
        ActiveEffectManager.addAbsorbingBarrier(serverPlayer.getUuid(), duration);
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f,
                0.8f);
        serverPlayer.sendMessage(Text.literal("Поглощающий барьер активен (10с)"), true);

        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            for (int i = 0; i < 20; i++) {
                double angle = serverWorld.random.nextDouble() * Math.PI * 2;
                double radius = 1.5 + serverWorld.random.nextDouble();
                double px = user.getX() + Math.cos(angle) * radius;
                double pz = user.getZ() + Math.sin(angle) * radius;
                double py = user.getY() + 0.5 + serverWorld.random.nextDouble();
                serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 1, 0, 0, 0, 0);
            }
        }

        return true;
    }

    @Override
    public int getManaCost() {
        return 50; // Level 1 cost
    }
    
    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 50;
            case 2 -> 55;
            default -> 60; // level 3 (original)
        };
    }

    @Override
    public int getCooldown() {
        return 1100; // Level 1: 55 seconds
    }
    
    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 1100; // 55 seconds
            case 2 -> 1300; // 65 seconds
            default -> 1500; // 75 seconds (level 3, original)
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.ABSORBING_BARRIER;
    }

    @Override
    public boolean hasActiveEffects() {
        return true;
    }
}
