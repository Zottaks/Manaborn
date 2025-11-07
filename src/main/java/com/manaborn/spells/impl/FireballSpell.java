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

/**
 * Fireball spell implementation
 */
public class FireballSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return true;
        }

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
                            2.0f, // Explosion power (1.0 = ghast fireball, 4.0 = TNT)
                            World.ExplosionSourceType.NONE);
                }
            }
        };

        fireball.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());

        // Set custom velocity for faster travel
        fireball.setVelocity(direction.x * 2.0, direction.y * 2.0, direction.z * 2.0);

        world.spawnEntity(fireball);
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);

        return true;
    }

    @Override
    public int getManaCost() {
        return 20;
    }

    @Override
    public int getCooldown() {
        return 100; // 5 seconds
    }

    @Override
    public SpellType getType() {
        return SpellType.FIREBALL;
    }
}
