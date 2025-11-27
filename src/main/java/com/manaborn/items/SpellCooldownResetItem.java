package com.manaborn.items;

import com.manaborn.spells.SpellDataPersistence;
import com.manaborn.spells.SpellType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Предмет для сброса кулдауна заклинания за 1 уровень опыта
 */
public class SpellCooldownResetItem extends Item {
    
    public SpellCooldownResetItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        
        ServerPlayerEntity player = (ServerPlayerEntity) user;
        ItemStack stack = player.getStackInHand(hand);
        
        // Проверяем есть ли посох в другой руке
        ItemStack otherHandStack = hand == Hand.MAIN_HAND ? 
            player.getOffHandStack() : player.getMainHandStack();
        
        if (!(otherHandStack.getItem() instanceof MagicStaffItem)) {
            player.sendMessage(
                Text.literal("Держите посох в другой руке!").formatted(Formatting.RED),
                true
            );
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, 
                SoundCategory.PLAYERS, 0.8f, 1.0f);
            return ActionResult.FAIL;
        }
        
        // Получаем выбранное заклинание
        SpellType selectedSpell = SpellDataPersistence.getSelectedSpell(otherHandStack);
        
        if (selectedSpell == null) {
            player.sendMessage(
                Text.literal("На посохе не выбрано заклинание!").formatted(Formatting.RED),
                true
            );
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, 
                SoundCategory.PLAYERS, 0.8f, 1.0f);
            return ActionResult.FAIL;
        }
        
        // Проверяем есть ли кулдаун на заклинании
        long now = world.getTime();
        long cooldownEnd = SpellDataPersistence.getSpellCooldownEnd(otherHandStack, selectedSpell);
        
        if (cooldownEnd <= now) {
            player.sendMessage(
                Text.literal("На заклинании ").formatted(Formatting.GRAY)
                    .append(Text.literal(selectedSpell.getDisplayName()).formatted(selectedSpell.getColor()))
                    .append(Text.literal(" нет кулдауна!").formatted(Formatting.GRAY)),
                true
            );
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, 
                SoundCategory.PLAYERS, 0.8f, 1.0f);
            return ActionResult.FAIL;
        }
        
        // Проверяем опыт
        if (player.experienceLevel < 1) {
            player.sendMessage(
                Text.literal("Недостаточно опыта! Требуется: 1 уровень").formatted(Formatting.RED),
                true
            );
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, 
                SoundCategory.PLAYERS, 0.8f, 1.0f);
            return ActionResult.FAIL;
        }
        
        // Тратим опыт
        player.addExperienceLevels(-1);
        
        // Убираем кулдаун
        SpellDataPersistence.removeCooldown(otherHandStack, selectedSpell);
        
        // Тратим предмет
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        
        // Эффекты
        player.sendMessage(
            Text.literal("✨ Кулдаун на ").formatted(Formatting.GREEN)
                .append(Text.literal(selectedSpell.getDisplayName()).formatted(selectedSpell.getColor()))
                .append(Text.literal(" сброшен!").formatted(Formatting.GREEN)),
            true
        );
        
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 
            SoundCategory.PLAYERS, 1.0f, 1.5f);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, 
            SoundCategory.PLAYERS, 0.5f, 2.0f);
        
        // Частицы
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                net.minecraft.particle.ParticleTypes.ENCHANT,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                20,
                0.3, 0.5, 0.3,
                0.1
            );
            serverWorld.spawnParticles(
                net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER,
                player.getX(),
                player.getY() + 1.0,
                player.getZ(),
                10,
                0.3, 0.5, 0.3,
                0.05
            );
        }
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("Свиток Сброса Кулдауна").formatted(Formatting.LIGHT_PURPLE);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, 
            net.minecraft.component.type.TooltipDisplayComponent tooltipDisplay,
            java.util.function.Consumer<Text> tooltip, 
            net.minecraft.item.tooltip.TooltipType type) {
        super.appendTooltip(stack, context, tooltipDisplay, tooltip, type);
        
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Древний магический свиток,").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("способный обнулить время").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("восстановления заклинания.").formatted(Formatting.GRAY));
        
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Использование:").formatted(Formatting.GOLD));
        tooltip.accept(Text.literal("• Держите посох в руке").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("• Выберите заклинание с кулдауном").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("• ПКМ этим предметом").formatted(Formatting.YELLOW));
        
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Стоимость: ").formatted(Formatting.AQUA)
            .append(Text.literal("1 уровень опыта").formatted(Formatting.GREEN)));
        
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Предмет исчезнет после").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        tooltip.accept(Text.literal("использования").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }
}
