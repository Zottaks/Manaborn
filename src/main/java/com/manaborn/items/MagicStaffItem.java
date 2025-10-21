package com.manaborn.items;

import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class MagicStaffItem extends Item {
    public MagicStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (user.isSneaking()) {
            // Shift + ПКМ для смены заклинания (ВСЕГДА доступно, даже на кулдауне)
            if (!world.isClient()) {
                SpellManager.cycleLearnedSpell(stack, (net.minecraft.server.network.ServerPlayerEntity) user);
                SpellType selected = SpellManager.getSelectedSpell(stack);
                
                // Сообщение с цветом заклинания
                Text message = Text.literal("Выбрано заклинание: ")
                    .append(Text.literal(selected.getDisplayName()).formatted(selected.getColor()))
                    .append(Text.literal(" (стоимость: " + selected.getManaCost() + " маны)"));
                user.sendMessage(message, true);
            }
            return ActionResult.SUCCESS;
        } else {
            // Проверяем кулдаун перед кастом заклинания
            if (user.getItemCooldownManager().isCoolingDown(stack)) {
                return ActionResult.PASS;
            }
            
            // Обычное использование для каста заклинания
            SpellManager.castSpell(world, user, hand);
            return ActionResult.SUCCESS;
        }
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return false; // Отключаем постоянный эффект зачарования
    }
    
    /**
     * Переопределяем имя предмета для отображения выбранного заклинания
     */
    @Override
    public Text getName(ItemStack stack) {
        SpellType selectedSpell = SpellManager.getSelectedSpell(stack);
        
        if (selectedSpell == null) {
            return Text.literal("Магический посох")
                .append(Text.literal(" [Нет заклинания]").formatted(Formatting.DARK_GRAY));
        }
        
        return Text.literal("Магический посох ")
            .append(Text.literal("[" + selectedSpell.getDisplayName() + "]").formatted(selectedSpell.getColor()));
    }
    
    /**
     * Добавляем подробное описание предмета
     * 
     * Метод appendTooltip помечен как deprecated в Minecraft 1.21.10,
     * но остается единственным рабочим способом добавления tooltip.
     * Используем @SuppressWarnings для подавления предупреждения.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay, Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltipDisplay, tooltip, type);
        
        SpellType selectedSpell = SpellManager.getSelectedSpell(stack);
        
        // Пустая строка для разделения
        tooltip.accept(Text.literal(""));
        
        // Информация о выбранном заклинании
        if (selectedSpell != null) {
            tooltip.accept(Text.literal("Активное заклинание:").formatted(Formatting.GOLD));
            tooltip.accept(Text.literal("  " + selectedSpell.getDisplayName())
                .formatted(selectedSpell.getColor()));
            tooltip.accept(Text.literal("  Стоимость: " + selectedSpell.getManaCost() + " маны")
                .formatted(Formatting.AQUA));
        } else {
            tooltip.accept(Text.literal("Активное заклинание:").formatted(Formatting.GOLD));
            tooltip.accept(Text.literal("  Нет")
                .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }
        
        // Пустая строка
        tooltip.accept(Text.literal(""));
        
        // Описание посоха
        tooltip.accept(Text.literal("Древний артефакт, наполненный").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("магической энергией. Позволяет").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("творить мощные заклинания.").formatted(Formatting.GRAY));
        
        // Пустая строка
        tooltip.accept(Text.literal(""));
        
        // Управление
        tooltip.accept(Text.literal("Управление:").formatted(Formatting.YELLOW));
        tooltip.accept(Text.literal("  ПКМ").formatted(Formatting.WHITE)
            .append(Text.literal(" - Использовать заклинание").formatted(Formatting.GRAY)));
        tooltip.accept(Text.literal("  Shift + ПКМ").formatted(Formatting.WHITE)
            .append(Text.literal(" - Сменить заклинание").formatted(Formatting.GRAY)));
        
        // Пустая строка
        tooltip.accept(Text.literal(""));
        
        // Изученные заклинания (только на клиенте)
        // Используем рефлексию для безопасного доступа к клиентскому классу
        try {
            Class<?> clientStateClass = Class.forName("com.manaborn.client.state.ClientLearnedSpellsState");
            java.lang.reflect.Method getLearnedSpellsMethod = clientStateClass.getMethod("getLearnedSpells");
            @SuppressWarnings("unchecked")
            java.util.Set<SpellType> learnedSpells = (java.util.Set<SpellType>) getLearnedSpellsMethod.invoke(null);
            
            if (!learnedSpells.isEmpty()) {
                tooltip.accept(Text.literal("Изученные заклинания:").formatted(Formatting.LIGHT_PURPLE));
                for (SpellType spell : learnedSpells) {
                    boolean isSelected = selectedSpell != null && spell == selectedSpell;
                    String prefix = isSelected ? "  ► " : "  • ";
                    tooltip.accept(Text.literal(prefix + spell.getDisplayName())
                        .formatted(spell.getColor())
                        .append(Text.literal(" (" + spell.getManaCost() + " маны)")
                            .formatted(Formatting.DARK_GRAY)));
                }
            } else {
                tooltip.accept(Text.literal("Нет изученных заклинаний!").formatted(Formatting.RED, Formatting.ITALIC));
                tooltip.accept(Text.literal("Найдите книги заклинаний.").formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        } catch (Exception e) {
            // На сервере или если класс не найден - показываем все заклинания
            tooltip.accept(Text.literal("Доступные заклинания:").formatted(Formatting.LIGHT_PURPLE));
            for (SpellType spell : SpellType.values()) {
                String prefix = spell == selectedSpell ? "  ► " : "  • ";
                tooltip.accept(Text.literal(prefix + spell.getDisplayName())
                    .formatted(spell.getColor())
                    .append(Text.literal(" (" + spell.getManaCost() + " маны)")
                        .formatted(Formatting.DARK_GRAY)));
            }
        }
    }
}