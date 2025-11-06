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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;

public class SpellManager {
    private static final String SPELL_NBT_KEY = "selectedSpell";
    private static final String COOLDOWN_PREFIX = "cd_until_";
    
    // Хранилище активных стен огня: UUID стены -> данные о стене
    private static final Map<UUID, FireWallData> activeFireWalls = new HashMap<>();
    
    // Активные ауры стужи: UUID игрока -> тики до конца
    private static final Map<UUID, Integer> activeFrostAuras = new HashMap<>();
    // Активные поглощающие барьеры: UUID игрока -> тики до конца
    private static final Map<UUID, Integer> activeAbsorbingBarriers = new HashMap<>();
    // Активные стены льда: UUID -> данные стены
    private static final Map<UUID, IceWallData> activeIceWalls = new HashMap<>();
    // Активный нагрев оружия: UUID игрока -> данные о combo
    private static final Map<UUID, HeatWeaponData> activeHeatWeapons = new HashMap<>();
    
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

    

    private static class IceWallData {
        final ServerWorld world;
        final List<BlockPos> blocks;
        int ticksRemaining;

        IceWallData(ServerWorld world, List<BlockPos> blocks, int durationTicks) {
            this.world = world;
            this.blocks = blocks;
            this.ticksRemaining = durationTicks;
        }
    }

    private static class HeatWeaponData {
        int attackCount;
        
        HeatWeaponData() {
            this.attackCount = 0;
        }
    }

    public static boolean hasAbsorbingBarrier(UUID playerId) {
        return activeAbsorbingBarriers.containsKey(playerId);
    }

