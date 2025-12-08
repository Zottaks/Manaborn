package com.manaborn.spells.impl;

import com.manaborn.mana.ManaManager;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellLevelManager;
import com.manaborn.spells.SpellType;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Clarifying Tone (Проясняющий тон) - заклинание магии звука очищающее дебаффы.
 *
 * Варианты каста:
 * - Обычный ЛКМ: снимает эффекты с цели под прицелом (игрок), либо с кастующего, если цели нет.
 * - Shift + ЛКМ: AoE вокруг кастующего (2 / 3 / 5 блоков по уровням), снимает эффекты со всех игроков в радиусе.
 *
 * Стоимость (single target):
 *   L1: 25 маны, кд 45с
 *   L2: 30 маны, кд 40с
 *   L3: 30 маны, кд 30с
 *
 * Стоимость AoE: +30 маны к базовой стоимости уровня.
 *
 * Уровни очистки:
 *   L1: слабые дебаффы (WEAKNESS, SLOWNESS I, HUNGER, POISON I)
 *   L2: + средние (SLOWNESS до II, BLINDNESS, MINING_FATIGUE до II, POISON до II)
 *   L3: все отрицательные эффекты, КРОМЕ обморожения (FREEZING).
 */
public class ClarifyingToneSpell implements LeveledSpell {

