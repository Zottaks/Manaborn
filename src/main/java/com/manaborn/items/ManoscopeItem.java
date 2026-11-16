package com.manaborn.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Маноскоп - магические очки для обнаружения магической пыли
 * Надеваются в слот головы и позволяют видеть пылинки в мире
 */
public class ManoscopeItem extends Item {
    
    public ManoscopeItem(Settings settings) {
        super(settings);
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay, Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltipDisplay, tooltip, type);
        
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Магические очки для обнаружения").formatted(Formatting.DARK_AQUA));
        tooltip.accept(Text.literal("магической пыли в мире.").formatted(Formatting.DARK_AQUA));
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Надевается в слот головы").formatted(Formatting.GRAY));
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Магическое свечение
    }
}
