package com.smarthome.view.component;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import javafx.util.Duration;

/**
 * ПАТТЕРН: Factory Method
 *
 * Причина: анимации разбросаны по Room3DView и Device3DModel → сложно поддерживать.
 * Следствие: все анимации создаются через единую фабрику; вызывающий код
 *            получает готовый Animation и вызывает .play() — не знает деталей.
 *
 * Каждый метод — фабричный метод, возвращающий конкретную анимацию.
 */
public class AnimationFactory {

    // Утилитный класс — конструктор закрыт
    private AnimationFactory() {}

    // === 1. Анимация вкл/выкл устройства ===

    /**
     * Причина: мгновенное изменение состояния устройства незаметно для пользователя.
     * Следствие: ScaleTransition (пульс) + смена цвета через FillTransition-аналог
     *            делают момент переключения очевидным и приятным.
     *
     * @param node  3D-группа устройства
     * @param isOn  новое состояние (влияет на цвет подсветки)
     */
    public static Animation toggleDevice(Node node, boolean isOn) {
        // Пульс: сжимается до 0.7 → возвращается к 1.0
        ScaleTransition pulse = new ScaleTransition(Duration.millis(180), node);
        pulse.setFromX(1.0); pulse.setFromY(1.0); pulse.setFromZ(1.0);
        pulse.setToX(0.72);  pulse.setToY(0.72);  pulse.setToZ(0.72);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.setInterpolator(Interpolator.EASE_BOTH);

        // После пульса — подсветка состояния
        Color glowColor = isOn ? Color.LIGHTYELLOW : Color.web("#555555");
        KeyFrame kf1 = new KeyFrame(Duration.millis(0),
                new KeyValue(node.opacityProperty(), node.getOpacity()));
        KeyFrame kf2 = new KeyFrame(Duration.millis(90),
                new KeyValue(node.opacityProperty(), isOn ? 1.0 : 0.6,
                        Interpolator.EASE_BOTH));
        KeyFrame kf3 = new KeyFrame(Duration.millis(360),
                new KeyValue(node.opacityProperty(), isOn ? 1.0 : 0.55,
                        Interpolator.EASE_BOTH));
        Timeline opacityAnim = new Timeline(kf1, kf2, kf3);

        // Параллельная анимация: пульс + прозрачность одновременно
        ParallelTransition parallel = new ParallelTransition(pulse, opacityAnim);
        return parallel;
    }

    // === 2. Появление комнаты ===

    /**
     * Причина: комнаты появляются мгновенно при drawHouse() — резко и скачкообразно.
     * Следствие: FadeTransition от 0 до 1 за 400мс делает появление плавным.
     *
     * @param node  Room3DModel (Group)
     */
    public static FadeTransition roomAppear(Node node) {
        // Причина: узел должен быть невидим до старта анимации
        // Следствие: setOpacity(0) перед play() — плавное появление
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(450), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_IN);
        return fade;
    }

    // === 3. Hover на устройстве ===

    /**
     * Причина: пользователь не понимает, что устройство интерактивно.
     * Следствие: при наведении курсора устройство увеличивается 1.0→1.15,
     *            при уходе — возвращается, давая тактильный feedback.
     *
     * Вызов: attachHoverAnimation(deviceNode) — навешивает обработчики мыши.
     *
     * @param node  3D-группа устройства
     */
    public static void attachHoverAnimation(Node node) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), node);
        scaleUp.setToX(1.15); scaleUp.setToY(1.15); scaleUp.setToZ(1.15);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), node);
        scaleDown.setToX(1.0); scaleDown.setToY(1.0); scaleDown.setToZ(1.0);
        scaleDown.setInterpolator(Interpolator.EASE_OUT);

        node.setOnMouseEntered(e -> {
            scaleDown.stop();
            scaleUp.playFromStart();
        });
        node.setOnMouseExited(e -> {
            scaleUp.stop();
            scaleDown.playFromStart();
        });
    }

    // === 4. Переход orbit ↔ FPS ===

    /**
     * Причина: переход между режимами камеры происходит резко — пользователь теряет ориентацию.
     * Следствие: TranslateTransition плавно перемещает cameraGroup в целевую позицию.
     *
     * @param cameraGroup  Group, содержащий камеру
     * @param targetX      целевая координата X
     * @param targetY      целевая координата Y
     * @param targetZ      целевая координата Z
     * @param onFinished   действие после завершения перехода
     */
    public static TranslateTransition cameraTransition(
            Node cameraGroup, double targetX, double targetY, double targetZ,
            Runnable onFinished) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), cameraGroup);
        tt.setToX(targetX);
        tt.setToY(targetY);
        tt.setToZ(targetZ);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        if (onFinished \!= null) {
            tt.setOnFinished(e -> onFinished.run());
        }
        return tt;
    }

    // === 5. Подсветка выбранной комнаты (бонус) ===

    /**
     * Причина: выбранная комната должна визуально выделяться.
     * Следствие: мигание opacity (1.0 → 0.7 → 1.0) привлекает взгляд.
     *
     * @param node  Room3DModel
     */
    public static Animation roomHighlight(Node node) {
        KeyFrame kf1 = new KeyFrame(Duration.ZERO,
                new KeyValue(node.opacityProperty(), 1.0));
        KeyFrame kf2 = new KeyFrame(Duration.millis(200),
                new KeyValue(node.opacityProperty(), 0.65, Interpolator.EASE_BOTH));
        KeyFrame kf3 = new KeyFrame(Duration.millis(400),
                new KeyValue(node.opacityProperty(), 1.0, Interpolator.EASE_BOTH));
        Timeline blink = new Timeline(kf1, kf2, kf3);
        blink.setCycleCount(2);
        return blink;
    }
}
