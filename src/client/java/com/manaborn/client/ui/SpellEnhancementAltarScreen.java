package com.manaborn.client.ui;

import com.manaborn.screen.SpellEnhancementAltarScreenHandler;
import com.manaborn.spells.SpellType;
import com.manaborn.client.state.ClientLearnedSpellsState;
import com.manaborn.client.state.ClientSpellLevelsState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SpellEnhancementAltarScreen extends HandledScreen<SpellEnhancementAltarScreenHandler> {
    private ButtonWidget upgradeButton;
    private List<ButtonWidget> spellButtons = new ArrayList<>();
    private SpellType selectedSpell = null;
    private List<SpellType> learnedSpells = new ArrayList<>();

    public SpellEnhancementAltarScreen(SpellEnhancementAltarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94; // Позиция надписи инвентаря
    }

    @Override
    protected void init() {
        super.init();
        
        // Получаем список изученных заклинаний
        Set<SpellType> learned = ClientLearnedSpellsState.getLearnedSpells();
        learnedSpells.clear();
        learnedSpells.addAll(learned);
        spellButtons.clear();
        
        // Создаём кнопки для заклинаний слева
        int baseX = (this.width - this.backgroundWidth) / 2;
        int baseY = (this.height - this.backgroundHeight) / 2;
        
        int spellButtonX = baseX + 8;
        int spellButtonY = baseY + 20;
        int spellButtonWidth = 65;
        int spellButtonHeight = 16;
        int spellButtonGap = 2;
        
        for (int i = 0; i < learnedSpells.size() && i < 6; i++) {
            SpellType spell = learnedSpells.get(i);
            int yPos = spellButtonY + i * (spellButtonHeight + spellButtonGap);
            
            ButtonWidget spellButton = ButtonWidget.builder(
                Text.literal(spell.getDisplayName()),
                btn -> {
                    selectedSpell = spell;
                    updateUpgradeButton();
                }
            )
            .dimensions(spellButtonX, yPos, spellButtonWidth, spellButtonHeight)
            .build();
            
            this.addDrawableChild(spellButton);
            spellButtons.add(spellButton);
        }
        
        // Автоматически выбираем первое заклинание
        if (!learnedSpells.isEmpty() && selectedSpell == null) {
            selectedSpell = learnedSpells.get(0);
        }
        
        // ЗЕЛЁНАЯ кнопка улучшения - справа внизу
        int upgradeButtonX = baseX + 120;
        int upgradeButtonY = baseY + 55;
        
        this.upgradeButton = ButtonWidget.builder(Text.literal("Улучшить"), button -> {
            if (selectedSpell != null && client != null && client.player != null) {
                // Отправляем пакет на сервер для улучшения
                com.manaborn.network.SpellUpgradeC2SPacket packet = 
                    new com.manaborn.network.SpellUpgradeC2SPacket(selectedSpell.getId());
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(packet);
            }
        })
        .dimensions(upgradeButtonX, upgradeButtonY, 50, 16)
        .build();
        
        this.addDrawableChild(upgradeButton);
        updateUpgradeButton();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        
        // Тёмный фон
        int bg = 0xDD000000;
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, bg);
        
        // Рамка (рисуем вручную)
        int borderColor = 0xFF555555;
        context.fill(x, y, x + backgroundWidth, y + 1, borderColor); // Верх
        context.fill(x, y + backgroundHeight - 1, x + backgroundWidth, y + backgroundHeight, borderColor); // Низ
        context.fill(x, y, x + 1, y + backgroundHeight, borderColor); // Лево
        context.fill(x + backgroundWidth - 1, y, x + backgroundWidth, y + backgroundHeight, borderColor); // Право
        
        // Выделяем ОДИН слот для ресурсов (справа вверху)
        int slotX = x + 140;
        int slotY = y + 18;
        int slotSize = 18;
        int highlightColor = 0xFFFFD700; // Золотой
        
        // Рисуем золотую рамку вокруг слота (толщиной 2 пикселя)
        int borderThickness = 2;
        int areaX = slotX - borderThickness;
        int areaY = slotY - borderThickness;
        int areaSize = slotSize + borderThickness * 2;
        
        // Верхняя линия
        context.fill(areaX, areaY, areaX + areaSize, areaY + borderThickness, highlightColor);
        // Нижняя линия
        context.fill(areaX, areaY + areaSize - borderThickness, areaX + areaSize, areaY + areaSize, highlightColor);
        // Левая линия
        context.fill(areaX, areaY, areaX + borderThickness, areaY + areaSize, highlightColor);
        // Правая линия
        context.fill(areaX + areaSize - borderThickness, areaY, areaX + areaSize, areaY + areaSize, highlightColor);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Заголовок
        context.drawText(this.textRenderer, "Spell Enhancement", 8, 6, 0xFFFFFF, false);
        
        // === ПРАВАЯ ЧАСТЬ: ТРЕБОВАНИЯ ===
        if (selectedSpell != null) {
            // Подпись к слоту
            context.drawText(this.textRenderer, "Ресурсы:", 125, 8, 0xAAAAAA, false);
            
            int currentLevel = ClientSpellLevelsState.getSpellLevel(selectedSpell);
            
            // === ТРЕБОВАНИЯ ===
            int reqX = 80;
            int reqY = 45;
            
            if (currentLevel < 3) {
                context.drawText(this.textRenderer, "Нужно:", reqX, reqY, 0xFFAA00, false);
                reqY += 9;
                
                String req = getRequirementText(selectedSpell, currentLevel);
                // Разбиваем на строки если длинное
                String[] parts = req.split(" ", 2);
                if (parts.length == 2) {
                    context.drawText(this.textRenderer, parts[0], reqX, reqY, 0xFFFFFF, false);
                    reqY += 9;
                    context.drawText(this.textRenderer, parts[1], reqX, reqY, 0xFFFFFF, false);
                } else {
                    context.drawText(this.textRenderer, req, reqX, reqY, 0xFFFFFF, false);
                }
            } else {
                context.drawText(this.textRenderer, "МАКС!", reqX, reqY, 0x55FF55, true);
            }
        } else {
            // Если заклинание не выбрано
            int hintX = 80;
            int hintY = 30;
            context.drawText(this.textRenderer, "← Выбери", hintX, hintY, 0x888888, false);
            context.drawText(this.textRenderer, "заклинание", hintX, hintY + 9, 0x888888, false);
        }
        
        // Инвентарь игрока (будет виден благодаря слотам из ScreenHandler)
        context.drawText(this.textRenderer, "Inventory", 8, this.playerInventoryTitleY, 0x404040, false);
    }

    // TODO: Implement spell selection via mouse clicks
    // For now, the first spell is auto-selected on GUI open

    private void updateUpgradeButton() {
        if (upgradeButton != null) {
            upgradeButton.active = selectedSpell != null;
        }
    }

    private String getRequirementText(SpellType spell, int currentLevel) {
        if (currentLevel == 1) {
            int dust = getDustRequiredFor1to2(spell);
            return dust + " Magic Dust";
        } else if (currentLevel == 2) {
            int ether = getEtherRequiredFor2to3(spell);
            return ether + " Ether";
        }
        return "???";
    }

    private int getDustRequiredFor1to2(SpellType spell) {
        return switch (spell) {
            case FIREBALL, INNER_HEAT -> 15;
            case ICE_SHARD, FROST_AURA -> 20;
            case FIRE_WALL, SOUND_STRIKE, BATTLE_HYMN -> 25;
            case WEAKNESS_CURSE, HEAT_WEAPON -> 30;
            case ABSORBING_BARRIER, ICE_WALL, SILENCE_ZONE -> 40;
        };
    }

    private int getEtherRequiredFor2to3(SpellType spell) {
        return switch (spell) {
            case INNER_HEAT -> 1;
            case WEAKNESS_CURSE -> 2;
            case FIREBALL, ICE_SHARD, FROST_AURA, ICE_WALL, HEAT_WEAPON, SOUND_STRIKE, BATTLE_HYMN -> 3;
            case FIRE_WALL, ABSORBING_BARRIER, SILENCE_ZONE -> 4;
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // Делаем кнопку Upgrade зелёной
        if (upgradeButton != null) {
            // Это косметический эффект, настоящий цвет кнопки задаётся в рендере кнопки
        }
    }
    
    @Override
    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        super.drawMouseoverTooltip(context, x, y);
    }
}
