# Smart Home 3D UI — Прогресс разработки

## Текущий шаг: ЗАВЕРШЕНО ✅

## Завершённые шаги

### ✅ ШАГ 1 — OBJ-загрузчик моделей
**Дата:** 2026-04-20  
**Причина:** устройства отображались только геометрическими примитивами → выглядело как прототип.  
**Следствие:** реализован нативный OBJ-загрузчик через JavaFX TriangleMesh; Device3DModel
               пробует OBJ сначала и автоматически падает на примитив если файл отсутствует.

**Изменённые файлы:**
- `src/main/java/com/smarthome/view/component/ObjModelLoader.java` — создан (новый)
- `src/main/java/com/smarthome/view/component/Device3DModel.java` — обновлён (OBJ + fallback)
- `src/main/resources/models/README.md` — создан (инструкция по скачиванию моделей)
- `pom.xml` — добавлен комментарий о FXyz3D и причине нативной реализации

**GoF паттерн:**
  Factory Method — ObjModelLoader.load() и Device3DModel.createModel()

---

### ✅ ШАГ 2 — FPS движение как в игре
**Дата:** 2026-04-21  
**Причина:** базовый FPS без инерции и коллизий → движение казалось скользящим и неестественным.  
**Следствие:** добавлены инерция (lerp), коллизии со стенами, бег (Shift), боббинг камеры,
               захват мыши (Cursor.NONE), прицел (Canvas overlay).

**Изменённые файлы:**
- `src/main/java/com/smarthome/view/component/FpsCameraController.java` — полный рефакторинг
- `src/main/java/com/smarthome/view/component/Room3DView.java` — Canvas-прицел + передача bounds

**GoF паттерн:**
  Strategy — FpsCameraController и CameraController взаимозаменяемы через attach/detach

---

### ✅ ШАГ 3 — Смена темы (тёмная / светлая / синяя)
**Дата:** 2026-04-21  
**Причина:** одна тема → пользователь не может настроить интерфейс под своё освещение и вкус.  
**Следствие:** три темы (тёмная, светлая, синяя) переключаются без перезапуска;
               фон 3D-сцены синхронизируется с активной темой.

**Изменённые файлы:**
- `src/main/java/com/smarthome/service/ThemeService.java` — создан (Singleton + Strategy)
- `src/main/resources/css/theme-dark.css` — создан (тёмная тема)
- `src/main/resources/css/theme-light.css` — создан (светлая тема)
- `src/main/resources/css/theme-blue.css` — создан (синяя тема, морской стиль)
- `src/main/resources/fxml/main.fxml` — добавлены три кнопки тем, CSS переключён на theme-dark.css
- `src/main/java/com/smarthome/view/component/Room3DView.java` — метод setSubSceneBackground()
- `src/main/java/com/smarthome/controller/MainController.java` — обработчики onThemeDark/Light/Blue

**GoF паттерн:**
  Strategy — ThemeStrategy (интерфейс) реализован тремя конкретными стратегиями.
  Singleton — единственный экземпляр ThemeService.getInstance() на приложение.

---

### ✅ ШАГ 4 — Мультиоконный интерфейс
**Дата:** 2026-04-22  
**Причина:** всё в одном окне — тесно; автоматизация, детали устройств и лог событий теряются.  
**Следствие:** три немодальных окна (Modality.NONE) работают параллельно с главным окном.

**Изменённые файлы:**
- `src/main/java/com/smarthome/view/window/AutomationWindow.java` — создан (стратегии + история)
- `src/main/java/com/smarthome/view/window/DeviceDetailWindow.java` — создан (детали устройства)
- `src/main/java/com/smarthome/view/window/LogWindow.java` — создан (живой лог EventBus)
- `src/main/java/com/smarthome/event/EventBus.java` — добавлен unsubscribeAll()
- `src/main/java/module-info.java` — добавлены exports/opens для view.window
- `src/main/java/com/smarthome/controller/MainController.java` — initWindows(), двойной клик, кнопки
- `src/main/resources/fxml/main.fxml` — кнопки "Автоматизация" и "Журнал"

**GoF паттерн:**
  Observer — LogWindow подписывается на EventBus.subscribeAll() и отписывается при закрытии.
  Facade — все действия окон идут через SmartHomeFacade.

---

### ✅ ШАГ 5 — Текстуры комнат
**Дата:** 2026-04-22  
**Причина:** однотонный цвет пола → все комнаты выглядят одинаково и пусто.  
**Следствие:** процедурные текстуры (WritableImage) накладываются через PhongMaterial.setDiffuseMap();
               каждый RoomType получает уникальную поверхность: дерево, плитка, ковёр, бетон.

**Изменённые файлы:**
- `src/main/java/com/smarthome/view/component/RoomTextureFactory.java` — создан (Factory Method)
- `src/main/java/com/smarthome/view/component/Room3DModel.java` — метод createFloorMaterial()
- `src/main/resources/textures/README.md` — создан (документация)

**GoF паттерн:**
  Factory Method — RoomTextureFactory.getFloorTexture(RoomType) создаёт нужную текстуру;
  кэширование через EnumMap — каждый тип генерируется один раз.


---

### ✅ ШАГ 6 — Анимации
**Дата:** 2026-04-22  
**Причина:** 3D-сцена статична — нет обратной связи на действия пользователя.  
**Следствие:** четыре вида анимаций через AnimationFactory делают интерфейс живым
               и понятным без текстовых подсказок.

**Изменённые файлы:**
- `src/main/java/com/smarthome/view/component/AnimationFactory.java` — создан (Factory Method)
- `src/main/java/com/smarthome/view/component/Room3DView.java` — FadeTransition, TranslateTransition, hover, toggle, highlight
- `src/main/java/com/smarthome/view/component/Room3DModel.java` — deviceModelById map, getDeviceModelById()
- `src/main/java/com/smarthome/controller/MainController.java` — playToggleAnimation() при onToggleDevice()

**GoF паттерн:**
  Factory Method — AnimationFactory создаёт Animation-объекты; вызывающий код
  не знает деталей (ScaleTransition, FadeTransition, TranslateTransition, Timeline).


---

## Очередь

- [x] ШАГ 1 — OBJ-загрузчик
- [x] ШАГ 2 — FPS движение
- [x] ШАГ 3 — Смена темы
- [x] ШАГ 4 — Мультиоконный интерфейс
- [x] ШАГ 5 — Текстуры комнат
- [x] ШАГ 6 — Анимации
- [x] ШАГ 7 — Финальный отчёт + статический анализ
