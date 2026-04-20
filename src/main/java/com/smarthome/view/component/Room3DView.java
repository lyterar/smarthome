package com.smarthome.view.component;

import com.smarthome.model.house.House;
import com.smarthome.model.room.Room;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Основная 3D сцена приложения.
 *
 * Два режима:
 *  - Orbit (по умолчанию): вид сверху, вращение мышью вокруг центра, zoom колёсиком.
 *    Устройства можно перетаскивать мышью.
 *  - FPS: камера внутри комнаты, WASD + мышь, управляет FpsCameraController.
 */
public class Room3DView extends Pane {

    private static final double SPACING_X = 280;
    private static final double SPACING_Z = 220;
    private static final int MAX_COLS = 3;

    // --- Orbit-режим ---
    private final Group orbitCameraGroup = new Group();
    private final Rotate orbitRotateX = new Rotate(-25, Rotate.X_AXIS);
    private final Rotate orbitRotateY = new Rotate(0, Rotate.Y_AXIS);

    // --- Общая камера ---
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    // --- Корень сцены ---
    private final Group root3D = new Group();
    private SubScene subScene;

    // --- FPS-контроллер ---
    private FpsCameraController fpsController;
    private boolean fpsMode = false;

    // --- Состояние orbit-мыши ---
    private double orbitLastX;
    private double orbitLastY;

    // roomId -> 3D модель
    private final Map<String, Room3DModel> roomModels = new HashMap<>();
    private String highlightedRoomId;

    public Room3DView() {
        buildScene();
        setupOrbitHandlers();
    }

    // =========================================================
    //  Построение сцены
    // =========================================================

    private void buildScene() {
        // Orbit: камера далеко, смотрит на центр
        camera.setTranslateZ(-800);
        camera.setFieldOfView(45);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);

        orbitCameraGroup.getTransforms().addAll(orbitRotateX, orbitRotateY);
        orbitCameraGroup.getChildren().add(camera);

        AmbientLight ambient = new AmbientLight(Color.rgb(80, 80, 110));
        PointLight mainLight = new PointLight(Color.WHITE);
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

    // =========================================================
    //  Orbit-управление (мышь + колёсико)
    // =========================================================

    private void setupOrbitHandlers() {
        subScene.setOnMousePressed(e -> {
            orbitLastX = e.getSceneX();
            orbitLastY = e.getSceneY();
        });

        subScene.setOnMouseDragged(e -> {
            if (fpsMode) return; // в FPS режиме orbit не работает
            double dx = e.getSceneX() - orbitLastX;
            double dy = e.getSceneY() - orbitLastY;

            if (e.isPrimaryButtonDown()) {
                // Вращение вокруг центра сцены
                orbitRotateY.setAngle(orbitRotateY.getAngle() + dx * 0.3);
                double newAngle = orbitRotateX.getAngle() - dy * 0.3;
                orbitRotateX.setAngle(Math.max(-89, Math.min(0, newAngle)));
            } else if (e.isSecondaryButtonDown()) {
                // Панорама
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

    /** Перерисовывает весь дом */
    public void drawHouse(House house) {
        root3D.getChildren().removeAll(roomModels.values());
        roomModels.clear();

        if (house == null) return;

        int count = house.getRooms().size();
        double offsetX = -(Math.min(count, MAX_COLS) - 1) * SPACING_X / 2.0;

        int col = 0;
        for (Room room : house.getRooms()) {
            double posX = offsetX + (col % MAX_COLS) * SPACING_X;
            double posZ = (col / MAX_COLS) * SPACING_Z;

            Room3DModel model = new Room3DModel(room);
            model.setTranslateX(posX);
            model.setTranslateZ(posZ);

            if (room.getId().equals(highlightedRoomId)) {
                model.setHighlighted(true);
            }

            // Перетаскивание устройств мышью (только в orbit-режиме)
            setupDeviceDrag(model);

            roomModels.put(room.getId(), model);
            root3D.getChildren().add(model);
            col++;
        }
    }

    /**
     * Вешает drag-обработчики на каждую 3D-модель устройства в комнате.
     * Перетаскивание двигает устройство по полу (X и Z, Y не меняется).
     */
    private void setupDeviceDrag(Room3DModel roomModel) {
        List<Group> deviceModels = roomModel.getDeviceModels();
        for (Group deviceModel : deviceModels) {
            final double[] dragStart = new double[2]; // [sceneX, sceneZ]

            deviceModel.setOnMousePressed(e -> {
                if (fpsMode) return;
                dragStart[0] = e.getSceneX();
                dragStart[1] = e.getSceneY();
                e.consume(); // не передаём orbit-обработчику
            });

            deviceModel.setOnMouseDragged(e -> {
                if (fpsMode) return;
                // Грубое преобразование экранных координат в смещение в мире
                // Коэффициент зависит от текущего zoom (translateZ камеры)
                double scale = Math.abs(camera.getTranslateZ()) / 800.0;
                double dx = (e.getSceneX() - dragStart[0]) * scale * 0.4;
                double dz = (e.getSceneY() - dragStart[1]) * scale * 0.4;

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
     * Камера перемещается в центр комнаты на уровень глаз.
     */
    public void enterFpsMode(Room room) {
        if (fpsMode) exitFpsMode();

        // Находим модель комнаты и берём её позицию в мире
        Room3DModel model = roomModels.get(room.getId());
        double spawnX = model != null ? model.getTranslateX() : 0;
        double spawnZ = model != null ? model.getTranslateZ() : 0;

        // Убираем камеру из orbit-группы
        orbitCameraGroup.getChildren().remove(camera);

        // Создаём FPS-контроллер, onExit = выход из FPS
        fpsController = new FpsCameraController(camera, this::exitFpsMode);
        fpsController.spawnAt(spawnX, 0, spawnZ);
        fpsController.attach(subScene);

        // Добавляем playerGroup в сцену вместо orbit-группы
        root3D.getChildren().add(fpsController.getPlayerGroup());

        fpsMode = true;
    }

    /**
     * Выходит из FPS-режима, возвращает orbit-камеру.
     */
    public void exitFpsMode() {
        if (!fpsMode || fpsController == null) return;

        // Отключаем FPS-контроллер
        fpsController.detach(subScene);
        root3D.getChildren().remove(fpsController.getPlayerGroup());
        fpsController = null;

        // Возвращаем камеру в orbit-группу
        orbitCameraGroup.getChildren().add(camera);
        camera.setTranslateZ(-800);

        fpsMode = false;
    }

    // =========================================================
    //  Подсветка комнаты
    // =========================================================

    /** Подсвечивает выбранную комнату, снимает подсветку с остальных */
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

    public boolean isFpsMode() {
        return fpsMode;
    }
}
