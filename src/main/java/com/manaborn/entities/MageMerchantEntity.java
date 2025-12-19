package com.manaborn.entities;

import com.manaborn.ManabornMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;


/**
 * Маг-торговец - NPC, который спавнится в доме мага и продаёт магические предметы.
 */
public class MageMerchantEntity extends MerchantEntity {

    // Торговые предложения будут инициализированы при первом взаимодействии
    private boolean tradingInitialized = false;

    public MageMerchantEntity(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
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
        if (tradingInitialized) return;
        tradingInitialized = true;

        TradeOfferList offers = this.getOffers();

        // === Базовые товары (всегда доступны) ===
        
        // Магическая пыль за изумруды
        offers.add(createBuyOffer(Items.EMERALD, 2, ManabornMod.MAGIC_DUST, 8, 16, 2));
        
        // Кристалл маны за изумруды
        offers.add(createBuyOffer(Items.EMERALD, 4, ManabornMod.MANA_POTION, 2, 12, 3));
        
        // Сосуд маны за изумруды и пыль
        offers.add(createBuyOffer2(Items.EMERALD, 8, ManabornMod.MAGIC_DUST, 16, 
                ManabornMod.MANA_VESSEL, 1, 6, 5));
        

        // === Книги заклинаний (случайные 1-3 книги) ===
        
        Item[] spellBooks = {
                ManabornMod.FIREBALL_SPELL_BOOK,
                ManabornMod.ICE_SHARD_SPELL_BOOK,
                ManabornMod.BATTLE_HYMN_SPELL_BOOK,
                ManabornMod.INNER_HEAT_SPELL_BOOK,
                ManabornMod.FROST_AURA_SPELL_BOOK,
                ManabornMod.WEAKNESS_CURSE_SPELL_BOOK
        };
        
        // Перемешиваем и берём 1-3 книги
        java.util.List<Item> bookList = new java.util.ArrayList<>(java.util.Arrays.asList(spellBooks));
        java.util.Collections.shuffle(bookList, new java.util.Random(this.getUuid().hashCode()));
        
        int bookCount = 1 + this.random.nextInt(3); // 1-3 книги
        for (int i = 0; i < Math.min(bookCount, bookList.size()); i++) {
            Item book = bookList.get(i);
            // Цены зависят от книги
            if (book == ManabornMod.FIREBALL_SPELL_BOOK) {
                // Fireball - 7 изумрудов + 50 пыли
                offers.add(createBuyOffer2(Items.EMERALD, 7, ManabornMod.MAGIC_DUST, 50,
                        book, 1, 3, 10));
            } else if (book == ManabornMod.ICE_SHARD_SPELL_BOOK) {
                // Ice Shard - 8 изумрудов + 40 пыли
                offers.add(createBuyOffer2(Items.EMERALD, 8, ManabornMod.MAGIC_DUST, 40,
                        book, 1, 3, 10));
            } else if (book == ManabornMod.BATTLE_HYMN_SPELL_BOOK) {
                // Battle Hymn - 15 изумрудов + 2 эфира
                offers.add(createBuyOffer2(Items.EMERALD, 15, ManabornMod.ETHER, 2,
                        book, 1, 3, 10));
            } else if (book == ManabornMod.INNER_HEAT_SPELL_BOOK) {
                // Inner Heat - 10 изумрудов + 1 эфир
                offers.add(createBuyOffer2(Items.EMERALD, 10, ManabornMod.ETHER, 1,
                        book, 1, 3, 10));
            } else if (book == ManabornMod.FROST_AURA_SPELL_BOOK) {
                // Frost Aura - 17 изумрудов + 1 эфир
                offers.add(createBuyOffer2(Items.EMERALD, 17, ManabornMod.ETHER, 1,
                        book, 1, 3, 10));
            } else if (book == ManabornMod.WEAKNESS_CURSE_SPELL_BOOK) {
                // Weakness Curse - 13 изумрудов + 2 эфира
                offers.add(createBuyOffer2(Items.EMERALD, 13, ManabornMod.ETHER, 2,
                        book, 1, 3, 10));
            }
        }

        // === Особые товары ===
        
        // Маноскоп - 10 изумрудов + 20 лазурита
        offers.add(createBuyOffer2(Items.EMERALD, 10, Items.LAPIS_LAZULI, 20,
                ManabornMod.MANOSCOPE, 1, 3, 10));

        // === Обратная торговля (покупка у игрока) ===
        
        // Покупаем магическую пыль за изумруды
        offers.add(createSellOffer(ManabornMod.MAGIC_DUST, 16, Items.EMERALD, 1, 16, 2));
        
        // Покупаем эфир за изумруды (1 эфир = 3 изумруда)
        offers.add(createSellOffer(ManabornMod.ETHER, 1, Items.EMERALD, 3, 12, 3));
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
     * Создаёт предложение с двумя входными предметами
     */
    private TradeOffer createBuyOffer2(Item buyItem1, int buyCount1, Item buyItem2, int buyCount2,
                                        Item sellItem, int sellCount, int maxUses, int merchantXp) {
        return new TradeOffer(
                new TradedItem(buyItem1, buyCount1),
                java.util.Optional.of(new TradedItem(buyItem2, buyCount2)),
                new ItemStack(sellItem, sellCount),
                maxUses,
                merchantXp,
                0.05f
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
