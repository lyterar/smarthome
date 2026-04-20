# Smart Home Constructor

JavaFX приложение для конструирования умного дома с визуальным редактором комнат
и управлением устройствами.

## Архитектура

```
com.smarthome
├── model/
│   ├── device/      — Устройства (лампы, датчики, термостаты...)
│   ├── room/        — Комнаты и их свойства
│   └── house/       — Дом как контейнер комнат
├── service/         — Бизнес-логика и сервисы
├── controller/      — JavaFX контроллеры
├── view/
│   ├── component/   — Переиспользуемые UI компоненты
│   └── dialog/      — Диалоговые окна
├── pattern/
│   ├── creational/  — Factory, Builder, Singleton, Prototype
│   ├── structural/  — Adapter, Decorator, Composite, Facade, Proxy
│   └── behavioral/  — Observer, Strategy, Command, State, Iterator, Template Method
├── mcp/             — MCP сервер (HTTP JSON-RPC)
├── event/           — Система событий
└── util/            — Утилиты
```

## 15 GoF Паттернов

| #  | Паттерн          | Где используется                                    |
|----|------------------|-----------------------------------------------------|
| 1  | **Singleton**    | `SmartHomeEngine` — единственный движок системы     |
| 2  | **Factory**      | `DeviceFactory` — создание устройств по типу        |
| 3  | **Builder**      | `RoomBuilder` — пошаговое конструирование комнат    |
| 4  | **Prototype**    | `DevicePrototype` — клонирование устройств          |
| 5  | **Adapter**      | `DeviceAdapter` — адаптация mock/real устройств     |
| 6  | **Decorator**    | `LoggingDeviceDecorator` — логирование действий     |
| 7  | **Composite**    | `DeviceGroup` — группы устройств                    |
| 8  | **Facade**       | `SmartHomeFacade` — упрощённый API для UI            |
| 9  | **Proxy**        | `DeviceProxy` — ленивая загрузка + контроль доступа |
| 10 | **Observer**     | `EventBus` — уведомления об изменениях              |
| 11 | **Strategy**     | `AutomationStrategy` — стратегии автоматизации      |
| 12 | **Command**      | `DeviceCommand` — команды устройствам (undo/redo)   |
| 13 | **State**        | `DeviceState` — состояния устройств (ON/OFF/ERROR)  |
| 14 | **Iterator**     | `RoomIterator` — обход устройств в комнате          |
| 15 | **Template**     | `DeviceInitTemplate` — шаблон инициализации         |

## Mock → Real устройства

Все устройства реализуют интерфейс `DeviceDriver`. Mock-реализации в пакете
`model.device` можно заменить на реальные (ESP32, Zigbee и т.д.) просто
подставив другую реализацию в `DeviceFactory`.

```java
// Сейчас (mock):
factory.register("light", config -> new MockLightDriver());

// Потом (реальное):
factory.register("light", config -> new Esp32LightDriver(config.getAddress()));
```

## MCP Сервер

Встроенный HTTP-сервер (порт 3001) предоставляет JSON-RPC API:
- `tools/list` — список доступных инструментов
- `tools/call` — вызов инструмента (get_patterns, get_devices, control_device...)

## Запуск

```bash
mvn javafx:run
```
