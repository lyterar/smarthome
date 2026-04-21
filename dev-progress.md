# Smart Home 3D UI — Прогресс разработки

## Текущий шаг: 4 (Мультиоконный интерфейс)

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
- **Исправлено:** backslash-\! синтаксические ошибки в FpsCameraController, Device3DModel, ObjModelLoader, Room3DView

**GoF паттерн:**
  Strategy — ThemeStrategy (интерфейс) реализован тремя конкретными стратегиями (DarkTheme,
  LightTheme, BlueTheme). ThemeService — контекст, делегирующий применение CSS нужной стратегии.
  Singleton — единственный экземпляр ThemeService.getInstance() на приложение.

---

## Очередь

- [ ] ШАГ 4 — Мультиоконный интерфейс (AutomationWindow, DeviceDetailWindow, LogWindow)
- [ ] ШАГ 5 — Текстуры комнат
- [ ] ШАГ 6 — Анимации
- [ ] ШАГ 7 — Финальный отчёт + mvn compile
