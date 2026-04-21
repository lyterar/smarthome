# ФИНАЛЬНЫЙ ОТЧЁТ — Smart Home Constructor 3D UI

**Проект:** Курсовая работа — JavaFX Smart Home с 3D интерфейсом  
**Период разработки:** 2026-04-20 — 2026-04-22  
**Репозиторий:** https://github.com/lyterar/smarthome  
**Ветка:** main  

---

## Итог: 7/7 шагов выполнено ✅

---

## Выполненные шаги

### ШАГ 1 — OBJ-загрузчик моделей
**Причина:** устройства — только геометрические примитивы, выглядит как прототип.  
**Следствие:** нативный OBJ-загрузчик через JavaFX TriangleMesh; автоматический fallback на примитив.  
**GoF:** Factory Method — `ObjModelLoader.load()` + `Device3DModel.createModel()`

### ШАГ 2 — FPS движение (игровой режим)
**Причина:** базовый FPS без инерции — движение скользкое и неестественное.  
**Следствие:** инерция (lerp), коллизии со стенами, Shift=бег, боббинг камеры, захват мыши, прицел.  
**GoF:** Strategy — `FpsCameraController` и `CameraController` взаимозаменяемы

### ШАГ 3 — Смена темы (тёмная / светлая / синяя)
**Причина:** одна тема — пользователь не может настроить под своё освещение.  
**Следствие:** три CSS-темы переключаются без перезапуска; фон 3D-сцены синхронизируется.  
**GoF:** Strategy (ThemeStrategy) + Singleton (ThemeService)

### ШАГ 4 — Мультиоконный интерфейс
**Причина:** всё в одном окне — тесно, детали устройств и логи теряются.  
**Следствие:** три немодальных окна (`Modality.NONE`) работают параллельно:
- `AutomationWindow` — стратегии автоматизации + история командного лога
- `DeviceDetailWindow` — детали устройства по двойному клику (ID, параметры, toggle)
- `LogWindow` — живой цветной лог всех событий EventBus с фильтром и паузой  
**GoF:** Observer (`LogWindow` ↔ `EventBus`), Facade (все действия через `SmartHomeFacade`)

### ШАГ 5 — Текстуры комнат
**Причина:** однотонный пол — все комнаты визуально одинаковые.  
**Следствие:** процедурные текстуры (WritableImage 128×128) через `PhongMaterial.setDiffuseMap()`:

| RoomType    | Текстура | Алгоритм                              |
|-------------|----------|---------------------------------------|
| LIVING_ROOM | Дерево   | Горизонтальные доски, зернистость     |
| OFFICE      | Дерево   | То же — паркет                        |
| BEDROOM     | Ковёр    | Случайный ворс, мягкий синий тон      |
| KITCHEN     | Плитка   | Шахматный узор 16px, затирка          |
| BATHROOM    | Плитка   | Голубой 12px, бликовый шов            |
| HALLWAY     | Бетон    | Шум + крупные пятна                   |
| GARAGE      | Бетон    | Тёмнее, грубее                        |

**GoF:** Factory Method (`RoomTextureFactory.getFloorTexture`) + Flyweight (кэш `EnumMap`)

### ШАГ 6 — Анимации
**Причина:** 3D-сцена статична — нет обратной связи на действия пользователя.  
**Следствие:** четыре вида анимаций через `AnimationFactory`:

| Событие              | Анимация              | Длительность |
|----------------------|-----------------------|-------------|
| Toggle устройства    | ScaleTransition (пульс) + opacity | 360мс |
| Появление комнаты    | FadeTransition 0→1 с задержкой col×80мс | 450мс |
| Hover на устройстве  | ScaleTransition 1.0→1.15 | 150мс |
| Переход orbit↔FPS   | TranslateTransition камеры | 500мс |
| Выбор комнаты        | Timeline opacity 1→0.65→1 (×2) | 400мс |

