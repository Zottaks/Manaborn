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
import net.minecraft.text.Text;

public class SpellManager {
    private static final String SPELL_NBT_KEY = "selectedSpell";

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

            switch (selectedSpell) {
                case FIREBALL:
                    castFireball(world, user);
                    break;
                case ICE_SHARD:
                    castIceShard(world, user);
                    break;
            }
        }

        user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
        user.getItemCooldownManager().set(stack, 100);
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