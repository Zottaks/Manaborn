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
 * Heat Weapon spell implementation
 */
public class HeatWeaponSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }

        // Activate heat weapon for 30 seconds (600 ticks)
        ActiveEffectManager.addHeatWeapon(serverPlayer.getUuid());

        // Visual effects: fire particles around player's hands
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            double baseX = user.getX();
            double baseY = user.getY() + 1.2; // hand level
            double baseZ = user.getZ();
            for (int i = 0; i < 20; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 1.2;
                double offsetY = (world.random.nextDouble() - 0.5) * 0.6;
                double offsetZ = (world.random.nextDouble() - 0.5) * 1.2;
                serverWorld.spawnParticles(ParticleTypes.FLAME,
                        baseX + offsetX,
                        baseY + offsetY,
                        baseZ + offsetZ,
                        1,
                        0.0, 0.01, 0.0,
                        0.01);
            }
        }

        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);
        serverPlayer.sendMessage(Text.literal("Нагрев оружия активирован! Следующие 10 атак поджигают цели."), true);

        return true;
    }

    @Override
    public int getManaCost() {
        return 45;
    }

    @Override
    public int getCooldown() {
        return 100; // 5 seconds
    }

    @Override
    public SpellType getType() {
        return SpellType.HEAT_WEAPON;
    }

    @Override
    public boolean hasActiveEffects() {
        return true;
    }
}
