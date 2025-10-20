package com.manaborn.blocks;

import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.util.math.intprovider.UniformIntProvider;

/**
 * Руда магической пыли - добывается для получения магической пыли
 */
public class MagicOreBlock extends ExperienceDroppingBlock {
    
    public MagicOreBlock(Settings settings) {
        // Дроп 2-5 опыта при добыче
        super(UniformIntProvider.create(2, 5), settings);
    }
}
