package com.manaborn.world;

import com.manaborn.ManabornMod;
import com.manaborn.entities.MageMerchantEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Обработчик для спавна маг-торговца в доме мага.
 * Использует проверку на серверном тике - спавнит мага когда игрок рядом со структурой.
 */
public class MageHouseStructureHandler {
    
    // Множество уже обработанных структур (чтобы не спавнить повторно)
    private static final Set<Long> processedChunks = new HashSet<>();
    
    // Проверяем каждые 2 секунды (40 тиков)
    private static int tickCounter = 0;
    private static final int CHECK_INTERVAL = 40;
    
    // ID структуры дома мага
    private static final Identifier MAGE_HOUSE_ID = Identifier.of("manaborn", "mage_house");
    
    public static void register() {
        // ОТКЛЮЧЕНО - вызывает зависание мира при проверке структур
        // Маг-торговец спавнится только через яйцо призыва: /give @s manaborn:mage_merchant_spawn_egg
        System.out.println("[Manaborn] MageHouseStructureHandler DISABLED (use spawn egg instead)");
    }
    
    /**
     * Проверяет, находится ли игрок рядом со структурой дома мага,
     * и спавнит маг-торговца если нужно.
     */
    private static void checkAndSpawnNearPlayer(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        BlockPos playerPos = player.getBlockPos();
        ChunkPos playerChunk = new ChunkPos(playerPos);
        
        // Проверяем чанки в радиусе 2 от игрока
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                ChunkPos checkChunk = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
                long chunkKey = checkChunk.toLong();
                
                // Пропускаем уже обработанные
                if (processedChunks.contains(chunkKey)) continue;
                
                // Проверяем, есть ли структура в этом чанке
                BlockPos structurePos = findMageHouseInChunk(world, checkChunk);
                if (structurePos != null) {
                    // Помечаем как обработанный
                    processedChunks.add(chunkKey);
                    
                    // Спавним мага
                    BlockPos spawnPos = findSpawnPosition(world, structurePos);
                    if (spawnPos != null) {
                        spawnMageMerchantAt(world, spawnPos);
                    }
                }
            }
        }
    }
    
    /**
     * Ищет структуру дома мага в чанке
     */
    private static BlockPos findMageHouseInChunk(ServerWorld world, ChunkPos chunkPos) {
        try {
            // Получаем все структуры в чанке
            var structureAccessor = world.getStructureAccessor();
            BlockPos chunkCenter = chunkPos.getStartPos().add(8, 64, 8);
            
            // Проверяем структуры в чанке через predicate
            var structures = structureAccessor.getStructureStarts(
                    chunkPos, 
                    structure -> {
                        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
                        RegistryKey<Structure> key = registry.getKey(structure).orElse(null);
                        return key != null && key.getValue().equals(MAGE_HOUSE_ID);
                    }
            );
            
            if (!structures.isEmpty()) {
                return chunkCenter;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Находит подходящую позицию для спавна (на твёрдом блоке)
     */
    private static BlockPos findSpawnPosition(ServerWorld world, BlockPos center) {
        // Ищем сверху вниз
        for (int y = 100; y > 40; y--) {
            BlockPos checkPos = new BlockPos(center.getX(), y, center.getZ());
            if (world.getBlockState(checkPos.down()).isSolidBlock(world, checkPos.down()) &&
                world.isAir(checkPos) && 
                world.isAir(checkPos.up())) {
                return checkPos;
            }
        }
        return null;
    }
    
    /**
     * Спавнит маг-торговца в указанной позиции
     */
    private static void spawnMageMerchantAt(ServerWorld world, BlockPos pos) {
        // Проверяем, нет ли уже маг-торговца в радиусе 32 блоков
        var existingMerchants = world.getEntitiesByClass(
                MageMerchantEntity.class,
                new net.minecraft.util.math.Box(pos).expand(32),
                e -> true
        );
        
        if (!existingMerchants.isEmpty()) {
            return; // Уже есть торговец
        }
        
        // Создаём и спавним маг-торговца
        MageMerchantEntity merchant = new MageMerchantEntity(ManabornMod.MAGE_MERCHANT_ENTITY, world);
        merchant.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        merchant.setPersistent(); // Не деспавнится
        world.spawnEntity(merchant);
        
        System.out.println("[Manaborn] Spawned Mage Merchant at " + pos + " (player nearby)");
    }
    
    /**
     * Очистка при выгрузке мира
     */
    public static void clearProcessedChunks() {
        processedChunks.clear();
    }
}
