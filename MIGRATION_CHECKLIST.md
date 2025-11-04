# Чеклист миграции на Minecraft 1.20.1

## 📋 Чеклист для разработчика

### Перед началом работы:
- [ ] Обновить `gradle.properties` - установить Minecraft 1.20.1
- [ ] Обновить `build.gradle` - Fabric API 0.92.2+1.20.1, Fabric Loader 0.15.11
- [ ] Установить Java 17 (если используется Java 21)
- [ ] Перечитать обновленные rules файлы

### Изменения в коде:

#### Networking (КРИТИЧЕСКИ ВАЖНО!):
- [ ] Удалить все упоминания `CustomPayload` → использовать `PacketByteBuf`
- [ ] Удалить все упоминания `PacketCodec` → использовать прямую запись/чтение
- [ ] Удалить все упоминания `RegistryByteBuf` → использовать `PacketByteBuf`
- [ ] Заменить `PayloadTypeRegistry` → использовать `ServerPlayNetworking.registerGlobalReceiver()`
- [ ] Проверить порядок `write*()`/`read*()` в буферах - ДОЛЖЕН СОВПАДАТЬ!
- [ ] Добавить импорт `PacketByteBufs.create()` для создания буферов

#### HUD Rendering:
- [ ] Заменить `HudElementRegistry` → использовать `HudRenderCallback`
- [ ] Изменить сигнатуру: `(DrawContext, RenderTickCounter)` → `(DrawContext, float tickDelta)`
- [ ] Импорт: `net.fabricmc.fabric.api.client.rendering.v1.hud.*` → `HudRenderCallback`
- [ ] Убрать `VanillaHudElements` (не нужен в HudRenderCallback)

#### Identifier:
- [ ] Заменить все `Identifier.of()` → `new Identifier()`
- [ ] Проверить все места создания идентификаторов

#### Item Registration:
- [ ] Удалить `RegistryKey` создание для предметов (если есть)
- [ ] Убрать `.registryKey()` из `Item.Settings()`
- [ ] Оставить только базовые параметры: `.maxCount()`, `.maxDamage()`, и т.д.

#### Thread Safety:
- [ ] Использовать `ConcurrentHashMap` для player data
- [ ] Обернуть клиентскую логику в пакетах в `client.execute()`
- [ ] Обернуть серверную логику в пакетах в `server.execute()`

### Сборка и компиляция:
- [ ] `./gradlew clean` - очистка старых артефактов
- [ ] `./gradlew build --warning-mode all` - сборка с предупреждениями
- [ ] Проверить отсутствие ошибок компиляции
- [ ] Проверить отсутствие deprecated warnings (или убедиться, что они понятны)
- [ ] `./gradlew --refresh-dependencies` - обновить зависимости

## 🧪 Инструкции по тестированию

### 1. Unit/Smoke тесты локально

**Компиляция и сборка**:
```bash
cd /path/to/your/mod
./gradlew clean
./gradlew build --warning-mode all
```

**Ожидаемый результат**: Build SUCCESS, нет критических warnings.

---

### 2. Запуск клиента (runClient)

**Команда**:
```bash
./gradlew runClient
```

**Проверки**:
- [ ] Игра запускается без краша
- [ ] В логах нет ошибок о missing классах (CustomPayload, HudElementRegistry, etc.)
- [ ] Мод загружается корректно (проверить в Mods menu)
- [ ] HUD отображается правильно (мана/другие элементы)
- [ ] Размеры и позиция HUD корректны на разных разрешениях

**Тест HUD**:
- Запустить мир
- Проверить отображение HUD элементов
- Изменить разрешение окна - HUD должен масштабироваться
- Нажать F1 (скрыть HUD) - элементы должны исчезнуть

---

### 3. Запуск сервера (runServer)

**Команда**:
```bash
./gradlew runServer
```

**Проверки**:
- [ ] Сервер запускается без краша
- [ ] В логах нет ошибок загрузки мода
- [ ] Предметы мода регистрируются корректно
- [ ] Events (ServerTickEvents, JOIN) работают

