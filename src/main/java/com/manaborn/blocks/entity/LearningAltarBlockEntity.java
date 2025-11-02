package com.manaborn.blocks.entity;

import com.manaborn.ManabornMod;
import com.manaborn.items.SpellBookItem;
import com.manaborn.spells.LearnedSpellsManager;
import com.manaborn.spells.SpellType;
import com.manaborn.screen.LearningAltarScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
// NBT methods intentionally omitted for 1.21.10 API compatibility draft; will add after compile pass
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class LearningAltarBlockEntity extends BlockEntity implements Inventory, NamedScreenHandlerFactory {
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int progress = 0;
    private int maxProgress = 200;
    private boolean learning = false;
    private int reservedCost = 0;
    private @Nullable PlayerEntity lastInteracting;
    private int effectTick = 0;
    private int requiredXp = 0;
    private boolean canStart = false;
    private boolean hasEnoughXp = false;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> learning ? 1 : 0;
                case 3 -> requiredXp;
                case 4 -> canStart ? 1 : 0;
                case 5 -> hasEnoughXp ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }

        @Override
        public int size() { return 6; }
    };

    public LearningAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ManabornMod.LEARNING_ALTAR_BE, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, LearningAltarBlockEntity be) {
        // Keep computed props fresh
        be.updateComputedProps(be.lastInteracting);
        if (!be.learning) {
            // Waiting for UI button to start learning
            be.progress = 0;
            return;
        }
        if (!be.canContinue()) {
            be.learning = false;
            be.progress = 0;
            be.reservedCost = 0;
            be.markDirty();
            return;
        }
        be.progress++;
        be.effectTick++;
        // Ambient effects during learning
        if (!world.isClient() && be.effectTick % 5 == 0) {
            ServerWorld sw = (ServerWorld) world;
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 1.1;
            double cz = pos.getZ() + 0.5;
            sw.spawnParticles(ParticleTypes.ENCHANT, cx, cy, cz, 6, 0.25, 0.1, 0.25, 0.01);
        }
        if (!world.isClient() && be.effectTick % 40 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.6f, 1.2f);
        }
        if (be.progress >= be.maxProgress) {
            be.finishLearning();
        }
        be.markDirty();
    }

    private boolean canContinue() {
        if (world == null || world.isClient()) return false;
        ItemStack wand = items.get(0);
        ItemStack book = items.get(1);
        if (wand.isEmpty() || book.isEmpty()) return false;
        if (!isWand(wand.getItem())) return false;
        SpellType spell = getSpellFromBook(book);
        if (spell == null) return false;
        return true;
    }

    private void finishLearning() {
        if (world == null || world.isClient()) return;
        ItemStack book = items.get(1);
        SpellType spell = getSpellFromBook(book);
        PlayerEntity player = lastInteracting;
        if (spell != null && player != null) {
            if (!LearnedSpellsManager.hasLearned(player.getUuid(), spell)) {
                LearnedSpellsManager.learnSpell((net.minecraft.server.network.ServerPlayerEntity) player, spell);
            }
        }
        if (!book.isEmpty()) {
            book.decrement(1);
        }
        learning = false;
        progress = 0;
        reservedCost = 0;
        effectTick = 0;
        // Finish effects
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.9f, 1.2f);
        if (world instanceof ServerWorld sw) {
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 1.2;
            double cz = pos.getZ() + 0.5;
            sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, cx, cy, cz, 16, 0.5, 0.3, 0.5, 0.1);
            sw.spawnParticles(ParticleTypes.ENCHANT, cx, cy, cz, 20, 0.6, 0.3, 0.6, 0.03);
        }
        markDirty();
        world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    public void tryStartLearning(PlayerEntity player) {
        if (world == null || world.isClient()) return;
        if (learning) return;
        ItemStack wand = items.get(0);
        ItemStack book = items.get(1);
        if (wand.isEmpty() || book.isEmpty()) return;
        if (!isWand(wand.getItem())) return;
        SpellType spell = getSpellFromBook(book);
        if (spell == null) return;
        if (LearnedSpellsManager.hasLearned(player.getUuid(), spell)) return;
        int cost = getXpCost(spell);
        if (player.experienceLevel < cost) return;
        player.addExperienceLevels(-cost);
        reservedCost = cost;
        learning = true;
        // ensure maxProgress corresponds to current spell
        maxProgress = getSpellDurationTicks(spell);
        progress = 0;
        effectTick = 0;
        // Start effects
        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 0.8f, 1.0f);
        if (world instanceof ServerWorld sw) {
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 1.1;
            double cz = pos.getZ() + 0.5;
            sw.spawnParticles(ParticleTypes.ENCHANT, cx, cy, cz, 12, 0.4, 0.2, 0.4, 0.02);
        }
        markDirty();
        updateComputedProps(player);
    }

    private int getXpCost(SpellType spell) {
        return switch (spell) {
            case FIREBALL -> 5;
            case ICE_SHARD -> 8;
            case FIRE_WALL -> 17;
            case INNER_HEAT -> 13;
            case FROST_AURA -> 12;
        };
    }

    private boolean isWand(Item item) {
        return item == ManabornMod.MANA_WAND;
    }

    private @Nullable SpellType getSpellFromBook(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ManabornMod.FIREBALL_SPELL_BOOK) return SpellType.FIREBALL;
        if (item == ManabornMod.ICE_SHARD_SPELL_BOOK) return SpellType.ICE_SHARD;
        if (item == ManabornMod.FIRE_WALL_SPELL_BOOK) return SpellType.FIRE_WALL;
        if (item == ManabornMod.INNER_HEAT_SPELL_BOOK) return SpellType.INNER_HEAT;
        if (item == ManabornMod.FROST_AURA_SPELL_BOOK) return SpellType.FROST_AURA;
        if (item instanceof SpellBookItem) {
            return null;
        }
        return null;
    }

    private int getSpellDurationTicks(SpellType spell) {
        return switch (spell) {
            case FIREBALL -> 20 * 20;      // 20 seconds
            case ICE_SHARD -> 45 * 20;     // 45 seconds
            case FIRE_WALL -> 60 * 20;     // 60 seconds
            case INNER_HEAT -> 120 * 20;   // 120 seconds
            case FROST_AURA -> 75 * 20;    // 75 seconds
        };
    }

    @Override
    public int size() { return items.size(); }

    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override
    public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount());
        markDirty();
        updateComputedProps(lastInteracting);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) { return player.squaredDistanceTo(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64.0; }

    @Override
    public void clear() { items.clear(); }

    @Override
    public Text getDisplayName() { return Text.translatable("block.manaborn.learning_altar"); }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, PlayerEntity player) {
        return new LearningAltarScreenHandler(syncId, playerInventory, this, properties);
    }

    public void setLastInteractingPlayer(PlayerEntity player) { this.lastInteracting = player; }

    private void updateComputedProps(@Nullable PlayerEntity player) {
        SpellType spell = getSpellFromBook(items.get(1));
        boolean hasWand = !items.get(0).isEmpty() && isWand(items.get(0).getItem());
        if (spell == null || !hasWand) {
            requiredXp = 0;
            canStart = false;
            hasEnoughXp = false;
            return;
        }
        // update per-spell duration dynamically for UI progress bar
        this.maxProgress = getSpellDurationTicks(spell);
        requiredXp = getXpCost(spell);
        boolean notLearned = (player == null) || !LearnedSpellsManager.hasLearned(player.getUuid(), spell);
        canStart = notLearned;
        hasEnoughXp = player != null && player.experienceLevel >= requiredXp;
    }

    // TODO: Add proper NBT save/load with 1.21.10 signatures when stabilizing
}
