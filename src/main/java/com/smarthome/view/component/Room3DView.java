package com.smarthome.view.component;

import com.smarthome.model.house.House;
import com.smarthome.model.room.Room;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Основная 3D сцена приложения (обновлена в Шаге 2).
 *
 * Два режима:
 *  - Orbit: вид сверху, вращение мышью, zoom колёсиком.
 *  - FPS: камера внутри комнаты, WASD+мышь, FpsCameraController.
 *
 * Новое в Шаге 2:
 *  - Прицел (Canvas overlay) в FPS-режиме.
 *  - Передача границ комнаты в FPS-контроллер для коллизий.
 */
public class Room3DView extends Pane {

    private static final double SPACING_X = 280;
    private static final double SPACING_Z = 220;
    private static final int    MAX_COLS  = 3;

    // Orbit-камера
    private final Group  orbitCameraGroup = new Group();
    private final Rotate orbitRotateX     = new Rotate(-25, Rotate.X_AXIS);
    private final Rotate orbitRotateY     = new Rotate(0,   Rotate.Y_AXIS);

    // Общая камера (переключается между Orbit и FPS)
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    // Корень 3D-сцены
    private final Group    root3D   = new Group();
    private       SubScene subScene;

    // FPS-контроллер
    private FpsCameraController fpsController;
    private boolean             fpsMode = false;

    // Прицел (Canvas поверх SubScene)
    // Причина: SubScene не позволяет добавлять 2D-элементы напрямую
    // Следствие: Canvas лежит поверх SubScene как отдельный слой Pane
    private Canvas crosshairCanvas;

    // Orbit-состояние мыши
    private double orbitLastX;
    private double orbitLastY;

    // roomId → 3D-модель
    private final Map<String, Room3DModel> roomModels    = new HashMap<>();
    private       String                   highlightedRoomId;

    public Room3DView() {
        buildScene();
        buildCrosshair();
        setupOrbitHandlers();
    }

    // =========================================================
    //  Построение сцены
    // =========================================================

    private void buildScene() {
        camera.setTranslateZ(-800);
        camera.setFieldOfView(45);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);

        orbitCameraGroup.getTransforms().addAll(orbitRotateX, orbitRotateY);
        orbitCameraGroup.getChildren().add(camera);

        AmbientLight ambient   = new AmbientLight(Color.rgb(80, 80, 110));
        PointLight   mainLight = new PointLight(Color.WHITE);
        mainLight.setTranslateY(-500);
        mainLight.setTranslateZ(-300);

        root3D.getChildren().addAll(ambient, mainLight, orbitCameraGroup);

