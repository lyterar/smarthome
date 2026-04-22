# Smart Home 3D UI — Прогресс разработки

## Текущий шаг: ШАГ 8 — Полноценный мультиоконный режим ✅

## Завершённые шаги

### ✅ ШАГ 1–6 (см. предыдущие отчёты)

### ✅ ШАГ 7 — Финальный отчёт + статический анализ
**Дата:** 2026-04-22

### ✅ ШАГ 8 — Полноценный мультиоконный режим
**Дата:** 2026-04-22

**Причина:** главное окно содержало всё: 3D-вид, список устройств, автоматизацию —
             интерфейс был перегружен, окна автоматизации и журнала не открывались
             из-за дублирующейся аннотации @FXML.
**Следствие:** 5 немодальных окон с чёткими ролями:

| Окно                | Роль                                             | Файл                      |
|---------------------|--------------------------------------------------|---------------------------|
| Главное             | Диспетчер: комнаты, темы, undo/redo, сохранение  | main.fxml + MainController|
| ViewWindow          | 2D / 3D / FPS просмотр на отдельном мониторе     | view/window/ViewWindow     |
| DeviceWindow        | Устройства: добавить, удалить, toggle, логи      | view/window/DeviceWindow   |
| AutomationWindow    | Стратегии автоматизации + история применений      | view/window/AutomationWindow|
| LogWindow           | Живой лог EventBus в реальном времени             | view/window/LogWindow      |
| DeviceDetailWindow  | Детали устройства (двойной клик)                  | view/window/DeviceDetailWindow|

**Изменённые файлы:**
- `src/main/java/com/smarthome/view/window/ViewWindow.java` — создан
- `src/main/java/com/smarthome/view/window/DeviceWindow.java` — создан
- `src/main/java/com/smarthome/controller/MainController.java` — полный рефакторинг
- `src/main/resources/fxml/main.fxml` — главное окно = диспетчер

**GoF паттерны:**
- Facade — все окна работают через SmartHomeFacade
- Observer — ViewWindow и DeviceWindow подписаны на EventBus
- Command — toggle в DeviceWindow идёт через CommandHistory

## Очередь
- [x] ШАГ 1–7 выполнены
- [x] ШАГ 8 — Полноценный мультиоконный режим
