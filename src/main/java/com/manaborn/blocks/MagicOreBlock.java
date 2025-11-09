package com.manaborn.blocks;

import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.util.math.intprovider.UniformIntProvider;

/**
 * Руда магической пыли - добывается для получения магической пыли
 * Имеет небольшое свечение (уровень света 7)
 * Генерируется на высотах от -64 до 10
 */
public class MagicOreBlock extends ExperienceDroppingBlock {

    public MagicOreBlock(Settings settings) {
        // Дроп 2-5 опыта при добыче
        super(UniformIntProvider.create(2, 5), settings);
    }
}
