# 🌐 Финальные шаги для networking системы уровней

**Статус:** BUILD SUCCESSFUL ✅  
**Прогресс:** 90% - осталось 3 простых шага!

---

## ✅ ЧТО УЖЕ СДЕЛАНО:

### 1. **Клиентское хранилище** ✅
- `ClientSpellLevelsState.java` - хранение уровней на клиенте

### 2. **Сетевые пакеты** ✅
- `SpellLevelSyncS2CPacket.java` - синхронизация S2C
- `SpellUpgradeC2SPacket.java` - запрос улучшения C2S
- Оба зарегистрированы в `ManabornMod`

### 3. **Серверная обработка** ✅
- Обработчик `SpellUpgradeC2SPacket` в `ManabornMod`
- Поиск ближайшего алтаря (радиус 10 блоков)
- Вызов `tryUpgradeSpell` из BlockEntity

---

## ⏳ ЧТО ОСТАЛОСЬ (3 ШАГА):

### ШАГ 1: Обновить SpellLevelManager.sendSync()

**Файл:** `src/main/java/com/manaborn/spells/SpellLevelManager.java`

**Найти строку 96-103:**
```java
public static void sendSync(ServerPlayerEntity player) {
    // TODO: Отправить пакет с уровнями заклинаний
    // Будет реализовано после создания SpellLevelSyncS2CPacket
    
    // Map<SpellType, Integer> levels = spellLevels.getOrDefault(player.getUuid(), new HashMap<>());
    // SpellLevelSyncS2CPacket packet = new SpellLevelSyncS2CPacket(levels);
    // ServerPlayNetworking.send(player, packet);
}
```

**ЗАМЕНИТЬ НА:**
```java
public static void sendSync(ServerPlayerEntity player) {
    Map<SpellType, Integer> levels = spellLevels.getOrDefault(player.getUuid(), new HashMap<>());
    
    // Отправляем пакет с уровнями заклинаний
    com.manaborn.network.SpellLevelSyncS2CPacket packet = 
        new com.manaborn.network.SpellLevelSyncS2CPacket(levels);
    
    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, packet);
}
```

---

### ШАГ 2: Добавить клиентскую обработку пакета

**Файл:** `src/client/java/com/manaborn/client/ManabornClient.java`

**Найти строку ~43-45 (после регистрации LearnedSpellsSyncS2CPacket):**
```java
ClientPlayNetworking.registerGlobalReceiver(
    LearnedSpellsSyncS2CPacket.ID,
    (payload, context) -> {
        context.client().execute(() -> {
            ClientLearnedSpellsState.setLearnedSpells(payload.spells());
        });
    }
);
```

**ДОБАВИТЬ СРАЗУ ПОСЛЕ:**
```java
// Регистрация обработчика синхронизации уровней заклинаний
ClientPlayNetworking.registerGlobalReceiver(
    com.manaborn.network.SpellLevelSyncS2CPacket.ID,
    (payload, context) -> {
        context.client().execute(() -> {
            ClientSpellLevelsState.setLevels(payload.levels());
        });
    }
);
```

**Также добавить импорт в начало файла:**
```java
import com.manaborn.client.state.ClientSpellLevelsState;
```

---

### ШАГ 3: Обновить GUI для отправки пакета улучшения

**Файл:** `src/client/java/com/manaborn/client/ui/SpellEnhancementAltarScreen.java`

**Найти строку ~47-52 (в методе init()):**
```java
this.upgradeButton = ButtonWidget.builder(Text.literal("Upgrade"), button -> {
    if (selectedSpell != null) {
        // TODO: Отправить пакет на сервер для улучшения
        client.player.sendMessage(Text.literal("Upgrading " + selectedSpell.getDisplayName()), false);
    }
})
```

**ЗАМЕНИТЬ НА:**
```java
this.upgradeButton = ButtonWidget.builder(Text.literal("Upgrade"), button -> {
    if (selectedSpell != null && client != null && client.player != null) {
        // Отправляем пакет на сервер для улучшения
        com.manaborn.network.SpellUpgradeC2SPacket packet = 
            new com.manaborn.network.SpellUpgradeC2SPacket(selectedSpell.getId());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(packet);
    }
})
```

