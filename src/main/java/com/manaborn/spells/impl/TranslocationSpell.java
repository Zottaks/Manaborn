package com.manaborn.spells.impl;

import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Translocation spell (Entropy magic).
 * Основная логика каста обрабатывается в MagicStaffItem (зарядка и режимы).
 */
public class TranslocationSpell implements LeveledSpell {

    @Override
    public boolean cast(World world, PlayerEntity user, Hand hand) {
        // Каст через посох обрабатывается в MagicStaffItem
        return true;
    }

    @Override
    public int getManaCost() {
        return 30;
    }

    @Override
    public int getManaCost(int level) {
        return 30;
    }

    @Override
    public int getCooldown() {
        return 20 * 20;
    }

    @Override
    public int getCooldown(int level) {
        return switch (level) {
            case 1 -> 20 * 20;
            case 2 -> 50 * 20;
            default -> 200 * 20;
        };
    }

    @Override
    public SpellType getType() {
        return SpellType.TRANSLOCATION;
    }
}
