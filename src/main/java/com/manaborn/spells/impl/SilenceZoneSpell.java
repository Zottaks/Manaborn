package com.manaborn.spells.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellType;
import com.manaborn.spells.SpellLevelManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Silence Zone spell - Область тишины
 * Создает зону, в которой блокируются все заклинания (включая заклинания кастующего)
 * 
 * Стоимость: 80 маны
 * Кулдаун: 70 секунд
 * 
 * Level 1: 7 секунд, радиус 5 блоков
 * Level 2: 8.5 секунд, радиус 6 блоков
 * Level 3: 10 секунд, радиус 7 блоков
 */
public class SilenceZoneSpell implements LeveledSpell {

    // Активные зоны тишины: позиция центра -> данные зоны
    private static final Map<BlockPos, SilenceZoneData> activeZones = new ConcurrentHashMap<>();
    
    // Данные о зоне тишины
    public static class SilenceZoneData {
        public final Vec3d center;
        public final double radius;
        public final long endTick;
        public final UUID casterUuid;
        
        public SilenceZoneData(Vec3d center, double radius, long endTick, UUID casterUuid) {
            this.center = center;
            this.radius = radius;
            this.endTick = endTick;
            this.casterUuid = casterUuid;
        }
    }

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }
        
        // Проверяем, есть ли уже активная зона у этого игрока - если да, отменяем её
        if (cancelPlayerZone(serverPlayer, world)) {
            return false; // Не тратим ману и кулдаун при отмене
        }

        int level = SpellLevelManager.getSpellLevel(user.getUuid(), SpellType.SILENCE_ZONE);
        
        // Параметры в зависимости от уровня
        int durationTicks = switch (level) {
            case 1 -> 140;   // 7 секунд
            case 2 -> 170;   // 8.5 секунд
            default -> 200;  // 10 секунд (level 3)
        };
        
        double radius = switch (level) {
            case 1 -> 5.0;
            case 2 -> 6.0;
            default -> 7.0; // level 3
        };

        // Позиция зоны - перед игроком
        Vec3d lookVec = user.getRotationVector();
        Vec3d zoneCenter = new Vec3d(user.getX(), user.getY(), user.getZ()).add(lookVec.multiply(3.0)).add(0, 0.5, 0);
        BlockPos zonePos = BlockPos.ofFloored(zoneCenter);
        
        // Создаем зону тишины
        long endTick = world.getTime() + durationTicks;
        activeZones.put(zonePos, new SilenceZoneData(zoneCenter, radius, endTick, user.getUuid()));
        
        // Визуальные эффекты создания зоны
        if (world instanceof ServerWorld serverWorld) {
            spawnCreationParticles(serverWorld, zoneCenter, radius);
        }
        
        // Звуковой эффект - глухой звук
        world.playSound(
            user,
            user.getBlockPos(),
            SoundEvents.BLOCK_BEACON_DEACTIVATE,
            SoundCategory.PLAYERS,
            1.5f,
            0.3f
        );
        
        // Второй звук - эхо
        world.playSound(
            user,
            zonePos,
            SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
            SoundCategory.PLAYERS,
            1.0f,
            0.5f
        );
        
        serverPlayer.sendMessage(Text.literal("§7☽ Область тишины создана! Длительность: " + 
            String.format("%.1f", durationTicks / 20.0) + " сек"), true);

        return true;
    }

    /**
     * Проверяет, находится ли позиция в зоне тишины
     * @param pos позиция для проверки
     * @param currentTick текущий тик мира
     * @return true если позиция в зоне тишины
     */
    public static boolean isInSilenceZone(Vec3d pos, long currentTick) {
        for (Map.Entry<BlockPos, SilenceZoneData> entry : activeZones.entrySet()) {
            SilenceZoneData zone = entry.getValue();
            
            // Проверяем не истекла ли зона
            if (currentTick > zone.endTick) {
                continue;
            }
            
            // Проверяем расстояние до центра зоны
            double distance = pos.distanceTo(zone.center);
            if (distance <= zone.radius) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Проверяет, находится ли игрок в зоне тишины
     */
    public static boolean isPlayerInSilenceZone(PlayerEntity player, World world) {
        return isInSilenceZone(new Vec3d(player.getX(), player.getY(), player.getZ()), world.getTime());
    }
    
    /**
     * Очищает истекшие зоны тишины
     * Вызывать каждый тик сервера
     */
    public static void tickSilenceZones(ServerWorld world) {
        long currentTick = world.getTime();
        
        activeZones.entrySet().removeIf(entry -> {
            SilenceZoneData zone = entry.getValue();
            
            if (currentTick > zone.endTick) {
                // Зона истекла - спавним частицы исчезновения
                spawnDissipateParticles(world, zone.center, zone.radius);
                
                // Звук исчезновения
                world.playSound(
                    null,
                    BlockPos.ofFloored(zone.center),
                    SoundEvents.BLOCK_BEACON_ACTIVATE,
                    SoundCategory.BLOCKS,
                    0.8f,
                    1.5f
                );
                
                return true; // Удаляем зону
            }
            
            // Зона активна - спавним частицы границы
            if (currentTick % 10 == 0) { // Каждые 0.5 секунды
                spawnBoundaryParticles(world, zone.center, zone.radius);
            }
            
            return false;
        });
    }
    
    /**
     * Частицы при создании зоны
     */
    private static void spawnCreationParticles(ServerWorld world, Vec3d center, double radius) {
        // Кольцо частиц на границе
        for (int i = 0; i < 60; i++) {
            double angle = (2 * Math.PI * i) / 60;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            
            world.spawnParticles(
                ParticleTypes.SMOKE,
                x, center.y, z,
                3,
                0.1, 0.3, 0.1,
                0.02
            );
            
            world.spawnParticles(
                ParticleTypes.ASH,
                x, center.y + 0.5, z,
                5,
                0.2, 0.5, 0.2,
                0.01
            );
        }
        
        // Центральный столб
        for (int y = 0; y < 10; y++) {
            world.spawnParticles(
                ParticleTypes.LARGE_SMOKE,
                center.x, center.y + y * 0.3, center.z,
                2,
                0.3, 0.1, 0.3,
                0.01
            );
        }
    }
    
    /**
     * Частицы границы зоны (периодические)
     */
    private static void spawnBoundaryParticles(ServerWorld world, Vec3d center, double radius) {
        // Редкие частицы на границе
        for (int i = 0; i < 12; i++) {
            double angle = (2 * Math.PI * i) / 12 + (world.getTime() * 0.05);
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            
            world.spawnParticles(
                ParticleTypes.SMOKE,
                x, center.y + 0.5, z,
                1,
                0.05, 0.2, 0.05,
                0.0
            );
        }
        
        // Частицы внутри зоны
        world.spawnParticles(
            ParticleTypes.ASH,
            center.x + (world.random.nextDouble() - 0.5) * radius * 2,
            center.y + world.random.nextDouble() * 2,
            center.z + (world.random.nextDouble() - 0.5) * radius * 2,
            2,
            0.1, 0.1, 0.1,
            0.0
        );
    }
    
    /**
     * Частицы при исчезновении зоны
     */
    private static void spawnDissipateParticles(ServerWorld world, Vec3d center, double radius) {
        // Взрыв частиц наружу
        for (int i = 0; i < 30; i++) {
            double angle = (2 * Math.PI * i) / 30;
            double x = center.x + radius * Math.cos(angle);
            double z = center.z + radius * Math.sin(angle);
            
            world.spawnParticles(
                ParticleTypes.POOF,
                x, center.y + 1, z,
                3,
                0.2, 0.3, 0.2,
                0.05
            );
        }
    }
    
    /**
     * Получить все активные зоны (для отладки)
     */
    public static Map<BlockPos, SilenceZoneData> getActiveZones() {
        return activeZones;
    }
    
    /**
     * Очистить все зоны (при выгрузке мира)
     */
    public static void clearAllZones() {
        activeZones.clear();
    }
    
    /**
     * Отменяет зону тишины игрока (если она существует)
     * @return true если зона была отменена
     */
    public static boolean cancelPlayerZone(ServerPlayerEntity player, World world) {
        UUID playerId = player.getUuid();
        
        for (Map.Entry<BlockPos, SilenceZoneData> entry : activeZones.entrySet()) {
            SilenceZoneData zone = entry.getValue();
            
            if (zone.casterUuid.equals(playerId)) {
                // Нашли зону этого игрока - удаляем
                BlockPos zonePos = entry.getKey();
                activeZones.remove(zonePos);
                
                // Эффекты отмены
                if (world instanceof ServerWorld serverWorld) {
                    spawnDissipateParticles(serverWorld, zone.center, zone.radius);
                    
                    serverWorld.playSound(
                        null,
                        BlockPos.ofFloored(zone.center),
                        SoundEvents.BLOCK_BEACON_ACTIVATE,
                        SoundCategory.BLOCKS,
                        0.8f,
                        1.8f
                    );
                }
                
                player.sendMessage(Text.literal("§7☽ Область тишины отменена!"), true);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Проверяет, есть ли у игрока активная зона тишины
     */
    public static boolean hasActiveZone(UUID playerId) {
        for (SilenceZoneData zone : activeZones.values()) {
            if (zone.casterUuid.equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getManaCost() {
        return 80;
    }
    
    @Override
    public int getManaCost(int level) {
        return 80; // Одинаковая стоимость на всех уровнях
    }

    @Override
    public int getCooldown() {
        return 1400; // 70 секунд
    }
    
    @Override
    public int getCooldown(int level) {
        return 1400; // 70 секунд на всех уровнях
    }

    @Override
    public SpellType getType() {
        return SpellType.SILENCE_ZONE;
    }
}
