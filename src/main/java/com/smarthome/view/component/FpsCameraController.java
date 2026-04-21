package com.smarthome.view.component;

import javafx.animation.AnimationTimer;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Rotate;

import java.util.HashSet;
import java.util.Set;

/**
 * Улучшенный FPS-контроллер камеры (Шаг 2).
 *
 * Причина: базовый FPS без инерции и коллизий — движение неестественное.
 * Следствие: добавлены инерция, коллизии, бег, боббинг, захват мыши.
 *
 * Иерархия:
 *   playerGroup (translateX/Z — позиция)
 *     └─ yawGroup   (Rotate Y — поворот влево/вправо)
 *          └─ pitchGroup (Rotate X — наклон вверх/вниз)
 *               └─ camera
 */
public class FpsCameraController {

    // Базовая скорость (юнит/кадр при 60fps)
    private static final double SPEED        = 3.5;
    // Множитель бега (Shift)
    private static final double SPRINT_MULT  = 2.2;
    // Коэффициент инерции (lerp): меньше = резче
    private static final double INERTIA      = 0.15;
    // Уровень глаз (Y отрицательный = выше пола)
    private static final double EYE_LEVEL_Y  = -50.0;
    // Амплитуда и частота боббинга
    private static final double BOB_AMP      = 2.5;
    private static final double BOB_FREQ     = 8.0;
    // Порог скорости для включения боббинга
    private static final double BOB_THRESH   = 0.3;
    // Отступ от стены при коллизии
    private static final double WALL_MARGIN  = 14.0;

    // Иерархия групп
    final Group playerGroup = new Group();
    private final Rotate yawRotate   = new Rotate(0, Rotate.Y_AXIS);
    private final Group  yawGroup    = new Group();
    private final Rotate pitchRotate = new Rotate(0, Rotate.X_AXIS);
    private final Group  pitchGroup  = new Group();

    // Состояние камеры
    private double yaw   = 0;
    private double pitch = 0;

    // Инерция: текущий вектор скорости
    private double velX = 0;
    private double velZ = 0;

    // Время боббинга
    private double walkTime = 0;

    // Границы комнаты для коллизий
    private double roomMinX = -Double.MAX_VALUE;
    private double roomMaxX =  Double.MAX_VALUE;
    private double roomMinZ = -Double.MAX_VALUE;
    private double roomMaxZ =  Double.MAX_VALUE;

    // Ввод
    private final Set<String> pressedKeys = new HashSet<>();
    private double lastMouseX = -1;
    private double lastMouseY = -1;

    // Обработчики
    private javafx.event.EventHandler<MouseEvent> mousePressHandler;
    private javafx.event.EventHandler<MouseEvent> mouseMoveHandler;
    private javafx.event.EventHandler<KeyEvent>   keyPressHandler;
    private javafx.event.EventHandler<KeyEvent>   keyReleaseHandler;

    private AnimationTimer movementTimer;
    private final Runnable onExit;

    public FpsCameraController(PerspectiveCamera camera, Runnable onExit) {
        this.onExit = onExit;

        // Причина: иерархия групп = независимые оси вращения (gimbal lock минимизирован)
        yawGroup.getTransforms().add(yawRotate);
        pitchGroup.getTransforms().add(pitchRotate);
        pitchGroup.getChildren().add(camera);
        yawGroup.getChildren().add(pitchGroup);
        playerGroup.getChildren().add(yawGroup);
        playerGroup.setTranslateY(EYE_LEVEL_Y);
    }

    /**
     * Задаёт границы комнаты для коллизий.
     * Причина: без границ игрок вылетает за стены.
     * Следствие: позиция ограничена внутри комнаты.
     */
    public void setRoomBounds(double centerX, double centerZ, double width, double depth) {
        roomMinX = centerX - width  / 2.0 + WALL_MARGIN;
        roomMaxX = centerX + width  / 2.0 - WALL_MARGIN;
        roomMinZ = centerZ - depth  / 2.0 + WALL_MARGIN;
        roomMaxZ = centerZ + depth  / 2.0 - WALL_MARGIN;
    }

    /** Устанавливает начальную позицию игрока (Y = уровень глаз). */
    public void spawnAt(double x, double y, double z) {
        playerGroup.setTranslateX(x);
        playerGroup.setTranslateY(EYE_LEVEL_Y);
        playerGroup.setTranslateZ(z);
    }

