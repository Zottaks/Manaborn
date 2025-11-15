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
 * Heat Weapon spell implementation with level-based scaling
 * 
 * Level 1 (слабый): 5 атак, только камень, кд 15с, мана 35
 * Level 2 (средний): 7-8 атак, до железа, кд 10с, мана 40
 * Level 3 (ОРИГИНАЛ): 10 атак, всё оружие, кд 5с, мана 45
 * 
 * ВАЖНО: Количество атак и совместимость оружия обрабатывается в ActiveEffectManager
 */
public class HeatWeaponSpell implements LeveledSpell {

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
        return 35; // Level 1 cost
    }
    
    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 35;
            case 2 -> 40;
            default -> 45; // level 3 (original)
        };
    }

    @Override
    public int getCooldown() {
        return 300; // Level 1: 15 seconds
    }
    
    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 300; // 15 seconds
            case 2 -> 200; // 10 seconds
            default -> 100; // 5 seconds (level 3, original)
        };
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
