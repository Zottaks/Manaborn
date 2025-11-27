package com.manaborn.blocks.entity;

import com.manaborn.ManabornMod;
import com.manaborn.screen.SpellEnhancementAltarScreenHandler;
import com.manaborn.spells.SpellLevelManager;
import com.manaborn.spells.SpellType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Block Entity для Алтаря Улучшения Заклинаний
 * Содержит 1 слот для ресурсов улучшения
 */
public class SpellEnhancementAltarBlockEntity extends BlockEntity
        implements Inventory, NamedScreenHandlerFactory {

    // 1 слот для ресурсов улучшения
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(1, ItemStack.EMPTY);

    public SpellEnhancementAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ManabornMod.SPELL_ENHANCEMENT_ALTAR_BE, pos, state);
    }

    // ========== INVENTORY IMPLEMENTATION ==========

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world.getBlockEntity(pos) != this) {
            return false;
        }
        return player.squaredDistanceTo((double) pos.getX() + 0.5,
                (double) pos.getY() + 0.5,
                (double) pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        items.clear();
    }

    // ========== SCREEN HANDLER ==========

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.manaborn.spell_enhancement_altar");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new SpellEnhancementAltarScreenHandler(syncId, inv, this);
    }

    // ========== NBT PERSISTENCE ==========
    // NBT methods intentionally omitted for 1.21.10 API compatibility
    // Items will be cleared on world reload (acceptable for this use case)

    // ========== SPELL UPGRADE LOGIC ==========

    /**
     * Попытка улучшить заклинание
     * @param player игрок, улучшающий заклинание
     * @param spell тип заклинания для улучшения
     * @return true если улучшение успешно
     */
    public boolean tryUpgradeSpell(ServerPlayerEntity player, SpellType spell) {
        int currentLevel = SpellLevelManager.getSpellLevel(player.getUuid(), spell);
        
        if (currentLevel >= 3) {
            player.sendMessage(Text.literal("Заклинание уже на максимальном уровне!"), false);
            return false;
        }

        // Проверяем требования
        if (!hasRequiredItems(spell, currentLevel)) {
            player.sendMessage(Text.literal("Недостаточно ресурсов для улучшения!"), false);
            return false;
        }

        // Забираем ресурсы
        consumeRequiredItems(spell, currentLevel);

        // Улучшаем заклинание
        SpellLevelManager.upgradeSpell(player.getUuid(), spell);
        
        int newLevel = SpellLevelManager.getSpellLevel(player.getUuid(), spell);
        player.sendMessage(Text.literal(spell.getDisplayName() + " улучшено до уровня " + newLevel + "!"), false);
        
        markDirty();
        return true;
    }

    /**
     * Проверка наличия требуемых ресурсов для конкретного заклинания
     */
    private boolean hasRequiredItems(SpellType spell, int currentLevel) {
        if (currentLevel == 1) {
            // Upgrade 1 -> 2
            int dustRequired = getDustRequiredFor1to2(spell);
            return hasItem(ManabornMod.MAGIC_DUST, dustRequired);
        } else if (currentLevel == 2) {
            // Upgrade 2 -> 3
            int etherRequired = getEtherRequiredFor2to3(spell);
            return hasItem(ManabornMod.ETHER, etherRequired);
        }
        return false;
    }

    /**
     * Потребление требуемых ресурсов для конкретного заклинания
     */
    private void consumeRequiredItems(SpellType spell, int currentLevel) {
        if (currentLevel == 1) {
            int dustRequired = getDustRequiredFor1to2(spell);
            consumeItem(ManabornMod.MAGIC_DUST, dustRequired);
        } else if (currentLevel == 2) {
            int etherRequired = getEtherRequiredFor2to3(spell);
            consumeItem(ManabornMod.ETHER, etherRequired);
        }
    }
    
    /**
     * Получить количество пыли для улучшения 1 -> 2
     */
    private int getDustRequiredFor1to2(SpellType spell) {
        return switch (spell) {
            case FIREBALL, INNER_HEAT -> 15;
            case ICE_SHARD, FROST_AURA -> 20;
            case FIRE_WALL, SOUND_STRIKE, BATTLE_HYMN -> 25;
            case WEAKNESS_CURSE, HEAT_WEAPON -> 30;
            case ABSORBING_BARRIER, ICE_WALL -> 40;
        };
    }
    
    /**
     * Получить количество эфира для улучшения 2 -> 3
     */
    private int getEtherRequiredFor2to3(SpellType spell) {
        return switch (spell) {
            case INNER_HEAT -> 1;
            case WEAKNESS_CURSE -> 2;
            case FIREBALL, ICE_SHARD, FROST_AURA, ICE_WALL, HEAT_WEAPON, SOUND_STRIKE, BATTLE_HYMN -> 3;
            case FIRE_WALL, ABSORBING_BARRIER -> 4;
        };
    }

    /**
     * Проверка наличия предмета в инвентаре
     */
    private boolean hasItem(net.minecraft.item.Item item, int count) {
        int found = 0;
        for (ItemStack stack : items) {
            if (stack.getItem() == item) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }

    /**
     * Потребление предмета из инвентаря
     */
    private void consumeItem(net.minecraft.item.Item item, int count) {
        int remaining = count;
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack stack = items.get(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.decrement(toRemove);
                remaining -= toRemove;
            }
        }
    }

    // ========== TICKER ==========

    public static void serverTick(World world, BlockPos pos, BlockState state,
            SpellEnhancementAltarBlockEntity blockEntity) {
        // Пока нет активной логики тикера
        // Можно добавить частицы или анимацию позже
    }
}
