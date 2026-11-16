package com.manaborn.passives;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PassiveEffectsManager {
    private PassiveEffectsManager() {
    }

    private static final Identifier FROST_SKIN_ARMOR_MOD_ID = Identifier.of("manaborn", "passive_frost_skin_armor");

    private static final Map<UUID, Long> lastCoalHeatProcMs = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastReactiveFrostProcMs = new ConcurrentHashMap<>();

    private static boolean hasPassive(ServerPlayerEntity player, PassiveType type) {
        PassiveType[] slots = PassiveManager.getActiveSlots(player.getUuid());
        for (PassiveType s : slots) {
            if (s == type) {
                return true;
            }
        }
        return false;
    }

    public static void onServerTick(Iterable<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            if (player == null || !player.isAlive()) {
                continue;
            }

            tickShadowShelter(player);
            tickFrostSkin(player);
        }
    }

    private static void tickShadowShelter(ServerPlayerEntity player) {
        if (!hasPassive(player, PassiveType.SHADOW_SHELTER)) {
            return;
        }

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        int light = world.getLightLevel(player.getBlockPos());
        if (light <= 7) {
            // Делаем эффект заметнее: скорость I + короткая невидимость.
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0, true, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 40, 0, true, false, true));
        }
    }

    private static void tickFrostSkin(ServerPlayerEntity player) {
        if (!hasPassive(player, PassiveType.FROST_SKIN)) {
            removeArmorModifier(player);
            return;
        }

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        boolean isColdBiome = world
                .getBiome(player.getBlockPos())
                .value()
                .isCold(player.getBlockPos(), 0);

        if (isColdBiome) {
            // +2 брони (заметнее, чем +1)
            EntityAttributeInstance armor = player.getAttributeInstance(EntityAttributes.ARMOR);
            if (armor != null && !armor.hasModifier(FROST_SKIN_ARMOR_MOD_ID)) {
                armor.addTemporaryModifier(new EntityAttributeModifier(
                        FROST_SKIN_ARMOR_MOD_ID,
                        2.0,
                        EntityAttributeModifier.Operation.ADD_VALUE
                ));
            }
        } else {
            removeArmorModifier(player);
        }

        // Быстрее "оттаивать" (заметнее, чем 25%)
        int frozen = player.getFrozenTicks();
        if (frozen > 0) {
            player.setFrozenTicks(Math.max(0, frozen - 2));
        }

        // Медлительность: уменьшаем длительность быстрее (мягко, без скачков)
        StatusEffectInstance slowness = player.getStatusEffect(StatusEffects.SLOWNESS);
        if (slowness != null && slowness.getDuration() > 2) {
            int newDuration = slowness.getDuration() - 1;
            player.removeStatusEffect(StatusEffects.SLOWNESS);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, newDuration, slowness.getAmplifier(), slowness.isAmbient(), slowness.shouldShowParticles(), slowness.shouldShowIcon()));
        }
    }

    private static void removeArmorModifier(ServerPlayerEntity player) {
        EntityAttributeInstance armor = player.getAttributeInstance(EntityAttributes.ARMOR);
        if (armor != null && armor.hasModifier(FROST_SKIN_ARMOR_MOD_ID)) {
            armor.removeModifier(FROST_SKIN_ARMOR_MOD_ID);
        }
    }

    public static float modifyIncomingDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        if (player == null || source == null) {
            return amount;
        }

        // Жар углей: заметнее снижение огненного/лавового урона
        if (hasPassive(player, PassiveType.COAL_HEAT) && source.isIn(DamageTypeTags.IS_FIRE)) {
            return amount * 0.70f;
        }

        return amount;
    }

    public static void onPlayerDamaged(ServerPlayerEntity player, DamageSource source, float originalAmount) {
        if (player == null || source == null) {
            return;
        }

        LivingEntity attacker = source.getAttacker() instanceof LivingEntity le ? le : null;
        if (attacker == null) {
            return;
        }

        // Жар углей: шанс поджечь атакующего, КД
        if (hasPassive(player, PassiveType.COAL_HEAT)) {
            long now = System.currentTimeMillis();
            long last = lastCoalHeatProcMs.getOrDefault(player.getUuid(), 0L);
            if (now - last >= 9000) {
                if (player.getRandom().nextFloat() < 0.20f) {
                    attacker.setOnFireFor(3);
                    lastCoalHeatProcMs.put(player.getUuid(), now);
                }
            }
        }

        // Лёд реакции: шанс наложить медлительность I на атакующего, КД
        if (hasPassive(player, PassiveType.REACTIVE_FROST)) {
            long now = System.currentTimeMillis();
            long last = lastReactiveFrostProcMs.getOrDefault(player.getUuid(), 0L);
            if (now - last >= 9000) {
                if (player.getRandom().nextFloat() < 0.25f) {
                    attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 0));
                    lastReactiveFrostProcMs.put(player.getUuid(), now);
                }
            }
        }
    }
}
