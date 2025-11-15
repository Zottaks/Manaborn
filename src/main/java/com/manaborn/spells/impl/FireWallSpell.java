package com.manaborn.spells.impl;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.ActiveEffectManager;
import com.manaborn.spells.SpellLevelManager;

/**
 * Fire Wall spell implementation with level-based scaling
 * 
 * Level 1 (слабый): 3x3, 7с, кд 15с, мана 30
 * Level 2 (средний): 4.5x3.5, 8.5с, кд 17.5с, мана 35
 * Level 3 (ОРИГИНАЛ): 6x4, 10с, кд 20с, мана 40
 */
public class FireWallSpell implements LeveledSpell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return true;
        }

        // Create fire wall 2 blocks in front of player
        Vec3d lookVec = user.getRotationVector();
        double distance = 2.0;
        double centerX = user.getX() + lookVec.x * distance;
        double centerY = user.getY();
        double centerZ = user.getZ() + lookVec.z * distance;

        // Calculate perpendicular direction for wall
        double perpX = -lookVec.z;
        double perpZ = lookVec.x;
        double length = Math.sqrt(perpX * perpX + perpZ * perpZ);
        if (length > 0) {
            perpX /= length;
            perpZ /= length;
        }

        int level = SpellLevelManager.getSpellLevel(user.getUuid(), SpellType.FIRE_WALL);
        
        // Level-based wall dimensions
        double wallWidth = switch (level) {
            case 1 -> 3.0;
            case 2 -> 4.5;
            default -> 6.0; // level 3 (original)
        };
        
        double wallHeight = switch (level) {
            case 1 -> 3.0;
            case 2 -> 3.5;
            default -> 4.0; // level 3 (original)
        };
        
        int cloudsHorizontal = switch (level) {
            case 1 -> 5;
            case 2 -> 7;
            default -> 9; // level 3 (original)
        };
        
        int cloudsVertical = switch (level) {
            case 1 -> 5;
            case 2 -> 5;
            default -> 6; // level 3 (original)
        };
        
        int duration = switch (level) {
            case 1 -> 140;  // 7 seconds
            case 2 -> 170;  // 8.5 seconds
            default -> 200; // 10 seconds (original)
        };

        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = centerY, maxY = centerY + wallHeight;
        double minZ = Double.MAX_VALUE, maxZ = Double.MIN_VALUE;

        double spacing = wallWidth / (cloudsHorizontal - 1);
        double vSpacing = wallHeight / (cloudsVertical - 1);

        // Create main grid of clouds
        for (int h = 0; h < cloudsHorizontal; h++) {
            double offset = (h - (cloudsHorizontal - 1) / 2.0) * spacing;
            double cloudX = centerX + perpX * offset;
            double cloudZ = centerZ + perpZ * offset;

            minX = Math.min(minX, cloudX - 0.5);
            maxX = Math.max(maxX, cloudX + 0.5);
            minZ = Math.min(minZ, cloudZ - 0.5);
            maxZ = Math.max(maxZ, cloudZ + 0.5);

            for (int v = 0; v < cloudsVertical; v++) {
                double cloudY = centerY + v * vSpacing;

                AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, cloudX, cloudY, cloudZ);
                cloud.setRadius(0.5f);
                cloud.setDuration(duration);
                cloud.setRadiusOnUse(0);
                cloud.setRadiusGrowth(0);
                cloud.setWaitTime(0);
                cloud.setParticleType(ParticleTypes.FLAME);

                world.spawnEntity(cloud);
            }
        }

        // Add staggered clouds to fill gaps
        for (int h = 0; h < cloudsHorizontal - 1; h++) {
            double offset = (h - (cloudsHorizontal - 1) / 2.0) * spacing + spacing / 2.0;
            double cloudX = centerX + perpX * offset;
            double cloudZ = centerZ + perpZ * offset;

            for (int v = 0; v < cloudsVertical - 1; v++) {
                double cloudY = centerY + v * vSpacing + vSpacing / 2.0;

                AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, cloudX, cloudY, cloudZ);
                cloud.setRadius(0.5f);
                cloud.setDuration(duration);
                cloud.setRadiusOnUse(0);
                cloud.setRadiusGrowth(0);
                cloud.setWaitTime(0);
                cloud.setParticleType(ParticleTypes.FLAME);

                world.spawnEntity(cloud);
            }
        }

        // Create damage box
        double damageThickness = 0.2;
        double thickMinX = minX - Math.abs(lookVec.x) * damageThickness;
        double thickMaxX = maxX + Math.abs(lookVec.x) * damageThickness;
        double thickMinZ = minZ - Math.abs(lookVec.z) * damageThickness;
        double thickMaxZ = maxZ + Math.abs(lookVec.z) * damageThickness;
        double forwardOffset = 0.6;
        thickMinX += lookVec.x * forwardOffset;
        thickMaxX += lookVec.x * forwardOffset;
        thickMinZ += lookVec.z * forwardOffset;
        thickMaxZ += lookVec.z * forwardOffset;

        Box wallBox = new Box(thickMinX, minY, thickMinZ, thickMaxX, maxY, thickMaxZ);
        ActiveEffectManager.addFireWall(wallBox, serverWorld, 200);

        return true;
    }

    @Override
    public int getManaCost() {
        return 30; // Level 1 cost
    }
    
    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 30;
            case 2 -> 35;
            default -> 40; // level 3 (original)
        };
    }

    @Override
    public int getCooldown() {
        return 300; // Level 1: 15 seconds
    }
    
    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 300; // 15 seconds
            case 2 -> 350; // 17.5 seconds
            default -> 400; // 20 seconds (level 3, original)
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.FIRE_WALL;
    }

    @Override
    public boolean hasActiveEffects() {
        return true;
    }
}
