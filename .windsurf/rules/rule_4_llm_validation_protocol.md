---
trigger: always_on
---

# Rule 4: LLM Code Validation Protocol

## 🎯 Цель
Минимизация ошибок при работе с LLM в Windsurf через строгий протокол проверки ПЕРЕД генерацией кода.

---

## ⚠️ КРИТИЧЕСКОЕ ПРАВИЛО

**ПЕРЕД написанием ЛЮБОГО кода, LLM ОБЯЗАН выполнить ALL пункты этого протокола.**

Если хотя бы ОДИН пункт не пройден - код НЕ генерируется, вместо этого LLM должен:
1. Сообщить пользователю о проблеме
2. Запросить уточнение/подтверждение
3. Изучить документацию

---

## 📋 Обязательный чеклист перед генерацией кода

### STEP 1: Проверка целевой версии ⚡
```
□ Определить версию Minecraft из build.gradle или fabric.mod.json
□ Определить версию Fabric API из build.gradle
□ Записать версии в рабочую память для текущей сессии
□ Проверить соответствие версий в правилах (.windsurf/rules)

❌ STOP: Если версии не определены или противоречивы - ЗАПРОСИТЬ у пользователя
```

**Текущий проект:**
- Minecraft: **1.20.1** (из правил) vs **1.21.10** (из README)
- Fabric API: **0.92.2+1.20.1** (из правил)
- Java: **17**

⚠️ **ВНИМАНИЕ**: Несоответствие версий! Приоритет - `build.gradle` или уточнение у пользователя.

### STEP 2: Валидация API методов 🔍

**ДО использования ЛЮБОГО API метода, проверить:**

#### Networking API
```
IF версия >= 1.20.5:
  ✅ ИСПОЛЬЗОВАТЬ: CustomPayload, PacketCodec, PayloadTypeRegistry
  ❌ НЕ ИСПОЛЬЗОВАТЬ: PacketByteBuf напрямую
ELSE IF версия == 1.20.1:
  ✅ ИСПОЛЬЗОВАТЬ: PacketByteBuf, PacketByteBufs.create()
  ❌ НЕ ИСПОЛЬЗОВАТЬ: CustomPayload, PacketCodec
```

#### HUD Rendering API
```
IF версия >= 1.21:
  ✅ ИСПОЛЬЗОВАТЬ: HudElementRegistry, RenderTickCounter
  ❌ НЕ ИСПОЛЬЗОВАТЬ: HudRenderCallback
ELSE IF версия == 1.20.1:
  ✅ ИСПОЛЬЗОВАТЬ: HudRenderCallback, float tickDelta
  ❌ НЕ ИСПОЛЬЗОВАТЬ: HudElementRegistry, RenderTickCounter
```

#### Identifier Creation
```
IF версия >= 1.21:
  ✅ ИСПОЛЬЗОВАТЬ: Identifier.of("modid", "name")
  ❌ НЕ ИСПОЛЬЗОВАТЬ: new Identifier()
ELSE IF версия <= 1.20.1:
  ✅ ИСПОЛЬЗОВАТЬ: new Identifier("modid", "name")
  ❌ НЕ ИСПОЛЬЗОВАТЬ: Identifier.of()
```

#### Item.Settings
```
IF версия >= 1.20.5:
  ✅ ИСПОЛЬЗОВАТЬ: new Item.Settings().registryKey(key)
ELSE IF версия == 1.20.1:
  ✅ ИСПОЛЬЗОВАТЬ: new Item.Settings() БЕЗ registryKey
  ❌ НЕ ИСПОЛЬЗОВАТЬ: .registryKey()
```

### STEP 3: Проверка импортов 📦

**ПЕРЕД добавлением импорта, убедиться:**

```
□ Импорт существует в целевой версии Fabric API
□ Импорт не из более новой версии (проверить по таблице ниже)
□ Клиентские импорты ТОЛЬКО в src/client/
□ Серверные импорты не смешаны с клиентскими
```

#### ❌ ЗАПРЕЩЕННЫЕ импорты для 1.20.1:

```java
// Networking (появилось в 1.20.5+)
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.RegistryByteBuf;

// HUD (появилось в 1.21+)
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.render.RenderTickCounter;
```

#### ✅ РАЗРЕШЕННЫЕ импорты для 1.20.1:

```java
// Networking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;

// HUD
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;

// Общие
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
```

### STEP 4: Проверка структуры проекта 🗂️

```
□ Клиентский код только в src/client/java/
□ Серверный код только в src/main/java/
□ НЕТ импортов клиентских классов в серверном коде
□ Проверка разделения через world.isClient()
```

### STEP 5: Валидация сетевых пакетов 📡

**Для 1.20.1 ОБЯЗАТЕЛЬНО:**

