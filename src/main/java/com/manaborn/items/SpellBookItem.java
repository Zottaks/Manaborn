package com.manaborn.items;

import com.manaborn.spells.LearnedSpellsManager;
import com.manaborn.spells.SpellType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Книга заклинания
 * 
 * При использовании (ПКМ) изучает заклинание и исчезает.
 * Каждая книга содержит одно конкретное заклинание.
 */
public class SpellBookItem extends Item {
    private final SpellType spell;
    
    public SpellBookItem(Settings settings, SpellType spell) {
        super(settings);
        this.spell = spell;
    }
    
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            user.sendMessage(
                Text.literal("Используйте Алтарь Заклинаний для изучения этой книги.")
                    .formatted(Formatting.YELLOW),
                false
            );
            world.playSound(null, user.getBlockPos(), SoundEvents.UI_BUTTON_CLICK,
                SoundCategory.PLAYERS, 0.6f, 1.0f);
        }
        return ActionResult.SUCCESS;
    }
    
    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("Книга заклинания: ")
            .formatted(Formatting.LIGHT_PURPLE)
            .append(Text.literal(spell.getDisplayName()).formatted(spell.getColor()));
    }
    
    /**
     * Добавляем описание книги
     */
    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay, Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltipDisplay, tooltip, type);
        
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Древний том, содержащий знания").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("о магическом заклинании.").formatted(Formatting.GRAY));
        
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Заклинание: ").formatted(Formatting.GOLD)
            .append(Text.literal(spell.getDisplayName()).formatted(spell.getColor())));
        tooltip.accept(Text.literal("Стоимость: " + spell.getManaCost() + " маны").formatted(Formatting.AQUA));
        
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Используйте Алтарь Заклинаний").formatted(Formatting.YELLOW)
            .append(Text.literal(" для изучения").formatted(Formatting.GRAY)));
        tooltip.accept(Text.literal("(книга тратится при успешном изучении)").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }

    public SpellType getSpell() {
        return spell;
    }
}
