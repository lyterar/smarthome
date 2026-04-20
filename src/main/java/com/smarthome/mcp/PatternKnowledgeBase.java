package com.smarthome.mcp;

import java.util.*;

/**
 * База знаний GoF паттернов, привязанная к реальным классам проекта SmartHome.
 *
 * Каждый паттерн содержит:
 *  - Теорию из книги (назначение, применимость, результаты)
 *  - Маппинг: роль из книги -> конкретный класс проекта + пакет
 *  - Описание того, как именно паттерн реализован в проекте
 *
 * Источник: "Design Patterns" GoF, русское издание.
 */
public class PatternKnowledgeBase {

    public record Participant(
            String bookRole,
            String projectClass,
            String projectPackage,
            String description
    ) {}

    public record PatternInfo(
            String name,
            String nameRu,
            String classification,
            String intent,
            String projectUsage,
            List<String> applicability,
            List<Participant> participants,
            List<String> consequences,
            int bookPage
    ) {}

    private final Map<String, PatternInfo> patterns = new LinkedHashMap<>();

    public PatternKnowledgeBase() {
        loadAllPatterns();
    }

    public PatternInfo getPattern(String name) {
        String lower = name.toLowerCase().trim();
        for (var e : patterns.entrySet()) {
            if (e.getKey().toLowerCase().equals(lower)
                    || e.getValue().nameRu().toLowerCase().equals(lower))
                return e.getValue();
        }
        return null;
    }

    public List<PatternInfo> getAllPatterns() { return new ArrayList<>(patterns.values()); }

    public List<PatternInfo> getByClassification(String c) {
        return patterns.values().stream().filter(p -> p.classification().equals(c)).toList();
    }

    public List<String> getPatternNames() {
        return patterns.values().stream().map(p -> p.name() + " (" + p.nameRu() + ")").toList();
    }

