# Smart Home 3D UI — Прогресс разработки

## Текущий шаг: 3 (Смена темы — Strategy)

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

**Примечание по FXyz3D:**
  FXyz3D 0.6.0 недоступен (Bintray закрыт в 2021). Нативный парсер OBJ
  реализован на JavaFX TriangleMesh — полностью эквивалентная замена.

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

**Детали реализации:**
- Инерция: lerp текущей скорости к целевой (коэф. 0.15 + адаптация к dt)
- Коллизии: setRoomBounds() передаёт AABB комнаты → clamp позиции при каждом кадре
- Бег (Shift): множитель 2.2× скорости
- Боббинг: Math.sin(walkTime * 8) * 2.5 по оси Y playerGroup
- Прицел: Canvas поверх SubScene, mouseTransparent=true, крест с окантовкой
- Захват мыши: setCursor(Cursor.NONE) при attach, DEFAULT при detach
- setOnMouseMoved: поворот камеры без зажатой кнопки (как в настоящем FPS)
- AnimationTimer с dt в секундах (защита от лагспайков > 0.05с)

**GoF паттерн:**
  Strategy — FpsCameraController и CameraController взаимозаменяемы через attach/detach

---

## Очередь

- [ ] ШАГ 3 — Смена темы (dark/light/blue) — паттерн Strategy
- [ ] ШАГ 4 — Мультиоконный интерфейс
- [ ] ШАГ 5 — Текстуры комнат
- [ ] ШАГ 6 — Анимации
- [ ] ШАГ 7 — Финальный отчёт + mvn compile
