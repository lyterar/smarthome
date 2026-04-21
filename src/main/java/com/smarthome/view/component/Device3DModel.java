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
 *
 * ПРИЧИНА: Устройства отображались только геометрическими примитивами →
 *          выглядело как прототип, не как готовый продукт.
 * СЛЕДСТВИЕ: Теперь сначала пробуем загрузить реальную 3D модель из OBJ-файла.
 *            Если OBJ отсутствует — автоматически используется примитив.
 *            Замена OBJ не требует правок кода — достаточно добавить файл
 *            в resources/models/.
 *
 * GoF паттерн: Factory Method.
 *   createModel() — фабричный метод, скрывающий выбор между OBJ и примитивом.
 *   ObjModelLoader.load() — вложенный Factory Method для OBJ.
 *
 * Соглашение по именам OBJ: DeviceType.name().toLowerCase()
 *   LIGHT      → resources/models/light.obj
 *   THERMOSTAT → resources/models/thermostat.obj
 *   SENSOR     → resources/models/sensor.obj
 *   LOCK       → resources/models/lock.obj
 *   CAMERA     → resources/models/camera.obj
 *   SPEAKER    → resources/models/speaker.obj
 */
public class Device3DModel {

    /**
     * Создаёт 3D модель устройства.
     *
     * ПРИЧИНА: нужна единая точка входа для создания любой модели устройства.
     * СЛЕДСТВИЕ: вызывающий код не знает, OBJ это или примитив —
     *            переключение прозрачно при добавлении OBJ-файла.
     *
     * @param type  тип устройства
     * @param isOn  текущее состояние (влияет на цвет)
     * @return      Group с 3D представлением
     */
    public static Group createModel(DeviceType type, boolean isOn) {
        // Имя OBJ совпадает с именем типа в нижнем регистре
        String modelName = type.name().toLowerCase();

        // Пытаемся загрузить OBJ-модель с цветом состояния
        Color stateColor = getStateColor(type, isOn);
        Group objModel = ObjModelLoader.load(modelName, 1.0, stateColor);

        if (objModel != null) {
            // OBJ успешно загружен — добавляем свет для лампы
            if (type == DeviceType.LIGHT && isOn) {
                PointLight glow = new PointLight(Color.LIGHTYELLOW);
                objModel.getChildren().add(glow);
            }
            return objModel;
        }

        // Fallback: геометрические примитивы (если OBJ не найден)
        return switch (type) {
            case LIGHT      -> createLamp(isOn);
            case THERMOSTAT -> createThermostat(isOn);
            case SENSOR     -> createSensor(isOn);
            case LOCK       -> createLock(isOn);
            case CAMERA     -> createCamera();
            case SPEAKER    -> createSpeaker();
        };
    }

    /**
     * Возвращает цвет, соответствующий типу и состоянию устройства.
     *
     * ПРИЧИНА: OBJ-модель без контекста состояния — всегда серая.
     * СЛЕДСТВИЕ: единый метод определяет смысловой цвет (вкл/выкл)
     *            и для OBJ, и для примитивов — нет дублирования логики.
     */
    private static Color getStateColor(DeviceType type, boolean isOn) {
        return switch (type) {
            case LIGHT      -> isOn ? Color.YELLOW : Color.DARKGRAY;
            case THERMOSTAT -> isOn ? Color.TOMATO : Color.CORNFLOWERBLUE;
            case SENSOR     -> isOn ? Color.LIMEGREEN : Color.DIMGRAY;
            case LOCK       -> isOn ? Color.LIMEGREEN : Color.TOMATO;
            case CAMERA     -> Color.DARKGRAY;
            case SPEAKER    -> Color.SLATEGRAY;
        };
    }

    // =========================================================
    //  Примитивы (fallback если OBJ не найден)
    // =========================================================

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

        // Включённая лампа излучает свет
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

    /** Замок: прямоугольный корпус + дужка */
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
