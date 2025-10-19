---
trigger: always_on
---

# Fabric API - Краткий справочник

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

### Сеть
```java
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.RegistryByteBuf;
```

### HUD (Клиент)
```java
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
```

### События
```java
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
```

## 📦 Типы PacketCodec

### Примитивные типы
```java
PacketCodecs.BYTE          // byte
PacketCodecs.SHORT         // short
PacketCodecs.INTEGER       // int (4 байта)
PacketCodecs.VAR_INT       // int (переменная длина, эффективнее)
PacketCodecs.LONG          // long
PacketCodecs.VAR_LONG      // long (переменная длина)
PacketCodecs.FLOAT         // float
PacketCodecs.DOUBLE        // double
PacketCodecs.BOOLEAN       // boolean
PacketCodecs.STRING        // String
```

### Minecraft типы
```java
BlockPos.PACKET_CODEC      // BlockPos
ItemStack.PACKET_CODEC     // ItemStack
Identifier.PACKET_CODEC    // Identifier
```

### Составные пакеты
```java
// Tuple для нескольких полей
PacketCodec.tuple(
    PacketCodecs.VAR_INT, MyPacket::field1,
    PacketCodecs.STRING, MyPacket::field2,
    MyPacket::new
);

// Список
PacketCodecs.collection(ArrayList::new, elementCodec);

// Optional
codec.collect(PacketCodecs::optional);
```

## 🎮 Vanilla HUD элементы

Для позиционирования относительно ванильных элементов:
```java
VanillaHudElements.CROSSHAIR        // Прицел
VanillaHudElements.HOTBAR           // Хотбар
VanillaHudElements.EXPERIENCE_BAR   // Полоса опыта
VanillaHudElements.HEALTH           // Здоровье
VanillaHudElements.FOOD             // Голод
VanillaHudElements.AIR              // Воздух (под водой)
VanillaHudElements.ARMOR            // Броня
VanillaHudElements.CHAT             // Чат
VanillaHudElements.PLAYER_LIST      // TAB список
VanillaHudElements.DEBUG            // F3 дебаг
```

## �� DrawContext методы

### Рисование примитивов
```java
context.fill(x1, y1, x2, y2, color);                    // Прямоугольник
context.drawBorder(x, y, width, height, color);         // Рамка
context.drawHorizontalLine(x1, x2, y, color);          // Горизонтальная линия
context.drawVerticalLine(x, y1, y2, color);            // Вертикальная линия
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

## ⏱️ RenderTickCounter

```java
// Получить прогресс между тиками (0.0 - 1.0)
float tickProgress = tickCounter.getTickProgress(false);

// Для анимаций по реальному времени
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

### ColorHelper
```java
ColorHelper.lerp(amount, color1, color2)  // Смешивание цветов
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
- [ ] Все пакеты зарегистрированы
- [ ] Синхронизация данных работает корректно
- [ ] Добавлены комментарии к сложной логике
- [ ] Проверена работа на клиенте и сервере

---

**Версия**: 1.0 для Fabric 1.21.10
