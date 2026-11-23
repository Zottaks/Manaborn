package com.manaborn.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Магическая пыль - базовый ресурс для крафта магических предметов
 */
public class MagicDustItem extends Item {
    
    public MagicDustItem(Settings settings) {
        super(settings);
    }
    
    /**
     * Добавляем описание предмета
     */
    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay, Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltipDisplay, tooltip, type);
        
        // Пустая строка
        tooltip.accept(Text.literal(""));
        
        // Описание
        tooltip.accept(Text.literal("Мерцающая магическая пыль,").formatted(Formatting.DARK_GRAY));
        tooltip.accept(Text.literal("обнаруживаемая с помощью маноскопа.").formatted(Formatting.DARK_GRAY));
        
        // Пустая строка
        tooltip.accept(Text.literal(""));
        
        // Использование
        tooltip.accept(Text.literal("Используется для крафта").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("магических предметов.").formatted(Formatting.GRAY));
    }
    
    /**
     * Эффект зачарования отключён — пыль не должна блестеть
     */
    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }
}
