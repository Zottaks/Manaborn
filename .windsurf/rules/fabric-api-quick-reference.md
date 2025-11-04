---
trigger: always_on
---

# Fabric API - Краткий справочник

**Целевая версия**: Minecraft 1.20.1 + Fabric API 0.92.2+1.20.1

## 🔗 Основные импорты

### Инициализация
```java
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ClientModInitializer;
```

### Регистрация
```java
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
// ADDED: RegistryKey доступен в 1.20.1, но не требуется для Item.Settings
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
```

### Предметы
```java
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
```

### Сеть (1.20.1)
```java
// REPLACED: В 1.20.1 используется PacketByteBuf, а НЕ CustomPayload/PacketCodec
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

// ADDED: Для работы с буфером
import io.netty.buffer.Unpooled;
```

**⚠️ ВАЖНО для 1.20.1**: API `CustomPayload` и `PacketCodec` появились в 1.20.5+. В 1.20.1 используется `PacketByteBuf`.

### HUD (Клиент) - 1.20.1
```java
// REPLACED: HudElementRegistry появился позже, используем HudRenderCallback
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
// ADDED: DrawContext появился в 1.20+, заменил MatrixStack
```

**⚠️ ВАЖНО**: `HudElementRegistry` и `RenderTickCounter` доступны с Fabric API 0.116+ (1.21+). В 1.20.1 используйте `HudRenderCallback`.

### События
```java
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
```

## 📦 Сетевое взаимодействие в 1.20.1

### Работа с PacketByteBuf

**Запись примитивов**:
```java
PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
buf.writeByte(value);          // byte
buf.writeShort(value);         // short
buf.writeInt(value);           // int (4 байта)
buf.writeVarInt(value);        // int (переменная длина, эффективнее)
buf.writeLong(value);          // long
buf.writeVarLong(value);       // long (переменная длина)
buf.writeFloat(value);         // float
buf.writeDouble(value);        // double
buf.writeBoolean(value);       // boolean
buf.writeString(value);        // String (max 32767)
buf.writeString(value, maxLen); // String с ограничением
```

**Чтение примитивов**:
```java
byte b = buf.readByte();
short s = buf.readShort();
int i = buf.readInt();
int vi = buf.readVarInt();
long l = buf.readLong();
long vl = buf.readVarLong();
float f = buf.readFloat();
double d = buf.readDouble();
boolean bool = buf.readBoolean();
String str = buf.readString();          // max 32767
String str2 = buf.readString(maxLen);   // с ограничением
```

**Minecraft типы**:
```java
// Запись
buf.writeBlockPos(blockPos);
buf.writeItemStack(itemStack);
buf.writeIdentifier(identifier);

// Чтение
BlockPos pos = buf.readBlockPos();
ItemStack stack = buf.readItemStack();
Identifier id = buf.readIdentifier();
```

## 🎮 HUD рендеринг (1.20.1)

### Регистрация через HudRenderCallback
```java
// В ClientModInitializer
HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.player == null || client.options.hudHidden) return;
    
    // tickDelta - прогресс между тиками (0.0 - 1.0)
    // Рисуем на HUD
    drawContext.fill(x1, y1, x2, y2, color);
    drawContext.drawText(textRenderer, text, x, y, color, shadow);
});
```

**Параметры**:
- `drawContext` - контекст рисования (DrawContext)
- `tickDelta` - float прогресс между тиками (0.0 - 1.0)

## 🖼️ DrawContext методы

### Рисование примитивов
```java
context.fill(x1, y1, x2, y2, color);                    // Прямоугольник
context.drawBorder(x, y, width, height, color);         // Рамка
context.drawHorizontalLine(x1, x2, y, color);          // Горизонтальная линия (RenderSystem)
context.drawVerticalLine(x, y1, y2, color);            // Вертикальная линия (RenderSystem)
```

### Рисование текста
```java
context.drawText(textRenderer, text, x, y, color, shadow);
context.drawTextWithShadow(textRenderer, text, x, y, color);
context.drawCenteredTextWithShadow(textRenderer, text, centerX, y, color);
```

### Рисование текстур
```java
context.drawTexture(identifier, x, y, u, v, width, height);
context.drawTexture(identifier, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
```

