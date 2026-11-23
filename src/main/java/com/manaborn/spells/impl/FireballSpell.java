package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.SpellLevelManager;

/**
 * Fireball spell implementation with level-based scaling.
 *
 * Визуал:
 *  - при касте — конус огненного выброса из рук и слоёный звук
 *  - в полёте — плотный след из пламени, дыма и искр (густота растёт с уровнем)
 *  - при взрыве — кольцо пламени, веер искр, столб дыма и вспышка
 *
 * Level 1 (слабый): Взрыв 1.0, скорость 0.5x, кд 7с, мана 25
 * Level 2 (средний): Взрыв 1.5, скорость 1.25x, кд 6с, мана 22
 * Level 3 (ОРИГИНАЛ): Взрыв 2.0, скорость 2.0x, кд 5с, мана 20
 */
public class FireballSpell implements LeveledSpell {

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
            public void tick() {
                super.tick();
                spawnTrail(this, level);
            }

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
                    spawnImpact(world, this.getX(), this.getY(), this.getZ(), explosionPower, level);
                }
            }
        };

        // Спавним снаряд впереди и чуть ниже линии взгляда, чтобы партиклы не закрывали обзор
        fireball.setPos(
            user.getX() + direction.x * 1.2,
            user.getEyeY() - 0.3 + direction.y * 1.2,
            user.getZ() + direction.z * 1.2
        );

        // Set velocity based on level
        fireball.setVelocity(
            direction.x * velocityMultiplier,
            direction.y * velocityMultiplier,
            direction.z * velocityMultiplier
        );

        world.spawnEntity(fireball);

        // === ЭФФЕКТЫ КАСТА ===
        spawnCastBurst(world, user, direction, level);

        // Слоёный звук: выброс огня + выстрел огненного элементаля
        float pitch = 0.85f + level * 0.1f;
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, pitch);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.6f, pitch + 0.15f);

        return true;
    }

    // ── Визуальные эффекты ───────────────────────────────────────────────

    /**
     * Огненный выброс в момент каста: цепочка вспышек вдоль направления взгляда,
     * смещённая вперёд и на уровень рук, чтобы не закрывать обзор.
     */
    private static void spawnCastBurst(World world, PlayerEntity user, Vec3d dir, int level) {
        if (!(world instanceof ServerWorld sw)) return;

        double baseX = user.getX();
        double baseY = user.getEyeY() - 0.35; // на уровне рук, ниже перекрестия
        double baseZ = user.getZ();

        // Вспышки пламени вдоль линии каста — эффект выброса из рук
        int streakCount = 4 + level * 2;
        for (int i = 0; i < streakCount; i++) {
            double d = 0.9 + i * 0.3;
            sw.spawnParticles(ParticleTypes.FLAME,
                    baseX + dir.x * d, baseY + dir.y * d, baseZ + dir.z * d,
                    2,
                    0.05, 0.05, 0.05,
                    0.02);
        }

        // Пара горячих искр у точки выброса
        sw.spawnParticles(ParticleTypes.LAVA,
                baseX + dir.x * 1.1, baseY + dir.y * 1.1, baseZ + dir.z * 1.1,
                1 + level,
                0.08, 0.06, 0.08,
                0.0);

        // Лёгкий дымок — ниже линии взгляда
        sw.spawnParticles(ParticleTypes.SMOKE,
                baseX + dir.x * 0.9, baseY - 0.2 + dir.y * 0.9, baseZ + dir.z * 0.9,
                3,
                0.06, 0.04, 0.06,
                0.01);
    }

    /**
     * След снаряда в полёте: пламя + дым, с уровнем — искры и обжигающий ореол.
     */
    private static void spawnTrail(SmallFireballEntity fireball, int level) {
        if (!(fireball.getEntityWorld() instanceof ServerWorld sw)) return;
        // Первые тики снаряд ещё рядом с лицом игрока — не спамим партиклами в обзор
        if (fireball.age < 3) return;

        double x = fireball.getX();
        double y = fireball.getY();
        double z = fireball.getZ();

        // Основное пламя (густеет с уровнем)
        sw.spawnParticles(ParticleTypes.FLAME,
                x, y, z, 1 + level,
                0.06, 0.06, 0.06,
                0.005);

        // Дымный хвост каждые 2 тика
        if (fireball.age % 2 == 0) {
            sw.spawnParticles(ParticleTypes.SMOKE,
                    x, y, z, 1,
                    0.05, 0.05, 0.05,
                    0.004);
        }

        // Со 2 уровня — летящие искры
        if (level >= 2 && fireball.age % 3 == 0) {
            sw.spawnParticles(ParticleTypes.LAVA,
                    x, y, z, 1,
                    0.08, 0.08, 0.08,
                    0.0);
        }

        // С 3 уровня — бело-горячий ореол (частица вспышки редко, мягко)
        if (level >= 3 && fireball.age % 5 == 0) {
            sw.spawnParticles(ParticleTypes.SMALL_FLAME,
                    x, y, z, 2,
                    0.12, 0.12, 0.12,
                    0.002);
        }
    }

    /**
     * Взрыв: кольцо пламени по земле, веер искр, столб дыма, вспышка.
     */
    private static void spawnImpact(World world, double x, double y, double z, float power, int level) {
        if (!(world instanceof ServerWorld sw)) return;

        // Кольцо пламени, разлетающееся от эпицентра
        int ringPoints = 16 + level * 6;
        double radius = power * 0.9;
        for (int i = 0; i < ringPoints; i++) {
            double angle = (Math.PI * 2 * i) / ringPoints;
            double vx = Math.cos(angle) * 0.18 * radius;
            double vz = Math.sin(angle) * 0.18 * radius;
            sw.spawnParticles(ParticleTypes.FLAME,
                    x, y + 0.2, z, 1,
                    vx, 0.02, vz,
                    1.0);
        }

        // Веер горячих искр
        sw.spawnParticles(ParticleTypes.LAVA,
                x, y + 0.3, z, 6 + level * 4,
                0.3 * power, 0.3, 0.3 * power,
                0.0);

        // Столб дыма
        sw.spawnParticles(ParticleTypes.LARGE_SMOKE,
                x, y + 0.4, z, 8,
                0.2 * power, 0.25, 0.2 * power,
                0.02);

        // Яркая вспышка в эпицентре (только со 2 уровня)
        if (level >= 2) {
            sw.spawnParticles(net.minecraft.particle.TintedParticleEffect.create(ParticleTypes.FLASH, 0xCCFFFFFF),
                    x, y + 0.2, z, 1,
                    0.0, 0.0, 0.0,
                    0.0);
        }

        // Догорающие угли, оседающие вокруг
        sw.spawnParticles(ParticleTypes.SMALL_FLAME,
                x, y + 0.1, z, 10 + level * 4,
                0.4 * power, 0.15, 0.4 * power,
                0.008);

        // Глухой «вжух» догорания поверх ванильного звука взрыва
        sw.playSound(null, x, y, z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.7f, 0.8f);
        if (level >= 3) {
            sw.playSound(null, x, y, z, SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.5f, 0.7f);
        }
    }

    @Override
    public int getManaCost() {
        // Will be overridden by level-based cost in SpellManager
        return 25; // Level 1 cost
    }

    @Override
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

    @Override
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
