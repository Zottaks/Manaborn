package com.manaborn.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class IceShardEntity extends ArrowEntity {
    public IceShardEntity(EntityType<? extends ArrowEntity> entityType, World world) {
        super(entityType, world);
    }
    
    public IceShardEntity(World world, LivingEntity owner) {
        super(com.manaborn.ManabornMod.ICE_SHARD_ENTITY, world);
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }
    
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (entityHitResult.getEntity() instanceof LivingEntity target) {
            // Применяем только эффект замедления на 5 секунд
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2), this.getOwner());
            
            // Звук попадания
            this.playSound(SoundEvents.ENTITY_SNOWBALL_THROW, 1.0f, 1.2f);
        }
        this.discard();
    }
    
    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.playSound(SoundEvents.BLOCK_GLASS_BREAK, 0.5f, 1.5f);
        }
    }
    
    @Override
    protected ItemStack asItemStack() {
        return ItemStack.EMPTY;
    }
}