```java
// ✅ ПРАВИЛЬНО
// 1. Создание буфера
PacketByteBuf buf = PacketByteBufs.create();

// 2. Запись данных (СТРОГИЙ порядок!)
buf.writeVarInt(value1);
buf.writeString(value2);
buf.writeBoolean(value3);

// 3. Отправка
ServerPlayNetworking.send(player, PACKET_ID, buf);

// 4. Чтение (ИДЕНТИЧНЫЙ порядок!)
ClientPlayNetworking.registerGlobalReceiver(PACKET_ID, (client, handler, buf, sender) -> {
    int value1 = buf.readVarInt();     // ← ТОТ ЖЕ порядок!
    String value2 = buf.readString();  // ← ТОТ ЖЕ порядок!
    boolean value3 = buf.readBoolean(); // ← ТОТ ЖЕ порядок!
    
    client.execute(() -> {
        // Обработка в главном потоке
    });
});
```

**ЗАПРЕЩЕНО:**
- ❌ Разный порядок read/write
- ❌ Разные типы (writeInt → readVarInt)
- ❌ Обработка вне client.execute() или server.execute()

### STEP 6: Thread Safety проверка 🔒

```
□ Данные игроков в ConcurrentHashMap
□ Пакеты обрабатываются через client.execute() / server.execute()
□ НЕТ статических изменяемых полей без синхронизации
□ НЕТ прямого доступа к клиентским данным с сервера
```

### STEP 7: Проверка совместимости методов 🔄

**ПЕРЕД вызовом метода, проверить его сигнатуру для версии:**

#### DrawContext (1.20.1)
```java
// ✅ ДОСТУПНО в 1.20.1
context.fill(x1, y1, x2, y2, color);
context.drawBorder(x, y, width, height, color);
context.drawText(textRenderer, text, x, y, color, shadow);
context.drawTexture(...);
context.drawItem(stack, x, y);

// ❌ ПРОВЕРИТЬ доступность конкретных перегрузок!
```

#### PacketByteBuf (1.20.1)
```java
// ✅ ДОСТУПНО
buf.writeVarInt(int)
buf.writeString(String)
buf.writeBoolean(boolean)
buf.writeBlockPos(BlockPos)
buf.writeItemStack(ItemStack)

// ✅ ДОСТУПНО для чтения
buf.readVarInt()
buf.readString()
buf.readBoolean()
buf.readBlockPos()
buf.readItemStack()
```

---

## 🚨 Стоп-сигналы (когда НЕ генерировать код)

### ❌ STOP #1: Версия не определена
```
Если не можешь точно определить версию Minecraft/Fabric API:
→ STOP
→ Попросить пользователя подтвердить версию
→ Проверить build.gradle
```

### ❌ STOP #2: API метод не подтвержден
```
Если не уверен, что метод существует в целевой версии:
→ STOP
→ Проверить Fabric API Javadoc для версии
→ Проверить memory/rules для версионности
→ Запросить подтверждение у пользователя
```

### ❌ STOP #3: Противоречие в документации
```
Если README.md и правила указывают разные версии:
→ STOP
→ Сообщить о противоречии
→ Запросить приоритетный источник
```

### ❌ STOP #4: Смешение клиента и сервера
```
Если код требует клиентских импортов в серверном классе:
→ STOP
→ Предложить архитектурное решение (разделение)
→ НЕ смешивать в одном файле
```

---

## 🎯 Алгоритм работы LLM в Windsurf

```
1. ПОЛУЧИТЬ запрос пользователя
   ↓
2. ОПРЕДЕЛИТЬ целевую версию (build.gradle / fabric.mod.json)
   ↓
3. ЗАГРУЗИТЬ правила для версии в рабочую память
   ↓
4. ПРОАНАЛИЗИРОВАТЬ требуемые API/методы
   ↓
5. ПРОВЕРИТЬ каждый метод/импорт по чеклисту
   ↓
6. ЕСЛИ хотя бы 1 проверка провалена → STOP + сообщить пользователю
   ↓
7. ЕСЛИ все проверки пройдены → ГЕНЕРИРОВАТЬ код
   ↓
8. ПОСЛЕ генерации → самопроверка импортов и версионности
   ↓
9. ЕСЛИ самопроверка не прошла → ОТКАТИТЬ + исправить
```

---

## 📊 Таблица совместимости API (Quick Reference)

| API / Метод | 1.20.1 | 1.20.5 | 1.21+ | Альтернатива для 1.20.1 |
|------------|---------|---------|-------|-------------------------|
| `CustomPayload` | ❌ | ✅ | ✅ | `PacketByteBuf` |
| `PacketCodec` | ❌ | ✅ | ✅ | Manual read/write |
| `HudElementRegistry` | ❌ | ❌ | ✅ | `HudRenderCallback` |
| `RenderTickCounter` | ❌ | ❌ | ✅ | `float tickDelta` |
| `Identifier.of()` | ❌ | ❌ | ✅ | `new Identifier()` |
| `Item.Settings.registryKey()` | ❌ | ✅ | ✅ | Не использовать |
| `PacketByteBuf` | ✅ | ✅ | ✅ | - |
| `HudRenderCallback` | ✅ | ✅ | ⚠️ | Deprecated в 1.21 |
| `DrawContext` | ✅ | ✅ | ✅ | - |

