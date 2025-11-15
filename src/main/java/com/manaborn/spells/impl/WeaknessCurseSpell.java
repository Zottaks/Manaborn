package com.manaborn.spells.impl;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.List;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellLevelManager;
import com.manaborn.spells.SpellType;

/**
 * Weakness Curse spell implementation with level-based scaling
 * 
 * Level 1 (слабый): Weakness I, 10с, дальность 10, кд 30с, мана 25
 * Level 2 (средний): Weakness II, 15с, дальность 11, кд 52.5с, мана 30
 * Level 3 (ОРИГИНАЛ): Weakness III, 20с, дальность 12, кд 75с, мана 35
 */
public class WeaknessCurseSpell implements LeveledSpell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return true;
        }

        int level = SpellLevelManager.getSpellLevel(user.getUuid(), SpellType.WEAKNESS_CURSE);

        // Level-based parameters
        double maxRange = switch (level) {
            case 1 -> 10.0;
            case 2 -> 11.0;
            default -> 12.0; // level 3 (original)
        };
        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVector().normalize();

        // Search for targets in expanded box
        Box search = user.getBoundingBox().expand(maxRange, maxRange * 0.5, maxRange);
        List<LivingEntity> candidates = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                search,
                e -> e != null && e.isAlive() && e != user);

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double cosThreshold = Math.cos(Math.toRadians(20.0)); // narrow cone

        for (LivingEntity e : candidates) {
            Vec3d to = e.getEyePos().subtract(eye);
            double dist = to.length();
            if (dist > maxRange || dist < 0.1)
                continue;
            Vec3d dir = to.normalize();
            double dot = dir.dotProduct(look);
            if (dot < cosThreshold)
                continue; // outside cone
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }

        if (best == null) {
            if (user instanceof ServerPlayerEntity sp) {
                sp.sendMessage(Text.literal("Нет подходящей цели перед вами"), true);
            }
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f,
                    0.6f);
            return true;
        }

        // Apply weakness effect based on level
        int duration = switch (level) {
            case 1 -> 200;  // 10 seconds
            case 2 -> 300;  // 15 seconds
            default -> 400; // 20 seconds (original)
        };
        
        int amplifier = switch (level) {
            case 1 -> 0; // Weakness I
            case 2 -> 1; // Weakness II
            default -> 2; // Weakness III (original)
        };
        
        best.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, amplifier, false, true, true),
                user);

        // Visual effects around target
        double tx = best.getX();
        double ty = best.getY() + best.getHeight() * 0.6;
        double tz = best.getZ();
        for (int i = 0; i < 28; i++) {
            double a = serverWorld.random.nextDouble() * Math.PI * 2;
            double r = 0.4 + serverWorld.random.nextDouble() * 0.9;
            double px = tx + Math.cos(a) * r;
            double pz = tz + Math.sin(a) * r;
            double py = ty + (serverWorld.random.nextDouble() - 0.5) * 0.8;
            serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 1, 0, 0, 0, 0);
            if (i % 3 == 0) {
                serverWorld.spawnParticles(ParticleTypes.SOUL, px, py, pz, 1, 0.02, 0.02, 0.02, 0.005);
            }
        }
        world.playSound(null, best.getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.7f,
                0.8f);
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.5f,
                0.7f);

        if (user instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.literal("Цель проклята слабостью (20с)"), true);
        }

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
            case 2 -> 30;
            default -> 35; // level 3 (original)
        };
    }

    @Override
    public int getCooldown() {
        return 600; // Level 1: 30 seconds
    }
    
    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 600;  // 30 seconds
            case 2 -> 1050; // 52.5 seconds
            default -> 1500; // 75 seconds (level 3, original)
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.WEAKNESS_CURSE;
    }
}
