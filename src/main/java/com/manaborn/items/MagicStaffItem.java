package com.manaborn.items;

import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class MagicStaffItem extends Item {
    public MagicStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        if (user.isSneaking()) {
            // Shift + ПКМ для смены заклинания
            if (!world.isClient()) {
                SpellManager.cycleSpell(stack);
                SpellType selected = SpellManager.getSelectedSpell(stack);
                user.sendMessage(Text.literal("Выбрано заклинание: " + selected.getDisplayName() + " (стоимость: " + selected.getManaCost() + " маны)"), true);
            }
            return ActionResult.SUCCESS;
        } else {
            // Обычное использование для каста заклинания
            SpellManager.castSpell(world, user, hand);
            return ActionResult.SUCCESS;
        }
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Волшебная палочка всегда светится
    }
}