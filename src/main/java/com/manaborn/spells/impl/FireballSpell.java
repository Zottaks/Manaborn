package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.SpellLevelManager;

/**
 * Fireball spell implementation with level-based scaling
 * 
 * Level 1 (слабый): Взрыв 1.0, скорость 0.5x, кд 7с, мана 25
 * Level 2 (средний): Взрыв 1.5, скорость 1.25x, кд 6с, мана 22
 * Level 3 (ОРИГИНАЛ): Взрыв 2.0, скорость 2.0x, кд 5с, мана 20
 */
public class FireballSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return true;
        }

        int level = SpellLevelManager.getSpellLevel(user.getUuid(), SpellType.FIREBALL);
        
        // Level-based parameters
        float explosionPower = switch (level) {
            case 1 -> 1.0f;
            case 2 -> 1.5f;
            default -> 2.0f; // level 3 (original)
        };
        
        double velocityMultiplier = switch (level) {
            case 1 -> 0.5;
            case 2 -> 1.25; // среднее между 0.5 и 2.0
            default -> 2.0; // level 3 (original)
        };

        // Create small fireball with player's direction
        Vec3d direction = user.getRotationVector();
        SmallFireballEntity fireball = new SmallFireballEntity(world, user, direction) {
            @Override
            public void onCollision(net.minecraft.util.hit.HitResult hitResult) {
                super.onCollision(hitResult);
                if (!world.isClient()) {
                    // Create real explosion at impact point
                    world.createExplosion(
                            this,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            explosionPower,
                            World.ExplosionSourceType.NONE);
                }
            }
        };

        fireball.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());

        // Set velocity based on level
        fireball.setVelocity(
            direction.x * velocityMultiplier, 
            direction.y * velocityMultiplier, 
            direction.z * velocityMultiplier
        );

        world.spawnEntity(fireball);
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        return true;
    }

    @Override
    public int getManaCost() {
        // Will be overridden by level-based cost in SpellManager
        return 25; // Level 1 cost
    }
    
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 25;
            case 2 -> 22;
            default -> 20; // level 3
        };
    }

    @Override
    public int getCooldown() {
        // Will be overridden by level-based cooldown
        return 140; // Level 1: 7 seconds
    }
    
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 140; // 7 seconds
            case 2 -> 120; // 6 seconds
            default -> 100; // 5 seconds (level 3)
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.FIREBALL;
    }
}
