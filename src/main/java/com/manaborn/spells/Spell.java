package com.manaborn.spells;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Interface for all spells in the Manaborn mod
 */
public interface Spell {
    /**
     * Cast the spell
     * 
     * @param world the world
     * @param user  the player casting the spell
     * @param hand  the hand used
     * @return true if spell was cast successfully
     */
    boolean cast(World world, PlayerEntity user, Hand hand);

    /**
     * Get mana cost of this spell
     * 
     * @return mana cost
     */
    int getManaCost();

    /**
     * Get cooldown in ticks for this spell
     * 
     * @return cooldown in ticks
     */
    int getCooldown();

    /**
     * Get spell type identifier
     * 
     * @return spell type
     */
    SpellType getType();

    /**
     * Called every server tick for active effects of this spell
     * 
     * @param world the server world
     */
    default void tick(ServerWorld world) {
        // Default implementation does nothing
    }

    /**
     * Check if this spell has active effects that need ticking
     * 
     * @return true if spell has active effects
     */
    default boolean hasActiveEffects() {
        return false;
    }

    /**
     * Clean up active effects when spell expires
     * 
     * @param world the server world
     */
    default void cleanup(ServerWorld world) {
        // Default implementation does nothing
    }
}
