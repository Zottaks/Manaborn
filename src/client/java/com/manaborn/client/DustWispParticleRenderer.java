package com.manaborn.client;

import com.manaborn.entities.DustWispEntity;
import com.manaborn.items.ManoscopeItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;
import net.minecraft.client.world.ClientWorld;

import java.util.List;

/**
 * Рендерит частицы вокруг пылинок, видимые только когда игрок носит Маноскоп
 */
public class DustWispParticleRenderer {
    
    private static final double PARTICLE_RADIUS = 320.0; // Радиус видимости пылинок (20 чанков)
    private static final int PARTICLES_PER_WISP = 1; // Количество частиц на пылинку за тик (меньше = труднее заметить)
    private static final double PARTICLE_SPAWN_CHANCE = 0.6; // 60% шанс появления частицы
    
    /**
     * Вызывается каждый клиентский тик для отрисовки частиц
     */
    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        
        // Проверяем, носит ли игрок Маноскоп
        if (!isWearingManoscope(client)) return;
        
        ClientWorld world = client.world;
        
        // Находим все пылинки рядом с игроком
        Box searchBox = client.player.getBoundingBox().expand(PARTICLE_RADIUS);
        List<DustWispEntity> wisps = world.getEntitiesByClass(
            DustWispEntity.class,
            searchBox,
            wisp -> true
        );
        
        // Рисуем частицы вокруг каждой пылинки
        for (DustWispEntity wisp : wisps) {
            spawnParticlesAroundWisp(world, wisp);
        }
    }
    
    /**
     * Проверяет, носит ли игрок Маноскоп
     */
    private static boolean isWearingManoscope(MinecraftClient client) {
        if (client.player == null) return false;
        return client.player.getEquippedStack(EquipmentSlot.HEAD).getItem() instanceof ManoscopeItem;
    }
    
    /**
     * Создает магические частицы вокруг пылинки
     */
    private static void spawnParticlesAroundWisp(ClientWorld world, DustWispEntity wisp) {
        // Не каждый тик спавним частицы
        if (world.random.nextDouble() > PARTICLE_SPAWN_CHANCE) return;
        
        for (int i = 0; i < PARTICLES_PER_WISP; i++) {
            // Случайное смещение вокруг пылинки (меньший радиус)
            double offsetX = (world.random.nextDouble() - 0.5) * 0.3;
            double offsetY = (world.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.3;
            
            double x = wisp.getX() + offsetX;
            double y = wisp.getY() + offsetY;
            double z = wisp.getZ() + offsetZ;
            
            // Очень медленное движение (едва заметные частицы)
            double velocityX = (world.random.nextDouble() - 0.5) * 0.01;
            double velocityY = 0.01 + world.random.nextDouble() * 0.01;
            double velocityZ = (world.random.nextDouble() - 0.5) * 0.01;
            
            // Используем только END_ROD частицы (меньше эффектов = труднее заметить)
            MinecraftClient.getInstance().particleManager.addParticle(
                ParticleTypes.END_ROD, x, y, z, velocityX, velocityY, velocityZ);
            
            // Очень редко добавляем PORTAL частицы
            if (world.random.nextFloat() < 0.1f) {
                MinecraftClient.getInstance().particleManager.addParticle(
                    ParticleTypes.PORTAL, x, y, z,
                    (world.random.nextDouble() - 0.5) * 0.05,
                    (world.random.nextDouble() - 0.5) * 0.05,
                    (world.random.nextDouble() - 0.5) * 0.05);
            }
        }
    }
}