**Тест серверной логики**:
- Создать тестовый мир на сервере
- Подключиться клиентом
- Проверить, что ServerPlayConnectionEvents.JOIN срабатывает

---

### 4. Network/Packet тесты (КРИТИЧЕСКИ ВАЖНО!)

**Тест синхронизации**:
```bash
# Терминал 1: Запустить сервер
./gradlew runServer

# Терминал 2: Запустить клиент
./gradlew runClient
```

**Сценарий**:
1. Подключиться к локальному серверу (localhost)
2. Использовать предмет, который отправляет пакет (например, Magic Staff)
3. Проверить в логах:
   - Пакет отправлен с сервера
   - Пакет получен клиентом
   - Данные корректно прочитаны
4. Проверить HUD обновление после получения пакета

**Проверки**:
- [ ] Нет `IndexOutOfBoundsException` при чтении буфера
- [ ] Нет `DecoderException` в логах
- [ ] Данные синхронизируются корректно (например, мана отображается правильно)
- [ ] Синхронизация работает для нескольких игроков одновременно

**Логи для проверки**:
```bash
# В логах должно быть:
[Server] Sending packet to player: <username>
[Client] Received packet, data: <expected values>
```

---

### 5. Мультиплеер тест

**Сценарий**:
1. Запустить dedicated server (или через runServer)
2. Подключить 2+ клиента
3. Взаимодействие между игроками (использование магических предметов, etc.)

**Проверки**:
- [ ] Пакеты корректно отправляются всем игрокам
- [ ] Нет race conditions в ConcurrentHashMap
- [ ] При отключении игрока нет memory leaks (данные очищаются)
- [ ] При переподключении данные синхронизируются

---

### 6. Тест HUD на разных разрешениях

**Разрешения для проверки**:
- 1920x1080 (Full HD)
- 1280x720 (HD)
- 2560x1440 (2K)
- 3840x2160 (4K) - если доступно

**Проверки**:
- [ ] HUD элементы видны и читаемы
- [ ] Позиционирование корректное (не выходит за границы)
- [ ] Масштабирование работает (GUI Scale: Auto, Small, Normal, Large)

---

### 7. Persistence тест (если есть сохранение данных)

**Сценарий**:
1. Запустить мир
2. Изменить данные (например, изучить заклинание)
3. Закрыть игру
4. Заново открыть тот же мир
5. Проверить, что данные сохранились

**Проверки**:
- [ ] Данные сохраняются в файл корректно
- [ ] Данные загружаются при старте мира
- [ ] UUID игрока корректно используется в dev режиме (см. memory о UUID)

---

## ⚠️ Риски и примечания

### 1. Миграция на 1.20.5/1.21.x

**КРИТИЧЕСКИЕ BREAKING CHANGES**:

#### Networking (1.20.5+):
- **Полная переработка API** - `PacketByteBuf` устарел
- Требуется переписать ВСЮ сетевую логику на `CustomPayload` + `PacketCodec`
- Регистрация через `PayloadTypeRegistry` вместо `registerGlobalReceiver`
- **Несовместимость**: Код 1.20.1 НЕ работает в 1.20.5+ без изменений

**Оценка сложности**: ⚠️⚠️⚠️ ВЫСОКАЯ (2-4 часа на полную миграцию networking)

#### HUD Rendering (1.21+):
- `HudRenderCallback` deprecated → `HudElementRegistry`
- Изменение параметров: `float tickDelta` → `RenderTickCounter`
- Новые методы позиционирования (attachBefore/After)
- **Несовместимость**: Старый код работает, но с deprecated warning

**Оценка сложности**: ⚠️ СРЕДНЯЯ (30 минут - 1 час)

#### Identifier (1.21+):
- `new Identifier()` устарел → `Identifier.of()`
- Простая замена через find/replace
- **Несовместимость**: Старый код работает, но с deprecated warning

**Оценка сложности**: ✅ НИЗКАЯ (10-15 минут)

---

### 2. API, которые часто ломают совместимость

**Высокий риск изменений** (проверять при каждой минорной версии):

