package com.manaborn.items;

import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * Эфирный скипетр - улучшенная версия магического посоха.
 * Снижает стоимость всех заклинаний на 15 маны.
 */
public class EtherScepterItem extends MagicStaffItem {
    public static final int MANA_DISCOUNT = 15;

    public EtherScepterItem(Settings settings) {
        super(settings);
    }

    /**
     * Переопределяем имя предмета для отображения выбранного заклинания
     */
    @Override
    public Text getName(ItemStack stack) {
        SpellType selectedSpell = SpellManager.getSelectedSpell(stack);

        if (selectedSpell == null) {
            return Text.literal("Эфирный скипетр")
                    .append(Text.literal(" [Нет заклинания]").formatted(Formatting.DARK_GRAY));
        }

        return Text.literal("Эфирный скипетр ")
                .append(Text.literal("[" + selectedSpell.getDisplayName() + "]").formatted(selectedSpell.getColor()));
    }

    /**
     * Добавляем подробное описание предмета с информацией о бонусе
     */
    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay,
            Consumer<Text> tooltip, TooltipType type) {
        SpellType selectedSpell = SpellManager.getSelectedSpell(stack);

        // Пустая строка для разделения
        tooltip.accept(Text.literal(""));

        // Информация о бонусе скипетра
        tooltip.accept(Text.literal("✦ Эфирное усиление ✦").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        tooltip.accept(Text.literal("  Снижает стоимость заклинаний").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("  на " + MANA_DISCOUNT + " маны").formatted(Formatting.AQUA, Formatting.BOLD));

        // Пустая строка
        tooltip.accept(Text.literal(""));

        // Информация о выбранном заклинании
        if (selectedSpell != null) {
            int originalCost = selectedSpell.getManaCost();
            int discountedCost = Math.max(0, originalCost - MANA_DISCOUNT);
            
            tooltip.accept(Text.literal("Активное заклинание:").formatted(Formatting.GOLD));
            tooltip.accept(Text.literal("  " + selectedSpell.getDisplayName())
                    .formatted(selectedSpell.getColor()));
            tooltip.accept(Text.literal("  Стоимость: ")
                    .formatted(Formatting.AQUA)
                    .append(Text.literal(String.valueOf(discountedCost)).formatted(Formatting.GREEN, Formatting.BOLD))
                    .append(Text.literal(" маны ").formatted(Formatting.AQUA))
                    .append(Text.literal("(было " + originalCost + ")").formatted(Formatting.DARK_GRAY)));
        } else {
            tooltip.accept(Text.literal("Активное заклинание:").formatted(Formatting.GOLD));
            tooltip.accept(Text.literal("  Нет")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }

        // Пустая строка
        tooltip.accept(Text.literal(""));

        // Описание скипетра
        tooltip.accept(Text.literal("Скипетр, пропитанный эфирной").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("энергией. Позволяет использовать").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("заклинания с меньшими затратами.").formatted(Formatting.GRAY));

        // Пустая строка
        tooltip.accept(Text.literal(""));

        // Управление
        tooltip.accept(Text.literal("Управление:").formatted(Formatting.YELLOW));
        tooltip.accept(Text.literal("  ПКМ").formatted(Formatting.WHITE)
                .append(Text.literal(" - Использовать заклинание").formatted(Formatting.GRAY)));
        tooltip.accept(Text.literal("  Shift + ПКМ").formatted(Formatting.WHITE)
                .append(Text.literal(" - Открыть меню выбора").formatted(Formatting.GRAY)));

        // Пустая строка
        tooltip.accept(Text.literal(""));

        // Изученные заклинания (только на клиенте)
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
                    int originalCost = spell.getManaCost();
                    int discountedCost = Math.max(0, originalCost - MANA_DISCOUNT);
                    
                    tooltip.accept(Text.literal(prefix + spell.getDisplayName())
                            .formatted(spell.getColor())
                            .append(Text.literal(" (" + discountedCost + " маны)")
                                    .formatted(Formatting.GREEN)));
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
                int originalCost = spell.getManaCost();
                int discountedCost = Math.max(0, originalCost - MANA_DISCOUNT);
                
                tooltip.accept(Text.literal(prefix + spell.getDisplayName())
                        .formatted(spell.getColor())
                        .append(Text.literal(" (" + discountedCost + " маны)")
                                .formatted(Formatting.GREEN)));
            }
        }
    }
}
