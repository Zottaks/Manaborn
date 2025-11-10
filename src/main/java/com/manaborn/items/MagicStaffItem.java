package com.manaborn.items;

import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

public class MagicStaffItem extends Item {
    public MagicStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public void onCraft(ItemStack stack, World world) {
        // Выдаем ачивку при крафте волшебной палочки
        if (world instanceof ServerWorld serverWorld) {
            // Ищем ближайшего игрока (крафт обычно происходит в контексте игрока)
            serverWorld.getPlayers().forEach(player -> {
                // Проверяем, есть ли у игрока только что созданный предмет в инвентаре
                if (player.getInventory().contains(stack)) {
                    grantMagicStaffAdvancement(player, serverWorld);
                }
            });
        }
        super.onCraft(stack, world);
    }

    private void grantMagicStaffAdvancement(ServerPlayerEntity player, ServerWorld world) {
        // Получаем advancement по ID
        AdvancementEntry advancement = world.getServer().getAdvancementLoader().get(
                Identifier.of("manaborn", "magic_staff"));

        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);
            if (!progress.isDone()) {
                // Выдаем ачивку
                player.getAdvancementTracker().grantCriterion(advancement, "has_magic_staff");
            }
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.isSneaking()) {
            // Shift + ПКМ теперь обрабатывается на клиенте (открывает радиальное меню)
            // На сервере просто возвращаем SUCCESS, чтобы не мешать клиентской логике
            return ActionResult.SUCCESS;
        } else {
            // Если выбрана Стена Льда — начинаем удержание ПКМ (зарядка)
            SpellType selected = SpellManager.getSelectedSpell(stack);
            if (selected == SpellType.ICE_WALL) {
                user.setCurrentHand(hand);
                return ActionResult.CONSUME;
            }
            // Иначе — обычный мгновенный каст
            SpellManager.castSpell(world, user, hand);
            return ActionResult.SUCCESS;
        }
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player))
            return false;
        SpellType selected = SpellManager.getSelectedSpell(stack);
        if (selected != SpellType.ICE_WALL)
            return false;
        if (world.isClient())
            return false;

        int usedTicks = getMaxUseTime(stack, user) - remainingUseTicks;
        if (usedTicks < 0) {
            usedTicks = 0;
        }
        // Кэпним заряд до 3 секунд (60 тиков), чтобы длительное удержание не
        // "перезапускало" логику
        usedTicks = Math.min(usedTicks, 60);
        float usedSeconds = usedTicks / 20.0f;

        // Пороговые уровни заряда (ускоренные)
        int width, height, durationTicks, manaCost;
        if (usedSeconds >= 3.0f) {
            // Большая стена 4x6, держится 10с, 45 маны
            width = 4;
            height = 6;
            durationTicks = 10 * 20;
            manaCost = 45;
        } else if (usedSeconds >= 1.5f) {
            // Средняя стена 3x4, держится 20с, 25 маны
            width = 3;
            height = 4;
            durationTicks = 20 * 20;
            manaCost = 25;
        } else {
            // Малая стена 2x2, держится 35с, 10 маны
            width = 2;
            height = 2;
            durationTicks = 35 * 20;
            manaCost = 10;
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        if (!com.manaborn.mana.ManaManager.tryConsume(serverPlayer, manaCost)) {
            serverPlayer.sendMessage(Text.literal("Недостаточно маны: нужно " + manaCost), true);
            return false;
        }

        if (world instanceof ServerWorld sw) {
            SpellManager.castIceWall(sw, serverPlayer, width, height, durationTicks);
        }
        // Кулдаун для стены льда не ставим (зарядка уже ограничивает), при
        // необходимости добавим позже
        return true;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        // Как у лука: большое время использования, чтобы можно было держать бесконечно
        // Фактический уровень заряда кэпним серверно на 3 сек (60 тиков)
        return 72000;
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
     * TEST TEST
     * Добавляем подробное описание предмета
     * 
     * Метод appendTooltip помечен как deprecated в Minecraft 1.21.10,
     * но остается единственным рабочим способом добавления tooltip.
     * Используем @SuppressWarnings для подавления предупреждения.
     */
    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay,
            Consumer<Text> tooltip, TooltipType type) {
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
                .append(Text.literal(" - Открыть меню выбора").formatted(Formatting.GRAY)));

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