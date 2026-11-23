package com.manaborn.spells;

import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.manaborn.network.LearnedSpellsSyncS2CPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.nio.file.*;

/**
 * Управляет изученными заклинаниями
 * 
 * ВАЖНО: Для удобства отладки в dev окружении все заклинания хранятся глобально
 * и доступны всем игрокам. Это решает проблему с изменяющимся UUID в dev режиме.
 */
public class LearnedSpellsManager {
    // Глобальный набор изученных заклинаний (доступен всем игрокам)
    private static final Set<SpellType> globalLearnedSpells = ConcurrentHashMap.newKeySet();
    
    // Путь к файлу сохранения
    private static final String SAVE_FILE = "manaborn_data/global_spells.txt";
    private static boolean loaded = false;

    /**
     * Инициализация не требуется, так как используется глобальное хранилище
     */
    public static void ensureInitialized(ServerPlayerEntity player) {
        // Ничего не делаем - глобальное хранилище всегда готово
    }
    
    /**
     * Загружает изученные заклинания из файла при первом вызове
     */
    public static void loadFromDisk() {
        if (loaded) return;
        loaded = true;
        
        try {
            Path path = Paths.get(SAVE_FILE);
            if (!Files.exists(path)) {
                System.out.println("[Manaborn] Файл сохранения заклинаний не найден, начинаем с пустого списка");
                return;
            }
            
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    SpellType spell = SpellType.fromId(line);
                    globalLearnedSpells.add(spell);
                }
            }
            
            System.out.println("[Manaborn] Загружено " + globalLearnedSpells.size() + " изученных заклинаний из файла");
        } catch (IOException e) {
            System.err.println("[Manaborn] Ошибка при загрузке заклинаний: " + e.getMessage());
        }
    }
    
    /**
     * Сохраняет изученные заклинания в файл
     */
    public static void saveToDisk() {
        try {
            Path path = Paths.get(SAVE_FILE);
            
            // Создаём директорию если не существует
            Files.createDirectories(path.getParent());
            
            // Записываем ID заклинаний построчно
            List<String> lines = globalLearnedSpells.stream()
                .map(SpellType::getId)
                .sorted()
                .toList();
            
            Files.write(path, lines);
            
            System.out.println("[Manaborn] Сохранено " + lines.size() + " заклинаний в файл");
        } catch (IOException e) {
            System.err.println("[Manaborn] Ошибка при сохранении заклинаний: " + e.getMessage());
        }
    }

    /**
     * Проверяет, изучено ли заклинание (глобально)
     * UUID игрока игнорируется - все заклинания доступны всем
     */
    public static boolean hasLearned(UUID playerId, SpellType spell) {
        return globalLearnedSpells.contains(spell);
    }

    /**
     * Изучает заклинание глобально (для всех игроков)
     */
    public static boolean learnSpell(ServerPlayerEntity player, SpellType spell) {
        boolean learned = globalLearnedSpells.add(spell);
        if (learned) {
            // Новое заклинание всегда изучается с 1 уровня —
            // сбрасываем возможные устаревшие данные из файла сохранения
            SpellLevelManager.setSpellLevel(player.getUuid(), spell, 1);

            // Сохраняем на диск
            saveToDisk();

            // Отправляем обновление текущему игроку
            // Другие игроки получат обновление при входе
            sendSync(player);
        }
        return learned;
    }

    /**
     * Получает все изученные заклинания (глобально)
     * UUID игрока игнорируется
     */
    public static Set<SpellType> getLearnedSpells(UUID playerId) {
        return new HashSet<>(globalLearnedSpells);
    }

    /**
     * Отправляет синхронизацию одному игроку
     */
    public static void sendSync(ServerPlayerEntity player) {
        List<String> spellIds = globalLearnedSpells.stream()
            .map(SpellType::getId)
            .toList();
        ServerPlayNetworking.send(player, new LearnedSpellsSyncS2CPacket(spellIds));
    }

    /**
     * Очистка не требуется - глобальное хранилище сохраняется
     * Метод оставлен для совместимости
     */
    public static void clearPlayer(UUID playerId) {
        // Ничего не делаем - глобальное хранилище не очищается
    }
    
    /**
     * Полная очистка всех изученных заклинаний (для отладки)
     */
    public static void clearAll() {
        globalLearnedSpells.clear();
    }
}
