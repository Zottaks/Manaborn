package com.manaborn.client.ui;

import com.manaborn.ManabornMod;
import com.manaborn.screen.SpellEnhancementAltarScreenHandler;
import com.manaborn.spells.SpellType;
import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.client.state.ClientSpellLevelsState;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Spell Enhancement Altar UI - полный реворк v2
 * Красивый интерфейс для улучшения заклинаний с уровнями 1-3
 */
public class SpellEnhancementAltarScreen extends HandledScreen<SpellEnhancementAltarScreenHandler> {
    // UI Constants
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 260;
    
    // Colors
    private static final int COLOR_BG = 0xF0101020;
    private static final int COLOR_BORDER_INNER = 0xFF3D2A5C;
    private static final int COLOR_PANEL = 0xE0181830;
    private static final int COLOR_TITLE = 0xFFE0B0FF;
    private static final int COLOR_TEXT = 0xFFCCCCCC;
    private static final int COLOR_HIGHLIGHT = 0xFFFFD700;
    private static final int COLOR_STAR_FILLED = 0xFFFFD700;
    private static final int COLOR_STAR_EMPTY = 0xFF444444;
    private static final int COLOR_MAX_LEVEL = 0xFF55FF55;
    private static final int COLOR_SELECTED = 0xFF8B5CF6;
    private static final int COLOR_ERROR = 0xFFFF5555;
    
    // Spell list
    private List<SpellType> learnedSpells = new ArrayList<>();
    private SpellType selectedSpell = null;
    private int scrollOffset = 0;
    private static final int VISIBLE_SPELLS = 7;
    
    // Buttons
    private ButtonWidget upgradeButton;
    
    // Status message
    private String statusMessage = "";
    private int statusColor = COLOR_TEXT;
    private int statusTimer = 0;
    
    // Animation
    private float animationProgress = 0f;
    private boolean isAnimating = false;
    private int glowPulse = 0;

    public SpellEnhancementAltarScreen(SpellEnhancementAltarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_WIDTH;
        this.backgroundHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        
        // Получаем изученные заклинания
        Set<SpellType> learned = ClientLearnedSpellsState.getLearnedSpells();
        learnedSpells.clear();
        learnedSpells.addAll(learned);
        
        // Авто-выбор первого
        if (!learnedSpells.isEmpty() && selectedSpell == null) {
            selectedSpell = learnedSpells.get(0);
        }
        
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;
        
        // Кнопка улучшения
        int upgradeBtnX = x + 175;
        int upgradeBtnY = y + 135;
        
        this.upgradeButton = ButtonWidget.builder(
            Text.literal("✦ УЛУЧШИТЬ ✦").formatted(Formatting.BOLD),
            btn -> attemptUpgrade()
        )
        .dimensions(upgradeBtnX, upgradeBtnY, 100, 24)
        .build();
        this.addDrawableChild(upgradeButton);
        
        updateButtons();
    }
    
    private void attemptUpgrade() {
        if (selectedSpell == null || client == null) return;
        
        int level = ClientSpellLevelsState.getSpellLevel(selectedSpell);
        if (level >= 3) {
            showStatus("Заклинание уже максимального уровня!", COLOR_ERROR);
            return;
        }
        
        // Проверяем ресурсы в слоте
        ItemStack slotStack = handler.getSlot(0).getStack();
        int required = getRequiredAmount(selectedSpell, level);
        boolean needsDust = level == 1;
        
        if (slotStack.isEmpty()) {
            String itemName = needsDust ? "Магическую Пыль" : "Эфир";
            showStatus("Положите " + itemName + " в слот!", COLOR_ERROR);
            return;
        }
        
        boolean correctItem = needsDust 
            ? slotStack.isOf(ManabornMod.MAGIC_DUST)
            : slotStack.isOf(ManabornMod.ETHER);
            
        if (!correctItem) {
            String itemName = needsDust ? "Магическую Пыль" : "Эфир";
            showStatus("Нужен " + itemName + "!", COLOR_ERROR);
            return;
        }
        
        if (slotStack.getCount() < required) {
            showStatus("Нужно " + required + " шт, есть " + slotStack.getCount(), COLOR_ERROR);
            return;
        }
        
        // Всё ок - отправляем пакет
        com.manaborn.network.SpellUpgradeC2SPacket packet = 
            new com.manaborn.network.SpellUpgradeC2SPacket(selectedSpell.getId());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(packet);
        
        // Запускаем анимацию
        isAnimating = true;
        animationProgress = 0f;
        showStatus("Улучшение...", COLOR_HIGHLIGHT);
    }
    
