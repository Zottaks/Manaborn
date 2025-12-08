package com.manaborn.spells;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.world.World;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active spell effects in the mod
 */
public class ActiveEffectManager {

    // Active fire walls: UUID wall -> wall data
    private static final Map<UUID, FireWallData> activeFireWalls = new ConcurrentHashMap<>();

    // Active frost auras: UUID player -> ticks remaining
    private static final Map<UUID, Integer> activeFrostAuras = new ConcurrentHashMap<>();

    // Active absorbing barriers: UUID player -> ticks remaining
    private static final Map<UUID, Integer> activeAbsorbingBarriers = new ConcurrentHashMap<>();

    // Active ice walls: UUID -> wall data
    private static final Map<UUID, IceWallData> activeIceWalls = new ConcurrentHashMap<>();

    // Active heat weapons: UUID player -> combo data
    private static final Map<UUID, HeatWeaponData> activeHeatWeapons = new ConcurrentHashMap<>();

    // Data classes
    public static class FireWallData {
        final Box boundingBox;
        final ServerWorld world;
        int ticksRemaining;

        FireWallData(Box boundingBox, ServerWorld world, int duration) {
            this.boundingBox = boundingBox;
            this.world = world;
            this.ticksRemaining = duration;
        }
    }

    public static class IceWallData {
        final ServerWorld world;
        final List<BlockPos> blocks;
        int ticksRemaining;

        IceWallData(ServerWorld world, List<BlockPos> blocks, int durationTicks) {
            this.world = world;
            this.blocks = blocks;
            this.ticksRemaining = durationTicks;
        }
    }

    public static class HeatWeaponData {
        int attackCount;
        int level; // Уровень заклинания (1-3)

        HeatWeaponData(int level) {
            this.attackCount = 0;
            this.level = level;
        }
        
        // Получить максимальное количество атак для уровня
        public int getMaxAttacks() {
            return switch (level) {
                case 1 -> 5;
                case 2 -> 7;
                default -> 10; // level 3
            };
        }
    }

    // Fire Wall management
    public static void addFireWall(Box boundingBox, ServerWorld world, int duration) {
        UUID wallId = UUID.randomUUID();
        activeFireWalls.put(wallId, new FireWallData(boundingBox, world, duration));
    }

    public static void tickFireWalls() {
        if (activeFireWalls.isEmpty())
            return;

        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, FireWallData> entry : activeFireWalls.entrySet()) {
            FireWallData data = entry.getValue();
            data.ticksRemaining--;

            if (data.ticksRemaining <= 0) {
                toRemove.add(entry.getKey());
                continue;
            }

            // Deal damage every 10 ticks
            if (data.ticksRemaining % 10 == 0) {
                List<LivingEntity> entities = data.world.getEntitiesByClass(
                        LivingEntity.class,
                        data.boundingBox,
                        entity -> entity != null && entity.isAlive());

                for (LivingEntity entity : entities) {
                    entity.setOnFireFor(5);
                    entity.damage(data.world, data.world.getDamageSources().inFire(), 1.0f);
                }
            }
        }

