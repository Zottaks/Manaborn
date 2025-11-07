# Правила разработки Minecraft модов на Fabric

## 📋 Общая информация

Этот проект - мод для Minecraft на Fabric loader.
- **Версия Minecraft**: 1.20.1
- **Версия Fabric Loader**: 0.15.11
- **Версия Fabric API**: 0.92.2+1.20.1
- **Версия Java**: 17
- **Система сборки**: Gradle с Fabric Loom 1.6

## 🎯 Основные принципы

### 1. Структура проекта
```
src/
├── main/
│   ├── java/com/manaborn/          # Основной код мода (серверная и общая логика)
│   └── resources/
│       ├── assets/manaborn/         # Ресурсы (текстуры, модели, локализация)
│       └── fabric.mod.json          # Метаданные мода
└── client/
    └── java/com/manaborn/client/    # Клиентский код (рендеринг, UI, HUD)
```

### 2. Разделение клиента и сервера
- **Серверный код** (`src/main/java`): логика игры, обработка данных, сетевое взаимодействие
- **Клиентский код** (`src/client/java`): рендеринг, UI, HUD, клиентские события
- **НИКОГДА** не импортируйте клиентские классы в серверный код
- Используйте `world.isClient()` для проверки стороны выполнения

### 3. Инициализация мода
- Основной класс реализует `ModInitializer` (в `src/main/java`)
- Клиентский класс реализует `ClientModInitializer` (в `src/client/java`)
- Регистрация в `fabric.mod.json`:
```json
"entrypoints": {
  "main": ["com.yourmod.YourMod"],
  "client": ["com.yourmod.client.YourModClient"]
}
```

## 🔧 API и лучшие практики

### Регистрация предметов (Items) - 1.20.1
```java
// REPLACED: В 1.20.1 НЕ используется RegistryKey в Item.Settings
// Создаем Identifier для предмета
Identifier itemId = new Identifier(MOD_ID, "item_name");

// Регистрируем предмет
Item item = Registry.register(
    Registries.ITEM, 
    itemId, 
    new CustomItem(new Item.Settings()
        .maxCount(64))
);

// Добавляем в группу предметов
ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
    .register(entries -> entries.add(item));
```

**⚠️ ВАЖНО**: `RegistryKey` в `Item.Settings()` появился в 1.20.5+. В 1.20.1 он не используется.

### Сетевое взаимодействие (Networking) - 1.20.1

**⚠️ КРИТИЧЕСКИ ВАЖНО**: В 1.20.1 используется `PacketByteBuf`, а НЕ `CustomPayload`/`PacketCodec` (они появились в 1.20.5+).

#### 1. Регистрация пакета (в ModInitializer)
```java
// Для пакетов Server -> Client
public static final Identifier MANA_SYNC_PACKET = new Identifier(MOD_ID, "mana_sync");

@Override
public void onInitialize() {
    // Регистрация не требуется на сервере для отправки
}
```

#### 2. Получение пакета (в ClientModInitializer)
```java
@Override
public void onInitializeClient() {
    ClientPlayNetworking.registerGlobalReceiver(MANA_SYNC_PACKET, 
        (client, handler, buf, responseSender) -> {
            // Читаем данные из буфера
            int mana = buf.readVarInt();
            int maxMana = buf.readVarInt();
            
            // Выполняем в главном потоке клиента
            client.execute(() -> {
                // Обработка данных пакета
                ClientManaState.set(mana, maxMana);
            });
        });
}
```

#### 3. Отправка пакета Server -> Client
```java
// С сервера клиенту
public static void sendManaSync(ServerPlayerEntity player, int mana, int maxMana) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeVarInt(mana);
    buf.writeVarInt(maxMana);
    
    ServerPlayNetworking.send(player, MANA_SYNC_PACKET, buf);
}
```

#### 4. Отправка пакета Client -> Server
```java
// Регистрация на сервере (в ModInitializer)
ServerPlayNetworking.registerGlobalReceiver(SPELL_CAST_PACKET,
    (server, player, handler, buf, responseSender) -> {
        // Читаем данные
        int spellId = buf.readVarInt();
        
        // Обработка в главном потоке сервера
        server.execute(() -> {
            // Логика на сервере
        });
    });

// Отправка с клиента (в ClientModInitializer или клиентском коде)
PacketByteBuf buf = PacketByteBufs.create();
buf.writeVarInt(spellId);
ClientPlayNetworking.send(SPELL_CAST_PACKET, buf);
```