    private void showStatus(String message, int color) {
        this.statusMessage = message;
        this.statusColor = color;
        this.statusTimer = 80;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Обновляем анимацию
        glowPulse = (glowPulse + 1) % 60;
        if (statusTimer > 0) statusTimer--;
        if (isAnimating) {
            animationProgress += 0.05f;
            if (animationProgress >= 1.0f) {
                isAnimating = false;
                animationProgress = 0f;
            }
        }
        
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;
        
        // Основной фон
        context.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, COLOR_BG);
        
        // Анимация свечения при улучшении
        if (isAnimating) {
            int glowAlpha = (int)(Math.sin(animationProgress * Math.PI * 4) * 60 + 60);
            int glowColor = (glowAlpha << 24) | 0xFFD700;
            context.fill(x + 2, y + 2, x + GUI_WIDTH - 2, y + GUI_HEIGHT - 2, glowColor);
        }
        
        // Внешняя рамка (фиолетовая с пульсацией)
        int pulseOffset = (int)(Math.sin(glowPulse * 0.1) * 20);
        int borderR = Math.min(255, 107 + pulseOffset);
        int borderColor = 0xFF000000 | (borderR << 16) | (75 << 8) | 163;
        drawBorder(context, x, y, GUI_WIDTH, GUI_HEIGHT, borderColor, 2);
        
        // Внутренняя рамка
        drawBorder(context, x + 3, y + 3, GUI_WIDTH - 6, GUI_HEIGHT - 6, COLOR_BORDER_INNER, 1);
        
        // === ЛЕВАЯ ПАНЕЛЬ: Список заклинаний ===
        int leftPanelX = x + 10;
        int leftPanelY = y + 28;
        int leftPanelW = 140;
        int leftPanelH = 115;
        
        context.fill(leftPanelX, leftPanelY, leftPanelX + leftPanelW, leftPanelY + leftPanelH, COLOR_PANEL);
        drawBorder(context, leftPanelX, leftPanelY, leftPanelW, leftPanelH, COLOR_BORDER_INNER, 1);
        
        // Рисуем список заклинаний
        int spellY = leftPanelY + 4;
        int spellHeight = 16;
        
        for (int i = 0; i < VISIBLE_SPELLS && (i + scrollOffset) < learnedSpells.size(); i++) {
            SpellType spell = learnedSpells.get(i + scrollOffset);
            int itemY = spellY + i * spellHeight;
            
            // Подсветка выбранного
            if (spell == selectedSpell) {
                context.fill(leftPanelX + 2, itemY, leftPanelX + leftPanelW - 2, itemY + spellHeight - 1, COLOR_SELECTED);
            }
            
            // Название заклинания
            int level = ClientSpellLevelsState.getSpellLevel(spell);
            String name = spell.getDisplayName();
            int textColor = spell == selectedSpell ? 0xFFFFFFFF : COLOR_TEXT;
            context.drawText(this.textRenderer, name, leftPanelX + 5, itemY + 4, textColor, false);
            
            // Звёзды уровня справа
            int starX = leftPanelX + leftPanelW - 28;
            for (int s = 0; s < 3; s++) {
                int starColor = s < level ? COLOR_STAR_FILLED : COLOR_STAR_EMPTY;
                context.drawText(this.textRenderer, "★", starX + s * 8, itemY + 4, starColor, false);
            }
        }
        
