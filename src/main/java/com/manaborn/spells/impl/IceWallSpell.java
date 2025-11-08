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
        // Special effect for maximum level (3x3 wall with hole that lifts player)
        if (width == 4 && height == 6) {
            createSpecialIceWall(world, user, durationTicks);
            return;
        }

        // Regular wall for other levels
        createRegularIceWall(world, user, width, height, durationTicks);
    }

    /**
     * Creates a Highground: base 4x4 platform under feet (Y+3), and surrounding 3x3
     * ring walls with center hole
     */
    private static void createSpecialIceWall(ServerWorld world, PlayerEntity user, int durationTicks) {
        int baseX = (int) Math.floor(user.getX());
        int baseY = (int) Math.floor(user.getY());
        int baseZ = (int) Math.floor(user.getZ());

        // Target platform height: +3 from ground. Player will stand at Y+4
        int platformY = baseY + 3;

        List<BlockPos> placed = new ArrayList<>();

        // Base platform 4x4 under feet (centered around player)
        for (int dx = -1; dx <= 2; dx++) {
            for (int dz = -1; dz <= 2; dz++) {
                BlockPos pos = new BlockPos(baseX + dx, platformY, baseZ + dz);
                if (!canPlaceIceBlock(world, pos))
                    continue;
                world.setBlockState(pos, Blocks.PACKED_ICE.getDefaultState());
                placed.add(pos);
            }
        }

        // Walls around: 3x3 ring with center hole at platformY+1 and platformY+2
        // (window to shoot)
        for (int h = 1; h <= 2; h++) {
            int y = platformY + h;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0)
                        continue; // hole in the middle
                    BlockPos pos = new BlockPos(baseX + dx, y, baseZ + dz);
                    if (!canPlaceIceBlock(world, pos))
                        continue;
                    world.setBlockState(pos, Blocks.PACKED_ICE.getDefaultState());
                    placed.add(pos);
                }
            }
        }

        if (!placed.isEmpty()) {
            ActiveEffectManager.addIceWall(world, placed, durationTicks);

            // Teleport player onto the platform. Feet should be just above platformY
            user.teleport(baseX + 0.5, platformY + 1.01, baseZ + 0.5, false);

            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f,
                    1.0f);
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.PLAYERS, 0.8f, 0.9f);

            for (int i = 0; i < 30; i++) {
                world.spawnParticles(ParticleTypes.SNOWFLAKE, baseX + 0.5, platformY + 1.5, baseZ + 0.5, 8, 2.0, 2.0,
                        2.0, 0.15);
            }
        }
    }

    /**
     * Creates regular ice wall (original implementation)
     */
    private static void createRegularIceWall(ServerWorld world, PlayerEntity user, int width, int height,
            int durationTicks) {
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
                if (!canPlaceIceBlock(world, pos))
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

    /**
     * Check if ice block can be placed at position
     */
    private static boolean canPlaceIceBlock(ServerWorld world, BlockPos pos) {
        if (!world.isAir(pos))
            return false;
        FluidState fluid = world.getFluidState(pos);
        if (!fluid.isEmpty())
            return false;
        Box blockBox = Box.from(Vec3d.ofCenter(pos)).expand(0.4);
        List<LivingEntity> colliders = world.getEntitiesByClass(LivingEntity.class, blockBox, e -> e.isAlive());
        return colliders.isEmpty();
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
