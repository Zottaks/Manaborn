package com.manaborn.items;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Книга рецептов Manaborn.
 * ПКМ — открывает иллюстрированный справочник крафтов мода.
 */
public class RecipeBookItem extends Item {

    /** Назначается из клиентского инициализатора — открывает экран книги. */
    public static Runnable clientOpener;

    public RecipeBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            if (clientOpener != null) {
                clientOpener.run();
            }
        } else {
            world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN,
                    SoundCategory.PLAYERS, 0.9f, 1.1f);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("Книга рецептов").formatted(Formatting.GOLD);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay,
            Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltipDisplay, tooltip, type);
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Иллюстрированный справочник").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("крафтов мира Manaborn.").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("ПКМ").formatted(Formatting.YELLOW)
                .append(Text.literal(" - Открыть книгу").formatted(Formatting.GRAY)));
    }
}