        for (UUID id : toRemove) {
            activeFireWalls.remove(id);
        }
    }

    public static boolean intersectsFireWall(ServerWorld world, Box box) {
        if (activeFireWalls.isEmpty())
            return false;
        for (FireWallData data : activeFireWalls.values()) {
            if (data.world == world && data.boundingBox.intersects(box)) {
                return true;
            }
        }
        return false;
    }

    // Frost Aura management
    public static void addFrostAura(UUID playerId, int duration) {
        activeFrostAuras.put(playerId, duration);
    }

    public static void tickFrostAuras(List<ServerPlayerEntity> players) {
        if (activeFrostAuras.isEmpty())
            return;

        List<UUID> toRemove = new ArrayList<>();

        for (ServerPlayerEntity owner : players) {
            Integer ticks = activeFrostAuras.get(owner.getUuid());
            if (ticks == null)
                continue;

            int remaining = ticks - 1;
            if (remaining <= 0) {
                toRemove.add(owner.getUuid());
                continue;
            }
            activeFrostAuras.put(owner.getUuid(), remaining);

            if (!(owner.getEntityWorld() instanceof ServerWorld world)) {
                continue;
            }

            // Cancel aura if player took damage
            DamageSource recent = owner.getRecentDamageSource();
            if (recent != null) {
                toRemove.add(owner.getUuid());
                continue;
            }

            // Particles every 5 ticks
            if (remaining % 5 == 0) {
                spawnFrostParticles(world, owner.getX(), owner.getY() + 1.0, owner.getZ(), 6);
            }

            double radius = 4.0;
            Box area = Box.of(new Vec3d(owner.getX(), owner.getY(), owner.getZ()), radius * 2, radius * 2, radius * 2);

            List<LivingEntity> targets = world.getEntitiesByClass(
                    LivingEntity.class,
                    area,
                    e -> e != null && e.isAlive() && e != owner
                            && (e instanceof Monster || (e instanceof ServerPlayerEntity))
                            && e.squaredDistanceTo(owner) <= radius * radius);

            for (LivingEntity target : targets) {
                // Медлительность для всех целей
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 1, false, true, true));

                // Обморожение-замерзание: имитируем поведение рыхлого снега для игроков, кроме владельца
                if (target instanceof ServerPlayerEntity playerTarget && !playerTarget.getUuid().equals(owner.getUuid())) {
                    int currentFrozen = playerTarget.getFrozenTicks();
                    playerTarget.setFrozenTicks(currentFrozen + 40); // ~2 секунды дополнительного замерзания
                }
            }

            // Damage every 2 seconds
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

    // Absorbing Barrier management
    public static void addAbsorbingBarrier(UUID playerId, int duration) {
        activeAbsorbingBarriers.put(playerId, duration);
    }

    public static boolean hasAbsorbingBarrier(UUID playerId) {
        return activeAbsorbingBarriers.containsKey(playerId);
    }

    public static void removeAbsorbingBarrier(ServerPlayerEntity owner) {
        if (owner == null)
            return;
        if (!activeAbsorbingBarriers.containsKey(owner.getUuid()))
            return;
        activeAbsorbingBarriers.remove(owner.getUuid());

        World w = owner.getEntityWorld();
        if (w instanceof ServerWorld world) {
            for (int i = 0; i < 16; i++) {
                double a = world.random.nextDouble() * Math.PI * 2;
                double r = 2.0 + world.random.nextDouble() * 0.8;
                world.spawnParticles(ParticleTypes.SOUL, owner.getX() + Math.cos(a) * r, owner.getY() + 1.0,
                        owner.getZ() + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        w.playSound(null, owner.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.8f,
                0.8f);
        owner.sendMessage(Text.literal("Барьер рассеян"), true);
    }

    public static void tickAbsorbingBarriers(List<ServerPlayerEntity> players) {
        if (activeAbsorbingBarriers.isEmpty())
            return;
        List<UUID> toRemove = new ArrayList<>();

        for (ServerPlayerEntity owner : players) {
            Integer ticks = activeAbsorbingBarriers.get(owner.getUuid());
            if (ticks == null)
                continue;
            int remaining = ticks - 1;
            if (remaining <= 0) {
                toRemove.add(owner.getUuid());
                continue;
            }
            activeAbsorbingBarriers.put(owner.getUuid(), remaining);

            World w = owner.getEntityWorld();
            if (!(w instanceof ServerWorld world))
                continue;

            double radius = 2.5;
            Box area = Box.of(new Vec3d(owner.getX(), owner.getY() + 1.0, owner.getZ()), radius * 2, radius * 2,
                    radius * 2);

            List<ProjectileEntity> projectiles = world.getEntitiesByClass(
                    ProjectileEntity.class,
                    area,
                    e -> e != null && e.isAlive() && e.getOwner() != owner);

            for (ProjectileEntity p : projectiles) {
                Vec3d vel = p.getVelocity();
                Vec3d toOwner = new Vec3d(owner.getX() - p.getX(), (owner.getEyeY()) - p.getY(),
                        owner.getZ() - p.getZ());
                if (vel.dotProduct(toOwner) <= 0)
                    continue;

                double hearts = 1.0;
                if (p instanceof SmallFireballEntity) {
                    hearts = 1.5;
                } else if (p instanceof PersistentProjectileEntity) {
                    hearts = 1.0;
                }

                // Восстановление маны зависит от уровня заклинания
                int spellLevel = SpellLevelManager.getSpellLevel(owner.getUuid(), SpellType.ABSORBING_BARRIER);
                double manaMultiplier = switch (spellLevel) {
                    case 1 -> 0.0;   // Нет восстановления
                    case 2 -> 2.5;   // 50% от оригинала
                    default -> 5.0;  // 100% (оригинал)
                };
                
                int manaRestore = (int) Math.round(hearts * manaMultiplier);
                if (manaRestore > 0) {
                    com.manaborn.mana.ManaManager.addMana(owner, manaRestore);
                }

                Vec3d newVel = vel.multiply(-1.2);
                p.setVelocity(newVel.x, newVel.y, newVel.z);
                p.setOwner(owner);

                world.playSound(null, owner.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN,
                        SoundCategory.PLAYERS, 0.6f, 0.6f);
                world.spawnParticles(ParticleTypes.SOUL, p.getX(), p.getY(), p.getZ(), 6, 0.1, 0.1, 0.1, 0.01);
            }

            if (remaining % 6 == 0) {
                double a = world.random.nextDouble() * Math.PI * 2;
                double r = 2.2;
                world.spawnParticles(ParticleTypes.SOUL, owner.getX() + Math.cos(a) * r, owner.getY() + 1.0,
                        owner.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }

        for (UUID id : toRemove) {
            activeAbsorbingBarriers.remove(id);
        }
    }

    // Ice Wall management
    public static void addIceWall(ServerWorld world, List<BlockPos> blocks, int duration) {
        UUID id = UUID.randomUUID();
        activeIceWalls.put(id, new IceWallData(world, blocks, duration));
    }

    public static void tickIceWalls() {
        if (activeIceWalls.isEmpty())
            return;
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, IceWallData> entry : activeIceWalls.entrySet()) {
            IceWallData data = entry.getValue();
            data.ticksRemaining--;
            if (data.ticksRemaining <= 0) {
                // Complete cleanup
                for (BlockPos p : data.blocks) {
                    if (data.world.getBlockState(p).isOf(Blocks.PACKED_ICE)) {
                        data.world.setBlockState(p, Blocks.AIR.getDefaultState());
                    }
                }
                toRemove.add(entry.getKey());
                continue;
            }
            // Crumbling in last 2 seconds
            if (data.ticksRemaining <= 40 && data.ticksRemaining % 2 == 0) {
                int removeCount = Math.max(1, data.blocks.size() / 12);
                for (int i = 0; i < removeCount && !data.blocks.isEmpty(); i++) {
                    int idx = data.world.random.nextInt(data.blocks.size());
                    BlockPos p = data.blocks.remove(idx);
                    if (data.world.getBlockState(p).isOf(Blocks.PACKED_ICE)) {
                        data.world.setBlockState(p, Blocks.AIR.getDefaultState());
                        data.world.spawnParticles(ParticleTypes.SNOWFLAKE, p.getX() + 0.5, p.getY() + 0.5,
                                p.getZ() + 0.5, 3, 0.1, 0.1, 0.1, 0.0);
                        data.world.playSound(null, p, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.3f, 1.3f);
                    }
                }
            }
        }

        for (UUID id : toRemove) {
            activeIceWalls.remove(id);
        }
    }

    // Heat Weapon management
    public static void addHeatWeapon(UUID playerId, int level) {
        activeHeatWeapons.put(playerId, new HeatWeaponData(level));
    }

    public static void onPlayerAttack(ServerPlayerEntity player, LivingEntity target, World world) {
        UUID playerId = player.getUuid();
        HeatWeaponData data = activeHeatWeapons.get(playerId);

        if (data == null)
            return;
        
        // Проверка типа оружия в зависимости от уровня
        net.minecraft.item.ItemStack weapon = player.getMainHandStack();
        if (!isWeaponAllowedForLevel(weapon, data.level)) {
            player.sendMessage(Text.literal("§cЭто оружие слишком слабое для вашего уровня заклинания!"), true);
            return;
        }

        data.attackCount++;
        target.setOnFireFor(3);

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getHeight() / 2, target.getZ(),
                    5, 0.2, 0.3, 0.2, 0.01);
        }

        int maxAttacks = data.getMaxAttacks();
        
        if (data.attackCount >= maxAttacks) {
            // Взрыв только на 3 уровне
            if (data.level >= 3) {
                world.createExplosion(player, target.getX(), target.getY(), target.getZ(), 3.0f,
                        World.ExplosionSourceType.NONE);

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

                world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                        SoundCategory.PLAYERS, 1.0f, 1.0f);

                activeHeatWeapons.remove(playerId);
                player.sendMessage(Text.literal("Взрыв! Нагрев оружия завершен."), true);
            } else {
                activeHeatWeapons.remove(playerId);
                player.sendMessage(Text.literal("Нагрев оружия завершен."), true);
            }
        } else {
            int remaining = maxAttacks - data.attackCount;
            player.sendMessage(Text.literal("Атака " + data.attackCount + "/" + maxAttacks + ". Осталось: " + remaining), true);
        }
    }
    
    /**
     * Проверяет, разрешено ли использовать данное оружие для указанного уровня заклинания
     * Level 1: только каменное оружие
     * Level 2: до железа включительно
     * Level 3: любое оружие
     */
    private static boolean isWeaponAllowedForLevel(net.minecraft.item.ItemStack weapon, int level) {
        if (weapon.isEmpty()) return false;
        
        net.minecraft.item.Item item = weapon.getItem();
        
        // Level 3 - любое оружие
        if (level >= 3) return true;
        
        // Проверяем материал оружия
        String itemName = net.minecraft.registry.Registries.ITEM.getId(item).getPath();
        
        // Level 1 - только каменное
        if (level == 1) {
            return itemName.contains("stone_sword") || 
                   itemName.contains("stone_axe") ||
                   itemName.contains("stone_shovel") ||
                   itemName.contains("stone_pickaxe") ||
                   itemName.contains("stone_hoe");
        }
        
        // Level 2 - до железа
        if (level == 2) {
            return itemName.contains("stone_") || 
                   itemName.contains("iron_") ||
                   itemName.contains("golden_") ||
                   itemName.contains("wooden_");
        }
        
        return false;
    }

    // Cleanup all effects
    public static void cleanupAll() {
        activeFireWalls.clear();
        activeFrostAuras.clear();
        activeAbsorbingBarriers.clear();
        activeIceWalls.clear();
        activeHeatWeapons.clear();
    }
}