        // Индикатор скролла
        if (learnedSpells.size() > VISIBLE_SPELLS) {
            if (scrollOffset > 0) {
                context.drawText(this.textRenderer, "▲", leftPanelX + leftPanelW / 2 - 3, leftPanelY - 8, 0xFF888888, false);
            }
            if (scrollOffset < learnedSpells.size() - VISIBLE_SPELLS) {
                context.drawText(this.textRenderer, "▼", leftPanelX + leftPanelW / 2 - 3, leftPanelY + leftPanelH + 1, 0xFF888888, false);
            }
        }
        
        // === ПРАВАЯ ПАНЕЛЬ: Детали заклинания ===
        int rightPanelX = x + 158;
        int rightPanelY = y + 28;
        int rightPanelW = 152;
        int rightPanelH = 100;
        
        context.fill(rightPanelX, rightPanelY, rightPanelX + rightPanelW, rightPanelY + rightPanelH, COLOR_PANEL);
        drawBorder(context, rightPanelX, rightPanelY, rightPanelW, rightPanelH, COLOR_BORDER_INNER, 1);
        
        // === СЛОТ ДЛЯ РЕСУРСОВ ===
        int slotX = x + 245;
        int slotY = y + 95;
        int slotSize = 16;
        
        // Золотая рамка вокруг слота
        int slotGlow = (int)(Math.sin(glowPulse * 0.15) * 30 + 225);
        int slotBorderColor = 0xFF000000 | (slotGlow << 16) | (slotGlow << 8);
        context.fill(slotX - 2, slotY - 2, slotX + slotSize + 2, slotY - 1, slotBorderColor);
        context.fill(slotX - 2, slotY + slotSize + 1, slotX + slotSize + 2, slotY + slotSize + 2, slotBorderColor);
        context.fill(slotX - 2, slotY - 1, slotX - 1, slotY + slotSize + 1, slotBorderColor);
        context.fill(slotX + slotSize + 1, slotY - 1, slotX + slotSize + 2, slotY + slotSize + 1, slotBorderColor);
        
        // === ИНВЕНТАРЬ ИГРОКА ===
        int invX = x + 72;
        int invY = y + 175;
        int invWidth = 9 * 18;
        int invHeight = 4 * 18 + 4;
        