    private void loadAllPatterns() {

        // 1. SINGLETON
        patterns.put("Singleton", new PatternInfo("Singleton", "Одиночка", "порождающий",
                "Гарантирует, что у класса есть только один экземпляр, и предоставляет к нему глобальную точку доступа.",
                "SmartHomeEngine - единственный движок. Хранит House, DeviceFactory, EventBus. Доступ через getInstance(). reset() для тестов.",
                List.of("Должен быть ровно один экземпляр класса, легко доступный всем клиентам",
                        "Единственный экземпляр должен расширяться путём порождения подклассов"),
                List.of(new Participant("Singleton", "SmartHomeEngine", "com.smarthome.pattern.creational",
                        "static instance, getInstance(), House+DeviceFactory+EventBus")),
                List.of("+ Контролируемый доступ к единственному экземпляру движка",
                        "+ Уменьшение глобальных переменных",
                        "- reset() для тестов - компромисс"),
                130));

        // 2. FACTORY METHOD
        patterns.put("Factory Method", new PatternInfo("Factory Method", "Фабричный метод", "порождающий",
                "Определяет интерфейс для создания объекта, но оставляет подклассам решение о том, какой класс инстанцировать.",
                "DeviceFactory создаёт Device по DeviceType. Реестр Map<DeviceType, Function<DeviceConfig, DeviceDriver>>. Замена mock->real: factory.register(DeviceType.LIGHT, cfg -> new Esp32LightDriver(cfg)).",
                List.of("Классу заранее неизвестно, объекты каких классов ему нужно создавать",
                        "Класс делегирует создание объектов подклассам или стратегиям",
                        "Нужна точка расширения для замены mock на реальные устройства"),
                List.of(new Participant("Creator", "DeviceFactory", "com.smarthome.pattern.creational",
                                "Реестр создателей + createDevice(). registerMockDrivers() заполняет mock-реализациями."),
                        new Participant("Product", "DeviceDriver", "com.smarthome.model.device",
                                "Интерфейс: turnOn/turnOff/getParameters/setParameter/isConnected."),
                        new Participant("ConcreteProduct", "MockLightDriver, MockThermostatDriver, MockSensorDriver, MockLockDriver, MockCameraDriver, MockSpeakerDriver", "com.smarthome.model.device",
                                "Mock-реализации DeviceDriver. Хранят состояние в памяти."),
                        new Participant("Config", "DeviceConfig", "com.smarthome.model.device",
                                "Конфигурация: имя, тип, адрес, порт, доп. параметры.")),
                List.of("+ Одна точка для замены mock на real (метод register())",
                        "+ Device работает через интерфейс DeviceDriver",
                        "+ Легко добавить новый тип: enum + драйвер + register()"),
                111));

        // 3. BUILDER
        patterns.put("Builder", new PatternInfo("Builder", "Строитель", "порождающий",
                "Отделяет конструирование сложного объекта от его представления.",
                "RoomBuilder - fluent API: new RoomBuilder(name).type(LIVING_ROOM).position(x,y).size(w,h).addDevice(d).build(). Используется в SmartHomeFacade.createRoom() и HouseSaveService.load().",
                List.of("Алгоритм создания не должен зависеть от частей объекта и их стыковки",
                        "Процесс конструирования должен обеспечивать различные представления"),
                List.of(new Participant("Builder", "RoomBuilder", "com.smarthome.pattern.creational",
                                "Fluent API: type(), position(), size(), addDevice() -> this. build() -> Room."),
                        new Participant("Product", "Room", "com.smarthome.model.room",
                                "Результат: комната с типом, позицией, размером, устройствами."),
                        new Participant("Director", "SmartHomeFacade / HouseSaveService", "com.smarthome.pattern.structural / com.smarthome.service",
                                "Используют RoomBuilder для создания комнат.")),
                List.of("+ Чистый и читаемый код создания комнат",
                        "+ Один Builder для UI и для загрузки из JSON"),
                102));

        // 4. PROTOTYPE
        patterns.put("Prototype", new PatternInfo("Prototype", "Прототип", "порождающий",
                "Задаёт виды создаваемых объектов с помощью экземпляра-прототипа и создаёт новые объекты путём копирования.",
                "Device implements Cloneable. clone() создаёт копию с новым UUID и суффиксом \" (копия)\". Для быстрого дублирования устройства в UI.",
                List.of("Нежелательно строить параллельную иерархию фабрик",
                        "Нужно быстро создать копию существующего сконфигурированного объекта"),
                List.of(new Participant("Prototype", "Device", "com.smarthome.model.device",
                        "Cloneable. clone() копирует поля, новый UUID, \" (копия)\" в имени.")),
                List.of("+ Быстрое дублирование сконфигурированного устройства",
                        "- Драйвер не клонируется (каждому устройству свой)"),
                121));

        // 5. ADAPTER
        patterns.put("Adapter", new PatternInfo("Adapter", "Адаптер", "структурный",
                "Преобразует интерфейс одного класса в интерфейс другого, который ожидают клиенты.",
                "DeviceAdapter адаптирует ExternalDeviceApi к DeviceDriver. turnOn() -> externalApi.powerOn(), isOn() -> getPowerState()==1.",
                List.of("Хотите использовать класс с несовместимым интерфейсом",
                        "Создаёте повторно используемый класс для взаимодействия с неизвестными интерфейсами"),
                List.of(new Participant("Target", "DeviceDriver", "com.smarthome.model.device",
                                "Интерфейс, который ожидает Device."),
                        new Participant("Adapter", "DeviceAdapter", "com.smarthome.pattern.structural",
                                "Реализует DeviceDriver, делегирует к ExternalDeviceApi."),
                        new Participant("Adaptee", "ExternalDeviceApi (inner interface)", "com.smarthome.pattern.structural.DeviceAdapter",
                                "Сторонний API: powerOn/powerOff/getPowerState/readStatus/writeConfig."),
                        new Participant("Client", "Device", "com.smarthome.model.device",
                                "Работает через DeviceDriver, не знает об ExternalDeviceApi.")),
                List.of("+ Интеграция устройств любых производителей без изменения Device",
                        "+ Один адаптер для разных реализаций ExternalDeviceApi"),
                141));

        // 6. DECORATOR
        patterns.put("Decorator", new PatternInfo("Decorator", "Декоратор", "структурный",
                "Динамически добавляет объекту новые обязанности. Гибкая альтернатива подклассам.",
                "LoggingDeviceDecorator оборачивает DeviceDriver и логирует turnOn/turnOff/setParameter с временной меткой. Хранит List<String> log.",
                List.of("Динамическое, прозрачное добавление обязанностей",
                        "Обязанности, которые могут быть сняты",
                        "Расширение подклассами создаёт комбинаторный взрыв"),
                List.of(new Participant("Component", "DeviceDriver", "com.smarthome.model.device",
                                "Общий интерфейс для оборачиваемого объекта и декоратора."),
                        new Participant("ConcreteComponent", "MockLightDriver и др. Mock*Driver", "com.smarthome.model.device",
                                "Конкретный объект, который оборачивается."),
                        new Participant("Decorator/ConcreteDecorator", "LoggingDeviceDecorator", "com.smarthome.pattern.structural",
                                "Хранит wrapped: DeviceDriver. Логирует действия + делегирует. getLog(), getRecentLog(n).")),
                List.of("+ Логирование добавляется/снимается без изменения драйвера",
                        "+ Можно вложить декораторы",
                        "- Множество мелких объектов"),
                173));

        // 7. COMPOSITE
        patterns.put("Composite", new PatternInfo("Composite", "Компоновщик", "структурный",
                "Компонует объекты в древовидные структуры часть-целое. Единообразная трактовка индивидуальных и составных объектов.",
                "DeviceGroup реализует DeviceDriver + List<DeviceDriver> children. turnOn/Off() вызывает у всех детей. isOn() - true если хоть один включён. Пример: \"Все лампы в гостиной\".",
                List.of("Иерархия часть-целое",
                        "Единообразная трактовка составных и индивидуальных объектов"),
                List.of(new Participant("Component", "DeviceDriver", "com.smarthome.model.device",
                                "Общий интерфейс для листьев и контейнеров."),
                        new Participant("Leaf", "MockLightDriver, MockThermostatDriver и т.д.", "com.smarthome.model.device",
                                "Листовые объекты - конкретные драйверы."),
                        new Participant("Composite", "DeviceGroup", "com.smarthome.pattern.structural",
                                "List<DeviceDriver> children. addDevice/removeDevice. Operation() делегирует всем детям.")),
                List.of("+ Единая команда для группы устройств",
                        "+ Клиент не отличает устройство от группы",
                        "- Может сделать проект слишком общим"),
                162));

        // 8. FACADE
        patterns.put("Facade", new PatternInfo("Facade", "Фасад", "структурный",
                "Унифицированный интерфейс вместо набора интерфейсов подсистемы.",
                "SmartHomeFacade - единая точка для MainController. Скрывает SmartHomeEngine, DeviceFactory, RoomBuilder, EventBus. Методы: createRoom(), addDeviceToRoom(), toggleDevice(), getHouseSummary().",
                List.of("Простой интерфейс к сложной подсистеме",
                        "Много зависимостей между клиентами и реализацией",
                        "Нужно разложить подсистему на слои"),
                List.of(new Participant("Facade", "SmartHomeFacade", "com.smarthome.pattern.structural",
                                "Делегирует: createRoom() -> RoomBuilder+House+EventBus. toggleDevice() -> Device+EventBus."),
                        new Participant("Классы подсистемы", "SmartHomeEngine, DeviceFactory, RoomBuilder, EventBus, House, Room, Device",
                                "com.smarthome.pattern.creational, model.*, event",
                                "Выполняют работу. Не знают о фасаде."),
                        new Participant("Client", "MainController", "com.smarthome.controller",
                                "UI-контроллер. Только SmartHomeFacade, не SmartHomeEngine напрямую.")),
                List.of("+ MainController не зависит от внутренней структуры",
                        "+ Легко менять реализацию без изменения UI",
                        "+ Прямой доступ к подсистеме не запрещён"),
                183));

        // 9. PROXY
        patterns.put("Proxy", new PatternInfo("Proxy", "Заместитель", "структурный",
                "Суррогат другого объекта, контролирующий доступ к нему.",
                "DeviceProxy - виртуальный заместитель. Хранит DeviceConfig+DeviceFactory. Реальный DeviceDriver создаётся при первом обращении (getReal()). isConnected()=false до инициализации. Для ESP32 - не подключаемся пока не нужно.",
                List.of("Виртуальный: тяжёлые объекты по требованию",
                        "Удалённый: представитель в другом адресном пространстве",
                        "Защищающий: контроль доступа"),
                List.of(new Participant("Subject", "DeviceDriver", "com.smarthome.model.device",
                                "Общий интерфейс для Proxy и реального драйвера."),
                        new Participant("Proxy", "DeviceProxy", "com.smarthome.pattern.structural",
                                "DeviceConfig+DeviceFactory. realDriver=null до первого вызова. getReal() - lazy init. isInitialized()."),
                        new Participant("RealSubject", "MockLightDriver (или Esp32LightDriver)", "com.smarthome.model.device",
                                "Создаётся через DeviceFactory при первом обращении.")),
                List.of("+ Не подключаемся к ESP32 пока не нужно",
                        "+ Прозрачен для Device",
                        "+ initialize() для принудительной инициализации"),
                203));

        // 10. OBSERVER
        patterns.put("Observer", new PatternInfo("Observer", "Наблюдатель", "поведения",
                "Зависимость один-ко-многим: при изменении одного объекта все зависящие оповещаются автоматически.",
                "EventBus - шина событий. subscribe(eventType, listener) / subscribeAll(). SmartHomeFacade публикует DeviceEvent. MainController подписывается и обновляет UI.",
                List.of("При изменении одного объекта нужно изменить другие, количество неизвестно",
                        "Объект уведомляет других, не зная их природы"),
                List.of(new Participant("Subject", "EventBus", "com.smarthome.event",
                                "Map<String, List<Consumer<DeviceEvent>>> listeners + globalListeners. subscribe/unsubscribe/publish/clear."),
                        new Participant("Observer", "Consumer<DeviceEvent> (лямбда в MainController)", "com.smarthome.controller",
                                "facade.getEventBus().subscribeAll(event -> Platform.runLater(this::refreshAll))."),
                        new Participant("Event", "DeviceEvent", "com.smarthome.event",
                                "type, targetId, timestamp, data."),
                        new Participant("Издатель", "SmartHomeFacade", "com.smarthome.pattern.structural",
                                "eventBus.publish(new DeviceEvent(...)) в каждом методе-действии.")),
                List.of("+ UI обновляется автоматически",
                        "+ Слабая связанность - фасад не знает о контроллере",
                        "- Каскад обновлений при частых изменениях"),
                280));

        // 11. STRATEGY
        patterns.put("Strategy", new PatternInfo("Strategy", "Стратегия", "поведения",
                "Семейство алгоритмов, инкапсулированных и взаимозаменяемых.",
                "AutomationStrategy - интерфейс: getName()+execute(Room). NightModeStrategy: лампы 10%, замки заблокированы, колонки off. EcoModeStrategy: лампы off, термостат 18C. AutomationService хранит список и применяет.",
                List.of("Много родственных классов, отличающихся только поведением",
                        "Несколько вариантов алгоритма",
                        "Много условных операторов - выносим в отдельные классы"),
                List.of(new Participant("Strategy", "AutomationStrategy", "com.smarthome.pattern.behavioral",
                                "Интерфейс: getName(), execute(Room)."),
                        new Participant("ConcreteStrategy A", "NightModeStrategy", "com.smarthome.pattern.behavioral",
                                "Ночной: лампы 10%, замки on, колонки off."),
                        new Participant("ConcreteStrategy B", "EcoModeStrategy", "com.smarthome.pattern.behavioral",
                                "Эко: лампы off, термостат 18C, камеры off."),
                        new Participant("Context", "AutomationService", "com.smarthome.service",
                                "List<AutomationStrategy>. applyStrategy(strategy, room) + событие.")),
                List.of("+ Легко добавить новый режим (PartyMode, Security...)",
                        "+ Избавляет от switch/if-else",
                        "+ Стратегия меняется на лету из UI ComboBox"),
                300));

        // 12. COMMAND
        patterns.put("Command", new PatternInfo("Command", "Команда", "поведения",
                "Инкапсулирует запрос как объект. Параметризация, очередь, отмена операций.",
                "DeviceCommand: execute/undo/getDescription. ToggleDeviceCommand - переключение с запоминанием wasOn. SetParameterCommand - параметр с oldValue. CommandHistory - стеки undo/redo (макс 50). MainController использует для кнопок Отмена/Повтор.",
                List.of("Параметризация объектов действием",
                        "Поддержка undo/redo",
                        "Протоколирование изменений"),
                List.of(new Participant("Command", "DeviceCommand", "com.smarthome.pattern.behavioral",
                                "Интерфейс: execute(), undo(), getDescription()."),
                        new Participant("ConcreteCommand (toggle)", "ToggleDeviceCommand", "com.smarthome.pattern.behavioral",
                                "Переключает Device. Сохраняет wasOn для undo."),
                        new Participant("ConcreteCommand (param)", "SetParameterCommand", "com.smarthome.pattern.behavioral",
                                "Устанавливает параметр. Сохраняет oldValue для undo."),
                        new Participant("Invoker", "CommandHistory", "com.smarthome.pattern.behavioral",
                                "ArrayDeque undoStack/redoStack. executeCommand(), undo(), redo()."),
                        new Participant("Receiver", "Device", "com.smarthome.model.device",
                                "Получатель: turnOn/turnOff/setParameter."),
                        new Participant("Client", "MainController", "com.smarthome.controller",
                                "Создаёт команды и передаёт в CommandHistory.")),
                List.of("+ Кнопки Отмена/Повтор в тулбаре",
                        "+ Каждое действие - объект с описанием",
                        "+ Легко добавить макрокоманды"),
                227));

        // 13. STATE
        patterns.put("State", new PatternInfo("State", "Состояние", "поведения",
                "Объект варьирует поведение в зависимости от внутреннего состояния.",
                "DeviceStateContext хранит DeviceState. Три inner class: OffState, OnState, ErrorState. Каждый определяет handleTurnOn/Off + getStateName/getColor. OffState.handleTurnOn() -> device.turnOn() + setState(ON_STATE).",
                List.of("Поведение зависит от состояния и меняется во время выполнения",
                        "Большие условные операторы по состоянию - каждая ветвь в отдельный класс"),
                List.of(new Participant("State", "DeviceState", "com.smarthome.pattern.behavioral",
                                "Интерфейс: handleTurnOn/Off(Device, Context), getStateName(), getColor()."),
                        new Participant("ConcreteState (OFF)", "DeviceStateContext.OffState", "com.smarthome.pattern.behavioral",
                                "private static inner. turnOn -> ON_STATE. Цвет #999999."),
                        new Participant("ConcreteState (ON)", "DeviceStateContext.OnState", "com.smarthome.pattern.behavioral",
                                "private static inner. turnOff -> OFF_STATE. Цвет #4CAF50."),
                        new Participant("ConcreteState (ERROR)", "DeviceStateContext.ErrorState", "com.smarthome.pattern.behavioral",
                                "private static inner. turnOff -> OFF_STATE (сброс). Цвет #F44336."),
                        new Participant("Context", "DeviceStateContext", "com.smarthome.pattern.behavioral",
                                "currentState: DeviceState + Device. turnOn/Off делегируют currentState.")),
                List.of("+ Нет switch/if по состоянию",
                        "+ Переходы явные и понятные",
                        "+ Легко добавить InitializingState, SleepState..."),
                291));

        // 14. ITERATOR
        patterns.put("Iterator", new PatternInfo("Iterator", "Итератор", "поведения",
                "Последовательный доступ к элементам без раскрытия внутреннего представления.",
                "Room implements Iterable<Device>. iterator() -> devices.iterator(). Позволяет for (Device d : room). Используется в NightModeStrategy, EcoModeStrategy.",
                List.of("Доступ к содержимому агрегата без раскрытия реализации",
                        "Несколько активных обходов одного агрегата",
                        "Единообразный интерфейс обхода разных структур"),
                List.of(new Participant("Aggregate", "Room (implements Iterable<Device>)", "com.smarthome.model.room",
                                "List<Device> devices. iterator() -> devices.iterator(). getDevices() -> защитная копия."),
                        new Participant("Iterator", "Iterator<Device> (Java standard)", "java.util",
                                "hasNext(), next(), remove()."),
                        new Participant("Client", "NightModeStrategy, EcoModeStrategy", "com.smarthome.pattern.behavioral",
                                "for (Device device : room) для обхода устройств.")),
                List.of("+ Чистый for-each: for (Device d : room)",
                        "+ Room не раскрывает List",
                        "+ Стандартный Java-подход"),
                249));

        // 15. TEMPLATE METHOD
        patterns.put("Template Method", new PatternInfo("Template Method", "Шаблонный метод", "поведения",
                "Скелет алгоритма, шаги которого переопределяются подклассами.",
                "DeviceInitTemplate.initialize(Device) - final: 1)checkConnection() 2)loadConfiguration() 3)applyDefaults() 4)runSelfTest(). Первые два abstract. applyDefaults/runSelfTest - hooks. MockDeviceInit - реализация для mock.",
                List.of("Зафиксировать инвариантные части алгоритма",
                        "Вычленить общее поведение подклассов (refactoring to generalize)",
                        "Управление расширениями - только через hooks"),
                List.of(new Participant("AbstractClass", "DeviceInitTemplate", "com.smarthome.pattern.behavioral",
                                "final initialize(Device). Abstract: checkConnection(), loadConfiguration(). Hooks: applyDefaults(), runSelfTest()."),
                        new Participant("ConcreteClass", "MockDeviceInit", "com.smarthome.pattern.behavioral",
                                "checkConnection() -> '[Mock] OK'. loadConfiguration() -> defaults. applyDefaults() -> device.turnOff().")),
                List.of("+ Один алгоритм инициализации для всех типов",
                        "+ Подклассы определяют только конкретные шаги",
                        "+ Для ESP32 - создать Esp32DeviceInit с HTTP в checkConnection()"),
                309));
    }
}
