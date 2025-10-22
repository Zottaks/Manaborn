package com.manaborn.spells;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.item.ItemStack;
import com.manaborn.mana.ManaManager;
import com.manaborn.entities.IceShardEntity;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class SpellManager {
    private static final String SPELL_NBT_KEY = "selectedSpell";
    
    // Хранилище активных стен огня: UUID стены -> данные о стене
    private static final Map<UUID, FireWallData> activeFireWalls = new HashMap<>();
    
    // Данные о стене огня
    private static class FireWallData {
        final Box boundingBox;
        final ServerWorld world;
        int ticksRemaining;
        
        FireWallData(Box boundingBox, ServerWorld world, int duration) {
            this.boundingBox = boundingBox;
            this.world = world;
            this.ticksRemaining = duration;
        }
    }

    public static void castSpell(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;
            
            // Проверяем, есть ли изученные заклинания
            List<SpellType> learnedSpells = getAvailableSpells(serverPlayer);
            if (learnedSpells.isEmpty()) {
                user.sendMessage(
                    Text.literal("У вас нет изученных заклинаний! Найдите книги заклинаний."),
                    true
                );
                return;
            }
            
            // Получаем выбранное заклинание или первое изученное
            SpellType selectedSpell = getSelectedSpell(stack, serverPlayer);
            if (selectedSpell == null) {
                // Автоматически выбираем первое изученное заклинание
                selectedSpell = learnedSpells.get(0);
                setSelectedSpell(stack, selectedSpell);
            }
            
            // Проверяем, изучено ли выбранное заклинание
            if (!LearnedSpellsManager.hasLearned(serverPlayer.getUuid(), selectedSpell)) {
                // Если текущее заклинание не изучено, выбираем первое изученное
                selectedSpell = learnedSpells.get(0);
                setSelectedSpell(stack, selectedSpell);
            }
            
            if (!ManaManager.tryConsume(serverPlayer, selectedSpell.getManaCost())) {
                return;
            }

            int cooldown;
            switch (selectedSpell) {
                case FIREBALL:
                    castFireball(world, user);
                    cooldown = 100; // 5 секунд
                    break;
                case ICE_SHARD:
                    castIceShard(world, user);
                    cooldown = 140; // 7 секунд (увеличен)
                    break;
                case FIRE_WALL:
                    castFireWall(world, user);
                    cooldown = 100; // 5 секунд
                    break;
                default:
                    cooldown = 100;
            }
            
            user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
            user.getItemCooldownManager().set(stack, cooldown);
        } else {
            user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
        }
    }

    private static void castFireball(World world, PlayerEntity user) {
        SmallFireballEntity fireball = new SmallFireballEntity(world, user, user.getRotationVector());
        fireball.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());
        world.spawnEntity(fireball);
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void castIceShard(World world, PlayerEntity user) {
        IceShardEntity iceShard = new IceShardEntity(world, user);
        iceShard.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 2.0f, 1.0f);
        world.spawnEntity(iceShard);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, SoundCategory.PLAYERS, 1.0f,
                1.2f);
    }
    
    private static void castFireWall(World world, PlayerEntity user) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        
        // Создаем стену огня на расстоянии 2 блоков перед игроком
        Vec3d lookVec = user.getRotationVector();
        double distance = 2.0;
        double centerX = user.getX() + lookVec.x * distance;
        double centerY = user.getY();
        double centerZ = user.getZ() + lookVec.z * distance;
        
        // Вычисляем перпендикулярное направление для стены
        // Поворачиваем вектор взгляда на 90 градусов по горизонтали
        double perpX = -lookVec.z; // Перпендикуляр к направлению взгляда
        double perpZ = lookVec.x;
        
        // Нормализуем перпендикулярный вектор
        double length = Math.sqrt(perpX * perpX + perpZ * perpZ);
        if (length > 0) {
            perpX /= length;
            perpZ /= length;
        }
        
        UUID wallId = UUID.randomUUID();
        double wallWidth = 6.0; // Ширина стены 6 блоков
        double wallHeight = 4.0; // Высота стены 4 блока (было 3)
        int cloudsHorizontal = 9; // 9 облаков по горизонтали
        int cloudsVertical = 6; // 6 облаков по вертикали (было 5)
        
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = centerY, maxY = centerY + wallHeight;
        double minZ = Double.MAX_VALUE, maxZ = Double.MIN_VALUE;
        
        double spacing = wallWidth / (cloudsHorizontal - 1);
        double vSpacing = wallHeight / (cloudsVertical - 1);
        
        // Создаём основную сетку облаков
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
                cloud.setRadius(0.5f); // Минимальная толщина (было 0.7)
                cloud.setDuration(200);
                cloud.setRadiusOnUse(0);
                cloud.setRadiusGrowth(0);
                cloud.setWaitTime(0);
                cloud.setParticleType(ParticleTypes.FLAME);
                
                world.spawnEntity(cloud);
            }
        }
        
        // Добавляем промежуточные облака со смещением (шахматный порядок)
        // Это закроет зазоры между основными облаками
        for (int h = 0; h < cloudsHorizontal - 1; h++) {
            double offset = (h - (cloudsHorizontal - 1) / 2.0) * spacing + spacing / 2.0;
            double cloudX = centerX + perpX * offset;
            double cloudZ = centerZ + perpZ * offset;
            
            for (int v = 0; v < cloudsVertical - 1; v++) {
                double cloudY = centerY + v * vSpacing + vSpacing / 2.0;
                
                AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, cloudX, cloudY, cloudZ);
                cloud.setRadius(0.5f); // Минимальная толщина (было 0.7)
                cloud.setDuration(200);
                cloud.setRadiusOnUse(0);
                cloud.setRadiusGrowth(0);
                cloud.setWaitTime(0);
                cloud.setParticleType(ParticleTypes.FLAME);
                
                world.spawnEntity(cloud);
            }
        }
        
        // Регистрируем ТОНКУЮ зону урона вдоль стены
        // Расширяем границы только вдоль стены, а не во все стороны
        double damageThickness = 0.4; // Уменьшенная толщина зоны урона (было 0.6)
        
        // Добавляем толщину в направлении перпендикулярном стене
        double thickMinX = minX - Math.abs(lookVec.x) * damageThickness;
        double thickMaxX = maxX + Math.abs(lookVec.x) * damageThickness;
        double thickMinZ = minZ - Math.abs(lookVec.z) * damageThickness;
        double thickMaxZ = maxZ + Math.abs(lookVec.z) * damageThickness;
        
        Box wallBox = new Box(thickMinX, minY, thickMinZ, thickMaxX, maxY, thickMaxZ);
        activeFireWalls.put(wallId, new FireWallData(wallBox, serverWorld, 200));
        
        // Звук
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 0.8f);
        
        int totalClouds = (cloudsHorizontal * cloudsVertical) + ((cloudsHorizontal - 1) * (cloudsVertical - 1));
        System.out.println("[Manaborn] FireWall created: " + totalClouds + " clouds in staggered pattern");
    }

    /**
     * Обрабатывает урон от активных стен огня
     * Вызывается каждый тик сервера
     */
    public static void tickFireWalls() {
        if (activeFireWalls.isEmpty()) return;
        
        List<UUID> toRemove = new ArrayList<>();
        
        for (Map.Entry<UUID, FireWallData> entry : activeFireWalls.entrySet()) {
            FireWallData data = entry.getValue();
            
            // Уменьшаем время жизни
            data.ticksRemaining--;
            
            if (data.ticksRemaining <= 0) {
                toRemove.add(entry.getKey());
                continue;
            }
            
            // Каждые 10 тиков наносим урон
            if (data.ticksRemaining % 10 == 0) {
                List<LivingEntity> entities = data.world.getEntitiesByClass(
                    LivingEntity.class,
                    data.boundingBox,
                    entity -> entity != null && entity.isAlive()
                );
                
                for (LivingEntity entity : entities) {
                    // Поджигаем на 2 секунды
                    entity.setOnFireFor(2);
                    // Наносим урон огнём (1 HP каждые 0.5 секунды)
                    entity.damage(data.world, data.world.getDamageSources().inFire(), 1.0f);
                }
            }
        }
        
        // Удаляем истёкшие стены
        for (UUID id : toRemove) {
            activeFireWalls.remove(id);
        }
    }
    
    /**
     * Получает выбранное заклинание из посоха
     * @return выбранное заклинание или null, если не выбрано
     */
    public static SpellType getSelectedSpell(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return null; // Нет выбранного заклинания
        }
        NbtCompound nbt = customData.copyNbt();
        String spellId = nbt.getString(SPELL_NBT_KEY).orElse("");
        if (spellId.isEmpty()) {
            return null;
        }
        return SpellType.fromId(spellId);
    }
    
    /**
     * Получает выбранное заклинание с учетом изученных заклинаний игрока
     * Если заклинание не выбрано или не изучено, возвращает первое изученное
     */
    public static SpellType getSelectedSpell(ItemStack stack, ServerPlayerEntity player) {
        SpellType selected = getSelectedSpell(stack);
        List<SpellType> learned = getAvailableSpells(player);
        
        if (learned.isEmpty()) {
            return null; // Нет изученных заклинаний
        }
        
        // Если заклинание не выбрано или не изучено, возвращаем первое изученное
        if (selected == null || !learned.contains(selected)) {
            return learned.get(0);
        }
        
        return selected;
    }

    public static void setSelectedSpell(ItemStack stack, SpellType spell) {
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = customData.copyNbt();
        nbt.putString(SPELL_NBT_KEY, spell.getId());
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static void cycleSpell(ItemStack stack) {
        SpellType current = getSelectedSpell(stack);
        SpellType[] spells = SpellType.values();
        int nextIndex = (current.ordinal() + 1) % spells.length;
        setSelectedSpell(stack, spells[nextIndex]);
    }
    
    /**
     * Переключает на следующее изученное заклинание
     * Если изученных заклинаний нет, ничего не делает
     */
    public static void cycleLearnedSpell(ItemStack stack, ServerPlayerEntity player) {
        List<SpellType> learnedSpells = getAvailableSpells(player);
        
        if (learnedSpells.isEmpty()) {
            player.sendMessage(
                Text.literal("У вас нет изученных заклинаний! Найдите книги заклинаний."),
                true
            );
            return; // Нет изученных заклинаний
        }
        
        SpellType current = getSelectedSpell(stack, player);
        int currentIndex = learnedSpells.indexOf(current);
        
        // Если текущее заклинание не изучено или это последнее, берём первое
        if (currentIndex == -1 || currentIndex >= learnedSpells.size() - 1) {
            setSelectedSpell(stack, learnedSpells.get(0));
        } else {
            setSelectedSpell(stack, learnedSpells.get(currentIndex + 1));
        }
    }
    
    /**
     * Получает список изученных заклинаний для игрока
     * Используется для отображения только доступных заклинаний
     */
    public static List<SpellType> getAvailableSpells(ServerPlayerEntity player) {
        List<SpellType> available = new ArrayList<>();
        for (SpellType spell : SpellType.values()) {
            if (LearnedSpellsManager.hasLearned(player.getUuid(), spell)) {
                available.add(spell);
            }
        }
        return available;
    }
}