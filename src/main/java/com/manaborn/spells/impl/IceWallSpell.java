package com.manaborn.spells.impl;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.ActiveEffectManager;

import java.util.List;
import java.util.ArrayList;

/**
 * Ice Wall spell implementation
 */
public class IceWallSpell implements Spell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        // Ice Wall is handled differently - it's a hold-and-cast spell
        // This implementation is for the spell registry, but actual casting
        // is handled in MagicStaffItem for the hold mechanic
        return true;
    }

    /**
     * Create ice wall with specified parameters
     */
    public static void createIceWall(ServerWorld world, PlayerEntity user, int width, int height, int durationTicks) {
        Vec3d lookVec = user.getRotationVector();
        double distance = 2.0;
        double centerX = user.getX() + lookVec.x * distance;
        double centerY = Math.floor(user.getY());
        double centerZ = user.getZ() + lookVec.z * distance;

        double perpX = -lookVec.z;
        double perpZ = lookVec.x;
        double length = Math.sqrt(perpX * perpX + perpZ * perpZ);
        if (length > 0) {
            perpX /= length;
            perpZ /= length;
        }

        int half = width / 2;
        List<BlockPos> placed = new ArrayList<>();

        for (int dx = -half; dx <= (width - 1 - half); dx++) {
            double offsetX = perpX * dx;
            double offsetZ = perpZ * dx;
            int baseX = (int) Math.floor(centerX + offsetX);
            int baseZ = (int) Math.floor(centerZ + offsetZ);
            for (int dy = 0; dy < height; dy++) {
                int by = (int) (centerY + dy);
                BlockPos pos = new BlockPos(baseX, by, baseZ);

                // Checks: air only, no fluids, no entities inside
                if (!world.isAir(pos))
                    continue;
                FluidState fluid = world.getFluidState(pos);
                if (!fluid.isEmpty())
                    continue;
                Box blockBox = Box.from(Vec3d.ofCenter(pos)).expand(0.4);
                List<LivingEntity> colliders = world.getEntitiesByClass(LivingEntity.class, blockBox, e -> e.isAlive());
                if (!colliders.isEmpty())
                    continue;

                world.setBlockState(pos, Blocks.PACKED_ICE.getDefaultState());
                placed.add(pos);
            }
        }

        if (!placed.isEmpty()) {
            ActiveEffectManager.addIceWall(world, placed, durationTicks);
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.PLAYERS, 0.8f, 0.9f);
            // Ice particles
            for (BlockPos p : placed) {
                world.spawnParticles(ParticleTypes.SNOWFLAKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.05,
                        0.05, 0.05, 0.0);
            }
        }
    }

    @Override
    public int getManaCost() {
        return 50;
    }

    @Override
    public int getCooldown() {
        return 200; // 10 seconds (but handled differently for hold mechanic)
    }

    @Override
    public SpellType getType() {
        return SpellType.ICE_WALL;
    }

    @Override
    public boolean hasActiveEffects() {
        return true;
    }
}
