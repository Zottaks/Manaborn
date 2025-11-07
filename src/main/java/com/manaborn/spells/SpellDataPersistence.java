package com.manaborn.spells;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Utility class for handling spell data persistence in NBT
 */
public class SpellDataPersistence {
    private static final String SPELL_NBT_KEY = "selectedSpell";
    private static final String COOLDOWN_PREFIX = "cd_until_";

    /**
     * Get selected spell from item stack
     */
    public static SpellType getSelectedSpell(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        NbtCompound nbt = customData.copyNbt();
        String spellId = nbt.getString(SPELL_NBT_KEY).orElse("");
        if (spellId.isEmpty()) {
            return null;
        }
        return SpellType.fromId(spellId);
    }

    /**
     * Set selected spell on item stack
     */
    public static void setSelectedSpell(ItemStack stack, SpellType spell) {
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = customData.copyNbt();
        nbt.putString(SPELL_NBT_KEY, spell.getId());
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    /**
     * Get cooldown end time for a spell
     */
    public static long getSpellCooldownEnd(ItemStack stack, SpellType spell) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null)
            return 0L;
        NbtCompound nbt = customData.copyNbt();
        return nbt.getLong(cooldownKey(spell)).orElse(0L);
    }

    /**
     * Set cooldown for a spell
     */
    public static void setSpellCooldown(ItemStack stack, World world, SpellType spell, int durationTicks) {
        long endTick = world.getTime() + durationTicks;
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = customData.copyNbt();
        nbt.putLong(cooldownKey(spell), endTick);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    /**
     * Check if spell is on cooldown
     */
    public static boolean isSpellOnCooldown(ItemStack stack, World world, SpellType spell) {
        long end = getSpellCooldownEnd(stack, spell);
        return world.getTime() < end;
    }

    /**
     * Get remaining cooldown ticks
     */
    public static int getCooldownRemainingTicks(ItemStack stack, World world, SpellType spell) {
        long end = getSpellCooldownEnd(stack, spell);
        long now = world.getTime();
        long diff = end - now;
        return (int) Math.max(0L, diff);
    }

    private static String cooldownKey(SpellType spell) {
        return COOLDOWN_PREFIX + spell.getId();
    }
}
