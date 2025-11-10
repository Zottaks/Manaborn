package com.manaborn.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import com.manaborn.spells.SpellManager;

public class IceShardEntity extends SnowballEntity {
    /**
     * Конструктор для загрузки сущности из мира
     */
    public IceShardEntity(EntityType<? extends SnowballEntity> entityType, World world) {
        super(entityType, world);
    }
    
    /**
     * Конструктор для создания сущности из кода
     */
    public IceShardEntity(World world, LivingEntity owner) {
        super(com.manaborn.ManabornMod.ICE_SHARD_ENTITY, world);
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }
    
    @Override
    public void tick() {
        super.tick();
        if (!this.getEntityWorld().isClient() && this.getEntityWorld() instanceof ServerWorld serverWorld) {
            Box box = this.getBoundingBox();
            if (SpellManager.intersectsFireWall(serverWorld, box)) {
                this.discard();
            }
        }
    }
    
    /**
     * Вызывается при попадании в сущность
     */
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        
        if (!this.getEntityWorld().isClient()) {
            if (entityHitResult.getEntity() instanceof LivingEntity target) {
                // Применяем эффект замедления на 5 секунд (100 тиков), уровень 2
                target.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2), 
                    this.getOwner()
                );
            }
        }
    }
    
    /**
     * Вызывается при попадании в блок
     */
    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        
        // Звук разбивающегося стекла при попадании в блок
        if (!this.getEntityWorld().isClient()) {
            this.getEntityWorld().playSound(
                null, 
                this.getX(), 
                this.getY(), 
                this.getZ(), 
                SoundEvents.BLOCK_GLASS_BREAK, 
                this.getSoundCategory(), 
                0.8f, 
                1.2f
            );
        }
    }
}