**Импорты для networking**:
```java
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
```

### HUD рендеринг (1.20.1)

**✅ ПРАВИЛЬНО для 1.20.1**: Используйте `HudRenderCallback`

```java
// В ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

@Override
public void onInitializeClient() {
    HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) {
            return;
        }
        
        // tickDelta - прогресс между тиками (0.0 - 1.0)
        // Рисуем на HUD
        drawContext.fill(x1, y1, x2, y2, color);
        drawContext.drawText(client.textRenderer, text, x, y, color, true);
    });
}
```

**⚠️ ВАЖНО**: `HudElementRegistry` появился в Fabric API 0.116+ (для 1.21+). В 1.20.1 используйте `HudRenderCallback`.

### События (Events)

#### Тики сервера
```java
ServerTickEvents.END_SERVER_TICK.register(server -> {
    // Логика каждый тик сервера (20 раз в секунду)
});
```

#### Тики клиента
```java
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (client.player != null) {
        // Логика каждый тик клиента
    }
});
```

#### Подключение игрока
```java
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    ServerPlayerEntity player = handler.player;
    // Логика при входе игрока
    // Отправляем синхронизирующие пакеты
});
```

### Хранение данных игрока

Используйте `Map<UUID, Data>` для хранения данных по игрокам:
```java
private static final Map<UUID, Integer> playerData = new ConcurrentHashMap<>();

public static int getData(UUID playerId) {
    return playerData.getOrDefault(playerId, defaultValue);
}

public static void setData(UUID playerId, int value) {
    playerData.put(playerId, value);
}

// ADDED: Thread-safe операции
public static void updateData(UUID playerId, int delta) {
    playerData.compute(playerId, (id, current) -> {
        return (current == null ? 0 : current) + delta;
    });
}
```

**⚠️ Thread Safety**: Всегда используйте `ConcurrentHashMap` для данных игроков, т.к. доступ может быть из разных потоков.

## ⚠️ Частые ошибки и как их избежать

### 1. Неправильные импорты пакетов (1.20.1)
❌ **НЕПРАВИЛЬНО** (API из 1.21+):
```java
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
```

✅ **ПРАВИЛЬНО** (для 1.20.1):
```java
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
```

### 2. Создание Identifier (1.20.1)
❌ **НЕПРАВИЛЬНО** (синтаксис 1.21+):
```java
Identifier.of("modid", "name")  // НЕ СУЩЕСТВУЕТ в 1.20.1!
```

✅ **ПРАВИЛЬНО** (для 1.20.1):
```java
new Identifier("modid", "name")
```

### 3. Item Settings (1.20.1)
❌ **НЕПРАВИЛЬНО** (1.20.5+):
```java
new Item.Settings()
    .maxCount(1)
    .registryKey(itemKey)  // НЕ СУЩЕСТВУЕТ в 1.20.1!
```

✅ **ПРАВИЛЬНО** (для 1.20.1):
```java
new Item.Settings()
    .maxCount(1)
```

### 4. Проверка стороны (Client/Server)
```java
@Override
public ActionResult use(World world, PlayerEntity user, Hand hand) {
    if (world.isClient()) {
        return ActionResult.PASS;  // Ничего не делаем на клиенте
    }
    
    // Серверная логика
    ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;
    // ...
    return ActionResult.SUCCESS;
}
```

### 5. Синхронизация данных
- Сервер - источник истины
- Всегда синхронизируйте важные данные с клиентом через пакеты
- Не полагайтесь на клиентские данные для серверной логики
- Отправляйте пакеты ТОЛЬКО при изменении данных (не каждый тик)

### 6. PacketByteBuf - порядок чтения/записи
❌ **НЕПРАВИЛЬНО**:
```java
// Запись
buf.writeVarInt(mana);
buf.writeString(playerName);

// Чтение (неправильный порядок!)
String name = buf.readString();
int mana = buf.readVarInt();
```

✅ **ПРАВИЛЬНО**:
```java
// Запись
buf.writeVarInt(mana);
buf.writeString(playerName);

// Чтение (тот же порядок!)
int mana = buf.readVarInt();
String name = buf.readString();
```

**⚠️ КРИТИЧЕСКИ ВАЖНО**: Порядок и типы операций чтения ДОЛЖНЫ точно соответствовать порядку и типам записи!

## 📚 Полезные ресурсы