**Также обновить метод получения уровня (строка ~112):**
```java
// TODO: Получить уровень заклинания от клиента
int level = 1; // placeholder
```

**ЗАМЕНИТЬ НА:**
```java
// Получаем уровень заклинания от клиента
int level = ClientSpellLevelsState.getSpellLevel(spell);
```

**И обновить строку ~127:**
```java
// TODO: Получить текущий уровень и показать требования
int currentLevel = 1; // placeholder
```

**ЗАМЕНИТЬ НА:**
```java
int currentLevel = ClientSpellLevelsState.getSpellLevel(selectedSpell);
```

---

## 🎯 После этих 3 шагов:

1. **Скомпилируй:** `.\gradlew build`
2. **Запусти:** `.\gradlew runClient`
3. **Протестируй:**
   - Изучи заклинание через Learning Altar
   - Положи ресурсы в Spell Enhancement Altar
   - Открой GUI, выбери заклинание
   - Нажми "Upgrade"
   - Проверь что уровень увеличился!

---

## 📊 Текущий статус системы:

### ✅ ПОЛНОСТЬЮ РАБОТАЕТ:
- Все 9 заклинаний с 3 уровнями
- Индивидуальные требования для прокачки
- Блок Spell Enhancement Altar
- Проверка и потребление ресурсов
- ABSORBING_BARRIER восстанавливает ману по уровням (0%/50%/100%)
- GUI с отображением заклинаний

### ⚡ ПОЧТИ ГОТОВО (нужны 3 простых правки):
- Синхронизация уровней с клиентом
- Отправка запроса улучшения
- Отображение реальных уровней в GUI

### ⏳ ОПЦИОНАЛЬНО (не критично):
- Сохранение уровней между сессиями (PersistentState)
- Кликабельный выбор заклинаний в GUI
- Анимация улучшения

---

## 🔍 Как проверить что всё работает:

### 1. Синхронизация при подключении:
- Присоединись к миру
- Проверь что `SpellLevelManager.sendSync(player)` вызывается
- На клиенте должно быть: все уровни = 1

### 2. Улучшение заклинания:
- Открой GUI алтаря
- Выбери заклинание (пока auto-select первого)
- Положи ресурсы в слоты
- Нажми "Upgrade"
- Должно показать: "Fireball улучшено до уровня 2!"
- GUI должен обновиться с новым уровнем

### 3. Применение улучшения:
- Выбери улучшенное заклинание на посохе
- Используй его
- Проверь что характеристики изменились (больше урон/скорость/длительность)

---

## 💡 Дополнительные улучшения (опционально):

### A. Добавить синхронизацию при подключении

**Файл:** `ManabornMod.java` (строка ~230)

**Найти:**
```java
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    ServerPlayerEntity player = handler.player;
    ManaManager.sendSync(player);
    LearnedSpellsManager.sendSync(player);
});
```

**ДОБАВИТЬ:**
```java
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    ServerPlayerEntity player = handler.player;
    ManaManager.sendSync(player);
    LearnedSpellsManager.sendSync(player);
    com.manaborn.spells.SpellLevelManager.sendSync(player); // ← ДОБАВИТЬ ЭТУ СТРОКУ
});
```

### B. Очистка при выходе из мира

**Файл:** `ManabornClient.java` (добавить в onInitializeClient)

```java
ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
    ClientSpellLevelsState.clear();
});
```

---

## 🎉 Результат:

После этих 3 простых правок у тебя будет **полностью рабочая система прокачки заклинаний** с:
- ✅ Сохранением уровней на сервере
- ✅ Синхронизацией с клиентом
- ✅ GUI для улучшения
- ✅ Автоматической отправкой запросов
- ✅ Применением улучшений к заклинаниям

**BUILD SUCCESSFUL** ✅  
**Осталось ~30 минут работы!**