**Легенда:**
- ✅ Доступно
- ❌ Не существует
- ⚠️ Deprecated (работает, но не рекомендуется)

---

## 🧪 Самопроверка перед коммитом кода

LLM должен мысленно ответить "ДА" на все вопросы:

```
✓ Я определил версию Minecraft и Fabric API?
✓ Все импорты существуют в целевой версии?
✓ Нет импортов из более новых версий?
✓ Клиентский код изолирован от серверного?
✓ PacketByteBuf: порядок read == порядок write?
✓ Сетевая логика выполняется в client.execute() / server.execute()?
✓ Thread-safe структуры для многопользовательских данных?
✓ Методы API проверены по документации для версии?
✓ НЕТ hardcoded значений, которые зависят от версии?
✓ Код скомпилируется без warnings?
```

Если хотя бы один ответ "НЕТ" → **ОСТАНОВИТЬСЯ и исправить**.

---

## 📝 Шаблон сообщения LLM при обнаружении проблемы

```
⚠️ STOP: Обнаружена потенциальная проблема версионности

Проблема: [описание]
Целевая версия: Minecraft X.X.X + Fabric API X.X.X
Конфликтующий элемент: [класс/метод/API]
Доступен с версии: [версия, когда появился]

Варианты решения:
1. [Альтернативный подход для текущей версии]
2. [Другая архитектура]
3. [Запрос подтверждения от пользователя]

Рекомендация: [что делать]

Продолжить? Требуется подтверждение.
```

---

## 🎓 Примеры применения протокола

### Пример 1: Правильная проверка

**Запрос:** "Добавь HUD для отображения маны"

**Действия LLM:**
```
1. ✓ Проверка версии: 1.20.1 (из build.gradle)
2. ✓ HUD API для 1.20.1: HudRenderCallback
3. ✓ Импорты: net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
4. ✓ Клиентский код: размещу в src/client/java/
5. ✓ Сигнатура: (DrawContext context, float tickDelta)
6. ✓ Все проверки пройдены → ГЕНЕРИРУЮ КОД
```

### Пример 2: Обнаружена проблема

**Запрос:** "Создай пакет для синхронизации маны"

**Действия LLM (если ошибочно решил использовать CustomPayload):**
```
1. ✓ Проверка версии: 1.20.1
2. ✗ Networking API: CustomPayload НЕ существует в 1.20.1!
3. → STOP
4. → Сообщаю пользователю:
   "⚠️ CustomPayload появился в 1.20.5+. Для 1.20.1 использую PacketByteBuf."
5. → Переключаюсь на PacketByteBuf реализацию
6. ✓ Генерирую корректный код
```

### Пример 3: Запрос уточнения

**Ситуация:** README.md указывает 1.21.10, но правила для 1.20.1

**Действия LLM:**
```
→ STOP
→ "⚠️ Обнаружено противоречие версий:
   - README.md: Minecraft 1.21.10
   - Правила: Minecraft 1.20.1
   
   Какая версия актуальна для разработки?
   
   Это критично, т.к. API значительно отличаются."
   
→ ОЖИДАТЬ ответа пользователя
```

---

## 🔗 Интеграция с существующими правилами

Этот протокол **дополняет** существующие правила:

- **Rule 1 (Structure)** → используется на STEP 4
- **Rule 2 (Best Practices)** → используется на STEP 2, 5, 6
- **Rule 3 (API Reference)** → используется на STEP 2, 3, 7

**Приоритет выполнения:**
1. **Сначала** - Rule 4 (этот протокол) для валидации
2. **Затем** - Rules 1-3 для имплементации

---

## 💡 Рекомендации для пользователя

### Как помочь LLM избежать ошибок:

1. **Указывайте версию явно** в запросах:
   ```
   "Для Minecraft 1.20.1 добавь систему..."
   ```

2. **Проверьте согласованность документации:**
   - README.md, build.gradle, fabric.mod.json должны указывать одну версию

3. **Используйте workspace-specific правила:**
   - Обновите .windsurf/rules с актуальной версией

4. **При обнаружении ошибки:**
   - Укажите LLM на конкретный API/метод
   - Запросите пересмотр с протоколом Rule 4

---

## 🔄 Обновление протокола

Этот протокол должен обновляться при:
- Смене целевой версии Minecraft
- Обновлении Fabric API
- Обнаружении новых паттернов ошибок

**Версия протокола:** 1.0
**Дата создания:** Ноябрь 2024
**Совместимость:** Minecraft 1.20.1+ с Fabric

---

## ✅ Checklist для пользователя перед запросом

Перед тем как попросить LLM написать код, убедитесь:

```
□ В build.gradle указана корректная версия Minecraft
□ В build.gradle указана корректная версия Fabric API
□ README.md соответствует реальным версиям проекта
□ .windsurf/rules обновлены для текущей версии
□ Вы указали версию в запросе (если есть сомнения)
```

---

**🎯 Главное правило:** Лучше спросить дважды, чем написать код с несовместимым API один раз.
