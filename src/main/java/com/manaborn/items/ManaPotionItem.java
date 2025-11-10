package com.manaborn.items;

import com.manaborn.mana.ManaManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Кристалл маны - восстанавливает 30 единиц маны при использовании
 */
public class ManaPotionItem extends Item {
    private static final int MANA_RESTORE_AMOUNT = 30;
    
    public ManaPotionItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (!world.isClient()) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;
            
            // Получаем текущую ману
            int currentMana = ManaManager.getMana(serverPlayer.getUuid());
            int maxMana = ManaManager.MAX_MANA;
            
            // Проверяем, не полная ли уже мана
            if (currentMana >= maxMana) {
                // Отправляем сообщение, что мана уже полная
                user.sendMessage(
                    Text.literal("Мана уже полная!").formatted(Formatting.YELLOW), 
                    true
                );
                return ActionResult.FAIL;
            }
            
            // Восстанавливаем ману
            int newMana = Math.min(currentMana + MANA_RESTORE_AMOUNT, maxMana);
            int actualRestored = newMana - currentMana;
            ManaManager.setMana(serverPlayer.getUuid(), newMana);
            
            // Синхронизируем с клиентом
            ManaManager.sendSync(serverPlayer);
            
            // Отправляем сообщение о восстановлении
            user.sendMessage(
                Text.literal("Восстановлено ")
                    .append(Text.literal("+" + actualRestored).formatted(Formatting.AQUA))
                    .append(Text.literal(" маны")),
                true
            );
            
            // Звук использования кристалла (звук поглощения опыта)
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.2f);
            
            // Уменьшаем количество предметов в стаке (кристалл расходуется всегда)
            stack.decrement(1);
            
            // Статистика
            user.incrementStat(Stats.USED.getOrCreateStat(this));
        }
        
        return ActionResult.SUCCESS;
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
        
        // Основной эффект
        tooltip.accept(Text.literal("Восстанавливает ").formatted(Formatting.GRAY)
            .append(Text.literal(MANA_RESTORE_AMOUNT + " маны").formatted(Formatting.AQUA)));
        
        // Пустая строка
        tooltip.accept(Text.literal(""));
        
        // Описание
        tooltip.accept(Text.literal("Кристаллизованная магическая").formatted(Formatting.DARK_GRAY));
        tooltip.accept(Text.literal("энергия в чистом виде.").formatted(Formatting.DARK_GRAY));
        
        // Пустая строка
        tooltip.accept(Text.literal(""));
        
        // Использование
        tooltip.accept(Text.literal("ПКМ").formatted(Formatting.YELLOW)
            .append(Text.literal(" - Использовать кристалл").formatted(Formatting.GRAY)));
    }
}
