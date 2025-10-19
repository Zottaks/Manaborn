package com.manaborn.spells;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.item.ItemStack;
import com.manaborn.mana.ManaManager;
import com.manaborn.entities.IceShardEntity;

public class SpellManager {
    private static final String SPELL_NBT_KEY = "selectedSpell";
    
    public static void castSpell(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        SpellType selectedSpell = getSelectedSpell(stack);
        
        if (!world.isClient()) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;
            if (!ManaManager.tryConsume(serverPlayer, selectedSpell.getManaCost())) {
                return;
            }
            
            switch (selectedSpell) {
                case FIREBALL:
                    castFireball(world, user);
                    break;
                case ICE_SHARD:
                    castIceShard(world, user);
                    break;
            }
        }
        
        user.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
        user.getItemCooldownManager().set(stack, 100);
    }
    
    private static void castFireball(World world, PlayerEntity user) {
        SmallFireballEntity fireball = new SmallFireballEntity(world, user, user.getRotationVector());
        fireball.setPos(user.getX(), user.getEyeY() - 0.1, user.getZ());
        world.spawnEntity(fireball);
        world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
    
    private static void castIceShard(World world, PlayerEntity user) {
        IceShardEntity iceShard = new IceShardEntity(world, user);
        iceShard.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 2.0f, 1.0f);
        world.spawnEntity(iceShard);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }
    
    public static SpellType getSelectedSpell(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return SpellType.FIREBALL; // по умолчанию
        }
        NbtCompound nbt = customData.copyNbt();
        String spellId = nbt.getString(SPELL_NBT_KEY).orElse("fireball");
        return SpellType.fromId(spellId);
    }
    
    public static void setSelectedSpell(ItemStack stack, SpellType spell) {
        NbtComponent customData = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = customData.copyNbt();
        nbt.putString(SPELL_NBT_KEY, spell.getId());
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
    
    public static void cycleSpell(ItemStack stack) {
        SpellType current = getSelectedSpell(stack);
        SpellType[] spells = SpellType.values();
        int nextIndex = (current.ordinal() + 1) % spells.length;
        setSelectedSpell(stack, spells[nextIndex]);
    }
}