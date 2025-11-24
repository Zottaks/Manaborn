package com.manaborn.spells.impl;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.SpellLevelManager;

/**
 * Sound Strike spell - магия звука
 * Направленная атака звуковой волной, требует прицеливания
 * 
 * Level 1: 0.5 сердца урона, стан 1 секунда
 * Level 2: 1.5 сердца урона, стан 2 секунды
 * Level 3: 2 сердца урона, стан 3 секунды
 * 
 * Особенность: кастующий должен стоять на месте при использовании
 */
public class SoundStrikeSpell implements LeveledSpell {

    private static final double MAX_RANGE = 20.0; // Максимальная дальность
    private static final double HIT_BOX_SIZE = 0.5; // Размер области попадания (узкий луч)

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }

        // Проверка что игрок стоит на месте (нет движения)
        Vec3d velocity = user.getVelocity();
        if (Math.abs(velocity.x) > 0.01 || Math.abs(velocity.z) > 0.01) {
            serverPlayer.sendMessage(Text.literal("§cВы должны стоять на месте для каста!"), true);
            return false;
        }

        int level = SpellLevelManager.getSpellLevel(user.getUuid(), SpellType.SOUND_STRIKE);
        
        // Параметры в зависимости от уровня
        float damage = switch (level) {
            case 1 -> 1.0f;  // 0.5 сердца
            case 2 -> 3.0f;  // 1.5 сердца
            default -> 4.0f; // 2 сердца (level 3)
        };
        
        int stunDuration = switch (level) {
            case 1 -> 20;   // 1 секунда
            case 2 -> 40;   // 2 секунды
            default -> 60;  // 3 секунды (level 3)
        };

        // Направление взгляда игрока
        Vec3d lookVec = user.getRotationVector();
        Vec3d start = user.getEyePos();
        Vec3d end = start.add(lookVec.multiply(MAX_RANGE));

        // Raycast для проверки столкновений с блоками
        HitResult blockHit = world.raycast(new RaycastContext(
            start, 
            end, 
            RaycastContext.ShapeType.COLLIDER, 
            RaycastContext.FluidHandling.NONE, 
            user
        ));

        // Если луч попал в блок, сокращаем дальность
        Vec3d actualEnd = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getPos() : end;

        // Ищем врагов в узком конусе
        Box searchBox = new Box(start, actualEnd).expand(HIT_BOX_SIZE);
        boolean hitTarget = false;

        for (Entity entity : world.getOtherEntities(user, searchBox)) {
            if (!(entity instanceof LivingEntity target)) continue;
            
            // Проверяем точность попадания (узкий луч)
            Vec3d targetPos = new Vec3d(target.getX(), target.getY() + target.getHeight() / 2, target.getZ());
            Vec3d toTarget = targetPos.subtract(start);
            double distance = toTarget.length();
            
            if (distance > MAX_RANGE) continue;
            
            Vec3d targetDir = toTarget.normalize();
            double dotProduct = lookVec.dotProduct(targetDir);
            
            // Требуется очень точное прицеливание (cos(5°) ≈ 0.996)
            if (dotProduct < 0.996) continue;

            // Наносим урон
            if (world instanceof ServerWorld serverWorld) {
                target.damage(serverWorld, world.getDamageSources().magic(), damage);
            }
            
            // Применяем стан (Slowness IV + Mining Fatigue)
            target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS, stunDuration, 10, false, false, true
            ));
            target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.MINING_FATIGUE, stunDuration, 2, false, false, true
            ));
            
            // Визуальный эффект попадания
            if (world instanceof ServerWorld serverWorld) {
                Vec3d hitPos = new Vec3d(target.getX(), target.getY() + target.getHeight() / 2, target.getZ());
                spawnHitParticles(serverWorld, hitPos);
            }
            
            hitTarget = true;
            serverPlayer.sendMessage(Text.literal("§eЗвуковой удар! Урон: " + (damage / 2.0f) + " ❤"), true);
        }

        // Звуковая волна - визуальный эффект
        if (world instanceof ServerWorld serverWorld) {
            spawnSoundWaveParticles(serverWorld, start, actualEnd, level);
        }

        // Звуковой эффект
        world.playSound(
            null, 
            user.getBlockPos(), 
            SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 
            SoundCategory.PLAYERS, 
            1.0f, 
            0.8f + (level * 0.2f)
        );

        if (!hitTarget) {
            serverPlayer.sendMessage(Text.literal("§7Промах!"), true);
        }

        return true;
    }

    /**
     * Создает частицы звуковой волны от игрока до цели
     */
    private void spawnSoundWaveParticles(ServerWorld world, Vec3d start, Vec3d end, int level) {
        Vec3d direction = end.subtract(start);
        double distance = direction.length();
        Vec3d step = direction.normalize().multiply(0.5);
        
        int particleCount = (int)(distance * 2);
        
        for (int i = 0; i < particleCount; i++) {
            Vec3d pos = start.add(step.multiply(i));
            
            // Основная волна - note particles
            world.spawnParticles(
                ParticleTypes.NOTE,
                pos.x, pos.y, pos.z,
                1,
                0.1, 0.1, 0.1,
                0.0
            );
            
            // Дополнительные частицы в зависимости от уровня
            if (level >= 2) {
                world.spawnParticles(
                    ParticleTypes.SONIC_BOOM,
                    pos.x, pos.y, pos.z,
                    1,
                    0.05, 0.05, 0.05,
                    0.0
                );
            }
            
            if (level >= 3) {
                world.spawnParticles(
                    ParticleTypes.ENCHANT,
                    pos.x, pos.y, pos.z,
                    2,
                    0.15, 0.15, 0.15,
                    0.01
                );
            }
        }
    }

    /**
     * Создает частицы при попадании в цель
     */
    private void spawnHitParticles(ServerWorld world, Vec3d pos) {
        // Взрыв нот при попадании
        for (int i = 0; i < 20; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
            double offsetY = (world.random.nextDouble() - 0.5) * 0.8;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;
            
            world.spawnParticles(
                ParticleTypes.NOTE,
                pos.x + offsetX,
                pos.y + offsetY,
                pos.z + offsetZ,
                1,
                offsetX * 0.1,
                offsetY * 0.1,
                offsetZ * 0.1,
                0.05
            );
        }
        
        // Ударная волна
        world.spawnParticles(
            ParticleTypes.SONIC_BOOM,
            pos.x, pos.y, pos.z,
            1,
            0, 0, 0,
            0
        );
    }

    @Override
    public int getManaCost() {
        return 25; // Level 1 cost
    }
    
    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 25;
            case 2 -> 30;
            default -> 35; // level 3
        };
    }

    @Override
    public int getCooldown() {
        return 200; // Level 1: 10 seconds
    }
    
    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 200; // 10 seconds
            case 2 -> 160; // 8 seconds
            default -> 120; // 6 seconds (level 3)
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.SOUND_STRIKE;
    }
}
