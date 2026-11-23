package com.manaborn.client;

import com.manaborn.client.state.ClientSpellLevelsState;
import com.manaborn.client.ui.ManabornGuiStyle;
import com.manaborn.items.MagicStaffItem;
import com.manaborn.spells.LeveledSpell;
import com.manaborn.spells.Spell;
import com.manaborn.spells.SpellManager;
import com.manaborn.spells.SpellRegistry;
import com.manaborn.spells.SpellType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

/**
 * Виджет кулдауна выбранного заклинания.
 *
 * Расположен в правом нижнем углу НАД числом маны (не пересекается
 * ни с хотбаром, ни с полоской маны). Сохранена фишка: оформление
 * меняется под школу магии выбранного заклинания (цвет, иконка стихии).
 */
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

        // Оставшееся время и общий кулдаун (зависит от уровня заклинания)
        int ticks = SpellManager.getCooldownRemainingTicks(staff, client.world, spell);
        int level = ClientSpellLevelsState.getSpellLevel(spell);
        Spell impl = SpellRegistry.getSpell(spell);
        int total = (impl instanceof LeveledSpell leveled)
                ? leveled.getCooldown(level)
                : (impl != null ? impl.getCooldown() : 0);

        boolean ready = ticks <= 0;
        float progress = total > 0
                ? MathHelper.clamp(1.0f - ticks / (float) total, 0.0f, 1.0f)
                : 1.0f;

        // Позиция: над числом маны (текст маны занимает sh-31..sh-23),
        // правый край совпадает с правым краем полоски маны
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int w = 96;
        int h = 22;
        int x = sw - 14 - w;
        int y = sh - 31 - h - 3;

        int accent = ManabornGuiStyle.elementRgb(spell);

        // Карточка в стиле выбранной школы магии
        ManabornGuiStyle.drawCard(ctx, x, y, w, h, accent, ready);

        // Иконка стихии слева
        ManabornGuiStyle.drawSpellElementIcon(ctx, client.textRenderer, spell, x + 11, y + h / 2 - 1, ready);

        // Подпись: время или «Готово»
        String label = ready
                ? "Готово"
                : String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0f) + " с";
        int labelColor = ready
                ? 0xFF000000 | ManabornGuiStyle.brightenRgb(accent, 0.35f)
                : ManabornGuiStyle.COLOR_TEXT;
        ctx.drawText(client.textRenderer, label, x + 23, y + 4, labelColor, true);

        // Полоска восстановления кулдауна
        ManabornGuiStyle.drawProgressBar(ctx, x + 23, y + 15, w - 32, 3, progress, accent);

        // Мягкий пульс рамки, когда заклинание готово
        if (ready) {
            long t = Util.getMeasuringTimeMs();
            float pulse = 0.5f + 0.5f * MathHelper.sin(t * 0.004f);
            ManabornGuiStyle.drawBorder(ctx, x, y, w, h,
                    ManabornGuiStyle.withAlpha(ManabornGuiStyle.brightenRgb(accent, 0.3f),
                            40 + (int) (60 * pulse)), 1);
        }
    }
}
