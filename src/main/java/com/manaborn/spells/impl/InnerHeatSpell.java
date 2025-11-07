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
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;

/**
 * Inner Heat spell implementation - gives speed effect
 */
public class InnerHeatSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            return true;
        }

        // Give speed effect: 10 seconds, level 1 (Speed II)
        int duration = 200; // 10 seconds
        int amplifier = 1; // Speed II
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
        return 30;
    }

    @Override
    public int getCooldown() {
        return 600; // 30 seconds
    }

    @Override
    public SpellType getType() {
        return SpellType.INNER_HEAT;
    }
}
