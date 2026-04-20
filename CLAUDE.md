# Smart Home Constructor

## Что это
JavaFX приложение умного дома с конструктором комнат, mock-устройствами и 15 GoF паттернами.
Встроенный MCP сервер (порт 3001) — справочник паттернов из книги GoF (русское издание).

## Стек
- Java 21, JavaFX 21, Maven
- Gson для JSON
- com.sun.net.httpserver для MCP (встроен в JDK)

## Сборка и запуск
```
mvn javafx:run
```

## Структура проекта
- `model/device/` — Device, DeviceDriver (интерфейс), Mock*Driver (6 штук), DeviceConfig, DeviceType
- `model/room/` — Room (Iterable<Device>), RoomType
- `model/house/` — House (контейнер комнат)
- `pattern/creational/` — SmartHomeEngine (Singleton), DeviceFactory (Factory), RoomBuilder (Builder)
- `pattern/structural/` — SmartHomeFacade (Facade), DeviceAdapter (Adapter), LoggingDeviceDecorator (Decorator), DeviceGroup (Composite), DeviceProxy (Proxy)
- `pattern/behavioral/` — AutomationStrategy + NightMode/EcoMode (Strategy), DeviceCommand + ToggleDeviceCommand + SetParameterCommand + CommandHistory (Command), DeviceState + DeviceStateContext (State), DeviceInitTemplate + MockDeviceInit (Template Method). Iterator реализован в Room.
- `event/` — EventBus (Observer), DeviceEvent
- `service/` — AutomationService, HouseSaveService
- `controller/` — MainController (JavaFX)
- `view/` — RoomCanvas, AddRoomDialog, AddDeviceDialog
- `mcp/` — McpServer, McpTool, McpTools (управление домом), PatternKnowledgeBase + McpPatternReferenceTools (справочник паттернов)

## Замена mock на реальные устройства
Все устройства работают через интерфейс `DeviceDriver`. Mock-драйверы хранят состояние в памяти.
Для замены на ESP32:
1. Создать класс `Esp32LightDriver implements DeviceDriver`
2. Зарегистрировать в фабрике: `factory.register(DeviceType.LIGHT, cfg -> new Esp32LightDriver(cfg.getAddress()))`

## 15 GoF паттернов
| Паттерн | Класс | Пакет |
|---------|-------|-------|
| Singleton | SmartHomeEngine | pattern.creational |
| Factory Method | DeviceFactory | pattern.creational |
| Builder | RoomBuilder | pattern.creational |
| Prototype | Device.clone() | model.device |
| Adapter | DeviceAdapter | pattern.structural |
| Decorator | LoggingDeviceDecorator | pattern.structural |
| Composite | DeviceGroup | pattern.structural |
| Facade | SmartHomeFacade | pattern.structural |
| Proxy | DeviceProxy | pattern.structural |
| Observer | EventBus | event |
| Strategy | AutomationStrategy | pattern.behavioral |
| Command | DeviceCommand + CommandHistory | pattern.behavioral |
| State | DeviceStateContext | pattern.behavioral |
| Iterator | Room implements Iterable | model.room |
| Template Method | DeviceInitTemplate | pattern.behavioral |

## MCP сервер (порт 3001)
Запускается автоматически с приложением. 12 инструментов:
- Управление домом: get_patterns, list_devices, control_device, list_rooms, get_house_status
- Справочник GoF: lookup_pattern, list_all_gof_patterns, search_patterns_by_category, get_pattern_participants, verify_implementation, get_pattern_applicability, compare_patterns

## Правила кода
- Язык комментариев: русский
- Избегать сложных конструкций — код должен быть понятным
- Каждый паттерн должен соответствовать описанию из книги GoF (русское издание)
- При добавлении паттерна — обновить PatternKnowledgeBase с привязкой к реальным классам
- UI через FXML + CSS, контроллер работает только через SmartHomeFacade (не напрямую с Engine)
