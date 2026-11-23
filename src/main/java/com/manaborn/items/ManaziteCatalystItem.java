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
 * Маназитный катализатор.
 *
 * Употребляется (ПКМ) и навсегда увеличивает личный максимум маны
 * на {@link #MAX_MANA_BONUS}. Работает только после пробуждения
 * (Фолиант магии), предел — {@link AwakeningManager#ABSOLUTE_MAX_MANA}.
 */
public class ManaziteCatalystItem extends Item {

    public static final int MAX_MANA_BONUS = 10;

    public ManaziteCatalystItem(Settings settings) {
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

        int result = AwakeningManager.addMaxManaBonus(player, MAX_MANA_BONUS);
        if (result == -1) {
            player.sendMessage(
                    Text.literal("Вам пока не понятна концепция магии. Сначала прочтите Фолиант магии.")
                            .formatted(Formatting.GRAY),
                    false);
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 0.8f, 1.0f);
            return ActionResult.FAIL;
        }
        if (result == 0) {
            player.sendMessage(
                    Text.literal("Ваш резервуар маны достиг предела (").formatted(Formatting.GRAY)
                            .append(Text.literal(String.valueOf(AwakeningManager.ABSOLUTE_MAX_MANA)).formatted(Formatting.AQUA))
                            .append(Text.literal(").").formatted(Formatting.GRAY)),
                    false);
            return ActionResult.FAIL;
        }

        // Расширение резервуара маны
        stack.decrement(1);
        ManaManager.sendSync(player);

        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.0f, 0.8f);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.6f);

        if (world instanceof ServerWorld sw) {
            double cx = player.getX();
            double cy = player.getY() + 1.2;
            double cz = player.getZ();
            sw.spawnParticles(ParticleTypes.WITCH, cx, cy, cz, 20, 0.35, 0.5, 0.35, 0.02);
            sw.spawnParticles(ParticleTypes.END_ROD, cx, cy, cz, 10, 0.25, 0.4, 0.25, 0.01);
        }

        player.sendMessage(
                Text.literal("◆ ").formatted(Formatting.LIGHT_PURPLE)
                        .append(Text.literal("Маназит резонирует с вашей магией!").formatted(Formatting.LIGHT_PURPLE)),
                false);
        player.sendMessage(
                Text.literal("Максимум маны увеличен: ").formatted(Formatting.AQUA)
                        .append(Text.literal(String.valueOf(result)).formatted(Formatting.WHITE))
                        .append(Text.literal(" / " + AwakeningManager.ABSOLUTE_MAX_MANA).formatted(Formatting.GRAY)),
                false);

        return ActionResult.SUCCESS;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("Маназитный катализатор").formatted(Formatting.AQUA);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay,
            Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltipDisplay, tooltip, type);

        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("Колба с эссенцией маназита,").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("резонирующей с резервуаром маны.").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal(""));
        tooltip.accept(Text.literal("ПКМ").formatted(Formatting.YELLOW)
                .append(Text.literal(" - Увеличить максимум маны на ").formatted(Formatting.GRAY))
                .append(Text.literal("+" + MAX_MANA_BONUS).formatted(Formatting.AQUA)));
        tooltip.accept(Text.literal("Предел: " + AwakeningManager.ABSOLUTE_MAX_MANA + " маны")
                .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
    }
}
