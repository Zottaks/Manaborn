package com.manaborn.spells;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import com.manaborn.mana.ManaManager;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.stat.Stats;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.LivingEntity;

/**
 * Main spell manager - now acts as a facade for the refactored spell system
 * Maintains backward compatibility while delegating to specialized components
 */
public class SpellManager {
    // Delegation to specialized managers

    // ========== DELEGATION METHODS FOR ACTIVE EFFECTS ==========

    public static boolean hasAbsorbingBarrier(UUID playerId) {
        return ActiveEffectManager.hasAbsorbingBarrier(playerId);
    }

    public static void removeAbsorbingBarrier(ServerPlayerEntity owner) {
        ActiveEffectManager.removeAbsorbingBarrier(owner);
    }

    public static void tickAbsorbingBarriers(List<ServerPlayerEntity> players) {
        ActiveEffectManager.tickAbsorbingBarriers(players);
    }

    public static void tickFireWalls() {
        ActiveEffectManager.tickFireWalls();
    }

    public static void tickFrostAuras(List<ServerPlayerEntity> players) {
        ActiveEffectManager.tickFrostAuras(players);
    }

    public static void tickIceWalls() {
        ActiveEffectManager.tickIceWalls();
    }

    public static boolean intersectsFireWall(ServerWorld world, Box box) {
        return ActiveEffectManager.intersectsFireWall(world, box);
    }

    public static void onPlayerAttack(ServerPlayerEntity player, LivingEntity target, World world) {
        ActiveEffectManager.onPlayerAttack(player, target, world);
    }

    public static void castIceWall(ServerWorld world, PlayerEntity user, int width, int height, int durationTicks) {
        com.manaborn.spells.impl.IceWallSpell.createIceWall(world, user, width, height, durationTicks);
    }

    // ========== MAIN SPELL CASTING METHOD ==========

    public static void castSpell(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;

            // Check for learned spells
            List<SpellType> learnedSpells = getAvailableSpells(serverPlayer);
            if (learnedSpells.isEmpty()) {
                user.sendMessage(
                        net.minecraft.text.Text.literal("У вас нет изученных заклинаний! Найдите книги заклинаний."),
                        true);
                return;
            }

            // Get selected spell or first learned
            SpellType selectedSpell = getSelectedSpell(stack, serverPlayer);
            if (selectedSpell == null) {
                selectedSpell = learnedSpells.get(0);
                setSelectedSpell(stack, selectedSpell);
            }

            // Check if selected spell is learned
            if (!LearnedSpellsManager.hasLearned(serverPlayer.getUuid(), selectedSpell)) {
                selectedSpell = learnedSpells.get(0);
                setSelectedSpell(stack, selectedSpell);
            }

            // Check cooldown before mana cost
            if (isOnCooldown(stack, world, selectedSpell)) {
                long now = world.getTime();
                long end = SpellDataPersistence.getSpellCooldownEnd(stack, selectedSpell);
                long ticksLeft = Math.max(0, end - now);
                double seconds = ticksLeft / 20.0;
                serverPlayer.sendMessage(net.minecraft.text.Text.literal(String.format("Кулдаун %.1fс", seconds)),
                        true);
                return;
            }

            // Get spell from registry and cast
            Spell spell = SpellRegistry.getSpell(selectedSpell);
            if (spell == null) {
                return;
            }

            // Calculate mana cost with Ether Scepter discount
            int baseCost = spell.getManaCost();
            int finalCost = baseCost;
            if (stack.getItem() instanceof com.manaborn.items.EtherScepterItem) {
                finalCost = Math.max(0, baseCost - com.manaborn.items.EtherScepterItem.MANA_DISCOUNT);
            }

            if (!ManaManager.tryConsume(serverPlayer, finalCost)) {
                serverPlayer.sendMessage(
                        net.minecraft.text.Text.literal("Недостаточно маны: нужно " + finalCost), true);
                return;
            }

            // Cast the spell
            boolean success = spell.cast(world, user, hand);
            if (success) {
                user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
                setCooldown(stack, world, selectedSpell, spell.getCooldown());
            }
        } else {
            user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
        }
    }

    // ========== NBT PERSISTENCE DELEGATION METHODS ==========

    public static SpellType getSelectedSpell(ItemStack stack) {
        return SpellDataPersistence.getSelectedSpell(stack);
    }

    public static SpellType getSelectedSpell(ItemStack stack, ServerPlayerEntity player) {
        SpellType selected = getSelectedSpell(stack);
        List<SpellType> learned = getAvailableSpells(player);

        if (learned.isEmpty()) {
            return null;
        }

        if (selected == null || !learned.contains(selected)) {
            return learned.get(0);
        }

        return selected;
    }

    public static void setSelectedSpell(ItemStack stack, SpellType spell) {
        SpellDataPersistence.setSelectedSpell(stack, spell);
    }

    public static boolean isOnCooldown(ItemStack stack, World world, SpellType spell) {
        return SpellDataPersistence.isSpellOnCooldown(stack, world, spell);
    }

    public static void setCooldown(ItemStack stack, World world, SpellType spell, int durationTicks) {
        SpellDataPersistence.setSpellCooldown(stack, world, spell, durationTicks);
    }

    public static int getCooldownRemainingTicks(ItemStack stack, World world, SpellType spell) {
        return SpellDataPersistence.getCooldownRemainingTicks(stack, world, spell);
    }

    // ========== SPELL CYCLING METHODS ==========

    public static void cycleSpell(ItemStack stack) {
        SpellType current = getSelectedSpell(stack);
        SpellType[] spells = SpellType.values();
        int nextIndex = (current.ordinal() + 1) % spells.length;
        setSelectedSpell(stack, spells[nextIndex]);
    }

    public static void cycleLearnedSpell(ItemStack stack, ServerPlayerEntity player) {
        List<SpellType> learnedSpells = getAvailableSpells(player);

        if (learnedSpells.isEmpty()) {
            player.sendMessage(
                    net.minecraft.text.Text.literal("У вас нет изученных заклинаний! Найдите книги заклинаний."),
                    true);
            return;
        }

        SpellType current = getSelectedSpell(stack, player);
        int currentIndex = learnedSpells.indexOf(current);

        if (currentIndex == -1 || currentIndex >= learnedSpells.size() - 1) {
            setSelectedSpell(stack, learnedSpells.get(0));
        } else {
            setSelectedSpell(stack, learnedSpells.get(currentIndex + 1));
        }
    }

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