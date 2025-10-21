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
        ItemStack stack = user.getStackInHand(hand);
        
        if (!world.isClient()) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;
            
            // Проверяем, изучено ли уже заклинание
            if (LearnedSpellsManager.hasLearned(serverPlayer.getUuid(), spell)) {
                user.sendMessage(
                    Text.literal("Вы уже изучили заклинание ")
                        .append(Text.literal(spell.getDisplayName()).formatted(spell.getColor()))
                        .append(Text.literal("!")),
                    false
                );
                world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, 
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
                return ActionResult.FAIL;
            }
            
            // Изучаем заклинание
            LearnedSpellsManager.learnSpell(serverPlayer, spell);
            
            // Уменьшаем стак на 1 (книга исчезает)
            stack.decrementUnlessCreative(1, user);
            
            // Сообщение об успехе
            user.sendMessage(
                Text.literal("✨ Вы изучили заклинание ")
                    .formatted(Formatting.GOLD)
                    .append(Text.literal(spell.getDisplayName()).formatted(spell.getColor()))
                    .append(Text.literal("!").formatted(Formatting.GOLD)),
                false
            );
            
            // Звук изучения
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, 
                SoundCategory.PLAYERS, 1.0f, 1.5f);
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Книга светится как зачарованный предмет
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
        tooltip.accept(Text.literal("ПКМ").formatted(Formatting.YELLOW)
            .append(Text.literal(" - Изучить заклинание").formatted(Formatting.GRAY)));
        tooltip.accept(Text.literal("(книга исчезнет после изучения)").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }
}