    private static final double MAX_RANGE = 16.0;

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }

        int level = SpellLevelManager.getSpellLevel(user.getUuid(), SpellType.CLARIFYING_TONE);
        int adjustedLevel = Math.max(1, Math.min(3, level));

        boolean isAoE = user.isSneaking();

        // При AoE требуется доплата 30 маны сверх уже списанной в SpellManager
        if (isAoE) {
            if (!ManaManager.tryConsume(serverPlayer, 30)) {
                serverPlayer.sendMessage(Text.literal("Недостаточно маны для очищения области!"), true);
                return false;
            }
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return true;
        }

        if (isAoE) {
            castAoE(serverWorld, serverPlayer, adjustedLevel);
        } else {
            castSingleTarget(serverWorld, serverPlayer, adjustedLevel);
        }

        return true;
    }

    private void castSingleTarget(ServerWorld world, ServerPlayerEntity caster, int level) {
        LivingEntity target = findLookTarget(world, caster);
        if (target == null) {
            target = caster; // Если цели нет, очищаем самого кастующего
        }

        int removed = cleanseEffects(target, level);

        playSoundAndParticles(world, caster, target, level, false);

        if (removed > 0) {
            if (target == caster) {
                caster.sendMessage(Text.literal("§e♪ Негативные эффекты рассеяны: " + removed), true);
            } else {
                caster.sendMessage(Text.literal("§e♪ Вы прояснили тон вокруг " + target.getName().getString()
                        + " (снято эффектов: " + removed + ")"), true);
            }
        } else {
            caster.sendMessage(Text.literal("§7Нечего очищать."), true);
        }
    }

    private void castAoE(ServerWorld world, ServerPlayerEntity caster, int level) {
        double radius = switch (level) {
            case 1 -> 2.0;
            case 2 -> 3.0;
            default -> 5.0;
        };

        Vec3d center = new Vec3d(caster.getX(), caster.getY(), caster.getZ());
        Box area = Box.of(center, radius * 2, radius * 2, radius * 2);

        // Сначала гарантированно очищаем самого кастера
        int selfRemoved = cleanseEffects(caster, level);
        int totalRemoved = selfRemoved;

        // Отладка: если с кастера ничего не снялось, покажем какие эффекты на нём есть
        if (selfRemoved == 0) {
            Collection<StatusEffectInstance> current = caster.getStatusEffects();
            if (current.isEmpty()) {
                caster.sendMessage(Text.literal("[DEBUG] На кастере нет статус-эффектов"), false);
            } else {
                caster.sendMessage(Text.literal("[DEBUG] Эффекты на кастере:"), false);
                for (StatusEffectInstance inst : current) {
                    String id = inst.getEffectType().value().toString();
                    int amp = inst.getAmplifier();
                    caster.sendMessage(Text.literal("[DEBUG] " + id + " amp=" + amp), false);
                }
            }
        }

        // Затем работаем по всем живым существам в области (игроки и мобы), кроме кастера
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class,
                area,
                e -> e != null && e.isAlive() && !e.getUuid().equals(caster.getUuid()));

        for (LivingEntity entity : entities) {
            totalRemoved += cleanseEffects(entity, level);
        }

        playSoundAndParticles(world, caster, null, level, true);

        caster.sendMessage(Text.literal("§e♪ Проясняющий тон по области. Снято эффектов: " + totalRemoved), true);
    }

    private LivingEntity findLookTarget(ServerWorld world, PlayerEntity caster) {
        Vec3d start = caster.getEyePos();
        Vec3d look = caster.getRotationVector();
        Vec3d end = start.add(look.multiply(MAX_RANGE));

        HitResult hit = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                caster));

        Vec3d rayEnd = hit.getType() == HitResult.Type.BLOCK ? hit.getPos() : end;
        Box box = new Box(start, rayEnd).expand(0.5);

        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> e != null && e.isAlive());
        if (entities.isEmpty()) {
            return null;
        }

        // Берём ближайшую к линии взгляда цель
        LivingEntity best = null;
        double bestDot = 0.95; // Требуем достаточно точное прицеливание

        for (LivingEntity entity : entities) {
            Vec3d to = entity.getEyePos().subtract(start).normalize();
            double dot = look.dotProduct(to);
            if (dot > bestDot) {
                bestDot = dot;
                best = entity;
            }
        }

        return best;
    }

    private int cleanseEffects(LivingEntity target, int level) {
        Collection<StatusEffectInstance> current = target.getStatusEffects();
        if (current.isEmpty()) {
            return 0;
        }

        List<RegistryEntry<net.minecraft.entity.effect.StatusEffect>> toRemove = new ArrayList<>();

        for (StatusEffectInstance inst : current) {
            RegistryEntry<net.minecraft.entity.effect.StatusEffect> type = inst.getEffectType();

            // Уровень 3: снимаем все отрицательные статус-эффекты
            if (level >= 3) {
                if (!type.value().isBeneficial()) {
                    toRemove.add(type);
                }
                continue;
            }

            // Для 1-2 уровней работаем по списку
            if (!shouldRemoveOnLevel(type, inst.getAmplifier(), level)) {
                continue;
            }

            toRemove.add(type);
        }

        for (RegistryEntry<net.minecraft.entity.effect.StatusEffect> type : toRemove) {
            target.removeStatusEffect(type);
        }

        return toRemove.size();
    }

    private boolean shouldRemoveOnLevel(RegistryEntry<net.minecraft.entity.effect.StatusEffect> type, int amplifier, int level) {
        // Общие лёгкие дебаффы для 1 уровня
        net.minecraft.entity.effect.StatusEffect effect = type.value();

        boolean level1 =
                effect == StatusEffects.WEAKNESS.value() ||
                effect == StatusEffects.HUNGER.value() ||
                (effect == StatusEffects.SLOWNESS.value() && amplifier <= 0) ||
                (effect == StatusEffects.POISON.value() && amplifier <= 0);

        if (level == 1) {
            return level1;
        }

        // Уровень 2: всё с уровня 1 + средние дебаффы
        if (level == 2) {
            if (level1) return true;

            if (effect == StatusEffects.SLOWNESS.value() && amplifier <= 1) return true; // до Slowness II
            if (effect == StatusEffects.BLINDNESS.value()) return true;
            if (effect == StatusEffects.MINING_FATIGUE.value() && amplifier <= 1) return true;
            if (effect == StatusEffects.POISON.value() && amplifier <= 1) return true; // Poison I-II

            return false;
        }

        return false;
    }

    private void playSoundAndParticles(ServerWorld world, PlayerEntity caster, LivingEntity target, int level, boolean aoe) {
        Vec3d center = aoe
                ? new Vec3d(caster.getX(), caster.getY(), caster.getZ()).add(0, 1.0, 0)
                : (target != null
                        ? new Vec3d(target.getX(), target.getY(), target.getZ()).add(0, target.getHeight() * 0.5, 0)
                        : new Vec3d(caster.getX(), caster.getY(), caster.getZ()).add(0, 1.0, 0));

        // Ноты (жёлто-белые)
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * Math.PI * 2;
            double radius = aoe ? (2.0 + level) : 1.2;
            double px = center.x + Math.cos(angle) * radius;
            double pz = center.z + Math.sin(angle) * radius;
            double py = center.y + world.random.nextDouble() * 0.6;

            world.spawnParticles(ParticleTypes.NOTE, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
        }

        // Голубые частицы очистки внутри
        for (int i = 0; i < 20; i++) {
            double dx = (world.random.nextDouble() - 0.5) * 1.5;
            double dz = (world.random.nextDouble() - 0.5) * 1.5;
            double dy = world.random.nextDouble() * 1.0;
            world.spawnParticles(ParticleTypes.END_ROD, center.x + dx, center.y + dy, center.z + dz,
                    1, 0.0, 0.02, 0.0, 0.0);
        }

        // Звуки - мягкая очищающая мелодия
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS,
                1.2f, 1.1f + 0.1f * level);
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS,
                0.8f, 1.4f + 0.1f * level);
    }

    @Override
    public int getManaCost() {
        return 25; // базовая стоимость уровня 1 (single target)
    }

    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 25;
            case 2 -> 30;
            default -> 30; // level 3
        };
    }

    @Override
    public int getCooldown() {
        return 45 * 20;
    }

    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 45 * 20;
            case 2 -> 40 * 20;
            default -> 30 * 20;
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.CLARIFYING_TONE;
    }
}
