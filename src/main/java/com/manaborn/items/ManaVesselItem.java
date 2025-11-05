package com.manaborn.items;

import com.manaborn.mana.ManaManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ManaVesselItem extends Item {
    public static final int CAPACITY = 200;
    private static final String KEY = "stored_mana";

    public ManaVesselItem(Settings settings) {
        super(settings);
    }

    public static int getStored(ItemStack stack) {
        NbtComponent data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        int val = data.copyNbt().getInt(KEY).orElse(0);
        return Math.max(0, Math.min(CAPACITY, val));
    }

    public static void setStored(ItemStack stack, int amount) {
        int clamped = Math.max(0, Math.min(CAPACITY, amount));
        NbtComponent data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        var nbt = data.copyNbt();
        nbt.putInt(KEY, clamped);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) return ActionResult.SUCCESS;
        ServerPlayerEntity sp = (ServerPlayerEntity) user;
        int stored = getStored(stack);
        if (stored <= 0) return ActionResult.FAIL;
        int current = ManaManager.getMana(sp.getUuid());
        int max = ManaManager.MAX_MANA;
        if (current >= max) return ActionResult.FAIL;
        int toRestore = Math.min(30, Math.min(stored, max - current));
        ManaManager.setMana(sp.getUuid(), current + toRestore);
        ManaManager.sendSync(sp);
        setStored(stack, stored - toRestore);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.1f);
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        user.sendMessage(Text.literal("+" + toRestore + " маны"), true);
        return ActionResult.SUCCESS;
    }
}