1. **Networking API** 
   - История: Полностью переписан в 1.20.5
   - Риск: ⚠️⚠️⚠️ ОЧЕНЬ ВЫСОКИЙ
   - Рекомендация: Минимизировать сетевой код, изолировать в отдельные классы

2. **Recipe/Data Generation API**
   - История: Breaking changes в 1.20 → 1.20.1 → 1.20.2
   - Риск: ⚠️⚠️ ВЫСОКИЙ
   - Рекомендация: Использовать JSON рецепты где возможно

3. **Entity Rendering API**
   - История: Изменения в каждой версии
   - Риск: ⚠️⚠️ ВЫСОКИЙ
   - Рекомендация: Следить за deprecated warnings

4. **Block Entity API**
   - История: Регулярные breaking changes
   - Риск: ⚠️ СРЕДНИЙ
   - Рекомендация: Использовать официальные примеры из Fabric docs

5. **Item/Block Properties**
   - История: Изменения в Settings API
   - Риск: ⚠️ СРЕДНИЙ (добавление обязательных полей)
   - Рекомендация: Проверять release notes каждой версии

**Низкий риск изменений** (стабильные API):
- Events API (ServerTickEvents, ClientTickEvents)
- Registry API (базовые методы)
- Basic Item/Block классы

---

### 3. Совместимость с другими модами

**Потенциальные конфликты**:
- Моды с custom networking (могут занять те же Identifier)
- Моды, модифицирующие HUD (конфликты рендеринга)
- Моды с миксинами в те же классы

**Рекомендации**:
- Использовать уникальные Identifier для пакетов (`modid:packet_name`)
- Тестировать с популярными модами (Sodium, Iris, REI, etc.)
- Документировать известные несовместимости

---

### 4. Thread Safety проблемы

**Частые ошибки**:
- Доступ к client/server state из пакетного потока
- Модификация коллекций без синхронизации
- Race conditions в ConcurrentHashMap при сложных операциях

**Решения**:
- ВСЕГДА использовать `client.execute()` / `server.execute()`
- Предпочитать `ConcurrentHashMap.compute()` вместо get-modify-put
- Избегать статических изменяемых полей

---

### 5. Dev режим особенности

**UUID проблема** (см. memory):
- В dev режиме UUID меняется при каждом перезапуске
- Решение: Глобальное хранилище или персистентность на диск
- Тестировать persistence на dedicated server

**Performance**:
- Dev режим медленнее на 20-30%
- Hot reload не работает для большинства изменений
- Использовать production build для финального тестирования

---

### 6. Версионирование правил

**Эти правила актуальны для**:
- ✅ Minecraft 1.20.1
- ✅ Fabric API 0.92.2+1.20.1
- ✅ Fabric Loader 0.15.11
- ✅ Java 17

**НЕ применимо для**:
- ❌ Minecraft 1.20.5+ (networking изменен)
- ❌ Minecraft 1.21+ (HUD API изменен, Identifier.of())
- ❌ Minecraft 1.19.x и ниже (DrawContext отсутствует)

**При обновлении версии**:
1. Читать release notes Fabric API
2. Проверять breaking changes в networking
3. Обновлять examples в rules
4. Тестировать весь код заново

---

## 📚 Дополнительные ресурсы

### Для troubleshooting:
- [Fabric Discord](https://discord.gg/v6v4pMv) - #mod-dev-help канал
- [Fabric Wiki - 1.20 Networking](https://fabricmc.net/wiki/tutorial:networking)
- [GitHub Issues - Fabric API](https://github.com/FabricMC/fabric/issues)

### Для изучения изменений API:
- [Fabric API Changelog](https://github.com/FabricMC/fabric/blob/1.20.1/CHANGELOG.md)
- [Minecraft Wiki - Version differences](https://minecraft.fandom.com/wiki/Java_Edition_version_history)

### Примеры модов на 1.20.1:
- [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod/tree/1.20)
- [Sodium](https://github.com/CaffeineMC/sodium-fabric) - networking примеры
- [Roughly Enough Items](https://github.com/shedaniel/RoughlyEnoughItems) - HUD примеры

---

**Дата создания**: Ноябрь 2024
**Версия чеклиста**: 1.0 для Minecraft 1.20.1