**GoF:** Factory Method — `AnimationFactory` возвращает `Animation`; вызывающий код не знает деталей

### ШАГ 7 — Финальная проверка
**Причина:** нужно убедиться что весь код корректен перед сдачей.  
**Следствие:** статический анализ всех 10 изменённых файлов — баланс скобок, ссылки на методы.

---

## Статический анализ (mvn compile недоступен в sandbox)

| Файл                        | Строк | Скобки | Ссылки |
|-----------------------------|-------|--------|--------|
| AutomationWindow.java       | 238   | ✅ 32/32 | ✅ |
| DeviceDetailWindow.java     | 203   | ✅ 13/13 | ✅ |
| LogWindow.java              | 220   | ✅ 19/19 | ✅ |
| RoomTextureFactory.java     | 194   | ✅ 21/21 | ✅ |
| AnimationFactory.java       | 154   | ✅ 10/10 | ✅ |
| Room3DModel.java            | 168   | ✅ 12/12 | ✅ |
| Room3DView.java             | 416   | ✅ 32/32 | ✅ |
| MainController.java         | 479   | ✅ 70/70 | ✅ |
| EventBus.java               | 81    | ✅ 11/11 | ✅ |
| module-info.java            | 31    | ✅ 1/1   | ✅ |

> **Примечание:** Maven недоступен в CI-sandbox (нет интернета для скачивания зависимостей JavaFX).
> Полная компиляция проверяется запуском на локальной машине разработчика:
> `mvn compile` из корня проекта.

---

## Применённые GoF паттерны (сводная таблица)

| Паттерн        | Группа      | Классы                                            |
|----------------|-------------|---------------------------------------------------|
| Singleton      | Creational  | `SmartHomeEngine`, `ThemeService`                |
| Factory Method | Creational  | `DeviceFactory`, `ObjModelLoader`, `RoomTextureFactory`, `AnimationFactory` |
| Builder        | Creational  | `RoomBuilder`                                    |
| Prototype      | Creational  | `Device.clone()`                                 |
| Facade         | Structural  | `SmartHomeFacade`                                |
| Decorator      | Structural  | `LoggingDeviceDecorator`                         |
| Composite      | Structural  | `DeviceGroup`                                    |
| Proxy          | Structural  | `DeviceProxy`                                    |
| Adapter        | Structural  | `DeviceAdapter`                                  |
| Strategy       | Behavioral  | `AutomationStrategy`, `ThemeStrategy`, `CameraController/FpsCameraController` |
| Observer       | Behavioral  | `EventBus` + `LogWindow`                         |
| Command        | Behavioral  | `ToggleDeviceCommand`, `SetParameterCommand`, `CommandHistory` |
| Template Method| Behavioral  | `DeviceInitTemplate`                             |
| State          | Behavioral  | `DeviceStateContext`                             |

---

## Структура view-слоя (итого)

```
view/
  component/
    Room3DView.java          — SubScene, orbit/FPS камеры, анимации
    Room3DModel.java         — 3D-комната, текстуры, deviceModelById
    Device3DModel.java       — фабрика 3D-моделей (OBJ + fallback)
    CameraController.java    — орбитальная камера
    FpsCameraController.java — FPS WASD+мышь+инерция+коллизии
    ObjModelLoader.java      — нативный OBJ-загрузчик
    RoomTextureFactory.java  — процедурные текстуры пола (Шаг 5)
    AnimationFactory.java    — все 3D-анимации (Шаг 6)
    RoomCanvas.java          — 2D-план
  dialog/
    AddRoomDialog.java
    AddDeviceDialog.java
    CreateGroupDialog.java
  window/                    ← ШАГ 4 (новый пакет)
    AutomationWindow.java    — немодальное окно стратегий
    DeviceDetailWindow.java  — немодальное окно деталей устройства
    LogWindow.java           — живой лог EventBus
```

---

*Отчёт сгенерирован автоматически. Все коммиты доступны в репозитории.*