    /**
     * Подключает обработчики к SubScene и запускает таймер движения.
     * Причина: метод attach/detach = паттерн Strategy — контроллер сменяем.
     */
    public void attach(SubScene subScene) {
        pressedKeys.clear();
        lastMouseX = -1;
        lastMouseY = -1;
        velX = 0;
        velZ = 0;

        // Захват мыши: скрываем курсор в FPS-режиме
        // Причина: курсор отвлекает от прицела
        // Следствие: взгляд управляется движением мыши без визуального курсора
        if (subScene.getScene() != null) {
            subScene.getScene().setCursor(Cursor.NONE);
        }

        mousePressHandler = e -> {
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
        };

        // Причина: setOnMouseMoved работает без зажатой кнопки — удобнее
        // Следствие: взгляд поворачивается свободным движением мыши
        mouseMoveHandler = e -> {
            if (lastMouseX < 0) {
                lastMouseX = e.getSceneX();
                lastMouseY = e.getSceneY();
                return;
            }
            double dx = e.getSceneX() - lastMouseX;
            double dy = e.getSceneY() - lastMouseY;
            yaw += dx * 0.25;
            yawRotate.setAngle(yaw);
            pitch = clamp(pitch - dy * 0.25, -80, 80);
            pitchRotate.setAngle(pitch);
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
        };

        keyPressHandler = e -> {
            pressedKeys.add(e.getCode().getName());
            // ESC: выход в Orbit-режим
            if (e.getCode() == KeyCode.ESCAPE && onExit != null) {
                onExit.run();
            }
        };

        keyReleaseHandler = e -> pressedKeys.remove(e.getCode().getName());

        subScene.setOnMousePressed(mousePressHandler);
        subScene.setOnMouseMoved(mouseMoveHandler);
        subScene.setOnMouseDragged(mouseMoveHandler);

        // Клавиши слушаем на уровне Scene (SubScene не фокусируется)
        if (subScene.getScene() != null) {
            subScene.getScene().setOnKeyPressed(keyPressHandler);
            subScene.getScene().setOnKeyReleased(keyReleaseHandler);
        } else {
            subScene.sceneProperty().addListener((obs, old, newScene) -> {
                if (newScene != null) {
                    newScene.setOnKeyPressed(keyPressHandler);
                    newScene.setOnKeyReleased(keyReleaseHandler);
                    newScene.setCursor(Cursor.NONE);
                }
            });
        }

        // Причина: AnimationTimer синхронизирован с JavaFX render-loop
        // Следствие: движение плавное, нет рывков (не зависит от Thread.sleep)
        movementTimer = new AnimationTimer() {
            private long lastNano = 0;
            @Override
            public void handle(long now) {
                double dt = lastNano == 0 ? 0.016 : Math.min((now - lastNano) / 1_000_000_000.0, 0.05);
                lastNano = now;
                processMovement(dt);
            }
        };
        movementTimer.start();
    }

    /** Отключает обработчики и останавливает таймер. */
    public void detach(SubScene subScene) {
        if (movementTimer != null) {
            movementTimer.stop();
            movementTimer = null;
        }

        subScene.setOnMousePressed(null);
        subScene.setOnMouseMoved(null);
        subScene.setOnMouseDragged(null);

        // Возвращаем курсор и снимаем клавишные обработчики
        if (subScene.getScene() != null) {
            subScene.getScene().setCursor(Cursor.DEFAULT);
            subScene.getScene().setOnKeyPressed(null);
            subScene.getScene().setOnKeyReleased(null);
        }

        pressedKeys.clear();
        velX = 0;
        velZ = 0;
    }

    /** Возвращает корневую группу игрока (добавляется в 3D-сцену). */
    public Group getPlayerGroup() {
        return playerGroup;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Приватная логика движения
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Петля движения: инерция → позиция → коллизии → боббинг.
     * Вызывается каждый кадр из AnimationTimer.
     */
    private void processMovement(double dt) {
        double yawRad = Math.toRadians(yaw);

        // Бег: Shift удваивает скорость
        // Причина: одна скорость монотонна — нет тактического выбора
        // Следствие: два режима скорости дают ощущение темпа
        boolean sprinting = pressedKeys.contains("Shift");
        double  speed     = SPEED * (sprinting ? SPRINT_MULT : 1.0);

        // Целевой вектор скорости по WASD
        double targetVX = 0, targetVZ = 0;
        if (pressedKeys.contains("W")) {
            targetVX += Math.sin(yawRad) * speed;
            targetVZ += Math.cos(yawRad) * speed;
        }
        if (pressedKeys.contains("S")) {
            targetVX -= Math.sin(yawRad) * speed;
            targetVZ -= Math.cos(yawRad) * speed;
        }
        if (pressedKeys.contains("A")) {
            targetVX -= Math.cos(yawRad) * speed;
            targetVZ += Math.sin(yawRad) * speed;
        }
        if (pressedKeys.contains("D")) {
            targetVX += Math.cos(yawRad) * speed;
            targetVZ -= Math.sin(yawRad) * speed;
        }

        // Инерция: lerp текущей скорости к целевой
        // Причина: мгновенный старт/стоп нереалистичен
        // Следствие: скорость плавно нарастает и затухает, как у живого персонажа
        double t = clamp(INERTIA + dt * 2.5, 0.01, 1.0);
        velX += (targetVX - velX) * t;
        velZ += (targetVZ - velZ) * t;

        // Позиция (нормировано к 60fps для стабильности при разных частотах)
        double newX = playerGroup.getTranslateX() + velX * dt * 60;
        double newZ = playerGroup.getTranslateZ() + velZ * dt * 60;

        // Коллизии: ограничиваем позицию в границах комнаты
        // Причина: без стоп-условия игрок видит закулисье сцены
        // Следствие: невидимый барьер у каждой стены останавливает игрока
        newX = clamp(newX, roomMinX, roomMaxX);
        newZ = clamp(newZ, roomMinZ, roomMaxZ);

        playerGroup.setTranslateX(newX);
        playerGroup.setTranslateZ(newZ);

        // Боббинг камеры при ходьбе
        // Причина: ровная камера — «мёртвый» взгляд, нет тактильного ощущения
        // Следствие: синус добавляет «вес» шагам, пространство оживает
        boolean moving = Math.abs(velX) > BOB_THRESH || Math.abs(velZ) > BOB_THRESH;
        if (moving) {
            walkTime += dt * BOB_FREQ * (sprinting ? 1.4 : 1.0);
            double bob = Math.sin(walkTime) * BOB_AMP;
            playerGroup.setTranslateY(EYE_LEVEL_Y + bob);
        } else {
            // Плавный возврат к базовому уровню глаз
            double curY = playerGroup.getTranslateY();
            playerGroup.setTranslateY(curY + (EYE_LEVEL_Y - curY) * 0.12);
        }
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
