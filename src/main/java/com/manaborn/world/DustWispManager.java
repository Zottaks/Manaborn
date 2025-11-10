package com.manaborn.world;

import com.manaborn.entities.DustWispEntity;
import com.manaborn.ManabornMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Random;

public class DustWispManager {
    // АГРЕССИВНЫЙ спавн для теста (может быть очень много пыли)
    public static final int MAX_WISPS_NEAR_PLAYER = 20; // было 25
    public static final int SCAN_RADIUS = 320; // 20 чанков (16 * 20)
    public static final double SPAWN_CHANCE_PER_TICK = 0.30; // было 0.55
    public static final int MIN_WISPS_PER_SPAWN = 3; // было 1
    public static final int MAX_WISPS_PER_SPAWN = 7; // было 3
    public static final int CLUSTER_RADIUS = 8; // было 4

    private static final Random RANDOM = new Random();

    public static void tick(Iterable<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            if (!(player.getEntityWorld() instanceof ServerWorld world)) continue;

            // Cap wisps near player
            int existing = world.getEntitiesByClass(DustWispEntity.class,
                    player.getBoundingBox().expand(SCAN_RADIUS), e -> true).size();
            if (existing >= MAX_WISPS_NEAR_PLAYER) continue;

            if (RANDOM.nextDouble() < SPAWN_CHANCE_PER_TICK) {
                // Спавним группу пылинок
                int wispCount = MIN_WISPS_PER_SPAWN + RANDOM.nextInt(MAX_WISPS_PER_SPAWN - MIN_WISPS_PER_SPAWN + 1);

                // Центр группы случайно в радиусе 20 чанков
                int centerDx = RANDOM.nextInt(SCAN_RADIUS * 2 + 1) - SCAN_RADIUS;
                int centerDz = RANDOM.nextInt(SCAN_RADIUS * 2 + 1) - SCAN_RADIUS;
                int centerX = (int) Math.floor(player.getX()) + centerDx;
                int centerZ = (int) Math.floor(player.getZ()) + centerDz;
                
                // Спавним пылинки группой
                for (int i = 0; i < wispCount; i++) {
                    // Смещение внутри группы
                    int offsetX = RANDOM.nextInt(CLUSTER_RADIUS * 2 + 1) - CLUSTER_RADIUS;
                    int offsetZ = RANDOM.nextInt(CLUSTER_RADIUS * 2 + 1) - CLUSTER_RADIUS;
                    int x = centerX + offsetX;
                    int z = centerZ + offsetZ;
                    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos pos = new BlockPos(x, y, z);

                    // Проверка поверхности: воздух сверху и твердый блок снизу
                    if (!world.isAir(pos) || world.isAir(pos.down())) continue;

                    DustWispEntity wisp = new DustWispEntity(ManabornMod.DUST_WISP_ENTITY, world);
                    wisp.refreshPositionAndAngles(x + 0.5, y + 0.1, z + 0.5, 0, 0);
                    world.spawnEntity(wisp);
                }
            }
        }
    }
}
