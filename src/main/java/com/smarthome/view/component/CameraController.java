package com.smarthome.view.component;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.util.Duration;

/**
 * Орбитальный контроллер камеры для режима обзора (overview).
 * Поддерживает вращение мышью и плавный фокус на комнате.
 */
public class CameraController {

    private final PerspectiveCamera camera;
    private final Group cameraGroup;

    // Состояние вращения
    private double anchorX;
    private double anchorY;
    private double anchorAngleX = -30;
    private double anchorAngleY = 0;

    // Текущие углы поворота cameraGroup
    private double rotateX = -30;
    private double rotateY = 0;

    public CameraController(PerspectiveCamera camera, Group cameraGroup, SubScene subScene) {
        this.camera = camera;
        this.cameraGroup = cameraGroup;

        applyRotation();
        attachHandlers(subScene);
    }

    /** Применяет текущие углы к группе камеры */
    private void applyRotation() {
        cameraGroup.setRotationAxis(javafx.scene.transform.Rotate.X_AXIS);
        cameraGroup.setRotate(rotateX);
    }

    /** Подключает обработчики мыши для орбитального вращения */
    private void attachHandlers(SubScene subScene) {
        subScene.setOnMousePressed(e -> {
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            anchorAngleX = rotateX;
            anchorAngleY = rotateY;
        });

        subScene.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - anchorX;
            double dy = e.getSceneY() - anchorY;

            // Вращение по вертикали (вокруг X)
            rotateX = clamp(anchorAngleX - dy * 0.3, -80, 0);
            // Вращение по горизонтали (вокруг Y)
            rotateY = anchorAngleY + dx * 0.3;

            cameraGroup.setRotationAxis(javafx.scene.transform.Rotate.X_AXIS);
            cameraGroup.setRotate(rotateX);
            camera.setRotationAxis(javafx.scene.transform.Rotate.Y_AXIS);
            camera.setRotate(rotateY);
        });

        // Зум колесиком мыши
        subScene.setOnScroll(e -> {
            double delta = e.getDeltaY();
            double newZ = camera.getTranslateZ() + delta * 1.5;
            camera.setTranslateZ(clamp(newZ, -2000, -200));
        });
    }

    /**
     * Плавно перемещает камеру для фокуса на заданных координатах.
     *
     * @param x целевая координата X
     * @param z целевая координата Z
     */
    public void focusOn(double x, double z) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(400),
                        new KeyValue(cameraGroup.translateXProperty(), x),
                        new KeyValue(cameraGroup.translateZProperty(), z),
                        new KeyValue(camera.translateZProperty(), -400)
                )
        );
        timeline.play();
    }

    /** Ограничивает значение в диапазоне [min, max] */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