    public static void removeAbsorbingBarrier(ServerPlayerEntity owner) {
        if (owner == null) return;
        if (!activeAbsorbingBarriers.containsKey(owner.getUuid())) return;
        activeAbsorbingBarriers.remove(owner.getUuid());

        World w = owner.getEntityWorld();
        if (w instanceof ServerWorld world) {
            for (int i = 0; i < 16; i++) {
                double a = world.random.nextDouble() * Math.PI * 2;
                double r = 2.0 + world.random.nextDouble() * 0.8;
                world.spawnParticles(ParticleTypes.SOUL, owner.getX() + Math.cos(a) * r, owner.getY() + 1.0, owner.getZ() + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        w.playSound(null, owner.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.8f, 0.8f);
        owner.sendMessage(Text.literal("Барьер рассеян"), true);
    }

    private static void castAbsorbingBarrier(World world, PlayerEntity user) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return;
        int duration = 200; // 10 секунд
        activeAbsorbingBarriers.put(serverPlayer.getUuid(), duration);
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 0.8f);
        serverPlayer.sendMessage(Text.literal("Поглощающий барьер активен (10с)"), true);
        if (world instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 20; i++) {
                double angle = serverWorld.random.nextDouble() * Math.PI * 2;
                double radius = 1.5 + serverWorld.random.nextDouble();
                double px = user.getX() + Math.cos(angle) * radius;
                double pz = user.getZ() + Math.sin(angle) * radius;
                double py = user.getY() + 0.5 + serverWorld.random.nextDouble();
                serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void tickAbsorbingBarriers(List<ServerPlayerEntity> players) {
        if (activeAbsorbingBarriers.isEmpty()) return;
        List<UUID> toRemove = new ArrayList<>();
        for (ServerPlayerEntity owner : players) {
            Integer ticks = activeAbsorbingBarriers.get(owner.getUuid());
            if (ticks == null) continue;
            int remaining = ticks - 1;
            if (remaining <= 0) {
                toRemove.add(owner.getUuid());
                continue;
            }
            activeAbsorbingBarriers.put(owner.getUuid(), remaining);

            World w = owner.getEntityWorld();
            if (!(w instanceof ServerWorld world)) continue;

            double radius = 2.5;
            Box area = Box.of(new Vec3d(owner.getX(), owner.getY() + 1.0, owner.getZ()), radius * 2, radius * 2, radius * 2);

            List<net.minecraft.entity.projectile.ProjectileEntity> projectiles = world.getEntitiesByClass(
                net.minecraft.entity.projectile.ProjectileEntity.class,
                area,
                e -> e != null && e.isAlive() && e.getOwner() != owner
            );

            for (net.minecraft.entity.projectile.ProjectileEntity p : projectiles) {
                Vec3d vel = p.getVelocity();
                Vec3d toOwner = new Vec3d(owner.getX() - p.getX(), (owner.getEyeY()) - p.getY(), owner.getZ() - p.getZ());
                if (vel.dotProduct(toOwner) <= 0) continue; // не летит к игроку

                double hearts = 1.0; // базовая оценка
                if (p instanceof net.minecraft.entity.projectile.SmallFireballEntity) {
                    hearts = 1.5;
                } else if (p instanceof net.minecraft.entity.projectile.PersistentProjectileEntity) {
                    hearts = 1.0;
                }

                int manaRestore = (int)Math.round(hearts * 5.0);
                com.manaborn.mana.ManaManager.addMana(owner, manaRestore);

                Vec3d newVel = vel.multiply(-1.2);
                p.setVelocity(newVel.x, newVel.y, newVel.z);
                p.setOwner(owner);

                world.playSound(null, owner.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.6f, 0.6f);
                world.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY(), p.getZ(), 6, 0.1, 0.1, 0.1, 0.01);
            }

            if (remaining % 6 == 0) {
                double a = world.random.nextDouble() * Math.PI * 2;
                double r = 2.2;
                world.spawnParticles(ParticleTypes.SOUL, owner.getX() + Math.cos(a) * r, owner.getY() + 1.0, owner.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
        for (UUID id : toRemove) {
            activeAbsorbingBarriers.remove(id);
        }
    }

    private static void castInnerHeat(World world, PlayerEntity user) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return;
        // Даем эффект скорости: 10 секунд, уровень 1 (Speed II будет amplifier=1, Speed I amplifier=0)
        int duration = 200; // 10 секунд
        int amplifier = 1; // Speed II
        serverPlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, amplifier, false, true, true));

        // Визуальные эффекты: частицы огня вокруг игрока
        if (world instanceof ServerWorld serverWorld) {
            double baseX = user.getX();
            double baseY = user.getY();
            double baseZ = user.getZ();
            for (int i = 0; i < 30; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
                double offsetY = world.random.nextDouble() * 1.8;
                double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;
                serverWorld.spawnParticles(ParticleTypes.FLAME,
                        baseX + offsetX,
                        baseY + offsetY,
                        baseZ + offsetZ,
                        1,
                        0.0, 0.01, 0.0,
                        0.01);
            }
        }

        // Звук активации
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }

    private static void castHeatWeapon(World world, PlayerEntity user) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return;
        
        // Активируем нагрев оружия на 30 секунд (600 тиков)
        UUID playerId = serverPlayer.getUuid();
        activeHeatWeapons.put(playerId, new HeatWeaponData());
        
        // Визуальные эффекты: огненные частицы вокруг рук игрока
        if (world instanceof ServerWorld serverWorld) {
            double baseX = user.getX();
            double baseY = user.getY() + 1.2; // уровень рук
            double baseZ = user.getZ();
            for (int i = 0; i < 20; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 1.2;
                double offsetY = (world.random.nextDouble() - 0.5) * 0.6;
                double offsetZ = (world.random.nextDouble() - 0.5) * 1.2;
                serverWorld.spawnParticles(ParticleTypes.FLAME,
                        baseX + offsetX,
                        baseY + offsetY,
                        baseZ + offsetZ,
                        1,
                        0.0, 0.01, 0.0,
                        0.01);
            }
        }
        
        // Звук активации
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.5f);
        serverPlayer.sendMessage(Text.literal("Нагрев оружия активирован! Следующие 10 атак поджигают цели."), true);
    }

