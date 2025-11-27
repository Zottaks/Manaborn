package com.manaborn.client;

import com.manaborn.items.MagicStaffItem;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class CooldownHudOverlay {
    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) return;

        // Ищем посох в руках (MagicStaffItem или EtherScepterItem)
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        ItemStack staff = null;
        if (main.getItem() instanceof MagicStaffItem) staff = main;
        else if (off.getItem() instanceof MagicStaffItem) staff = off;
        if (staff == null) return;

        // Определяем выбранное заклинание
        SpellType spell = SpellManager.getSelectedSpell(staff);
        if (spell == null) return;

        // Считаем оставшееся время кулдауна
        int ticks = SpellManager.getCooldownRemainingTicks(staff, client.world, spell);
        float seconds = ticks / 20.0f;

        // Позиция: правый нижний угол, ПОД маной
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int boxWidth = 80;
        int boxHeight = 20;
        int x = sw - boxWidth - 12; // отступ как у маны
        int y = sh - 39 + 18 + 6;   // чуть ниже строки маны

        // Всегда рисуем, даже если 0 — показываем READY
        drawStyledBox(ctx, x, y, boxWidth, boxHeight, spell);
        String label = (ticks > 0) ? String.format("%.1fs", seconds) : "READY";
        int color = 0xFFFFFFFF;
        ctx.drawText(client.textRenderer, Text.literal(label), x + 8, y + 6, color, true);
    }

    // Рисуем стиль под тип заклинания, переиспользуя цвета из SpellHudOverlay
    private static void drawStyledBox(DrawContext ctx, int x, int y, int w, int h, SpellType spell) {
        // Базовый фон
        int bg = 0xC0000000;
        ctx.fill(x, y, x + w, y + h, bg);

        switch (spell) {
            case FIREBALL, INNER_HEAT, HEAT_WEAPON -> {
                // огненная окантовка
                ctx.fill(x - 1, y - 1, x + w + 1, y, 0xCCFF8800);
                ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xCCFF8800);
                ctx.fill(x - 1, y, x, y + h, 0xCCFF8800);
                ctx.fill(x + w, y, x + w + 1, y + h, 0xCCFF8800);
            }
            case ICE_SHARD, ICE_WALL -> {
                // ледяная окантовка
                ctx.fill(x - 1, y - 1, x + w + 1, y, 0xCC00CCFF);
                ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xCC00CCFF);
                ctx.fill(x - 1, y, x, y + h, 0xCC00CCFF);
                ctx.fill(x + w, y, x + w + 1, y + h, 0xCC00CCFF);
            }
            case FIRE_WALL -> {
                // золотая окантовка
                ctx.fill(x - 1, y - 1, x + w + 1, y, 0xCCFFAA00);
                ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xCCFFAA00);
                ctx.fill(x - 1, y, x, y + h, 0xCCFFAA00);
                ctx.fill(x + w, y, x + w + 1, y + h, 0xCCFFAA00);
            }
            case FROST_AURA -> {
                // холодный циан
                ctx.fill(x - 1, y - 1, x + w + 1, y, 0xCC00FFFF);
                ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xCC00FFFF);
                ctx.fill(x - 1, y, x, y + h, 0xCC00FFFF);
                ctx.fill(x + w, y, x + w + 1, y + h, 0xCC00FFFF);
            }
            case ABSORBING_BARRIER, WEAKNESS_CURSE -> {
                // темная фиолетовая окантовка
                ctx.fill(x - 1, y - 1, x + w + 1, y, 0xCC5C3B89);
                ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xCC5C3B89);
                ctx.fill(x - 1, y, x, y + h, 0xCC5C3B89);
                ctx.fill(x + w, y, x + w + 1, y + h, 0xCC5C3B89);
            }
            case SOUND_STRIKE, BATTLE_HYMN -> {
                // жёлто-белая окантовка (звуковая магия)
                ctx.fill(x - 1, y - 1, x + w + 1, y, 0xCCFFFF00);
                ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xCCFFFF00);
                ctx.fill(x - 1, y, x, y + h, 0xCCFFFFFF);
                ctx.fill(x + w, y, x + w + 1, y + h, 0xCCFFFFFF);
            }
            case SILENCE_ZONE -> {
                // серая окантовка (область тишины)
                ctx.fill(x - 1, y - 1, x + w + 1, y, 0xCC888888);
                ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xCC888888);
                ctx.fill(x - 1, y, x, y + h, 0xCC666666);
                ctx.fill(x + w, y, x + w + 1, y + h, 0xCC666666);
            }
        }
    }
}
