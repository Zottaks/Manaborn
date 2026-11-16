package com.manaborn.spells.impl;

import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellLevelManager;
import com.manaborn.spells.SpellType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Battle Hymn - поддерживающее заклинание звуковой магии
 * 
 * Механика:
 * - Игрок должен стоять на месте и "петь" определённое время
 * - После завершения все игроки в радиусе получают бафф Силы
 * - Во время каста игрок получает повышенный урон и не может двигаться
 * - Проигрывается музыкальная мелодия
 * 
 * Уровни:
 * Level 1: 7с каст, 3 блока радиус, Сила I 10с, 30 маны, 60с кд
 * Level 2: 6с каст, 4 блока радиус, Сила I 20с, 35 маны, 55с кд
 * Level 3: 5с каст, 5 блоков радиус, Сила II 15с, 50 маны, 65с кд
 */
public class BattleHymnSpell implements LeveledSpell {
    
    // Хранилище активных кастов
    private static final Map<UUID, ChannelData> activeChannels = new HashMap<>();
    
    private static class ChannelData {
        long startTime;
        int level;
        double startX, startY, startZ;
        int noteCount = 0; // Счётчик проигранных нот
        
        ChannelData(long startTime, int level, double x, double y, double z) {
            this.startTime = startTime;
            this.level = level;
            this.startX = x;
            this.startY = y;
            this.startZ = z;
        }
    }
    
    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return true;
        }
        
        UUID playerId = user.getUuid();
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Если уже кастуем - проверяем прогресс
        if (activeChannels.containsKey(playerId)) {
            return updateChanneling(serverPlayer, serverWorld);
        }
        
        // Начинаем новый каст
        return startChanneling(serverPlayer, serverWorld);
    }
    
    private boolean startChanneling(ServerPlayerEntity player, ServerWorld world) {
        UUID playerId = player.getUuid();
        int level = getPlayerLevel(player);
        
        // Записываем начало каста
        long startTime = world.getTime();
        ChannelData data = new ChannelData(startTime, level, player.getX(), player.getY(), player.getZ());
        activeChannels.put(playerId, data);
        
        // Накладываем дебафф "Уязвимость" на время каста (скрытый от UI)
        // Weakness IV + Unluck для увеличения получаемого урона
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.WEAKNESS, 
            getChannelDuration(level) + 20, 
            3, // Уровень 4 - очень сильная уязвимость
            false,  // ambient = false
            false,  // showParticles = false
            false   // showIcon = false (скрыто в инвентаре)
        ));
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.UNLUCK, 
            getChannelDuration(level) + 20, 
            2, // Неудача III - дополнительный урон
            false,  // ambient = false
            false,  // showParticles = false
            false   // showIcon = false
        ));
        
        // Первая нота
        playNote(world, player, 0);
        
        player.sendMessage(Text.literal("§e♪ Начинаю боевой гимн..."), true);
        return true;
    }
    
    private boolean updateChanneling(ServerPlayerEntity player, ServerWorld world) {
        UUID playerId = player.getUuid();
        ChannelData data = activeChannels.get(playerId);
        
        if (data == null) {
            return false;
        }
        
        long currentTime = world.getTime();
        long elapsed = currentTime - data.startTime;
        int channelDuration = getChannelDuration(data.level);
        
        // Проверка движения - если сдвинулся, прерываем каст
        double dx = Math.abs(player.getX() - data.startX);
        double dy = Math.abs(player.getY() - data.startY);
        double dz = Math.abs(player.getZ() - data.startZ);
        
        if (dx > 0.1 || dy > 0.1 || dz > 0.1) {
            cancelChanneling(player, "§cВы сдвинулись! Боевой гимн прерван.");
            return false;
        }
        
        // Проигрываем ноты с РАЗНЫМ РИТМОМ для каждого уровня
        int noteTempo = getNoteTempoTicks(data.level);
        if (elapsed % noteTempo == 0 && elapsed > 0) {
            int noteIndex = (int) (elapsed / noteTempo);
            playNote(world, player, noteIndex);
            data.noteCount = noteIndex;
        }
        
        // Визуализация радиуса каждые 10 тиков (0.5 секунды)
        if (elapsed % 10 == 0) {
            spawnRadiusParticles(world, player, getRadius(data.level));
        }
        
        // Проверка завершения каста
        if (elapsed >= channelDuration) {
            completeChanneling(player, world, data);
            return true;
        }
        
        // Показываем прогресс с визуальным индикатором
        int secondsLeft = (channelDuration - (int) elapsed) / 20;
        int progress = (int) ((elapsed * 100) / channelDuration);
        String progressBar = createProgressBar(progress);
        player.sendMessage(Text.literal("§e♪ Боевой гимн " + progressBar + " §e" + secondsLeft + "с"), true);
        
        return false; // Не снимаем кулдаун, пока не закончится каст
    }
    
    private void completeChanneling(ServerPlayerEntity caster, ServerWorld world, ChannelData data) {
        UUID casterId = caster.getUuid();
        
        // Финальный аккорд
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 
            SoundCategory.PLAYERS, 2.0f, 2.0f);
        world.playSound(null, caster.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 
            SoundCategory.PLAYERS, 2.0f, 1.5f);
        
        // Определяем параметры баффа
        int radius = getRadius(data.level);
        int strengthAmplifier = data.level >= 3 ? 1 : 0; // Сила I или II
        int buffDuration = getBuffDuration(data.level);
        
        // Находим всех игроков в радиусе
        double casterX = caster.getX();
        double casterY = caster.getY();
        double casterZ = caster.getZ();
        Box searchBox = new Box(casterX - radius, casterY - radius, casterZ - radius,
                                casterX + radius, casterY + radius, casterZ + radius);
        int buffedCount = 0;
        
        for (PlayerEntity player : world.getPlayers()) {
            double playerX = player.getX();
            double playerY = player.getY();
            double playerZ = player.getZ();
            
            if (searchBox.contains(playerX, playerY, playerZ)) {
                double dx = playerX - casterX;
                double dy = playerY - casterY;
                double dz = playerZ - casterZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                
                if (distance <= radius) {
                    // Кастер всегда получает Силу I на 10 секунд (слабее чем союзники)
                    boolean isCaster = player.getUuid().equals(casterId);
                    int finalAmplifier = isCaster ? 0 : strengthAmplifier; // Кастер всегда Сила I
                    int finalDuration = isCaster ? 200 : buffDuration; // Кастер всегда 10 секунд
                    
                    // Накладываем бафф
                    player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.STRENGTH,
                        finalDuration,
                        finalAmplifier,
                        false,
                        true,
                        true
                    ));
                    
                    // Визуальный эффект
                    if (world instanceof ServerWorld serverWorld) {
                        spawnBuffParticles(serverWorld, player);
                    }
                    
                    buffedCount++;
                    
                    if (player != caster) {
                        player.sendMessage(Text.literal("§e♪ Боевой гимн вдохновляет вас!"), false);
                    }
                }
            }
        }
        
        // Удаляем дебаффы с кастера
        caster.removeStatusEffect(StatusEffects.WEAKNESS);
        caster.removeStatusEffect(StatusEffects.UNLUCK);
        
        // Убираем из активных кастов
        activeChannels.remove(casterId);
        
        String strengthLevel = strengthAmplifier == 0 ? "I" : "II";
        caster.sendMessage(Text.literal("§a♪ Боевой гимн завершён! Усилено игроков: " + buffedCount + 
            " (Сила " + strengthLevel + ")"), false);
    }
    
    private void cancelChanneling(ServerPlayerEntity player, String reason) {
        UUID playerId = player.getUuid();
        activeChannels.remove(playerId);
        player.removeStatusEffect(StatusEffects.WEAKNESS);
        player.removeStatusEffect(StatusEffects.UNLUCK);
        player.sendMessage(Text.literal(reason), true);
    }
    
    /**
     * Проигрывает музыкальную ноту (часть мелодии)
     * 3 УНИКАЛЬНЫЕ МЕЛОДИИ для каждого уровня!
     */
    private void playNote(ServerWorld world, PlayerEntity player, int noteIndex) {
        UUID playerId = player.getUuid();
        ChannelData data = activeChannels.get(playerId);
        if (data == null) return;
        
        int level = data.level;
        
        // Нотация: 0.5=C3, 0.53=C#3, 0.56=D3, 0.6=D#3, 0.63=E3, 0.67=F3, 0.7=F#3, 0.75=G3, 0.8=G#3, 0.84=A3, 0.9=A#3, 0.94=B3, 1.0=C4
        float[] melodyPitches;
        float[] bassNotes;
        
        if (level == 1) {
            // УРОВЕНЬ 1: ПРОСТОЙ ПРИЗЫВ (5 секунд)
            // РИТМ: Быстрый, прямой (как боевой клич)
            // ХАРАКТЕР: Агрессивный, повторяющийся
            melodyPitches = new float[] {
                1.0f,  // C4 - Вперёд!
                1.0f,  // C4 - Вперёд! (повтор - маршевый ритм)
                1.19f, // D4 - К бою!
                1.0f,  // C4 - Атака!
                0.84f  // A3 - Финал (резкое окончание)
            };
            
            bassNotes = new float[] {
                0.5f,  // C3 - БУМ
                0.5f,  // C3 - БУМ (повтор)
                0.56f, // D3 - БАМ
                0.5f,  // C3 - БУМ
                0.5f   // C3 - БУМ (акцент на C)
            };
            
        } else if (level == 2) {
            // УРОВЕНЬ 2: ВОЛНООБРАЗНЫЙ МАРШ (7 секунд)
            // РИТМ: Волнами вверх-вниз (динамичный)
            // ХАРАКТЕР: Героический, с подъёмами и спадами
            melodyPitches = new float[] {
                1.0f,  // C4 - Начало
                1.33f, // E4 - Подъём!
                1.19f, // D4 - Спад
                1.5f,  // G4 - Выше!
                1.33f, // E4 - Спад
                1.68f, // A4 - КУЛЬМИНАЦИЯ!
                1.0f   // C4 - Возврат (завершение круга)
            };
            
            bassNotes = new float[] {
                0.5f,  // C3 - БУМ
                0.63f, // E3 - БАМ (следуем за мелодией)
                0.56f, // D3 - БУМ
                0.75f, // G3 - БАМ
                0.63f, // E3 - БУМ
                0.84f, // A3 - МОЩЬ!
                0.5f   // C3 - БУМ (возврат)
            };
            
        } else {
            // УРОВЕНЬ 3: ЭПИЧЕСКОЕ ВОСХОЖДЕНИЕ (9 секунд)
            // РИТМ: Непрерывный подъём (как восхождение на вершину)
            // ХАРАКТЕР: Величественный, постоянно нарастающий
            melodyPitches = new float[] {
                0.75f, // G3 - Начало пути (низкое начало)
                0.84f, // A3 - Шаг вверх
                1.0f,  // C4 - Продолжаем
                1.19f, // D4 - Выше!
                1.33f, // E4 - Ещё выше!
                1.5f,  // G4 - Почти!
                1.68f, // A4 - Близко!
                1.89f, // B4 - На вершине!
                2.0f   // C5 - ЛЕГЕНДА!!! (высшая точка)
            };
            
            bassNotes = new float[] {
                0.5f,  // C3 - БУМ (основа)
                0.5f,  // C3 - БУМ
                0.5f,  // C3 - БУМ (постоянная основа)
                0.56f, // D3 - БАМ
                0.63f, // E3 - БАМ (начинаем подъём)
                0.75f, // G3 - БУМ
                0.84f, // A3 - БАМ (усиление)
                0.94f, // B3 - МОЩЬ!
                1.0f   // C4 - ТРИУМФ!!! (кульминация)
            };
        }
        
        // Получаем ноты
        float mainPitch = melodyPitches[noteIndex % melodyPitches.length];
        float bassPitch = bassNotes[noteIndex % bassNotes.length];
        
        // РАЗНЫЕ ИНСТРУМЕНТЫ для каждого уровня!
        if (level == 1) {
            // LEVEL 1: АГРЕССИВНЫЙ БОЕВОЙ КЛИЧ
            // Инструменты: Барабаны + Тарелки + Бас (военный, резкий)
            
            // Главный барабан - агрессивный ритм
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 
                SoundCategory.PLAYERS, 3.0f, bassPitch); // Очень громкий!
            
            // Тарелки на каждую ноту - быстрый ритм
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 
                SoundCategory.PLAYERS, 1.5f, 1.2f);
            
            // Снейр-барабан для агрессии
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(), 
                SoundCategory.PLAYERS, 2.0f, mainPitch);
            
            // Колокол на каждую 2-ю ноту
            if (noteIndex % 2 == 0) {
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 
                    SoundCategory.PLAYERS, 1.0f, mainPitch);
            }
            
        } else if (level == 2) {
            // LEVEL 2: ГЕРОИЧЕСКИЙ МАРШ
            // Инструменты: Арфа + Флейта + Барабаны (классический героический)
            
            // Арфа - главная мелодия
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 
                SoundCategory.PLAYERS, 2.0f, mainPitch);
            
            // Флейта - гармония
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(), 
                SoundCategory.PLAYERS, 1.5f, mainPitch * 1.5f); // Выше основной ноты
            
            // Барабаны - ритм
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 
                SoundCategory.PLAYERS, 2.0f, bassPitch);
            
            // Тарелки
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 
                SoundCategory.PLAYERS, 0.8f, 1.0f);
            
            // Колокола на сильных долях
            if (noteIndex % 2 == 0) {
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 
                    SoundCategory.PLAYERS, 1.5f, mainPitch * 0.5f);
            }
            
        } else {
            // LEVEL 3: ЭПИЧЕСКАЯ СИМФОНИЯ
            // Инструменты: Куранты + Колокола + Арфа + Флейта (величественный)
            
            // Куранты - главный величественный звук
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), 
                SoundCategory.PLAYERS, 2.5f, mainPitch);
            
            // Колокола - эпический аккомпанемент
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 
                SoundCategory.PLAYERS, 2.0f, mainPitch * 0.5f);
            
            // Арфа - мягкая гармония
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), 
                SoundCategory.PLAYERS, 1.5f, mainPitch);
            
            // Флейта - высокая гармония
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(), 
                SoundCategory.PLAYERS, 1.2f, mainPitch * 2.0f); // Октава выше
            
            // Глубокий бас - основа (медленный ритм)
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 
                SoundCategory.PLAYERS, 1.5f, bassPitch * 0.5f); // Ниже обычного
            
            // Ксилофон на каждую 3-ю ноту - магический эффект
            if (noteIndex % 3 == 0) {
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE.value(), 
                    SoundCategory.PLAYERS, 1.0f, mainPitch * 1.5f);
            }
        }
    }
    
    /**
     * Визуальные частицы при получении баффа
     */
    private void spawnBuffParticles(ServerWorld world, PlayerEntity player) {
        // Золотые нотные частицы вокруг игрока
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * Math.PI * 2;
            double radius = 0.5;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            world.spawnParticles(
                net.minecraft.particle.ParticleTypes.NOTE,
                player.getX() + offsetX,
                player.getY() + 1.0,
                player.getZ() + offsetZ,
                1,
                0, 0.2, 0,
                0.1
            );
            
            world.spawnParticles(
                net.minecraft.particle.ParticleTypes.END_ROD,
                player.getX() + offsetX,
                player.getY() + 0.5,
                player.getZ() + offsetZ,
                1,
                0, 0.1, 0,
                0.05
            );
        }
    }
    
    /**
     * Обновление всех активных кастов Battle Hymn (вызывается каждый тик)
     */
    public static void tickBattleHymns(Iterable<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            UUID playerId = player.getUuid();
            if (activeChannels.containsKey(playerId)) {
                // Обновляем каст этого игрока
                if (!(player.getEntityWorld() instanceof ServerWorld world)) continue;
                
                BattleHymnSpell spell = new BattleHymnSpell();
                spell.updateChanneling(player, world);
            }
        }
    }
    
    /**
     * Очистка активных кастов (вызывается при выходе игрока)
     */
    public static void cleanupPlayer(UUID playerId) {
        activeChannels.remove(playerId);
    }
    
    /**
     * Проверка активного каста
     */
    public static boolean isChanneling(UUID playerId) {
        return activeChannels.containsKey(playerId);
    }
    
    /**
     * Визуализация радиуса каста жёлто-белыми частицами
     */
    private void spawnRadiusParticles(ServerWorld world, ServerPlayerEntity player, int radius) {
        double centerX = player.getX();
        double centerY = player.getY() + 0.1;
        double centerZ = player.getZ();
        
        // Количество частиц по окружности (16 точек - не слишком много)
        int particleCount = 16;
        
        for (int i = 0; i < particleCount; i++) {
            double angle = (i / (double) particleCount) * Math.PI * 2;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;
            
            // Чередуем жёлтые и белые частицы
            boolean isYellow = i % 2 == 0;
            
            if (isYellow) {
                // Жёлтые частицы (END_ROD)
                world.spawnParticles(
                    net.minecraft.particle.ParticleTypes.END_ROD,
                    centerX + offsetX,
                    centerY,
                    centerZ + offsetZ,
                    1,
                    0, 0.05, 0,
                    0.02
                );
            } else {
                // Белые частицы (GLOW)
                world.spawnParticles(
                    net.minecraft.particle.ParticleTypes.GLOW,
                    centerX + offsetX,
                    centerY,
                    centerZ + offsetZ,
                    1,
                    0, 0.05, 0,
                    0.02
                );
            }
        }
    }
    
    /**
     * Создаёт визуальный прогресс-бар для action bar
     */
    private String createProgressBar(int progress) {
        int barLength = 10;
        int filled = (progress * barLength) / 100;
        
        StringBuilder bar = new StringBuilder("§7[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("§e█"); // Жёлтые заполненные блоки
            } else {
                bar.append("§8█"); // Серые пустые блоки
            }
        }
        bar.append("§7]");
        
        return bar.toString();
    }
    
    // === Параметры по уровням ===
    
    private int getChannelDuration(int level) {
        return switch (level) {
            case 1 -> 100; // 5 секунд (короткий каст)
            case 2 -> 140; // 7 секунд (средний каст)
            default -> 180; // 9 секунд (долгий каст для level 3)
        };
    }
    
    private int getRadius(int level) {
        return switch (level) {
            case 1 -> 3;
            case 2 -> 4;
            default -> 5; // level 3
        };
    }
    
    private int getBuffDuration(int level) {
        return switch (level) {
            case 1 -> 200;  // 10 секунд - Сила I
            case 2 -> 400;  // 20 секунд - Сила I (долгий)
            default -> 300; // 15 секунд - Сила II (мощнее но короче чем lvl 2)
        };
    }
    
    /**
     * Темп нот в тиках - РАЗНЫЙ для каждого уровня!
     * Level 1: Быстрый ритм (каждые 0.5 сек)
     * Level 2: Средний ритм (каждые 0.75 сек)
     * Level 3: Медленный величественный (каждые 1 сек)
     */
    private int getNoteTempoTicks(int level) {
        return switch (level) {
            case 1 -> 10;  // Каждые 0.5 сек - быстрый агрессивный ритм
            case 2 -> 15;  // Каждые 0.75 сек - средний героический ритм
            default -> 20; // Каждые 1 сек - медленный эпический ритм
        };
    }
    
    private int getPlayerLevel(PlayerEntity player) {
        // Получаем уровень через SpellLevelManager (как другие заклинания)
        int level = SpellLevelManager.getSpellLevel(player.getUuid(), SpellType.BATTLE_HYMN);
        // SpellLevelManager возвращает 1, 2, 3 для уровней
        int adjustedLevel = Math.max(1, Math.min(3, level));
        System.out.println("[BattleHymn] Player spell level: " + level + " -> " + adjustedLevel);
        return adjustedLevel;
    }
    
    @Override
    public int getManaCost() {
        return 30; // Level 1
    }
    
    @Override
    public int getManaCost(int level) {
        return switch (level) {
            case 1 -> 30;
            case 2 -> 35;
            default -> 50; // level 3
        };
    }
    
    @Override
    public int getCooldown() {
        return 1200; // 60 секунд (level 1)
    }
    
    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 1200; // 60 секунд
            case 2 -> 1100; // 55 секунд
            default -> 1300; // 65 секунд (level 3)
        };
    }
    
    @Override
    public SpellType getType() {
        return SpellType.BATTLE_HYMN;
    }
}