    public static void onPlayerAttack(ServerPlayerEntity player, LivingEntity target, World world) {
        UUID playerId = player.getUuid();
        HeatWeaponData data = activeHeatWeapons.get(playerId);
        
        if (data == null) return; // Нагрев оружия не активен
        
        // Увеличиваем счетчик атак
        data.attackCount++;
        
        // Поджигаем цель
        target.setOnFireFor(3); // Поджигаем на 3 секунды
        
        // Визуальные эффекты при атаке
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.FLAME, 
                target.getX(), target.getY() + target.getHeight() / 2, target.getZ(), 
                5, 0.2, 0.3, 0.2, 0.01);
        }
        
        // Проверяем на 10-ю атаку (взрыв)
        if (data.attackCount >= 10) {
            // Создаем взрыв, который не наносит урон владельцу
            world.createExplosion(null, target.getX(), target.getY(), target.getZ(), 3.0f, 
                World.ExplosionSourceType.NONE);
            
            // Наносим урон цели, но не владельцу
            target.damage((ServerWorld) world, world.getDamageSources().explosion(null), 8.0f);
            
            // Визуальные эффекты взрыва
            if (world instanceof ServerWorld serverWorld) {
                for (int i = 0; i < 20; i++) {
                    double offsetX = (serverWorld.random.nextDouble() - 0.5) * 2.0;
                    double offsetY = serverWorld.random.nextDouble() * 1.5;
                    double offsetZ = (serverWorld.random.nextDouble() - 0.5) * 2.0;
                    serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
                        target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ,
                        1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            
            // Звук взрыва
            world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 
                SoundCategory.PLAYERS, 1.0f, 1.0f);
            
            // Удаляем эффект нагрева оружия
            activeHeatWeapons.remove(playerId);
            player.sendMessage(Text.literal("Взрыв! Нагрев оружия завершен."), true);
        } else {
            // Сообщаем о прогрессе
            int remaining = 10 - data.attackCount;
            player.sendMessage(Text.literal("Атака " + data.attackCount + "/10. Осталось атак: " + remaining), true);
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
            // Пер-спелл кулдаун: проверяем до траты маны
            if (isSpellOnCooldown(stack, world, selectedSpell)) {
                long now = world.getTime();
                long end = getSpellCooldownEnd(stack, selectedSpell);
                long ticksLeft = Math.max(0, end - now);
                double seconds = ticksLeft / 20.0;
                serverPlayer.sendMessage(Text.literal(String.format("Кулдаун %.1fс", seconds)), true);
                return;
            }

            if (!ManaManager.tryConsume(serverPlayer, selectedSpell.getManaCost())) {
                serverPlayer.sendMessage(Text.literal("Недостаточно маны: нужно " + selectedSpell.getManaCost()), true);
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
                    cooldown = 400; // 20 секунд
                    break;
                case INNER_HEAT:
                    castInnerHeat(world, user);
                    cooldown = 600; // 30 секунд
                    break;
                case FROST_AURA:
                    castFrostAura(world, user);
                    cooldown = 1000; // 50 секунд
                    break;
                case ABSORBING_BARRIER:
                    castAbsorbingBarrier(world, user);
                    cooldown = 1500; // 75 секунд
                    break;
                case WEAKNESS_CURSE:
                    castWeaknessCurse(world, user);
                    cooldown = 1500; // 75 секунд
                    break;
                case ICE_WALL:
                    // Для Стены Льда используем механику удержания и каст из MagicStaffItem.
                    // Здесь ничего не делаем.
                    cooldown = 0;
                    break;
                case HEAT_WEAPON:
                    castHeatWeapon(world, user);
                    cooldown = 100; // 5 секунд
                    break;
                default:
                    cooldown = 100;
            }
            
            user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
            setSpellCooldown(stack, world, selectedSpell, cooldown);
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
    
    /**
     * Накладывает Проклятие Слабости на цель перед игроком.
     * Ищет ближайшую живую цель в конусе ~20° на дистанции до 12 блоков.
     * Применяет эффект WEAKNESS и проигрывает мрачные эффекты.
     */
    private static void castWeaknessCurse(World world, PlayerEntity user) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        double range = 12.0;
        Vec3d eye = user.getEyePos();
        Vec3d look = user.getRotationVector().normalize();

        // Поиск кандидатов в расширенном боксе
        Box search = user.getBoundingBox().expand(range, range * 0.5, range);
        List<LivingEntity> candidates = serverWorld.getEntitiesByClass(
            LivingEntity.class,
            search,
            e -> e != null && e.isAlive() && e != user
        );

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double cosThreshold = Math.cos(Math.toRadians(20.0)); // узкий конус

        for (LivingEntity e : candidates) {
            Vec3d to = e.getEyePos().subtract(eye);
            double dist = to.length();
            if (dist > range || dist < 0.1) continue;
            Vec3d dir = to.normalize();
            double dot = dir.dotProduct(look);
            if (dot < cosThreshold) continue; // вне конуса
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }

        if (best == null) {
            if (user instanceof ServerPlayerEntity sp) {
                sp.sendMessage(Text.literal("Нет подходящей цели перед вами"), true);
            }
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 0.6f);
            return;
        }

        int duration = 400; // 20 секунд
        int amplifier = 2; // Сильное ослабление
        best.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, amplifier, false, true, true), user);

        // Визуальные эффекты вокруг цели
        double tx = best.getX();
        double ty = best.getY() + best.getHeight() * 0.6;
        double tz = best.getZ();
        for (int i = 0; i < 28; i++) {
            double a = serverWorld.random.nextDouble() * Math.PI * 2;
            double r = 0.4 + serverWorld.random.nextDouble() * 0.9;
            double px = tx + Math.cos(a) * r;
            double pz = tz + Math.sin(a) * r;
            double py = ty + (serverWorld.random.nextDouble() - 0.5) * 0.8;
            serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 1, 0, 0, 0, 0);
            if (i % 3 == 0) {
                serverWorld.spawnParticles(ParticleTypes.SOUL, px, py, pz, 1, 0.02, 0.02, 0.02, 0.005);
            }
        }
        world.playSound(null, best.getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.7f, 0.8f);
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.5f, 0.7f);

        if (user instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.literal("Цель проклята слабостью (20с)"), true);
        }
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
        int totalClouds = (cloudsHorizontal * cloudsVertical) + ((cloudsHorizontal - 1) * (cloudsVertical - 1));
        System.out.println("[Manaborn] FireWall created: " + totalClouds + " clouds in staggered pattern");

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
        activeFireWalls.put(wallId, new FireWallData(wallBox, serverWorld, 200));
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
                    // Поджигаем на 5 секунд
                    entity.setOnFireFor(5);
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
    
    private static void castFrostAura(World world, PlayerEntity user) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) return;
        int duration = 400; // 20 секунд
        activeFrostAuras.put(serverPlayer.getUuid(), duration);
        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_SNOW_HIT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        if (world instanceof ServerWorld serverWorld) {
            spawnFrostParticles(serverWorld, user.getX(), user.getY() + 1.0, user.getZ(), 10);
        }
    }

    private static void spawnFrostParticles(ServerWorld world, double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 1.5 + world.random.nextDouble() * 1.5;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            double py = y + world.random.nextDouble() * 1.0;
            world.spawnParticles(ParticleTypes.SNOWFLAKE, px, py, pz, 1, 0.0, 0.01, 0.0, 0.0);
        }
    }

    /**
     * Обработка активных аур стужи. Вызывать каждый тик сервера.
     */
    public static void tickFrostAuras(List<ServerPlayerEntity> players) {
        if (activeFrostAuras.isEmpty()) return;

        List<UUID> toRemove = new ArrayList<>();

        for (ServerPlayerEntity owner : players) {
            Integer ticks = activeFrostAuras.get(owner.getUuid());
            if (ticks == null) continue;

            int remaining = ticks - 1;
            if (remaining <= 0) {
                toRemove.add(owner.getUuid());
                continue;
            }
            activeFrostAuras.put(owner.getUuid(), remaining);

            World ow = owner.getEntityWorld();
            if (!(ow instanceof ServerWorld world)) {
                continue;
            }
            // Сбиваем ауру, если по владельцу попали (получил урон)
            DamageSource recent = owner.getRecentDamageSource();
            if (recent != null) {
                toRemove.add(owner.getUuid());
                continue;
            }

            // Меньше частиц и не каждый тик, чтобы не мешали обзору
            if (remaining % 5 == 0) {
                spawnFrostParticles(world, owner.getX(), owner.getY() + 1.0, owner.getZ(), 6);
            }

            double radius = 4.0;
            Box area = Box.of(new Vec3d(owner.getX(), owner.getY(), owner.getZ()), radius * 2, radius * 2, radius * 2);

            List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                area,
                e -> e != null && e.isAlive() && e != owner && (
                    e instanceof Monster || (e instanceof ServerPlayerEntity)
                ) && e.squaredDistanceTo(owner) <= radius * radius
            );

            for (LivingEntity target : targets) {
                // Обновляем замедление каждый тик (Slowness II)
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 1, false, true, true));
            }

            // Раз в 2 секунды наносим урон 0.5 сердца (1.0f)
            if (remaining % 40 == 0) {
                for (LivingEntity target : targets) {
                    target.damage(world, world.getDamageSources().magic(), 1.0f);
                }
            }
        }

        for (UUID id : toRemove) {
            activeFrostAuras.remove(id);
        }
    }

    public static boolean intersectsFireWall(ServerWorld world, Box box) {
        if (activeFireWalls.isEmpty()) return false;
        for (FireWallData data : activeFireWalls.values()) {
            if (data.world == world && data.boundingBox.intersects(box)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Публичный хелпер: проверка кулдауна заклинания на предмете
     */
    public static boolean isOnCooldown(ItemStack stack, World world, SpellType spell) {
        return isSpellOnCooldown(stack, world, spell);
    }

    /**
     * Публичный хелпер: установка кулдауна заклинания на предмете
     */
    public static void setCooldown(ItemStack stack, World world, SpellType spell, int durationTicks) {
        setSpellCooldown(stack, world, spell, durationTicks);
    }

    /**
     * Публичный хелпер для HUD: оставшееся время кулдауна в тиках.
     */
    public static int getCooldownRemainingTicks(ItemStack stack, World world, SpellType spell) {
        long end = getSpellCooldownEnd(stack, spell);
        long now = world.getTime();
        long diff = end - now;
        return (int) Math.max(0L, diff);
    }

    /**
     * Создаёт стену льда из packed_ice, заполняя только воздух (без жидкостей и сущностей),
     * ориентированную перпендикулярно направлению взгляда игрока, на расстоянии ~2 блоков.
     *
     * @param world ServerWorld
     * @param user игрок-кастер
     * @param width ширина (по горизонтали) в блоках
     * @param height высота в блоках
     * @param durationTicks время жизни в тиках
     */
    public static void castIceWall(ServerWorld world, PlayerEntity user, int width, int height, int durationTicks) {
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
            int baseX = (int)Math.floor(centerX + offsetX);
            int baseZ = (int)Math.floor(centerZ + offsetZ);
            for (int dy = 0; dy < height; dy++) {
                int by = (int)(centerY + dy);
                BlockPos pos = new BlockPos(baseX, by, baseZ);

                // Проверки: только воздух, без жидкостей, без сущностей внутри
                if (!world.isAir(pos)) continue;
                FluidState fluid = world.getFluidState(pos);
                if (!fluid.isEmpty()) continue;
                Box blockBox = Box.from(Vec3d.ofCenter(pos)).expand(0.4);
                List<LivingEntity> colliders = world.getEntitiesByClass(LivingEntity.class, blockBox, e -> e.isAlive());
                if (!colliders.isEmpty()) continue;

                world.setBlockState(pos, Blocks.PACKED_ICE.getDefaultState());
                placed.add(pos);
            }
        }

        if (!placed.isEmpty()) {
            UUID id = UUID.randomUUID();
            activeIceWalls.put(id, new IceWallData(world, placed, durationTicks));
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.PLAYERS, 0.8f, 0.9f);
            // Ледяные частицы
            for (BlockPos p : placed) {
                world.spawnParticles(ParticleTypes.SNOWFLAKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.05, 0.05, 0.05, 0.0);
            }
        } else {
            // Нечего поставить — тихий отказ
        }
    }

    /**
     * Тик для активных ледяных стен: осыпание в последние 2 секунды и удаление.
     */
    public static void tickIceWalls() {
        if (activeIceWalls.isEmpty()) return;
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, IceWallData> entry : activeIceWalls.entrySet()) {
            IceWallData data = entry.getValue();
            data.ticksRemaining--;
            if (data.ticksRemaining <= 0) {
                // Полная очистка
                for (BlockPos p : data.blocks) {
                    if (data.world.getBlockState(p).isOf(Blocks.PACKED_ICE)) {
                        data.world.setBlockState(p, Blocks.AIR.getDefaultState());
                    }
                }
                toRemove.add(entry.getKey());
                continue;
            }
            // Осыпание в последние 2 секунды
            if (data.ticksRemaining <= 40 && data.ticksRemaining % 2 == 0) {
                int removeCount = Math.max(1, data.blocks.size() / 12);
                for (int i = 0; i < removeCount && !data.blocks.isEmpty(); i++) {
                    int idx = data.world.random.nextInt(data.blocks.size());
                    BlockPos p = data.blocks.remove(idx);
                    if (data.world.getBlockState(p).isOf(Blocks.PACKED_ICE)) {
                        data.world.setBlockState(p, Blocks.AIR.getDefaultState());
                        data.world.spawnParticles(ParticleTypes.SNOWFLAKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.0);
                        data.world.playSound(null, p, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.3f, 1.3f);
                    }
                }
            }
        }
        for (UUID id : toRemove) activeIceWalls.remove(id);
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

    private static String cooldownKey(SpellType spell) {
        return COOLDOWN_PREFIX + spell.getId();
    }

    private static long getSpellCooldownEnd(ItemStack stack, SpellType spell) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return 0L;
        NbtCompound nbt = customData.copyNbt();
        return nbt.getLong(cooldownKey(spell)).orElse(0L);
    }

    private static void setSpellCooldown(ItemStack stack, World world, SpellType spell, int durationTicks) {
        long endTick = world.getTime() + durationTicks;
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = customData.copyNbt();
        nbt.putLong(cooldownKey(spell), endTick);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private static boolean isSpellOnCooldown(ItemStack stack, World world, SpellType spell) {
        long end = getSpellCooldownEnd(stack, spell);
        return world.getTime() < end;
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