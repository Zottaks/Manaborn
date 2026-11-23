package com.manaborn.items;

import com.manaborn.mana.AwakeningManager;
import com.manaborn.mana.ManaManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Книга Пробуждения.
 *
 * Первый шаг в магии Manaborn: пока игрок её не прочитал (ПКМ),
 * у него нет маны и он не может изучать заклинания на Алтаре Изучения.
 * После прочтения книга рассыпается, у игрока появляется мана
 * с личным максимумом {@link AwakeningManager#AWAKENED_MAX_MANA}.
 */
public class AwakeningBookItem extends Item {

    public AwakeningBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        if (AwakeningManager.isAwakened(player.getUuid())) {
            player.sendMessage(
                    Text.literal("Ваша магия уже пробуждена.").formatted(Formatting.GRAY),
                    true);
            return ActionResult.FAIL;
        }

        // Пробуждение!
        AwakeningManager.awaken(player);
        ManaManager.setMana(player.getUuid(), AwakeningManager.AWAKENED_MAX_MANA);
        ManaManager.sendSync(player);

        // Книга рассыпается
        stack.decrement(1);

        // Звук и партиклы
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 0.9f);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.9f, 1.2f);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6f, 1.4f);

        if (world instanceof ServerWorld sw) {
            double cx = player.getX();
            double cy = player.getY() + 1.0;
            double cz = player.getZ();
            sw.spawnParticles(ParticleTypes.ENCHANT, cx, cy, cz, 60, 0.5, 0.8, 0.5, 0.05);
            sw.spawnParticles(ParticleTypes.WITCH, cx, cy + 0.5, cz, 25, 0.4, 0.6, 0.4, 0.02);
            sw.spawnParticles(ParticleTypes.END_ROD, cx, cy, cz, 12, 0.3, 0.5, 0.3, 0.01);
        }

        player.sendMessage(
                Text.literal("✦ ").formatted(Formatting.LIGHT_PURPLE)
                        .append(Text.literal("Вы чувствуете, как внутри просыпается магия...").formatted(Formatting.LIGHT_PURPLE)),
                false);
        player.sendMessage(
                Text.literal("Мана пробуждена: ").formatted(Formatting.AQUA)
                        .append(Text.literal(AwakeningManager.AWAKENED_MAX_MANA + "/" + AwakeningManager.AWAKENED_MAX_MANA)
                                .formatted(Formatting.WHITE))
                        .append(Text.literal(". Теперь вы можете изучать заклинания на Алтаре Изучения.")
                                .formatted(Formatting.GRAY)),
                false);

        return ActionResult.SUCCESS;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("Фолиант магии").formatted(Formatting.LIGHT_PURPLE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay,
            Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltipDisplay, tooltip, type);

        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Древний фолиант, в котором дремлет").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("искра магии, скрытая в каждом.").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("ПКМ").formatted(Formatting.YELLOW)
                .append(Text.literal(" - Прочитать и пробудить ману").formatted(Formatting.GRAY)));
        tooltip.accept(Text.literal("После прочтения: максимум маны ")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.literal(String.valueOf(AwakeningManager.AWAKENED_MAX_MANA)).formatted(Formatting.AQUA)));
        tooltip.accept(Text.literal("(книга рассыпется после прочтения)").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }
}
