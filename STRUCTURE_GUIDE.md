# 🏛️ Гайд по созданию структуры для Manaborn

## ✅ ЭТАП 1: Сохранение структуры (УЖЕ СДЕЛАЛ)

Ты построил структуру и получил Structure Block через:
```
/give @p structure_block
```

---

## 📝 ЭТАП 2: Настройка Structure Block

### Пошаговая инструкция:

1. **Поставь Structure Block** в **ЛЕВЫЙ НИЖНИЙ УГОЛ** твоей постройки
   - Это должен быть самый низкий блок структуры
   - Обычно на уровне земли

2. **ПКМ по блоку** → Откроется меню

3. **Выбери режим**: `SAVE` (в выпадающем меню сверху)

4. **Structure Name**: `manaborn:magic_shrine`
   - ⚠️ **ВАЖНО**: Название должно быть ТОЧНО таким!
   - Если хочешь другое название - скажи мне, я переименую файлы

5. **Настрой размеры:**
   
   **Вариант А) Автоматически:**
   - Нажми кнопку **DETECT**
   - Блок сам определит размеры структуры
   
   **Вариант Б) Вручную:**
   - **Structure Size X**: сколько блоков по X (например, 11)
   - **Structure Size Y**: высота (например, 15)
   - **Structure Size Z**: сколько блоков по Z (например, 11)
   
   💡 **Подсказка**: Ты увидишь белую рамку - она показывает границы структуры

6. **Включи опции:**
   - ✅ **Show air**: ВКЛ (чтобы сохранить пустое пространство внутри)
   - ✅ **Show bounding box**: ВКЛ (видна рамка)
   - ✅ **Include entities**: ВКЛ (если хочешь сохранить мобов/сундуки с лутом)

7. **Нажми кнопку SAVE** (внизу меню) ✅

---

## 📂 ЭТАП 3: Копирование NBT файла

### Где найти сохранённый файл:

Файл сохранился здесь:
```
C:\Users\<Твой пользователь>\AppData\Roaming\.minecraft\saves\<Название мира>\generated\minecraft\structures\manaborn\magic_shrine.nbt
```

