package com.manaborn.entities;

import com.manaborn.ManabornMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;



/**
 * Маг-торговец - NPC, который спавнится в доме мага и продаёт магические предметы.
 */
public class MageMerchantEntity extends MerchantEntity {
    private boolean emergencyTranslocationUsed = false;


    public MageMerchantEntity(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new EscapeDangerGoal(this, 0.8D));
        this.goalSelector.add(1, new FleeEntityGoal<>(this, HostileEntity.class, 10.0F, 0.6D, 0.8D));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.35D));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createMobAttributes() {
        return MerchantEntity.createMobAttributes()
                .add(EntityAttributes.MOVEMENT_SPEED, 0.35D);
    }

    @Override
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ManabornMod.MANA_WAND));
        return data;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        float healthBeforeHit = this.getHealth();
        boolean result = super.damage(world, source, amount);

        if (result
                && this.isAlive()
                && !this.emergencyTranslocationUsed
                && healthBeforeHit > this.getMaxHealth() * 0.5F
                && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            triggerEmergencyTranslocation(world);
        }

        return result;
    }

    private void triggerEmergencyTranslocation(ServerWorld world) {
        this.emergencyTranslocationUsed = true;

        Vec3d origin = new Vec3d(this.getX(), this.getY(), this.getZ());
        Vec3d destination = findRandomSafePosition(world, origin, 20.0, 40.0);

        spawnTranslocationEffect(world, origin);
        this.teleport(destination.x, destination.y, destination.z, false);
        spawnTranslocationEffect(world, destination);

        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.NEUTRAL, 1.0f, 1.05f);
        world.playSound(null, this.getBlockPos(), SoundEvents.ITEM_TOTEM_USE,
                SoundCategory.NEUTRAL, 0.55f, 1.8f);
    }

    private void spawnTranslocationEffect(ServerWorld world, Vec3d center) {
        world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y + 1.0, center.z,
                18, 0.45, 0.75, 0.45, 0.02);
        world.spawnParticles(ParticleTypes.POOF, center.x, center.y + 1.0, center.z,
                12, 0.3, 0.55, 0.3, 0.02);
        world.spawnParticles(ParticleTypes.FIREWORK, center.x, center.y + 1.0, center.z,
                6, 0.18, 0.35, 0.18, 0.01);
        world.spawnParticles(ParticleTypes.GLOW, center.x, center.y + 1.1, center.z,
                10, 0.25, 0.45, 0.25, 0.01);
    }

    private Vec3d findRandomSafePosition(ServerWorld world, Vec3d center, double minRadius, double maxRadius) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = minRadius + Math.sqrt(world.random.nextDouble()) * (maxRadius - minRadius);
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            int baseY = (int) Math.floor(center.y);

            for (int dy = 3; dy >= -3; dy--) {
                BlockPos pos = BlockPos.ofFloored(x, baseY + dy, z);
                if (world.isAir(pos) && world.isAir(pos.up())) {
                    return Vec3d.ofBottomCenter(pos);
                }
            }
        }
        return center;
    }

    @Override
    @Nullable
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null; // Не размножается
    }

    @Override
    public boolean isLeveledMerchant() {
        return false; // Не имеет уровней как обычный житель
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }
    
    @Override
    protected void dropLoot(ServerWorld world, net.minecraft.entity.damage.DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);
        // Дроп магической пыли (1-6 штук)
        int dustCount = 1 + this.random.nextInt(6);
        this.dropStack(world, new ItemStack(ManabornMod.MAGIC_DUST, dustCount));
    }

    @Override
    protected SoundEvent getTradingSound(boolean sold) {
        return sold ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO;
    }

    @Override
    public SoundEvent getYesSound() {
        return SoundEvents.ENTITY_VILLAGER_YES;
    }

    @Override
    protected void afterUsing(TradeOffer offer) {
        // Спавним опыт при торговле
        if (offer.shouldRewardPlayerExperience()) {
            int xp = 3 + this.random.nextInt(4);
            this.getEntityWorld().spawnEntity(new ExperienceOrbEntity(
                    this.getEntityWorld(), this.getX(), this.getY() + 0.5, this.getZ(), xp));
        }
    }

    @Override
    protected void fillRecipes() {
        TradeOfferList offers = this.getOffers();
        
        // Если уже есть трейды - не добавляем новые
        if (!offers.isEmpty()) return;
        
        // Фиксированный набор трейдов (без рандома чтобы избежать дубликатов)
        
        // 1. Магическая пыль
        offers.add(createBuyOffer(Items.EMERALD, 3, ManabornMod.MAGIC_DUST, 16, 16, 2));
        
        // 2. Зелье маны
        offers.add(createBuyOffer(Items.EMERALD, 6, ManabornMod.MANA_POTION, 2, 12, 3));
        
        // 3. Сосуд маны
        offers.add(createBuyOffer(Items.EMERALD, 15, ManabornMod.MANA_VESSEL, 1, 6, 5));
        
        // 4. Маноскоп
        offers.add(createBuyOffer(Items.EMERALD, 20, ManabornMod.MANOSCOPE, 1, 3, 10));
        
        // 5. Одна случайная книга (на основе UUID)
        Item[] spellBooks = {
                ManabornMod.FIREBALL_SPELL_BOOK,
                ManabornMod.ICE_SHARD_SPELL_BOOK,
                ManabornMod.BATTLE_HYMN_SPELL_BOOK,
                ManabornMod.INNER_HEAT_SPELL_BOOK,
                ManabornMod.FROST_AURA_SPELL_BOOK,
                ManabornMod.WEAKNESS_CURSE_SPELL_BOOK
        };
        int bookIndex = Math.abs(this.getUuid().hashCode()) % spellBooks.length;
        offers.add(createBuyOffer(Items.EMERALD, 25, spellBooks[bookIndex], 1, 3, 15));
        
        // 6. Покупка эфира у игрока
        offers.add(createSellOffer(ManabornMod.ETHER, 1, Items.EMERALD, 5, 12, 3));
    }

    /**
     * Создаёт предложение: игрок даёт item1 и получает resultItem
     */
    private TradeOffer createBuyOffer(Item buyItem, int buyCount, Item sellItem, int sellCount, 
                                       int maxUses, int merchantXp) {
        return new TradeOffer(
                new TradedItem(buyItem, buyCount),
                new ItemStack(sellItem, sellCount),
                maxUses,
                merchantXp,
                0.05f // price multiplier
        );
    }


    /**
     * Создаёт предложение: торговец покупает у игрока
     */
    private TradeOffer createSellOffer(Item buyItem, int buyCount, Item sellItem, int sellCount,
                                        int maxUses, int merchantXp) {
        return new TradeOffer(
                new TradedItem(buyItem, buyCount),
                new ItemStack(sellItem, sellCount),
                maxUses,
                merchantXp,
                0.05f
        );
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!this.isAlive() || this.hasCustomer() || this.isBaby()) {
            return ActionResult.PASS;
        }

        if (!this.getEntityWorld().isClient()) {
            // Инициализируем торговлю если ещё не сделали
            this.fillRecipes();
            
            this.setCustomer(player);
            this.sendOffers(player, this.getDisplayName(), 1);
        }

        player.incrementStat(Stats.TALKED_TO_VILLAGER);
        return ActionResult.SUCCESS;
    }
}
