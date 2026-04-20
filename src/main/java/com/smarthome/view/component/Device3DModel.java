package com.smarthome.view.component;

import com.smarthome.model.device.DeviceType;

import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;

/**
 * Фабрика 3D моделей устройств.
 * Создаёт 3D объект по DeviceType и состоянию устройства.
 */
public class Device3DModel {

    public static Group createModel(DeviceType type, boolean isOn) {
        return switch (type) {
            case LIGHT     -> createLamp(isOn);
            case THERMOSTAT -> createThermostat(isOn);
            case SENSOR    -> createSensor(isOn);
            case LOCK      -> createLock(isOn);
            case CAMERA    -> createCamera();
            case SPEAKER   -> createSpeaker();
        };
    }

    /** Лампа: цилиндр (ножка) + сфера (плафон), жёлтая если ON */
    private static Group createLamp(boolean isOn) {
        Group g = new Group();

        Cylinder stand = new Cylinder(1.5, 14);
        stand.setMaterial(new PhongMaterial(Color.GRAY));
        stand.setTranslateY(-7);

        Sphere bulb = new Sphere(7);
        bulb.setMaterial(new PhongMaterial(isOn ? Color.YELLOW : Color.DARKGRAY));
        bulb.setTranslateY(-19);

        g.getChildren().addAll(stand, bulb);

        // Включённая лампа светится
        if (isOn) {
            PointLight glow = new PointLight(Color.LIGHTYELLOW);
            glow.setTranslateY(-19);
            g.getChildren().add(glow);
        }
        return g;
    }

    /** Термостат: куб, синий=выкл, красный=вкл */
    private static Group createThermostat(boolean isOn) {
        Group g = new Group();
        Box body = new Box(13, 17, 7);
        body.setMaterial(new PhongMaterial(isOn ? Color.TOMATO : Color.CORNFLOWERBLUE));
        body.setTranslateY(-9);
        g.getChildren().add(body);
        return g;
    }

    /** Датчик: маленькая сфера (зелёная=активен) */
    private static Group createSensor(boolean isOn) {
        Group g = new Group();
        Sphere s = new Sphere(6);
        s.setMaterial(new PhongMaterial(isOn ? Color.LIMEGREEN : Color.DIMGRAY));
        s.setTranslateY(-6);
        g.getChildren().add(s);
        return g;
    }

    /** Замок: прямоугольный корпус + дужка (красный=заблокирован, зелёный=разблокирован) */
    private static Group createLock(boolean isOn) {
        Group g = new Group();

        Box body = new Box(11, 13, 6);
        // ON = разблокирован (зелёный), OFF = заблокирован (красный)
        body.setMaterial(new PhongMaterial(isOn ? Color.LIMEGREEN : Color.TOMATO));
        body.setTranslateY(-9);

        Cylinder shackle = new Cylinder(2.5, 7);
        shackle.setMaterial(new PhongMaterial(Color.SILVER));
        shackle.setTranslateY(-18);

        g.getChildren().addAll(body, shackle);
        return g;
    }

    /** Камера: цилиндр-основание + горизонтальный цилиндр-объектив */
    private static Group createCamera() {
        Group g = new Group();

        Cylinder base = new Cylinder(4, 7);
        base.setMaterial(new PhongMaterial(Color.DARKGRAY));
        base.setTranslateY(-4);

        Cylinder lens = new Cylinder(5, 9);
        lens.setMaterial(new PhongMaterial(Color.BLACK));
        lens.setTranslateY(-12);
        lens.setRotate(90);

        g.getChildren().addAll(base, lens);
        return g;
    }

    /** Колонка: цилиндр-корпус + сфера-мембрана */
    private static Group createSpeaker() {
        Group g = new Group();

        Cylinder body = new Cylinder(7, 18);
        body.setMaterial(new PhongMaterial(Color.SLATEGRAY));
        body.setTranslateY(-9);

        Sphere membrane = new Sphere(4);
        membrane.setMaterial(new PhongMaterial(Color.DARKSLATEGRAY));
        membrane.setTranslateY(-9);
        membrane.setTranslateZ(-7);

        g.getChildren().addAll(body, membrane);
        return g;
    }
}
