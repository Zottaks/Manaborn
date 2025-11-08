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
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;

import java.util.List;

/**
 * Weakness Curse spell implementation
 */
public class WeaknessCurseSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return true;
        }

        double range = 12.0;
        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVector().normalize();

        // Search for targets in expanded box
        Box search = user.getBoundingBox().expand(range, range * 0.5, range);
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
            if (dist > range || dist < 0.1)
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

        int duration = 400; // 20 seconds
        int amplifier = 2; // Strong weakness
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
        return 35;
    }

    @Override
    public int getCooldown() {
        return 1500; // 75 seconds
    }

    @Override
    public SpellType getType() {
        return SpellType.WEAKNESS_CURSE;
    }
}
