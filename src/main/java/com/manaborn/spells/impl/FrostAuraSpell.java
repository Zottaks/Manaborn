package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.ActiveEffectManager;

/**
 * Frost Aura spell implementation
 */
public class FrostAuraSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }

        int duration = 400; // 20 seconds
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
        return 50;
    }

    @Override
    public int getCooldown() {
        return 1000; // 50 seconds
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
