package com.manaborn.world;

import com.manaborn.ManabornMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Обработчик для замены книг заклинаний в бочках дома мага
 */
public class MageHouseStructureHandler {
    
    // Множество уже обработанных бочек (чтобы не обрабатывать повторно)
    private static final Set<BlockPos> processedBarrels = new HashSet<>();
    private static final Random random = new Random();
    
    public static void register() {
        // ВРЕМЕННО ОТКЛЮЧЕНО - вызывает зависание при загрузке чанков
        // TODO: Переделать на асинхронную обработку или использовать другой подход
        /*
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            processChunk(world, chunk);
        });
        */
        System.out.println("[Manaborn] MageHouseStructureHandler disabled (causes chunk loading issues)");
    }
    
    private static void processChunk(ServerWorld world, WorldChunk chunk) {
        // Проверяем все block entities в чанке
        for (BlockPos pos : chunk.getBlockEntityPositions()) {
            BlockEntity blockEntity = chunk.getBlockEntity(pos);
            
            if (blockEntity instanceof BarrelBlockEntity barrel) {
                processBarrel(barrel, world, pos);
            }
        }
    }
    
    private static void processBarrel(BarrelBlockEntity barrel, ServerWorld world, BlockPos pos) {
        // Проверяем, не обрабатывали ли уже эту бочку
        BlockPos globalPos = pos.toImmutable();
        if (processedBarrels.contains(globalPos)) {
            return;
        }
        
        // Ищем книгу огненного шара в бочке
        boolean hasFireballBook = false;
        int fireballSlot = -1;
        
        for (int i = 0; i < barrel.size(); i++) {
            ItemStack stack = barrel.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == ManabornMod.FIREBALL_SPELL_BOOK) {
                hasFireballBook = true;
                fireballSlot = i;
                break;
            }
        }
        
        // Если нашли книгу огненного шара - это бочка из дома мага
        if (hasFireballBook && fireballSlot >= 0) {
            // Выбираем случайную книгу
            Item newBook = getRandomSpellBook();
            
            // Заменяем книгу
            barrel.setStack(fireballSlot, new ItemStack(newBook));
            barrel.markDirty();
            
            // Помечаем бочку как обработанную
            processedBarrels.add(globalPos);
            
            System.out.println("[Manaborn] Replaced spell book in barrel at " + pos + " with " + newBook);
        }
    }
    
    /**
     * Возвращает случайную книгу заклинания с заданными шансами:
     * - Fireball: 40%
     * - Ice Shard: 40%
     * - Battle Hymn: 20%
     */
    private static Item getRandomSpellBook() {
        int roll = random.nextInt(100);
        
        if (roll < 40) {
            return ManabornMod.FIREBALL_SPELL_BOOK;
        } else if (roll < 80) {
            return ManabornMod.ICE_SHARD_SPELL_BOOK;
        } else {
            return ManabornMod.BATTLE_HYMN_SPELL_BOOK;
        }
    }
    
    /**
     * Очистка при выгрузке мира
     */
    public static void clearProcessedBarrels() {
        processedBarrels.clear();
    }
}