**Быстрый способ найти:**
1. Открой папку мира (в главном меню Minecraft → Edit → Open World Folder)
2. Зайди в `generated\minecraft\structures\manaborn\`
3. Найди файл `magic_shrine.nbt`

### Куда скопировать:

**Скопируй файл сюда:**
```
d:\minecraftModsD\Manaborn\src\main\resources\data\manaborn\structure\magic_shrine.nbt
```

✅ Папка `structure` уже создана!

---

## 🔧 ЭТАП 4: Я УЖЕ СОЗДАЛ ЭТИ ФАЙЛЫ:

### ✅ Созданные файлы:

1. **Structure definition:**
   ```
   src/main/resources/data/manaborn/worldgen/structure/magic_shrine.json
   ```
   - Определяет, КАК генерируется структура
   - Высота, адаптация к ландшафту, размер

2. **Template pool:**
   ```
   src/main/resources/data/manaborn/worldgen/template_pool/magic_shrine.json
   ```
   - Указывает на NBT файл структуры
   - Можно добавить несколько вариантов (например, разные версии)

3. **Structure set:**
   ```
   src/main/resources/data/manaborn/worldgen/structure_set/magic_shrines.json
   ```
   - Определяет, ГДЕ и КАК ЧАСТО генерируется
   - **Spacing: 24** - каждые 24 чанка возможна генерация
   - **Separation: 8** - минимум 8 чанков между структурами

4. **Biome tag:**
   ```
   src/main/resources/data/manaborn/tags/worldgen/biome/has_structure/magic_shrine.json
   ```
   - Список биомов где генерируется структура
   - Сейчас: равнины, леса, тайга, луга

---

## 🎮 ЭТАП 5: Тестирование

### После того как скопируешь NBT:

1. **Скомпилируй мод:**
   ```bash
   .\gradlew build
   ```

2. **Запусти клиент:**
   ```bash
   .\gradlew runClient
   ```

3. **Создай НОВЫЙ мир:**
   - ⚠️ Структуры НЕ генерируются в старых мирах!
   - Нужен новый мир или новые чанки

4. **Найди структуру:**
   
   **Способ 1) Команда локатора:**
   ```
   /locate structure manaborn:magic_shrine
   ```
   
   **Способ 2) Просто гуляй:**
   - Структура будет генерироваться каждые 24-32 чанка
   - Искать в биомах: Plains, Forest, Taiga, Meadow

---

## ⚙️ НАСТРОЙКИ ГЕНЕРАЦИИ

### Хочешь изменить частоту генерации?

Открой: `src/main/resources/data/manaborn/worldgen/structure_set/magic_shrines.json`

```json
{
  "placement": {
    "spacing": 24,    ← Увеличь для РЕЖЕ (например, 48)
    "separation": 8   ← Минимальное расстояние между структурами
  }
}
```

**Примеры:**
- **Часто:** spacing: 16, separation: 4
- **Средне:** spacing: 24, separation: 8 (текущее)
- **Редко:** spacing: 48, separation: 16
- **Очень редко:** spacing: 80, separation: 24

### Хочешь генерацию в других биомах?

Открой: `src/main/resources/data/manaborn/tags/worldgen/biome/has_structure/magic_shrine.json`

Добавь биомы:
```json
{
  "values": [
    "minecraft:plains",
    "minecraft:desert",           ← Добавить пустыню
    "minecraft:mountains",        ← Добавить горы
    "minecraft:savanna",          ← Добавить саванну
    "minecraft:jungle"            ← Добавить джунгли
  ]
}
```

---

## 🐛 ТРАБЛШУТИНГ

### Структура не генерируется:

1. ✅ **Проверь**: NBT файл скопирован в `src/main/resources/data/manaborn/structure/magic_shrine.nbt`
2. ✅ **Проверь**: Создал НОВЫЙ мир (не старый!)
3. ✅ **Проверь**: Ищешь в правильных биомах (Plains, Forest, Taiga)
4. ✅ **Попробуй команду**: `/locate structure manaborn:magic_shrine`

### Ошибка при компиляции:

- Проверь что все JSON файлы валидны (нет лишних запятых)
- Проверь что NBT файл на месте

### Хочешь переименовать структуру:

Скажи мне новое название, я изменю все файлы автоматически!

---

## 📊 СТРУКТУРА ФАЙЛОВ (Итого):

```
src/main/resources/data/manaborn/
├── structure/
│   └── magic_shrine.nbt              ← ТЫ ДОБАВЛЯЕШЬ СЮДА
├── worldgen/
│   ├── structure/
│   │   └── magic_shrine.json         ← Я СОЗДАЛ
│   ├── template_pool/
│   │   └── magic_shrine.json         ← Я СОЗДАЛ
│   └── structure_set/
│       └── magic_shrines.json        ← Я СОЗДАЛ
└── tags/worldgen/biome/has_structure/
    └── magic_shrine.json             ← Я СОЗДАЛ
```

---

## ✅ ЧЕКЛИСТ:

- [ ] Построил структуру в мире
- [ ] Получил Structure Block (`/give @p structure_block`)
- [ ] Поставил блок в левый нижний угол
- [ ] Настроил режим SAVE, название `manaborn:magic_shrine`
- [ ] Нажал DETECT или указал размеры вручную
- [ ] Включил "Show air" и "Show bounding box"
- [ ] Нажал кнопку SAVE
- [ ] Нашёл файл `magic_shrine.nbt` в папке мира
- [ ] Скопировал в `src/main/resources/data/manaborn/structure/magic_shrine.nbt`
- [ ] Запустил `.\gradlew build`
- [ ] Запустил `.\gradlew runClient`
- [ ] Создал НОВЫЙ мир
- [ ] Нашёл структуру через `/locate structure manaborn:magic_shrine`

---

## 🎉 ГОТОВО!

После того как скопируешь NBT файл - **дай мне знать**, я помогу с тестированием!

**Вопросы?** Спрашивай! 🚀
