package com.manaborn.spells.impl;

import com.manaborn.spells.ActiveEffectManager;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellLevelManager;
import com.manaborn.spells.SpellType;
import net.minecraft.entity.LivingEntity;
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

public class FrostCoffinSpell implements LeveledSpell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity caster) || !(world instanceof ServerWorld serverWorld)) {
            return true;
        }

        int level = SpellLevelManager.getSpellLevel(caster.getUuid(), SpellType.FROST_COFFIN);

        LivingEntity target = findLookTarget(serverWorld, caster, 16.0);
        if (target == null) {
            caster.sendMessage(Text.literal("Нет подходящей цели перед вами"), true);
            world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 0.6f);
            return true;
        }

        boolean applyBlindness = level >= 2;
        boolean holdVariant = level >= 3;

        ActiveEffectManager.addFrostCoffin(serverWorld, caster.getUuid(), target.getUuid(), applyBlindness, holdVariant);

        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.PLAYERS, 0.8f, 1.25f);
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_SNOW_HIT, SoundCategory.PLAYERS, 0.9f, 0.9f);

        serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE,
                target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ(),
                18, 0.6, 0.8, 0.6, 0.05);

        return true;
    }

    private LivingEntity findLookTarget(ServerWorld world, ServerPlayerEntity caster, double maxRange) {
        Vec3d eye = caster.getEyePos();
        Vec3d look = caster.getRotationVector().normalize();

        Box search = caster.getBoundingBox().expand(maxRange, maxRange * 0.5, maxRange);
        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class,
                search,
                e -> e != null && e.isAlive() && e != caster);

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double cosThreshold = Math.cos(Math.toRadians(20.0));

        for (LivingEntity e : candidates) {
            Vec3d to = e.getEyePos().subtract(eye);
            double dist = to.length();
            if (dist > maxRange || dist < 0.1) {
                continue;
            }
            Vec3d dir = to.normalize();
            double dot = dir.dotProduct(look);
            if (dot < cosThreshold) {
                continue;
            }
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }

        return best;
    }

    @Override
    public int getManaCost() {
        return 35;
    }

    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 35;
            case 2 -> 40;
            default -> 45;
        };
    }

    @Override
    public int getCooldown() {
        return 30 * 20;
    }

    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 30 * 20;
            case 2 -> 35 * 20;
            default -> 40 * 20;
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.FROST_COFFIN;
    }

    @Override
    public boolean hasActiveEffects() {
        return true;
    }
}
