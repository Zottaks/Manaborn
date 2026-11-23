package com.manaborn.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * Книга рецептов Manaborn: раскрытая книга в фирменном стиле.
 * Левая страница — сетка крафта и результат, правая — название,
 * иконка и краткое описание. Листание — стрелками, колёсиком
 * или клавишами <- ->.
 */
public class RecipeBookScreen extends Screen {

    private static final int W = 276;
    private static final int H = 166;
    private static final int ACCENT = 0x3CD8C4;

    private record RecipeEntry(String result, String title, String[] rows,
            Map<Character, String> key, String[] desc) {
    }

    private static RecipeEntry e(String result, String title, String r1, String r2, String r3,
            Map<Character, String> key, String... desc) {
        return new RecipeEntry(result, title, new String[] { r1, r2, r3 }, key, desc);
    }

    private static Map<Character, String> k(Object... pairs) {
        var m = new java.util.HashMap<Character, String>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(((String) pairs[i]).charAt(0), (String) pairs[i + 1]);
        }
        return m;
    }

    private static final List<RecipeEntry> ENTRIES = List.of(
            e("manaborn:awakening_book", "Фолиант магии",
                    "...", "PBL", "...",
                    k("P", "manaborn:magic_dust", "B", "minecraft:book", "L", "minecraft:lapis_lazuli"),
                    "Древний фолиант, дремлющая",
                    "искра магии. Прочтите его,",
                    "чтобы пробудить ману и начать",
                    "путь мага."),
            e("manaborn:magic_crafting_table", "Магический верстак",
                    "DDD", "DCD", "PPP",
                    k("D", "manaborn:magic_dust", "C", "minecraft:crafting_table",
                            "P", "minecraft:polished_deepslate"),
                    "Рабочее место чародея.",
                    "Создаёт посохи и магические",
                    "инструменты."),
            e("manaborn:learning_altar", "Алтарь изучения",
                    ".D.", ".B.", ".R.",
                    k("D", "manaborn:magic_dust", "B", "minecraft:book", "R", "minecraft:deepslate_bricks"),
                    "Позволяет изучать заклинания",
                    "из книг заклинаний, тратя",
                    "очки опыта."),
            e("manaborn:mana_converter", "Конвертор маны",
                    "DDD", "DHD", "TTT",
                    k("D", "manaborn:magic_dust", "H", "minecraft:hopper", "T", "minecraft:deepslate_tiles"),
                    "Переплавляет магическую",
                    "пыль в чистую ману."),
            e("manaborn:ether_converter", "Преобразователь эфира",
                    "DDD", "DAD", "ECE",
                    k("D", "manaborn:magic_dust", "A", "minecraft:anvil",
                            "E", "minecraft:cobbled_deepslate", "C", "minecraft:diamond"),
                    "Сплавляет 25 единиц пыли",
                    "в каплю чистого Эфира —",
                    "редкого магического вещества."),
            e("manaborn:mana_magnet", "Магнит маны",
                    "TDT", "DWD", "TVT",
                    k("T", "minecraft:deepslate_tiles", "D", "manaborn:magic_dust",
                            "W", "minecraft:lodestone", "V", "manaborn:mana_vessel"),
                    "Медленно вытягивает пылинки",
                    "магии из пространства. Пыль",
                    "накапливается внутри блока."),
            e("manaborn:mana_potion", "Кристалл маны",
                    ".D.", "DLD", ".D.",
                    k("D", "manaborn:magic_dust", "L", "minecraft:lapis_lazuli"),
                    "Концентрированная мана.",
                    "Используйте, чтобы пополнить",
                    "запас маны."),
            e("manaborn:manazite_catalyst", "Маназитный катализатор",
                    "MMM", "LLG", "...",
                    k("M", "manaborn:manazite", "L", "minecraft:lapis_lazuli", "G", "minecraft:glass"),
                    "Сфокусированный кристалл.",
                    "Увеличивает максимальный",
                    "запас маны при использовании."),
            e("manaborn:manoscope", "Маноскоп",
                    "LLL", "GSG", "LLL",
                    k("L", "minecraft:lapis_lazuli", "G", "minecraft:glass", "S", "minecraft:stick"),
                    "Магические очки. Наденьте,",
                    "чтобы видеть пылинки маны,",
                    "рассеянные в мире."),
            e("manaborn:ice_shard_spell_book", "Книга: Ледяной осколок",
                    ".P.", ".B.", ".S.",
                    k("P", "manaborn:magic_dust", "B", "minecraft:book", "S", "minecraft:snowball"),
                    "Хранит заклинание",
                    "«Ледяной осколок».",
                    "Изучается на Алтаре изучения."),
            e("manaborn:frost_coffin_spell_book", "Книга: Ледяной гроб",
                    ".P.", ".B.", ".I.",
                    k("P", "manaborn:magic_dust", "B", "minecraft:book", "I", "minecraft:packed_ice"),
                    "Хранит заклинание",
                    "«Ледяной гроб».",
                    "Изучается на Алтаре изучения."));

    private int page = 0;
    private int guiX;
    private int guiY;

    public RecipeBookScreen() {
        super(Text.literal("Книга рецептов"));
    }

    @Override
    protected void init() {
        guiX = (width - W) / 2;
        guiY = (height - H) / 2;
    }

    private static ItemStack stackOf(String id) {
        Item item = Registries.ITEM.get(Identifier.of(id));
        return new ItemStack(item);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        RecipeEntry entry = ENTRIES.get(page);

        drawBookFrame(ctx, guiX, guiY, W, H);
        drawBookTitle(ctx, "Книга рецептов", guiY + 8);

        // ── Левая страница: сетка крафта ──
        int lpX = guiX + 14;
        int lpY = guiY + 30;
        ManabornGuiStyle.drawPanel(ctx, lpX, lpY, 118, 108);
        ctx.drawText(textRenderer, "Крафт", lpX + (118 - textRenderer.getWidth("Крафт")) / 2, lpY + 6,
                ManabornGuiStyle.COLOR_TEXT_DIM, false);

        int cell = 20;
        int gx = lpX + 12;
        int gy = lpY + 20;
        ItemStack hovered = null;
        int hx = 0;
        int hy = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = gx + col * (cell + 4);
                int sy = gy + row * (cell + 4);
                char ch = entry.rows()[row].charAt(col);
                String itemId = entry.key().get(ch);
                ManabornGuiStyle.drawSlot(ctx, sx, sy, cell, ACCENT, itemId != null);
                if (itemId != null) {
                    ItemStack st = stackOf(itemId);
                    ctx.drawItem(st, sx + 2, sy + 2);
                    if (mouseX >= sx && mouseX < sx + cell && mouseY >= sy && mouseY < sy + cell) {
                        hovered = st;
                        hx = mouseX;
                        hy = mouseY;
                    }
                }
            }
        }

        // стрелка к результату
        ctx.drawText(textRenderer, "➜", lpX + 92, lpY + 52, ManabornGuiStyle.withAlpha(ACCENT, 220), false);

        // результат (справа от сетки не влезает — под сеткой)
        int rx = lpX + 92;
        int ry = lpY + 66;
        ItemStack result = stackOf(entry.result());
        ManabornGuiStyle.drawSlot(ctx, rx, ry, 22, ACCENT, true);
        ctx.drawItem(result, rx + 3, ry + 3);
        if (mouseX >= rx && mouseX < rx + 22 && mouseY >= ry && mouseY < ry + 22) {
            hovered = result;
            hx = mouseX;
            hy = mouseY;
        }

        // ── Правая страница: название, иконка, описание ──
        int rpX = guiX + 144;
        int rpY = guiY + 30;
        ManabornGuiStyle.drawPanel(ctx, rpX, rpY, 118, 108);

        // иконка результата крупно по центру панели
        int ix = rpX + (118 - 22) / 2;
        int iy = rpY + 8;
        ManabornGuiStyle.drawSlot(ctx, ix, iy, 22, ACCENT, true);
        ctx.drawItem(result, ix + 3, iy + 3);
        if (mouseX >= ix && mouseX < ix + 22 && mouseY >= iy && mouseY < iy + 22) {
            hovered = result;
            hx = mouseX;
            hy = mouseY;
        }

        // название
        int tw = textRenderer.getWidth(entry.title());
        ctx.drawText(textRenderer, entry.title(), rpX + (118 - tw) / 2, iy + 30,
                ManabornGuiStyle.COLOR_TITLE, true);
        // линия под названием
        int lw = Math.min(108, tw + 16);
        for (int i = 0; i < lw; i++) {
            float edge = 1.0f - Math.abs(i - lw / 2.0f) / (lw / 2.0f);
            ctx.fill(rpX + (118 - lw) / 2 + i, iy + 41, rpX + (118 - lw) / 2 + i + 1, iy + 42,
                    ManabornGuiStyle.withAlpha(ACCENT, (int) (110 * edge)));
        }

        // описание (с переносом, чтобы не вылезало за панель)
        int dy = iy + 48;
        int maxW = 102;
        for (String raw : entry.desc()) {
            for (String line : wrap(raw, maxW)) {
                if (dy > rpY + 100) {
                    break; // не выходим за нижний край панели
                }
                ctx.drawText(textRenderer, line, rpX + 8, dy, ManabornGuiStyle.COLOR_TEXT, false);
                dy += 11;
            }
        }

        // ── Навигация ──
        int navY = guiY + H - 24;
        boolean canPrev = page > 0;
        boolean canNext = page < ENTRIES.size() - 1;
        String prev = "◀ Назад";
        String next = "Вперёд ▶";
        ctx.drawText(textRenderer, prev, guiX + 18, navY,
                canPrev ? ManabornGuiStyle.COLOR_TITLE : ManabornGuiStyle.withAlpha(0x8E86A8, 90), canPrev);
        int nw = textRenderer.getWidth(next);
        ctx.drawText(textRenderer, next, guiX + W - 18 - nw, navY,
                canNext ? ManabornGuiStyle.COLOR_TITLE : ManabornGuiStyle.withAlpha(0x8E86A8, 90), canNext);
        String pageStr = (page + 1) + " / " + ENTRIES.size();
        ctx.drawText(textRenderer, pageStr, guiX + (W - textRenderer.getWidth(pageStr)) / 2, navY,
                ManabornGuiStyle.COLOR_TEXT_DIM, false);

        // тултип предмета поверх всего
        if (hovered != null) {
            ctx.drawItemTooltip(textRenderer, hovered, hx, hy);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        int mx = (int) click.x();
        int my = (int) click.y();
        int navY = guiY + H - 24;
        if (my >= navY - 4 && my <= navY + 12) {
            if (page > 0 && mx >= guiX + 14 && mx <= guiX + 80) {
                page--;
                return true;
            }
            if (page < ENTRIES.size() - 1 && mx >= guiX + W - 80 && mx <= guiX + W - 14) {
                page++;
                return true;
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0 && page > 0) {
            page--;
            return true;
        }
        if (verticalAmount < 0 && page < ENTRIES.size() - 1) {
            page++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput keyInput) {
        int keyCode = keyInput.key();
        // 263 = left, 262 = right (GLFW)
        if (keyCode == 263 && page > 0) {
            page--;
            return true;
        }
        if (keyCode == 262 && page < ENTRIES.size() - 1) {
            page++;
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ── Оформление в цветах книги (бирюза + золото) ─────────────────────

    /** Перенос строки по ширине панели (по словам). */
    private java.util.List<String> wrap(String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (textRenderer.getWidth(candidate) <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    /** Заголовок в цветах книги: бирюзовый текст, золотые искры по бокам. */
    private void drawBookTitle(DrawContext ctx, String title, int y) {
        long t = net.minecraft.util.Util.getMeasuringTimeMs();
        float pulse = 0.5f + 0.5f * net.minecraft.util.math.MathHelper.sin(t * 0.0016f);
        int tw = textRenderer.getWidth(title);
        int tx = guiX + (W - tw) / 2;
        ctx.drawText(textRenderer, title, tx, y, 0xFFBDFFF2, true);
        int orn = ManabornGuiStyle.withAlpha(
                ManabornGuiStyle.blendRgb(0xD8A840, 0xFFE9A0, pulse), 230);
        ctx.drawText(textRenderer, "✦", tx - 12, y, orn, false);
        ctx.drawText(textRenderer, "✦", tx + tw + 4, y, orn, false);
        int lineW = tw + 28;
        int lx = guiX + (W - lineW) / 2;
        for (int i = 0; i < lineW; i++) {
            float edge = 1.0f - Math.abs(i - lineW / 2.0f) / (lineW / 2.0f);
            ctx.fill(lx + i, y + 11, lx + i + 1, y + 12,
                    ManabornGuiStyle.withAlpha(ACCENT, (int) (110 * edge)));
        }
    }

    /**
     * Фон окна книги: как фирменная рамка, но в бирюзово-золотой гамме —
     * в цвет обложки самой книги рецептов.
     */
    private void drawBookFrame(DrawContext ctx, int x, int y, int w, int h) {
        long t = net.minecraft.util.Util.getMeasuringTimeMs();
        float pulse = 0.5f + 0.5f * net.minecraft.util.math.MathHelper.sin(t * 0.0011f);

        // мягкая тень
        ctx.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x55000000);
        // фон: глубокий тёмно-бирюзовый градиент
        ManabornGuiStyle.fillGradientV(ctx, x, y, x + w, y + h, 0xF80E2422, 0xF8081514);

        // мерцающие бирюзовые звёздочки
        for (int i = 0; i < 14; i++) {
            int hash = (int) ((i * 2654435761L) ^ ((w - 10) * 31L + (h - 10)));
            hash = (hash ^ (hash >>> 13)) * 1274126177;
            hash = hash ^ (hash >>> 16);
            int sx = x + 5 + Math.floorMod(hash, w - 10);
            int sy = y + 5 + Math.floorMod(hash >> 8, h - 10);
            float tw = 0.5f + 0.5f * net.minecraft.util.math.MathHelper.sin(t * 0.0022f + i * 1.7f);
            int alpha = (int) (tw * 85);
            if (alpha < 8) {
                continue;
            }
            ctx.fill(sx, sy, sx + 1, sy + 1, ManabornGuiStyle.withAlpha(0x9FF5E4, alpha));
        }

        // свечение и дышащая рамка (бирюза)
        int glowA = 24 + (int) (22 * pulse);
        ManabornGuiStyle.drawBorder(ctx, x - 1, y - 1, w + 2, h + 2,
                ManabornGuiStyle.withAlpha(0x3CD8C4, glowA), 1);
        int border = ManabornGuiStyle.withAlpha(
                ManabornGuiStyle.blendRgb(0x2E9E8C, 0x6FF0DA, pulse), 255);
        ManabornGuiStyle.drawBorder(ctx, x, y, w, h, border, 1);
        ManabornGuiStyle.drawBorder(ctx, x + 2, y + 2, w - 4, h - 4, 0xFF17443E, 1);

        // угловые самоцветы: золото
        int gem = ManabornGuiStyle.withAlpha(
                ManabornGuiStyle.blendRgb(0xD8A840, 0xFFE9A0, pulse), 255);
        bookGem(ctx, x, y, gem);
        bookGem(ctx, x + w - 1, y, gem);
        bookGem(ctx, x, y + h - 1, gem);
        bookGem(ctx, x + w - 1, y + h - 1, gem);
    }

    private static void bookGem(DrawContext ctx, int cx, int cy, int color) {
        ctx.fill(cx, cy - 2, cx + 1, cy + 3, color);
        ctx.fill(cx - 2, cy, cx + 3, cy + 1, color);
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, ManabornGuiStyle.withAlpha(0xFFFFFF, 120));
    }
}
