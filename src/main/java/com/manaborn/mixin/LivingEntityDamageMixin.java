package com.manaborn.mixin;

import com.manaborn.spells.SpellManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
    @Inject(method = "damage", at = @At("HEAD"))
    private void manaborn$onDamage(World world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(world != null && !world.isClient())) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return;
        if (!SpellManager.hasAbsorbingBarrier(player.getUuid())) return;

        boolean isProjectile = source.isIn(DamageTypeTags.IS_PROJECTILE);
        if (!isProjectile) {
            SpellManager.removeAbsorbingBarrier(player);
        }
    }
}