        subScene = new SubScene(root3D, 700, 500, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web("#1a1a2e"));
        subScene.setCamera(camera);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        getChildren().add(subScene);
    }

    /**
     * Создаёт Canvas-прицел поверх SubScene.
     *
     * Причина: в FPS-режиме нужно видеть центр экрана для прицеливания.
     * Следствие: тонкий крест «+» отрисован в центре Canvas и виден поверх 3D.
     */
    private void buildCrosshair() {
        crosshairCanvas = new Canvas();
        // Canvas прозрачен по умолчанию — не перекрывает 3D
        crosshairCanvas.setMouseTransparent(true); // события проходят насквозь

        // Привязываем размер к Pane
        crosshairCanvas.widthProperty().bind(widthProperty());
        crosshairCanvas.heightProperty().bind(heightProperty());

        // Перерисовываем прицел при изменении размера
        crosshairCanvas.widthProperty().addListener(e  -> drawCrosshair());
        crosshairCanvas.heightProperty().addListener(e -> drawCrosshair());

        // Скрыт до входа в FPS-режим
        crosshairCanvas.setVisible(false);

        getChildren().add(crosshairCanvas);
    }

    /** Рисует крест «+» в центре Canvas. */
    private void drawCrosshair() {
        GraphicsContext gc = crosshairCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, crosshairCanvas.getWidth(), crosshairCanvas.getHeight());

        double cx = crosshairCanvas.getWidth()  / 2;
        double cy = crosshairCanvas.getHeight() / 2;
        double arm = 10; // длина луча прицела
        double gap = 3;  // зазор в центре

        // Белые линии с тонкой чёрной окантовкой (для видимости на любом фоне)
        gc.setLineWidth(3);
        gc.setStroke(Color.BLACK);
        drawCross(gc, cx, cy, arm, gap);

        gc.setLineWidth(1.5);
        gc.setStroke(Color.WHITE);
        drawCross(gc, cx, cy, arm, gap);
    }

    private void drawCross(GraphicsContext gc, double cx, double cy, double arm, double gap) {
        // Горизонтальная линия
        gc.strokeLine(cx - arm - gap, cy, cx - gap, cy);
        gc.strokeLine(cx + gap, cy, cx + arm + gap, cy);
        // Вертикальная линия
        gc.strokeLine(cx, cy - arm - gap, cx, cy - gap);
        gc.strokeLine(cx, cy + gap, cx, cy + arm + gap);
    }

    // =========================================================
    //  Orbit-управление (мышь + колёсико)
    // =========================================================

    private void setupOrbitHandlers() {
        subScene.setOnMousePressed(e -> {
            orbitLastX = e.getSceneX();
            orbitLastY = e.getSceneY();
        });

        subScene.setOnMouseDragged(e -> {
            if (fpsMode) return;
            double dx = e.getSceneX() - orbitLastX;
            double dy = e.getSceneY() - orbitLastY;

            if (e.isPrimaryButtonDown()) {
                orbitRotateY.setAngle(orbitRotateY.getAngle() + dx * 0.3);
                double newAngle = orbitRotateX.getAngle() - dy * 0.3;
                orbitRotateX.setAngle(Math.max(-89, Math.min(0, newAngle)));
            } else if (e.isSecondaryButtonDown()) {
                orbitCameraGroup.setTranslateX(orbitCameraGroup.getTranslateX() - dx * 0.5);
                orbitCameraGroup.setTranslateY(orbitCameraGroup.getTranslateY() - dy * 0.5);
            }

            orbitLastX = e.getSceneX();
            orbitLastY = e.getSceneY();
        });

        subScene.setOnScroll(e -> {
            if (fpsMode) return;
            double newZ = camera.getTranslateZ() + e.getDeltaY() * 2;
            camera.setTranslateZ(Math.max(-2000, Math.min(-100, newZ)));
        });
    }

    // =========================================================
    //  Отрисовка дома
    // =========================================================

    /** Перерисовывает весь дом. */
    public void drawHouse(House house) {
        root3D.getChildren().removeAll(roomModels.values());
        roomModels.clear();

        if (house == null) return;

        int count   = house.getRooms().size();
        double offX = -(Math.min(count, MAX_COLS) - 1) * SPACING_X / 2.0;

        int col = 0;
        for (Room room : house.getRooms()) {
            double posX = offX + (col % MAX_COLS) * SPACING_X;
            double posZ = (col / MAX_COLS) * SPACING_Z;

            Room3DModel model = new Room3DModel(room);
            model.setTranslateX(posX);
            model.setTranslateZ(posZ);

            if (room.getId().equals(highlightedRoomId)) {
                model.setHighlighted(true);
            }

            setupDeviceDrag(model);
            roomModels.put(room.getId(), model);
            root3D.getChildren().add(model);
            col++;
        }
    }

    /** Вешает drag-обработчики на 3D-модели устройств (только в Orbit). */
    private void setupDeviceDrag(Room3DModel roomModel) {
        List<Group> deviceModels = roomModel.getDeviceModels();
        for (Group deviceModel : deviceModels) {
            final double[] dragStart = new double[2];

            deviceModel.setOnMousePressed(e -> {
                if (fpsMode) return;
                dragStart[0] = e.getSceneX();
                dragStart[1] = e.getSceneY();
                e.consume();
            });

            deviceModel.setOnMouseDragged(e -> {
                if (fpsMode) return;
                double scale = Math.abs(camera.getTranslateZ()) / 800.0;
                double dx    = (e.getSceneX() - dragStart[0]) * scale * 0.4;
                double dz    = (e.getSceneY() - dragStart[1]) * scale * 0.4;

                deviceModel.setTranslateX(deviceModel.getTranslateX() + dx);
                deviceModel.setTranslateZ(deviceModel.getTranslateZ() + dz);

                dragStart[0] = e.getSceneX();
                dragStart[1] = e.getSceneY();
                e.consume();
            });
        }
    }

    // =========================================================
    //  FPS-режим
    // =========================================================

    /**
     * Входит в FPS-режим внутри выбранной комнаты.
     *
     * Причина: пользователь хочет «зайти» в комнату и осмотреть её изнутри.
     * Следствие: камера перемещается в центр комнаты, активируется FPS-контроллер.
     */
    public void enterFpsMode(Room room) {
        if (fpsMode) exitFpsMode();

        Room3DModel model  = roomModels.get(room.getId());
        double      spawnX = model != null ? model.getTranslateX() : 0;
        double      spawnZ = model != null ? model.getTranslateZ() : 0;

        // Убираем камеру из orbit-группы
        orbitCameraGroup.getChildren().remove(camera);

        // Создаём FPS-контроллер
        fpsController = new FpsCameraController(camera, this::exitFpsMode);
        fpsController.spawnAt(spawnX, 0, spawnZ);

        // Передаём границы комнаты для коллизий
        // Причина: без этого FPS-контроллер не знает размеры комнаты
        // Следствие: игрок ограничен внутри конкретной комнаты
        fpsController.setRoomBounds(
                spawnX, spawnZ,
                room.getWidth(), room.getHeight()
        );

        fpsController.attach(subScene);
        root3D.getChildren().add(fpsController.getPlayerGroup());

        // Показываем прицел
        // Причина: в FPS без прицела сложно ориентироваться
        // Следствие: Canvas-прицел появляется поверх SubScene
        crosshairCanvas.setVisible(true);
        drawCrosshair();

        fpsMode = true;
    }

    /**
     * Выходит из FPS-режима, возвращает Orbit-камеру.
     */
    public void exitFpsMode() {
        if (!fpsMode || fpsController == null) return;

        fpsController.detach(subScene);
        root3D.getChildren().remove(fpsController.getPlayerGroup());
        fpsController = null;

        // Возвращаем камеру в orbit-группу
        orbitCameraGroup.getChildren().add(camera);
        camera.setTranslateZ(-800);

        // Скрываем прицел
        crosshairCanvas.setVisible(false);

        // Восстанавливаем orbit-обработчики (FPS мог перезаписать их на Scene)
        setupOrbitHandlers();

        fpsMode = false;
    }

    // =========================================================
    //  Подсветка комнаты
    // =========================================================

    /** Подсвечивает выбранную комнату, снимает подсветку с остальных. */
    public void highlightRoom(Room room) {
        roomModels.values().forEach(m -> m.setHighlighted(false));

        if (room != null) {
            highlightedRoomId = room.getId();
            Room3DModel m = roomModels.get(room.getId());
            if (m != null) m.setHighlighted(true);
        } else {
            highlightedRoomId = null;
        }
    }


    // =========================================================
    //  Смена фона (вызывается ThemeService)
    // =========================================================

    /**
     * Меняет цвет фона SubScene при смене темы.
     *
     * Причина: SubScene имеет независимый фон, не управляемый CSS.
     * Следствие: при смене темы фон 3D-сцены синхронизируется с новой палитрой.
     *
     * @param color новый цвет фона из ThemeService.getSubSceneColor()
     */
    public void setSubSceneBackground(javafx.scene.paint.Color color) {
        subScene.setFill(color);
    }

    public boolean isFpsMode() {
        return fpsMode;
    }
}
