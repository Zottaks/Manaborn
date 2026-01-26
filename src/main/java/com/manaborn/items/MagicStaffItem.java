package com.manaborn.items;

import com.manaborn.mana.ManaManager;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.SpellLevelManager;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellRegistry;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MagicStaffItem extends Item {
    private static final ConcurrentHashMap<UUID, Long> lastTranslocationClick = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> translocationAoERequested = new ConcurrentHashMap<>();
    private static final int TRANSLOCATION_DOUBLE_CLICK_WINDOW = 7;

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

        // Если выбрана Стена Льда или Транслокация — начинаем удержание ПКМ (зарядка)
        SpellType selected = SpellManager.getSelectedSpell(stack);
        if (selected == SpellType.ICE_WALL || selected == SpellType.TRANSLOCATION) {
            if (!world.isClient() && selected == SpellType.TRANSLOCATION && user instanceof ServerPlayerEntity serverPlayer) {
                handleTranslocationClick(serverPlayer, world);
            }
            user.setCurrentHand(hand);
            return ActionResult.CONSUME;
        }
        // Для всех остальных заклинаний — обычный мгновенный каст, даже при зажатом Shift
        SpellManager.castSpell(world, user, hand);
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player))
            return false;
        SpellType selected = SpellManager.getSelectedSpell(stack);
        if (world.isClient())
            return false;

        if (selected == SpellType.TRANSLOCATION) {
            return castTranslocation(stack, world, player, remainingUseTicks);
        }
        if (selected != SpellType.ICE_WALL)
            return false;

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        
        // Получаем уровень заклинания Ice Wall у игрока
        int spellLevel = com.manaborn.spells.SpellLevelManager.getSpellLevel(serverPlayer.getUuid(), SpellType.ICE_WALL);
        
        int usedTicks = getMaxUseTime(stack, user) - remainingUseTicks;
        if (usedTicks < 0) {
            usedTicks = 0;
        }
        // Кэпним заряд до 3 секунд (60 тиков), чтобы длительное удержание не
        // "перезапускало" логику
        usedTicks = Math.min(usedTicks, 60);
        float usedSeconds = usedTicks / 20.0f;

        // Пороговые уровни заряда в зависимости от уровня заклинания
        int width, height, durationTicks, manaCost;
        
        if (spellLevel >= 3 && usedSeconds >= 3.0f) {
            // Уровень 3: Highground (большая стена 4x6), держится 10с, 45 маны
            width = 4;
            height = 6;
            durationTicks = 10 * 20;
            manaCost = 45;
        } else if (spellLevel >= 2 && usedSeconds >= 1.5f) {
            // Уровень 2+: Средняя стена 3x4, держится 20с, 25 маны
            width = 3;
            height = 4;
            durationTicks = 20 * 20;
            manaCost = 25;
        } else {
            // Уровень 1 ИЛИ недостаточно времени: Малая стена 2x2, держится 35с, 10 маны
            width = 2;
            height = 2;
            durationTicks = 35 * 20;
            manaCost = 10;
        }
        
        // Apply Ether Scepter discount if applicable
        int finalManaCost = manaCost;
        if (stack.getItem() instanceof EtherScepterItem) {
            finalManaCost = Math.max(0, manaCost - EtherScepterItem.MANA_DISCOUNT);
        }
        
        if (!com.manaborn.mana.ManaManager.tryConsume(serverPlayer, finalManaCost)) {
            serverPlayer.sendMessage(Text.literal("Недостаточно маны: нужно " + finalManaCost), true);
            return false;
        }

        if (world instanceof ServerWorld sw) {
            SpellManager.castIceWall(sw, serverPlayer, width, height, durationTicks);
        }
        // Кулдаун для стены льда не ставим (зарядка уже ограничивает), при
        // необходимости добавим позже
        return true;
    }

    private void handleTranslocationClick(ServerPlayerEntity player, World world) {
        boolean isShiftDown = player.isSneaking();
        boolean isLookingDown = player.getPitch() > 60.0f;

        if (!(isShiftDown && isLookingDown)) {
            translocationAoERequested.remove(player.getUuid());
            return;
        }

        long now = world.getTime();
        Long last = lastTranslocationClick.get(player.getUuid());
        if (last != null && (now - last) <= TRANSLOCATION_DOUBLE_CLICK_WINDOW) {
            translocationAoERequested.put(player.getUuid(), true);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 0.8f, 1.6f);
        } else {
            translocationAoERequested.put(player.getUuid(), false);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.PLAYERS, 0.6f, 1.2f);
        lastTranslocationClick.put(player.getUuid(), now);
    }

    private boolean castTranslocation(ItemStack stack, World world, PlayerEntity player, int remainingUseTicks) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
            return false;
        }

        SpellType selected = SpellType.TRANSLOCATION;
        if (SpellManager.isOnCooldown(stack, world, selected)) {
            return false;
        }

        int level = SpellLevelManager.getSpellLevel(serverPlayer.getUuid(), selected);
        int maxChargeTicks = getMaxTranslocationChargeTicks(level);

        int usedTicks = getMaxUseTime(stack, player) - remainingUseTicks;
        if (usedTicks < 0) {
            usedTicks = 0;
        }
        usedTicks = Math.min(usedTicks, maxChargeTicks);

        float usedSeconds = usedTicks / 20.0f;
        if (usedSeconds < 3.0f) {
            serverPlayer.sendMessage(Text.literal("Нужно удерживать 3 секунды для каста!"), true);
            return false;
        }

        int maxRadius = getTranslocationRadius(usedSeconds);
        int minRadius = getTranslocationMinRadius(usedSeconds, maxRadius);
        boolean isShiftDown = player.isSneaking();
        boolean isLookingDown = player.getPitch() > 60.0f;
        boolean isSelfCast = isShiftDown && isLookingDown;
        boolean aoeRequested = Boolean.TRUE.equals(translocationAoERequested.remove(serverPlayer.getUuid()));

        LeveledSpell spell = (LeveledSpell) SpellRegistry.getSpell(selected);
        int baseCost = spell != null ? spell.getManaCost(level) : selected.getManaCost();
        int finalCost = baseCost;
        if (stack.getItem() instanceof EtherScepterItem) {
            finalCost = Math.max(0, baseCost - EtherScepterItem.MANA_DISCOUNT);
        }

        if (!ManaManager.tryConsume(serverPlayer, finalCost)) {
            serverPlayer.sendMessage(Text.literal("Недостаточно маны: нужно " + finalCost), true);
            return false;
        }

        if (isSelfCast) {
            if (aoeRequested && level >= 3 && usedSeconds >= 20.0f) {
                castSelfAoE(serverWorld, serverPlayer, 3.0, 3);
                SpellManager.setCooldown(stack, world, selected, getTranslocationCooldownForCharge(usedSeconds, true));
            } else {
                castSelf(serverWorld, serverPlayer, minRadius, maxRadius);
                SpellManager.setCooldown(stack, world, selected, getTranslocationCooldownForCharge(usedSeconds, false));
            }
        } else {
            LivingEntity target = findLookTarget(serverWorld, serverPlayer, getTranslocationTargetRange(level));
            if (target == null) {
                serverPlayer.sendMessage(Text.literal("Цель не найдена!"), true);
                return false;
            }
            castOnTarget(serverWorld, serverPlayer, target, minRadius, maxRadius);
            SpellManager.setCooldown(stack, world, selected, getTranslocationCooldownForCharge(usedSeconds, false));
        }

        return true;
    }

    private int getMaxTranslocationChargeTicks(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 60;
            case 2 -> 200;
            default -> 400;
        };
    }

    private int getTranslocationRadius(float usedSeconds) {
        if (usedSeconds >= 20.0f) {
            return 100;
        }
        if (usedSeconds >= 10.0f) {
            return 40;
        }
        return 10;
    }

    private int getTranslocationMinRadius(float usedSeconds, int maxRadius) {
        int minRadius = 0;
        if (usedSeconds >= 20.0f) {
            minRadius = 50;
        } else if (usedSeconds >= 10.0f) {
            minRadius = 10;
        }
        return Math.min(minRadius, maxRadius);
    }

    private double getTranslocationTargetRange(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> 4.0;
            case 2 -> 5.0;
            default -> 6.0;
        };
    }

    private int getTranslocationCooldownForCharge(float usedSeconds, boolean aoeCast) {
        if (aoeCast && usedSeconds >= 20.0f) {
            return 250 * 20;
        }
        if (usedSeconds >= 20.0f) {
            return 200 * 20;
        }
        if (usedSeconds >= 10.0f) {
            return 50 * 20;
        }
        return 10 * 20;
    }

    private void castOnTarget(ServerWorld world, ServerPlayerEntity caster, LivingEntity target, int minRadius, int maxRadius) {
        Vec3d center = new Vec3d(caster.getX(), caster.getY(), caster.getZ());
        Vec3d destination = findRandomSafePosition(world, center, minRadius, maxRadius);
        target.teleport(destination.x, destination.y, destination.z, false);
        randomizeView(caster, world);
        world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }

    private void castSelf(ServerWorld world, ServerPlayerEntity caster, int minRadius, int maxRadius) {
        Vec3d center = new Vec3d(caster.getX(), caster.getY(), caster.getZ());
        Vec3d destination = findRandomSafePosition(world, center, minRadius, maxRadius);
        caster.teleport(destination.x, destination.y, destination.z, false);
        randomizeView(caster, world);
        world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.9f);
    }

    private void castSelfAoE(ServerWorld world, ServerPlayerEntity caster, double radius, int maxTargets) {
        Vec3d center = new Vec3d(caster.getX(), caster.getY(), caster.getZ());
        Vec3d destination = findRandomSafePosition(world, center, 0.0, radius);
        caster.teleport(destination.x, destination.y, destination.z, false);
        randomizeView(caster, world);

        Box area = Box.of(center, radius * 2, radius * 2, radius * 2);
        List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, area,
                e -> e != null && e.isAlive() && e != caster);

        int count = 0;
        for (LivingEntity entity : nearby) {
            if (count >= maxTargets) {
                break;
            }
            Vec3d targetDestination = findRandomSafePosition(world, destination, 0.0, radius);
            entity.teleport(targetDestination.x, targetDestination.y, targetDestination.z, false);
            count++;
        }

        world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.2f, 0.8f);
    }

    private void randomizeView(ServerPlayerEntity caster, ServerWorld world) {
        float yaw = world.random.nextFloat() * 360.0f;
        float pitch = -30.0f + world.random.nextFloat() * 60.0f;
        caster.setYaw(yaw);
        caster.setPitch(pitch);
    }

    private LivingEntity findLookTarget(ServerWorld world, PlayerEntity caster, double range) {
        Vec3d start = caster.getEyePos();
        Vec3d look = caster.getRotationVector();
        Vec3d end = start.add(look.multiply(range));

        var hit = world.raycast(new net.minecraft.world.RaycastContext(
                start,
                end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                caster));

        Vec3d rayEnd = hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK ? hit.getPos() : end;
        Box box = new Box(start, rayEnd).expand(0.5);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> e != null && e.isAlive());

        LivingEntity best = null;
        double bestDot = 0.95;
        for (LivingEntity entity : entities) {
            Vec3d to = entity.getEyePos().subtract(start).normalize();
            double dot = look.dotProduct(to);
            if (dot > bestDot) {
                bestDot = dot;
                best = entity;
            }
        }
        return best;
    }

    private Vec3d findRandomSafePosition(ServerWorld world, Vec3d center, double minRadius, double maxRadius) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = minRadius + Math.sqrt(world.random.nextDouble()) * (maxRadius - minRadius);
            double x = center.x + Math.cos(angle) * distance;
            double z = center.z + Math.sin(angle) * distance;
            int baseY = (int) Math.floor(center.y);

            for (int dy = 3; dy >= -3; dy--) {
                BlockPos pos = BlockPos.ofFloored(x, baseY + dy, z);
                if (world.isAir(pos) && world.isAir(pos.up())) {
                    return Vec3d.ofBottomCenter(pos);
                }
            }
        }
        return center;
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