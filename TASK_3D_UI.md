# Задача: Переделать UI Smart Home на 3D

## Что нужно сделать

Полностью переделать интерфейс приложения Smart Home Constructor:
- Заменить 2D Canvas (RoomCanvas) на 3D сцену JavaFX (SubScene + Group)
- Боковое меню слева со списком комнат (вместо правой панели)
- Комнаты — 3D модели с полом, стенами, освещением (не плоские прямоугольники)
- Устройства внутри комнат — 3D объекты (лампа = цилиндр+сфера, термостат = куб с экраном и т.д.)

## Архитектура изменений

### 1. Удалить / заменить
- `view/component/RoomCanvas.java` → заменить на `view/component/Room3DView.java`
- `resources/fxml/main.fxml` → переделать layout (боковое меню + 3D сцена)
- `resources/css/style.css` → обновить под новый layout

### 2. Новые файлы
- `view/component/Room3DView.java` — основная 3D сцена (SubScene + PerspectiveCamera + AmbientLight/PointLight)
- `view/component/Room3DModel.java` — 3D модель одной комнаты (пол Box, 4 стены Box, потолок опционально)
- `view/component/Device3DModel.java` — фабрика 3D моделей устройств по DeviceType
- `view/component/CameraController.java` — управление камерой (вращение мышью, зум колёсиком)

### 3. Изменить
- `controller/MainController.java` — заменить RoomCanvas на Room3DView, обновить привязки
- `resources/fxml/main.fxml` — новый layout

## Требования к 3D

### Сцена
```java
// Основа — SubScene с 3D группой
Group root3D = new Group();
SubScene subScene = new SubScene(root3D, width, height, true, SceneAntialiasing.BALANCED);
subScene.setFill(Color.web("#1a1a2e")); // Тёмный фон

// Камера
PerspectiveCamera camera = new PerspectiveCamera(true);
camera.setTranslateZ(-800);
camera.setFieldOfView(45);
subScene.setCamera(camera);

// Освещение
AmbientLight ambient = new AmbientLight(Color.rgb(80, 80, 100));
PointLight mainLight = new PointLight(Color.WHITE);
mainLight.setTranslateY(-500);
```

### Комната (Room3DModel)
- Пол: `Box(width, 2, depth)` с PhongMaterial (цвет зависит от RoomType)
- Стены: 4 × `Box` по краям пола, высота ~100, полупрозрачные (opacity 0.3-0.5) чтобы видеть содержимое
- Цвет стен: из `RoomType.getColor()`, но светлее
- При наведении мыши — подсветка стен (эффект hover)
- При клике — выделение комнаты (стены ярче, рамка)
- Название комнаты: `Text` как 3D элемент над комнатой, или 2D Label через StackPane

Примерные размеры комнат:
```java
// Room с width=200, height=150 в 2D → в 3D:
double floorW = room.getWidth();  // ширина
double floorD = room.getHeight(); // глубина (бывший height стал depth)
double wallH = 80;                // высота стен

Box floor = new Box(floorW, 2, floorD);
floor.setMaterial(new PhongMaterial(Color.web(room.getType().getColor())));

// Стены
Box wallFront = new Box(floorW, wallH, 2);
wallFront.setTranslateZ(floorD / 2);
wallFront.setTranslateY(-wallH / 2);
// ... аналогично wallBack, wallLeft, wallRight
```

### Устройства (Device3DModel)
Создавать 3D модель по DeviceType:
```java
public static Node createModel(DeviceType type, boolean isOn) {
    return switch (type) {
        case LIGHT -> createLamp(isOn);       // Цилиндр (ножка) + Сфера (плафон), жёлтая если ON
        case THERMOSTAT -> createThermostat(); // Куб с синей/красной гранью
        case SENSOR -> createSensor();         // Маленькая сфера (зелёная=active)
        case LOCK -> createLock(isOn);         // Цилиндр (красный=locked, зелёный=unlocked)
        case CAMERA -> createCamera();         // Конус + цилиндр
        case SPEAKER -> createSpeaker();       // Цилиндр + конус (рупор)
    };
}
```

Включённые устройства — добавлять PointLight рядом (особенно для LIGHT).

### Камера (CameraController)
- Вращение: зажать ЛКМ + тянуть → вращение вокруг центра сцены
- Зум: колёсико мыши → приближение/отдаление (translateZ)
- Панорама: зажать ПКМ + тянуть → сдвиг камеры (translateX/Y)
- Двойной клик на комнату → камера плавно переходит к этой комнате (TranslateTransition)

```java
// Вращение через Rotate transforms
Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
cameraGroup.getTransforms().addAll(rotateX, rotateY);

scene.setOnMouseDragged(event -> {
    if (event.isPrimaryButtonDown()) {
        rotateY.setAngle(rotateY.getAngle() + (event.getSceneX() - lastX) * 0.3);
        rotateX.setAngle(rotateX.getAngle() - (event.getSceneY() - lastY) * 0.3);
    }
});

scene.setOnScroll(event -> {
    camera.setTranslateZ(camera.getTranslateZ() + event.getDeltaY() * 2);
});
```

## Требования к Layout (FXML)

```
┌──────────────┬─────────────────────────────────────────┐
│              │                                         │
│  БОКОВОЕ     │                                         │
│  МЕНЮ        │           3D СЦЕНА                      │
│              │         (SubScene)                       │
│  ┌────────┐  │                                         │
│  │Комнаты │  │                                         │
│  │ список │  │                                         │
│  │        │  │                                         │
│  └────────┘  │                                         │
│              │                                         │
│  ┌────────┐  │                                         │
│  │Устр-ва │  │                                         │
│  │выбр.   │  │                                         │
│  │комнаты │  │                                         │
│  └────────┘  │                                         │
│              │                                         │
│  [+Комната]  │                                         │
│  [+Устр-во]  │                                         │
│  [Вкл/Выкл]  │                                         │
│              │                                         │
│  Режим:      │                                         │
│  [ComboBox]  │                                         │
│  [Применить] │                                         │
│              │                                         │
│  [↩][↪]      │                                         │
│  [Сохранить] │                                         │
│  [Загрузить] │                                         │
├──────────────┼─────────────────────────────────────────┤
│ Статус: Готово                                         │
└────────────────────────────────────────────────────────┘
```

Боковое меню: ширина 280px, фон `#1e1e2e`, текст белый.
Кнопки: стиль как в текущем CSS но адаптированный под тёмную тему.

## Что НЕ менять
- Вся логика в `pattern/`, `model/`, `service/`, `mcp/`, `event/` — не трогать
- `SmartHomeFacade` — интерфейс фасада не меняется
- `MainController` — менять только привязки к UI, логика остаётся
- `DeviceDriver`, `Device`, `Room`, `House` — модели не трогать

## Порядок работы
1. Сначала создать `Room3DView`, `Room3DModel`, `Device3DModel`, `CameraController`
2. Потом переделать `main.fxml` на новый layout
3. Потом обновить `MainController` — заменить RoomCanvas на Room3DView
4. Обновить `style.css` под тёмную тему
5. Проверить что всё компилируется: `mvn compile`
6. Проверить что запускается: `mvn javafx:run`

## Используй MCP для сверки
Перед изменением паттернов — проверь через MCP:
```
POST http://localhost:3001/mcp
{"method":"tools/call","params":{"name":"verify_implementation","arguments":{"pattern_name":"Facade"}}}
```
Убедись что SmartHomeFacade остаётся единственной точкой входа для UI.
