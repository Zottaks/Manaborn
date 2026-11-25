package com.manaborn.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import com.manaborn.ManabornMod;
import com.manaborn.spells.SpellManager;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

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
    
    /**
     * Предмет, которым отображается снаряд (3D ледяной осколок вместо снежка)
     */
    @Override
    protected Item getDefaultItem() {
        return ManabornMod.ICE_SHARD_ITEM;
    }

    @Override
    public void tick() {
        super.tick();

        // Ледяной след из частиц (только на клиенте)
        if (this.getEntityWorld().isClient()) {
            double px = this.getX() + (this.random.nextDouble() - 0.5) * 0.18;
            double py = this.getY() + (this.random.nextDouble() - 0.5) * 0.18;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5) * 0.18;
            this.getEntityWorld().addParticleClient(ParticleTypes.SNOWFLAKE, px, py, pz, 0.0, 0.005, 0.0);
            if (this.age % 4 == 0) {
                this.getEntityWorld().addParticleClient(ParticleTypes.END_ROD,
                        this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }

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

                // Морозный эффект: редкие белые частицы вокруг пострадавшего
                if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
                    spawnFrostBurst(serverWorld, target);
                    FROSTED_TARGETS.merge(target.getUuid(), 100, Integer::sum);
                    // Тихий звон льда при попадании
                    serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.BLOCK_GLASS_BREAK, target.getSoundCategory(), 0.5f, 1.5f);
                }
            }
        }
    }

    /** Цели с остаточным морозным эффектом: UUID -> оставшиеся тики */
    private static final Map<UUID, Integer> FROSTED_TARGETS = new HashMap<>();

    /**
     * Вспышка частиц в момент попадания: немного снежинок и пара искр
     */
    private static void spawnFrostBurst(ServerWorld world, LivingEntity target) {
        double cx = target.getX();
        double cy = target.getY() + target.getHeight() * 0.5;
        double cz = target.getZ();
        double spread = target.getWidth() * 0.6;
        world.spawnParticles(ParticleTypes.SNOWFLAKE, cx, cy, cz, 8,
                spread, target.getHeight() * 0.4, spread, 0.01);
        world.spawnParticles(ParticleTypes.END_ROD, cx, cy, cz, 3,
                spread * 0.8, target.getHeight() * 0.4, spread * 0.8, 0.01);
    }

    /**
     * Остаточный морозный эффект: пока цель замедлена, раз в секунду
     * вокруг неё поднимается пара редких снежинок.
     * Вызывается из ServerTickEvents каждый тик сервера.
     */
    public static void tickFrostEffects(MinecraftServer server) {
        if (FROSTED_TARGETS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> it = FROSTED_TARGETS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            int left = entry.getValue() - 1;
            if (left <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(left);
            if (left % 20 != 0) {
                continue; // только раз в секунду — редко и ненавязчиво
            }
            for (ServerWorld world : server.getWorlds()) {
                Entity entity = world.getEntity(entry.getKey());
                if (entity instanceof LivingEntity living && living.isAlive()) {
                    world.spawnParticles(ParticleTypes.SNOWFLAKE,
                            living.getX(), living.getY() + living.getHeight() * 0.6, living.getZ(),
                            2, living.getWidth() * 0.5, living.getHeight() * 0.35,
                            living.getWidth() * 0.5, 0.005);
                    break;
                }
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