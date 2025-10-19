---
trigger: always_on
---

# Правила разработки Minecraft модов на Fabric

## 📋 Общая информация

Этот проект - мод для Minecraft на Fabric loader.
- **Версия Minecraft**: 1.21.10
- **Версия Fabric Loader**: 0.17.2
- **Версия Fabric API**: 0.134.1+1.21.10
- **Версия Java**: 21
- **Система сборки**: Gradle с Fabric Loom 1.11

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

### Регистрация предметов (Items)
```java
// Создаем ключ реестра
RegistryKey<Item> itemKey = RegistryKey.of(
    Registries.ITEM.getKey(), 
    Identifier.of(MOD_ID, "item_name")
);

// Регистрируем предмет
Item item = Registry.register(
    Registries.ITEM, 
    itemKey, 
    new CustomItem(new Item.Settings()
        .maxCount(64)
        .registryKey(itemKey))
);

// Добавляем в группу предметов
ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
    .register(entries -> entries.add(item));
```

### Сетевое взаимодействие (Networking)

#### 1. Создание пакета (Payload)
```java
public record MyPacketS2C(int data, String message) implements CustomPayload {
    // ID пакета
    public static final CustomPayload.Id<MyPacketS2C> ID = 
        new CustomPayload.Id<>(Identifier.of("modid", "packet_name"));
    
    // Кодек для сериализации/десериализации
    public static final PacketCodec<RegistryByteBuf, MyPacketS2C> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, MyPacketS2C::data,
            PacketCodecs.STRING, MyPacketS2C::message,
            MyPacketS2C::new
        );
    
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
```

#### 2. Регистрация пакета (в ModInitializer)
```java
// Для пакетов Server -> Client
PayloadTypeRegistry.playS2C().register(MyPacketS2C.ID, MyPacketS2C.CODEC);

// Для пакетов Client -> Server
PayloadTypeRegistry.playC2S().register(MyPacketC2S.ID, MyPacketC2S.CODEC);
```

#### 3. Отправка пакета
```java
// С сервера клиенту
ServerPlayNetworking.send(serverPlayer, new MyPacketS2C(42, "Hello"));

// С клиента серверу
ClientPlayNetworking.send(new MyPacketC2S(data));
```

#### 4. Получение пакета (в ClientModInitializer)
```java
ClientPlayNetworking.registerGlobalReceiver(MyPacketS2C.ID, (payload, context) -> {
    // Выполняем в главном потоке клиента
    context.client().execute(() -> {
        // Обработка данных пакета
        int data = payload.data();
        String message = payload.message();
    });
});
```

### HUD рендеринг (ВАЖНО!)

**⚠️ УСТАРЕЛО**: `HudRenderCallback` deprecated с Fabric API 0.116+

**✅ ПРАВИЛЬНО**: Используйте `HudElementRegistry`

```java
// В ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

HudElementRegistry.attachElementBefore(
    VanillaHudElements.CHAT,  // Рендерим перед чатом
    Identifier.of("modid", "hud_element"),
    MyHudRenderer::render
);

// Метод рендеринга
public static void render(DrawContext context, RenderTickCounter tickCounter) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.player == null || client.options.hudHidden) return;
    
    // Рисуем на HUD
    context.fill(x1, y1, x2, y2, color);
    context.drawText(textRenderer, text, x, y, color, shadow);
}
```

**Другие методы позиционирования:**
- `HudElementRegistry.addFirst()` - в начало
- `HudElementRegistry.addLast()` - в конец
- `HudElementRegistry.attachElementAfter()` - после элемента

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
```

## ⚠️ Частые ошибки и как их избежать

### 1. Неправильные импорты пакетов
❌ **НЕПРАВИЛЬНО:**
```java
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback; // DEPRECATED!
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry; // НЕВЕРНЫЙ ПУТЬ!
```

✅ **ПРАВИЛЬНО:**
```java
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
```

### 2. Создание Identifier
❌ **УСТАРЕЛО** (до 1.20):
```java
new Identifier("modid", "name")
```

✅ **ПРАВИЛЬНО** (1.20+):
```java
Identifier.of("modid", "name")
```

### 3. Item Settings
Всегда указывайте `registryKey` при создании предмета:
```java
new Item.Settings()
    .maxCount(1)
    .registryKey(itemKey)  // ОБЯЗАТЕЛЬНО!
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

## 📚 Полезные ресурсы

### Официальная документация
- [Fabric Wiki](https://wiki.fabricmc.net/tutorial:start) - туториалы и гайды
- [Fabric Documentation](https://docs.fabricmc.net/develop/) - современная документация
- [Fabric API Javadoc](https://maven.fabricmc.net/docs/) - документация API

### Важные страницы
- [Networking](https://docs.fabricmc.net/develop/networking) - сетевое взаимодействие
- [HUD Rendering](https://docs.fabricmc.net/develop/rendering/hud) - рендеринг на HUD
- [Items](https://docs.fabricmc.net/develop/items/first-item) - создание предметов
- [Blocks](https://docs.fabricmc.net/develop/blocks/first-block) - создание блоков

### Изменения в версиях
- [Fabric for 1.20](https://fabricmc.net/2023/05/25/120.html)
- [Fabric for 1.21](https://fabricmc.net/2024/05/31/121.html)

## 🔍 Проверка кода

### Компиляция с предупреждениями
```bash
./gradlew clean build --warning-mode all
```

### Проверка deprecated API
В `build.gradle` добавлено:
```groovy
tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
    it.options.compilerArgs << "-Xlint:deprecation"
}
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
 */
```

## 🎨 Стиль кода

- Используйте `final` для неизменяемых полей
- Предпочитайте `record` для простых data-классов
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

# Обновление зависимостей
./gradlew --refresh-dependencies
```

---

**Последнее обновление**: Октябрь 2025
**Версия правил**: 1.0

## 🔄 Обновление: Deprecated API в Fabric 1.21.10

### EntityRendererRegistry (Deprecated но рабочий)

**Статус**: Deprecated в Fabric API, но всё ещё официальный способ для 1.21.10

**Правильное использование**:
```java
/**
 * Регистрирует рендерер для кастомной сущности
 * 
 * EntityRendererRegistry помечен как deprecated,
 * но остается рабочим способом для Fabric 1.21.10
 */
@SuppressWarnings("deprecation")
private void registerCustomEntityRenderer() {
    EntityRendererRegistry.register(
        ModEntities.CUSTOM_ENTITY, 
        CustomEntityRenderer::new
    );
}
```

**Для сущностей-снарядов (стрелы, снежки и т.д.)**:
```java
@SuppressWarnings("deprecation")
private void registerProjectileRenderer() {
    EntityRendererRegistry.register(
        ModEntities.ICE_SHARD, 
        ArrowEntityRenderer::new  // Для стрел
    );
}
```

**Важно**: 
- Без регистрации рендерера игра **крашится** при попытке отрисовать сущность
- `@SuppressWarnings("deprecation")` подавляет предупреждение линтера
- Новый API для регистрации рендереров планируется в будущих версиях Fabric

---

**Дата обновления**: 19 октября 2025
