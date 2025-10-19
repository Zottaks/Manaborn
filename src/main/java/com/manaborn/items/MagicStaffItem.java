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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class MagicStaffItem extends Item {
    private static final String LOG_FILE = "manaborn_debug.txt";
    
    public MagicStaffItem(Settings settings) {
        super(settings);
    }
    
    /**
     * Логирование в файл для отладки
     */
    private void log(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            writer.println("[" + timestamp + "] " + message);
            writer.flush();
        } catch (IOException e) {
            System.err.println("Ошибка записи в лог: " + e.getMessage());
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // ЛОГИРОВАНИЕ для отладки
        String logMessage = "use() вызван. Sneaking: " + user.isSneaking() + 
                           ", Cooldown: " + user.getItemCooldownManager().isCoolingDown(stack) +
                           ", Side: " + (world.isClient() ? "CLIENT" : "SERVER");
        log(logMessage);
        System.out.println("[MagicStaff] " + logMessage);
        
        if (user.isSneaking()) {
            // Shift + ПКМ для смены заклинания (ВСЕГДА доступно, даже на кулдауне)
            log("Попытка смены заклинания (Shift + ПКМ)");
            if (!world.isClient()) {
                SpellManager.cycleSpell(stack);
                SpellType selected = SpellManager.getSelectedSpell(stack);
                log("Заклинание сменено на: " + selected.getDisplayName());
                
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
                log("ЗАБЛОКИРОВАНО КУЛДАУНОМ!");
                System.out.println("[MagicStaff] Заблокировано кулдауном!");
                return ActionResult.PASS;
            }
            
            log("Каст заклинания");
            // Обычное использование для каста заклинания
            SpellManager.castSpell(world, user, hand);
            return ActionResult.SUCCESS;
        }
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Волшебная палочка всегда светится
    }
    
    /**
     * Переопределяем имя предмета для отображения выбранного заклинания
     */
    @Override
    public Text getName(ItemStack stack) {
        SpellType selectedSpell = SpellManager.getSelectedSpell(stack);
        
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
        tooltip.accept(Text.literal("Активное заклинание:").formatted(Formatting.GOLD));
        tooltip.accept(Text.literal("  " + selectedSpell.getDisplayName())
            .formatted(selectedSpell.getColor()));
        tooltip.accept(Text.literal("  Стоимость: " + selectedSpell.getManaCost() + " маны")
            .formatted(Formatting.AQUA));
        
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
        
        // Доступные заклинания
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