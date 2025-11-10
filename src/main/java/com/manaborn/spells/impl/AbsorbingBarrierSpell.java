package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.ActiveEffectManager;

/**
 * Absorbing Barrier spell implementation
 */
public class AbsorbingBarrierSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }

        int duration = 200; // 10 seconds
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
        return 60;
    }

    @Override
    public int getCooldown() {
        return 1500; // 75 seconds
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
