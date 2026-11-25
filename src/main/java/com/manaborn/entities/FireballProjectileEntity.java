package com.manaborn.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.manaborn.ManabornMod;
import com.manaborn.spells.impl.FireballSpell;

/**
 * Снаряд заклинания «Огненный шар».
 * Отличается от ванильного SmallFireballEntity собственным типом сущности,
 * что позволяет рисовать его 3D-моделью огненной сферы (см. renderer)
 * и хранить параметры уровня заклинания.
 */
public class FireballProjectileEntity extends SmallFireballEntity {

    private float explosionPower = 2.0f;
    private int level = 3;

    /**
     * Конструктор для загрузки сущности из мира
     */
    public FireballProjectileEntity(EntityType<? extends SmallFireballEntity> entityType, World world) {
        super(entityType, world);
        if (ManabornMod.FIRE_ORB_ITEM != null) {
            this.setItem(new ItemStack(ManabornMod.FIRE_ORB_ITEM));
        }
    }

    /**
     * Конструктор для создания снаряда при касте
     */
    public FireballProjectileEntity(World world, LivingEntity owner, Vec3d direction,
            float explosionPower, int level) {
        super(ManabornMod.FIREBALL_PROJECTILE_ENTITY, world);
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.explosionPower = explosionPower;
        this.level = level;
        this.setItem(new ItemStack(ManabornMod.FIRE_ORB_ITEM));
    }

    /** Уровень заклинания — нужен рендереру для размера сферы */
    public int getSpellLevel() {
        return this.level;
    }

    @Override
    public void tick() {
        super.tick();
        FireballSpell.spawnTrail(this, this.level);
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getEntityWorld().isClient()) {
            this.getEntityWorld().createExplosion(
                    this,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    this.explosionPower,
                    World.ExplosionSourceType.NONE);
            FireballSpell.spawnImpact(this.getEntityWorld(), this.getX(), this.getY(), this.getZ(),
                    this.explosionPower, this.level);
        }
    }
}
