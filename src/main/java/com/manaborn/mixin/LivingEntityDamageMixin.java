package com.manaborn.mixin;

import com.manaborn.passives.PassiveEffectsManager;
import com.manaborn.spells.SpellManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float manaborn$modifyIncomingDamage(float amount, ServerWorld world, DamageSource source, float originalAmount) {
        if (world == null) return amount;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return amount;
        return PassiveEffectsManager.modifyIncomingDamage(player, source, amount);
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void manaborn$onDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (world == null) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return;

        PassiveEffectsManager.onPlayerDamaged(player, source, amount);

        if (!SpellManager.hasAbsorbingBarrier(player.getUuid())) return;

        boolean isProjectile = source.isIn(DamageTypeTags.IS_PROJECTILE);
        if (!isProjectile) {
            SpellManager.removeAbsorbingBarrier(player);
        }
    }
}