### Официальная документация
- [Fabric Wiki](https://wiki.fabricmc.net/tutorial:start) - туториалы и гайды
- [Fabric Documentation](https://docs.fabricmc.net/develop/) - современная документация
- [Fabric API Javadoc](https://maven.fabricmc.net/docs/fabric-api-0.92.2+1.20.1/) - документация API для 1.20.1

### Важные страницы
- [Networking](https://fabricmc.net/wiki/tutorial:networking) - сетевое взаимодействие (1.20.1)
- [Items](https://docs.fabricmc.net/develop/items/first-item) - создание предметов
- [Blocks](https://docs.fabricmc.net/develop/blocks/first-block) - создание блоков
- [HUD Rendering](https://fabricmc.net/wiki/tutorial:hud) - старый туториал для 1.20.1

### Изменения в версиях
- [Fabric for 1.20](https://fabricmc.net/2023/05/25/120.html) - релиз-ноты
- [Fabric for 1.20.5](https://fabricmc.net/2024/04/15/1205.html) - networking переработан!
- [Fabric for 1.21](https://fabricmc.net/2024/05/31/121.html) - HUD API обновлен

## 🔍 Проверка кода

### Компиляция с предупреждениями
```bash
./gradlew clean build --warning-mode all
```

### Проверка deprecated API
В `build.gradle` добавьте:
```groovy
tasks.withType(JavaCompile).configureEach {
    it.options.release = 17
    it.options.compilerArgs << "-Xlint:deprecation"
}
```

### Проверка модификаторов доступа
```bash
./gradlew build --info | grep "warning"
```

## 📝 Комментарии в коде

Используйте JavaDoc для документирования:
```java
/**
 * Краткое описание класса/метода
 * 
 * Подробное описание функциональности
 * 
 * @param parameter Описание параметра
 * @return Описание возвращаемого значения
 * @since 1.0.0
 */
```

## 🎨 Стиль кода

- Используйте `final` для неизменяемых полей
- Предпочитайте `record` для простых data-классов (Java 17+)
- Используйте `static final` для констант
- Именуйте константы `UPPER_SNAKE_CASE`
- Именуйте переменные и методы `camelCase`
- Именуйте классы `PascalCase`

## 🚀 Запуск и тестирование

```bash
# Очистка и сборка
./gradlew clean build

# Запуск клиента
./gradlew runClient

# Запуск сервера
./gradlew runServer

# Запуск с флагами для отладки networking
./gradlew runClient --debug-jvm

# Обновление зависимостей
./gradlew --refresh-dependencies
```

## 🔄 Миграция на 1.20.5+ и 1.21+

### Networking изменения (1.20.5+)
При миграции на 1.20.5+:
- ❌ Удалить `PacketByteBuf` код
- ✅ Использовать `CustomPayload` + `PacketCodec`
- ✅ Регистрировать через `PayloadTypeRegistry`

### HUD изменения (1.21+)
При миграции на 1.21+:
- ❌ Удалить `HudRenderCallback`
- ✅ Использовать `HudElementRegistry`
- ✅ Параметр `RenderTickCounter` вместо `float tickDelta`

### Identifier изменения (1.21+)
При миграции на 1.21+:
- ❌ Заменить `new Identifier()` на `Identifier.of()`

### Item Settings изменения (1.20.5+)
При миграции на 1.20.5+:
- ✅ Добавить `.registryKey()` в `Item.Settings()`

## ⚠️ Риски совместимости

### API, которые часто ломают совместимость:
1. **Networking** - полностью переработан в 1.20.5
2. **HUD Rendering** - изменен в 1.21 (новый API)
3. **Entity Rendering** - регулярные изменения
4. **Recipe API** - изменения в каждой минорной версии
5. **Data Generation** - breaking changes между версиями

### Критические точки для тестирования:
- ✅ Синхронизация данных клиент-сервер (через пакеты)
- ✅ HUD отображение на разных разрешениях
- ✅ Мультиплеер (несколько игроков одновременно)
- ✅ Перезаход в мир (сохранение/загрузка данных)
- ✅ Совместимость с другими модами

### Thread-safety проблемы:
- ⚠️ Всегда используйте `client.execute()` для клиентской логики в пакетах
- ⚠️ Всегда используйте `server.execute()` для серверной логики в пакетах
- ⚠️ `ConcurrentHashMap` для данных игроков
- ⚠️ Избегайте статических полей для изменяемых данных

---

**Последнее обновление**: Ноябрь 2024
**Версия правил**: 2.0 для Minecraft 1.20.1
**Совместимость**: Minecraft 1.20.1 + Fabric API 0.92.2+1.20.1
**Java**: 17