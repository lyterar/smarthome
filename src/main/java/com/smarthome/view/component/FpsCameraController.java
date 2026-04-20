package com.smarthome.view.component;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.transform.Rotate;

import java.util.HashSet;
import java.util.Set;

/**
 * FPS-камера: игрок стоит внутри комнаты и вращает головой.
 *
 * Иерархия объектов:
 *   playerGroup (translateX, translateZ — позиция в мире)
 *     └─ yawGroup   (Rotate Y_AXIS — поворот головы влево/вправо)
 *          └─ pitchGroup (Rotate X_AXIS — наклон вверх/вниз)
 *               └─ camera (translateZ = 0, translateY = 0)
 */
public class FpsCameraController {

    private static final double SPEED = 5.0;
    // Уровень глаз относительно пола (Y = 0)
    private static final double EYE_LEVEL_Y = -50.0;

    // Группа позиции игрока в мире
    final Group playerGroup = new Group();

    // Вращение вокруг оси Y — «рыскание» (поворот влево/вправо)
    private final Rotate yawRotate = new Rotate(0, Rotate.Y_AXIS);
    private final Group yawGroup = new Group();

    // Вращение вокруг оси X — «тангаж» (наклон вверх/вниз)
    private final Rotate pitchRotate = new Rotate(0, Rotate.X_AXIS);
    private final Group pitchGroup = new Group();

    // Текущие углы в градусах
    private double yaw = 0;
    private double pitch = 0;

    // Нажатые клавиши
    private final Set<String> pressedKeys = new HashSet<>();

    // AnimationTimer для плавного движения
    private AnimationTimer movementTimer;

    // Callback для выхода из FPS-режима (вызывается по ESC)
    private Runnable onExit;

    // Координаты мыши при последнем событии
    private double lastMouseX;
    private double lastMouseY;

    // Обработчики, которые надо снять при detach
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> mousePressHandler;
    private javafx.event.EventHandler<javafx.scene.input.MouseEvent> mouseDragHandler;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> keyPressHandler;
    private javafx.event.EventHandler<javafx.scene.input.KeyEvent> keyReleaseHandler;

    public FpsCameraController(PerspectiveCamera camera, Runnable onExit) {
        this.onExit = onExit;

        // Собираем иерархию групп
        yawGroup.getTransforms().add(yawRotate);
        pitchGroup.getTransforms().add(pitchRotate);

        pitchGroup.getChildren().add(camera);
        yawGroup.getChildren().add(pitchGroup);
        playerGroup.getChildren().add(yawGroup);

        // Уровень глаз
        playerGroup.setTranslateY(EYE_LEVEL_Y);
    }

    /**
     * Устанавливает позицию спауна игрока в мировых координатах.
     * Y головы фиксирован на уровне глаз.
     */
    public void spawnAt(double x, double y, double z) {
        playerGroup.setTranslateX(x);
        playerGroup.setTranslateY(EYE_LEVEL_Y);
        playerGroup.setTranslateZ(z);
    }

    /**
     * Подключает все обработчики к SubScene и запускает таймер движения.
     */
    public void attach(SubScene scene) {
        pressedKeys.clear();

        // --- Обработчик нажатия мыши (запоминаем позицию) ---
        mousePressHandler = e -> {
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
        };

        // --- Обработчик перемещения мыши (вращение головы) ---
        mouseDragHandler = e -> {
            double dx = e.getSceneX() - lastMouseX;
            double dy = e.getSceneY() - lastMouseY;

            // Поворот головы влево/вправо
            yaw += dx * 0.3;
            yawRotate.setAngle(yaw);

            // Наклон вверх/вниз с ограничением
            pitch = clamp(pitch - dy * 0.3, -80, 80);
            pitchRotate.setAngle(pitch);

            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
        };

        // --- Обработчик нажатия клавиш ---
        keyPressHandler = e -> {
            String code = e.getCode().getName();
            pressedKeys.add(code);

            // ESC — выход из FPS режима
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE && onExit != null) {
                onExit.run();
            }
        };

        // --- Обработчик отпускания клавиш ---
        keyReleaseHandler = e -> pressedKeys.remove(e.getCode().getName());

        scene.setOnMousePressed(mousePressHandler);
        scene.setOnMouseDragged(mouseDragHandler);

        // Клавиши слушаем на уровне сцены (не SubScene)
        if (scene.getScene() != null) {
            scene.getScene().setOnKeyPressed(keyPressHandler);
            scene.getScene().setOnKeyReleased(keyReleaseHandler);
        } else {
            // Если сцена ещё не добавлена в Scene — подписываемся при появлении
            scene.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.setOnKeyPressed(keyPressHandler);
                    newScene.setOnKeyReleased(keyReleaseHandler);
                }
            });
        }

        // --- AnimationTimer для плавного движения при зажатых клавишах ---
        movementTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                processMovement();
            }
        };
        movementTimer.start();
    }

    /**
     * Отключает все обработчики и останавливает таймер движения.
     */
    public void detach(SubScene scene) {
        // Останавливаем таймер
        if (movementTimer != null) {
            movementTimer.stop();
            movementTimer = null;
        }

        // Снимаем обработчики с SubScene
        scene.setOnMousePressed(null);
        scene.setOnMouseDragged(null);

        // Снимаем обработчики клавиш с JavaFX Scene
        if (scene.getScene() != null) {
            scene.getScene().setOnKeyPressed(null);
            scene.getScene().setOnKeyReleased(null);
        }

        pressedKeys.clear();
    }

    /**
     * Обрабатывает нажатые клавиши и перемещает игрока.
     * W/S — вперёд/назад, A/D — стрейф.
     */
    private void processMovement() {
        // Угол yaw в радианах для вычисления направления
        double yawRad = Math.toRadians(yaw);

        double moveX = 0;
        double moveZ = 0;

        if (pressedKeys.contains("W")) {
            // Вперёд: direction = (sin(yaw), cos(yaw)) в плоскости XZ
            moveX += Math.sin(yawRad) * SPEED;
            moveZ += Math.cos(yawRad) * SPEED;
        }
        if (pressedKeys.contains("S")) {
            // Назад
            moveX -= Math.sin(yawRad) * SPEED;
            moveZ -= Math.cos(yawRad) * SPEED;
        }
        if (pressedKeys.contains("A")) {
            // Стрейф влево: перпендикуляр к направлению = (cos(yaw), -sin(yaw))
            moveX -= Math.cos(yawRad) * SPEED;
            moveZ += Math.sin(yawRad) * SPEED;
        }
        if (pressedKeys.contains("D")) {
            // Стрейф вправо
            moveX += Math.cos(yawRad) * SPEED;
            moveZ -= Math.sin(yawRad) * SPEED;
        }

        playerGroup.setTranslateX(playerGroup.getTranslateX() + moveX);
        playerGroup.setTranslateZ(playerGroup.getTranslateZ() + moveZ);
    }

    /** Зажимает значение между min и max */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Возвращает группу игрока для добавления в сцену */
    public Group getPlayerGroup() {
        return playerGroup;
    }
}
