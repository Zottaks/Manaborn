package com.manaborn.items;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.util.Formatting;
import java.util.function.Consumer;

import com.manaborn.blocks.ManaConverterBlock;

public class ManaConverterBlockItem extends BlockItem {

    public ManaConverterBlockItem(ManaConverterBlock block, Settings settings) {
        super(block, settings);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay,
            Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltipDisplay, tooltip, type);

        // Пустая строка
        tooltip.accept(Text.literal(""));

        // Описание
        tooltip.accept(Text.literal("Преобразует предметы").formatted(Formatting.DARK_GRAY));
        tooltip.accept(Text.literal("в магическую ману.").formatted(Formatting.DARK_GRAY));

    }
}