### Рисование предметов
```java
context.drawItem(itemStack, x, y);
context.drawItemWithoutEntity(itemStack, x, y);
context.drawItemInSlot(textRenderer, itemStack, x, y);
```

## 🎯 Цвета (ARGB формат)

```java
0xAARRGGBB  // A=Alpha, R=Red, G=Green, B=Blue

// Примеры
0xFF000000  // Черный (непрозрачный)
0xFFFFFFFF  // Белый (непрозрачный)
0xFFFF0000  // Красный
0xFF00FF00  // Зеленый
0xFF0000FF  // Синий
0x80FFFFFF  // Белый (50% прозрачности)
0x00000000  // Полностью прозрачный
```

## 📊 Размеры экрана

```java
int screenWidth = context.getScaledWindowWidth();
int screenHeight = context.getScaledWindowHeight();
```

## ⏱️ Анимации и время

```java
// tickDelta из HudRenderCallback (0.0 - 1.0)
float partialTicks = tickDelta;

// Для анимаций по реальному времени
double currentTime = System.currentTimeMillis() / 1000.0;
// Или через Util
double currentTime = Util.getMeasuringTimeMs() / 1000.0;
```

## 🎲 Полезные утилиты

### MathHelper
```java
MathHelper.sin(angle)           // Синус
MathHelper.cos(angle)           // Косинус
MathHelper.abs(value)           // Абсолютное значение
MathHelper.clamp(value, min, max)  // Ограничение значения
MathHelper.lerp(delta, start, end) // Линейная интерполяция
```

### ColorHelper (1.20+)
```java
// ADDED: Доступен в 1.20.1+
ColorHelper.Argb.getAlpha(color);           // Извлечь альфа
ColorHelper.Argb.getRed(color);             // Извлечь красный
ColorHelper.Argb.getGreen(color);           // Извлечь зеленый
ColorHelper.Argb.getBlue(color);            // Извлечь синий
ColorHelper.Argb.getArgb(a, r, g, b);       // Создать цвет
```

## 🔄 Жизненный цикл событий

### Порядок выполнения при входе игрока:
1. `ServerPlayConnectionEvents.JOIN` - игрок подключился
2. Инициализация данных игрока
3. Отправка синхронизирующих пакетов
4. `ServerTickEvents` начинают обрабатывать игрока

### Порядок в тике:
1. `ServerTickEvents.START_SERVER_TICK`
2. Обработка логики мира
3. `ServerTickEvents.END_SERVER_TICK`

## 🛠️ Отладка

### Логирование
```java
System.out.println("Debug message");  // Простой вывод
LOGGER.info("Info message");          // Через SLF4J logger
LOGGER.error("Error message", exception);
```

### Проверка стороны
```java
world.isClient()     // true на клиенте
!world.isClient()    // true на сервере
```

## 📋 Чеклист перед коммитом

- [ ] Код компилируется без ошибок
- [ ] Нет предупреждений о deprecated API
- [ ] Клиентский код не импортируется в серверный
- [ ] Все пакеты зарегистрированы через `ServerPlayNetworking.registerGlobalReceiver()`
- [ ] Синхронизация данных работает корректно
- [ ] Добавлены комментарии к сложной логике
- [ ] Проверена работа на клиенте и сервере
- [ ] Буферы (`PacketByteBuf`) правильно читаются и записываются

## 🔄 Совместимость с 1.21+

**API, которые появились ПОСЛЕ 1.20.1:**
- ❌ `CustomPayload` / `PacketCodec` - появились в 1.20.5+
- ❌ `HudElementRegistry` - появился в Fabric API 0.116+ (1.21+)
- ❌ `RenderTickCounter` - появился в 1.21+
- ❌ `Identifier.of()` - появился в 1.21+
- ❌ `Item.Settings.registryKey()` - требуется с 1.20.5+

**Что использовать в 1.20.1:**
- ✅ `PacketByteBuf` для сети
- ✅ `HudRenderCallback` для HUD
- ✅ `new Identifier()` вместо `Identifier.of()`
- ✅ `Item.Settings` без `registryKey()`

---

**Версия**: 2.0 для Fabric 1.20.1
**Дата обновления**: Ноябрь 2024
**Совместимость**: Minecraft 1.20.1 + Fabric API 0.92.2+1.20.1
