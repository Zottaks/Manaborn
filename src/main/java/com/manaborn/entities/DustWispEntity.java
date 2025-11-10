package com.manaborn.entities;

import com.manaborn.ManabornMod;
import com.manaborn.items.ManoscopeItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.storage.WriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class DustWispEntity extends Entity {
    private static final int LIFETIME_TICKS = 20 * 60; // 60s
    private static final double PICKUP_RADIUS = 0.5; // Нужно подойти вплотную

    public DustWispEntity(EntityType<? extends DustWispEntity> type, World world) {
        super(type, world);
        this.noClip = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getEntityWorld().isClient()) {
            // Logic for client particles is handled in client code to avoid client imports here
            return;
        }

        // Server-side: lifetime and pickup logic
        if (this.age > LIFETIME_TICKS) {
            this.discard();
            return;
        }

        // Find nearby players with Manoscope worn
        List<PlayerEntity> players = this.getEntityWorld().getEntitiesByClass(
                PlayerEntity.class,
                new Box(getX() - PICKUP_RADIUS, getY() - PICKUP_RADIUS, getZ() - PICKUP_RADIUS,
                        getX() + PICKUP_RADIUS, getY() + PICKUP_RADIUS, getZ() + PICKUP_RADIUS),
                p -> p.isAlive() && isWearingManoscope(p));

        if (!players.isEmpty()) {
            PlayerEntity p = players.get(0);
            int count = 1 + this.random.nextInt(2); // 1-2 dust
            p.giveItemStack(new net.minecraft.item.ItemStack(ManabornMod.MAGIC_DUST, count));
            
            // Звук подбора (магический звук)
            this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 
                    0.3f, 1.5f + this.random.nextFloat() * 0.3f);
            
            this.discard();
        }
    }

    private boolean isWearingManoscope(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.HEAD).getItem() instanceof ManoscopeItem;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) { }

    @Override
    protected void readCustomData(ReadView view) { }

    @Override
    protected void writeCustomData(WriteView view) { }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false; // Invulnerable
    }
}