        context.fill(invX - 4, invY - 4, invX + invWidth + 4, invY + invHeight, 0x80000000);
        drawBorder(context, invX - 4, invY - 4, invWidth + 8, invHeight + 4, COLOR_BORDER_INNER, 1);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Заголовок
        String title = "✧ Алтарь Улучшения Заклинаний ✧";
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, (GUI_WIDTH - titleWidth) / 2, 8, COLOR_TITLE, true);
        
        // Подзаголовок списка
        context.drawText(this.textRenderer, "Заклинания:", 12, 18, COLOR_TEXT, false);
        
        // === ПРАВАЯ ПАНЕЛЬ: Детали выбранного заклинания ===
        if (selectedSpell != null) {
            int detailX = 163;
            int detailY = 31;
            
            // Название заклинания
            Integer spellColor = selectedSpell.getColor().getColorValue();
            context.drawText(this.textRenderer, selectedSpell.getDisplayName(), detailX, detailY, 
                spellColor != null ? spellColor : 0xFFFFFF, true);
            
            // Уровень со звёздами
            int level = ClientSpellLevelsState.getSpellLevel(selectedSpell);
            detailY += 12;
            context.drawText(this.textRenderer, "Уровень:", detailX, detailY, COLOR_TEXT, false);
            
            int starX = detailX + 48;
            for (int i = 0; i < 3; i++) {
                int starColor = i < level ? COLOR_STAR_FILLED : COLOR_STAR_EMPTY;
                context.drawText(this.textRenderer, "★", starX + i * 10, detailY, starColor, true);
            }
            
            if (level < 3) {
                // Требуемые ресурсы
                int required = getRequiredAmount(selectedSpell, level);
                boolean needsDust = level == 1;
                
                context.drawText(this.textRenderer, "Ресурс:", 245, 80, 0xFFAA00, false);
                context.drawText(this.textRenderer, required + "x", 270, 80, COLOR_HIGHLIGHT, false);
                
                // Иконка ресурса
                ItemStack requiredItem = needsDust 
                    ? new ItemStack(ManabornMod.MAGIC_DUST, 1)
                    : new ItemStack(ManabornMod.ETHER, 1);
                context.drawItem(requiredItem, 290, 76);
                
                // Описание улучшения
                detailY += 16;
                context.drawText(this.textRenderer, "При улучшении:", detailX, detailY, 0xFF88FF88, false);
                detailY += 11;
                
                String upgradeDesc = getUpgradeDescription(selectedSpell, level);
                String[] lines = upgradeDesc.split("\n");
                for (String line : lines) {
                    context.drawText(this.textRenderer, "• " + line, detailX, detailY, 0xFFAAAAAA, false);
                    detailY += 10;
                }
            } else {
                detailY += 16;
                context.drawText(this.textRenderer, "✓ МАКСИМУМ!", detailX, detailY, COLOR_MAX_LEVEL, true);
                detailY += 14;
                context.drawText(this.textRenderer, "Заклинание полностью", detailX, detailY, 0xFF888888, false);
                detailY += 10;
                context.drawText(this.textRenderer, "улучшено!", detailX, detailY, 0xFF888888, false);
            }
        } else {
            context.drawText(this.textRenderer, "Выберите", 200, 55, 0xFF888888, false);
            context.drawText(this.textRenderer, "заклинание", 195, 67, 0xFF888888, false);
            context.drawText(this.textRenderer, "из списка ←", 193, 79, 0xFF888888, false);
        }
        
        // Статус сообщение
        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            int msgWidth = this.textRenderer.getWidth(statusMessage);
            int msgX = (GUI_WIDTH - msgWidth) / 2;
            int msgY = 158;
            
            context.fill(msgX - 4, msgY - 2, msgX + msgWidth + 4, msgY + 10, 0xCC000000);
            context.drawText(this.textRenderer, statusMessage, msgX, msgY, statusColor, true);
        }
        
        // Подпись инвентаря
        context.drawText(this.textRenderer, "Инвентарь", 72, 165, 0xFF666666, false);
    }
    
    private String getUpgradeDescription(SpellType spell, int currentLevel) {
        int nextLevel = currentLevel + 1;
        return switch (spell) {
            case FIREBALL -> nextLevel == 2 
                ? "Взрыв: 1.0→1.5\nСкорость: +50%" 
                : "Взрыв: 1.5→2.0\nМана: 22→20";
            case ICE_SHARD -> nextLevel == 2 
                ? "Урон: +25%\nЗамедление: +1с" 
                : "Урон: +50%\nМана: -3";
            case FROST_COFFIN -> nextLevel == 2
                ? "Разблок: слепота\nУдержание: нет"
                : "Разблок: удержание Shift\nДренаж маны";
            case FIRE_WALL -> nextLevel == 2 
                ? "Длительность: +2с\nУрон: +20%" 
                : "Ширина: +1 блок\nКД: -1с";
            case INNER_HEAT -> nextLevel == 2 
                ? "Длительность: +30с\nОгнеупорность" 
                : "Полный иммунитет\nк огню";
            case FROST_AURA -> nextLevel == 2 
                ? "Радиус: +1 блок\nЗамедление: +10%" 
                : "Радиус: +2 блока\nУрон врагам";
            case ABSORBING_BARRIER -> nextLevel == 2 
                ? "Поглощение: +25%\nДлительность: +2с" 
                : "Поглощение: +50%\nОтражение урона";
            case WEAKNESS_CURSE -> nextLevel == 2 
                ? "Длительность: +3с\nСлабость II" 
                : "Длительность: +5с\nСлабость III";
            case ICE_WALL -> nextLevel == 2 
                ? "Прочность: +50%\nВысота: +1" 
                : "Прочность: x2\nЗамораживание";
            case HEAT_WEAPON -> nextLevel == 2 
                ? "Огненный урон: +2\nДлительность: +5с" 
                : "Огненный урон: +4\nПоджог врагов";
            case SOUND_STRIKE -> nextLevel == 2 
                ? "Урон: +30%\nОглушение: +0.5с" 
                : "Урон: +60%\nОглушение: +1с";
            case BATTLE_HYMN -> nextLevel == 2 
                ? "Сила: II→III\nДлительность: +5с" 
                : "Сила: III→IV\nРадиус: +2 блока";
            case CLARIFYING_TONE -> nextLevel == 2 
                ? "Скорость: II→III\nДлительность: +5с" 
                : "Скорость: III→IV\nРегенерация";
            case SILENCE_ZONE -> nextLevel == 2 
                ? "Радиус: +2 блока\nДлительность: +3с" 
                : "Радиус: +4 блока\nБлок заклинаний";
            case TRANSLOCATION -> nextLevel == 2
                ? "Дальность: 10→40\nКД: 20с→50с"
                : "Дальность: 40→100\nAoE телепорт";
        };
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;
        
        int leftPanelX = x + 10;
        int leftPanelY = y + 28;
        int leftPanelW = 140;
        int spellHeight = 16;
        
        double mouseX = click.x();
        double mouseY = click.y();
        
        if (click.button() == 0 && mouseX >= leftPanelX && mouseX < leftPanelX + leftPanelW) {
            int relY = (int) mouseY - leftPanelY - 4;
            if (relY >= 0) {
                int index = relY / spellHeight + scrollOffset;
                if (index >= 0 && index < learnedSpells.size()) {
                    selectedSpell = learnedSpells.get(index);
                    updateButtons();
                    return true;
                }
            }
        }
        
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;
        
        if (mouseX >= x + 10 && mouseX < x + 150 && mouseY >= y + 25 && mouseY < y + 150) {
            if (verticalAmount > 0 && scrollOffset > 0) {
                scrollOffset--;
            } else if (verticalAmount < 0 && scrollOffset < Math.max(0, learnedSpells.size() - VISIBLE_SPELLS)) {
                scrollOffset++;
            }
            updateButtons();
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateButtons() {
        if (upgradeButton != null) {
            boolean canUpgrade = selectedSpell != null && 
                ClientSpellLevelsState.getSpellLevel(selectedSpell) < 3;
            upgradeButton.active = canUpgrade;
        }
    }
    
    private int getRequiredAmount(SpellType spell, int level) {
        if (level == 1) {
            return getDustRequiredFor1to2(spell);
        } else if (level == 2) {
            return getEtherRequiredFor2to3(spell);
        }
        return 0;
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color, int thickness) {
        context.fill(x, y, x + w, y + thickness, color);
        context.fill(x, y + h - thickness, x + w, y + h, color);
        context.fill(x, y, x + thickness, y + h, color);
        context.fill(x + w - thickness, y, x + w, y + h, color);
    }

    private int getDustRequiredFor1to2(SpellType spell) {
        return switch (spell) {
            case FIREBALL, INNER_HEAT -> 15;
            case ICE_SHARD, FROST_AURA -> 20;
            case FIRE_WALL, SOUND_STRIKE, BATTLE_HYMN, CLARIFYING_TONE -> 25;
            case WEAKNESS_CURSE, HEAT_WEAPON -> 30;
            case FROST_COFFIN -> 50;
            case ABSORBING_BARRIER, ICE_WALL, SILENCE_ZONE, TRANSLOCATION -> 40;
        };
    }

    private int getEtherRequiredFor2to3(SpellType spell) {
        return switch (spell) {
            case INNER_HEAT -> 1;
            case WEAKNESS_CURSE -> 2;
            case FIREBALL, ICE_SHARD, FROST_AURA, ICE_WALL, HEAT_WEAPON, SOUND_STRIKE, BATTLE_HYMN, CLARIFYING_TONE, FROST_COFFIN -> 3;
            case FIRE_WALL, ABSORBING_BARRIER, SILENCE_ZONE, TRANSLOCATION -> 4;
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
