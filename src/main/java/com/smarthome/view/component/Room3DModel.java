package com.smarthome.view.component;

import com.smarthome.model.device.Device;
import com.smarthome.model.room.Room;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * 3D модель одной комнаты: пол, 4 стены, устройства, название.
 * Стены полупрозрачные — видно содержимое комнаты.
 */
public class Room3DModel extends Group {

    private static final double WALL_H = 80;
    private static final double WALL_THICKNESS = 4;

    private final Room room;
    private final Box[] walls = new Box[4];
    private final PhongMaterial wallMaterial;
    private final PhongMaterial wallHighlightMaterial;

    // Список 3D-моделей устройств (для drag в orbit-режиме)
    private final List<Group> deviceModels = new ArrayList<>();

    // Маппинг deviceId → 3D-группа (для toggle-анимации, Шаг 6)
    // Причина: анимация toggle должна найти нужную Group по ID устройства.
    // Следствие: при создании каждой Group она регистрируется в этом маппинге.
    private final Map<String, Group> deviceModelById = new HashMap<>();

    public Room3DModel(Room room) {
        this.room = room;

        Color roomColor = Color.web(room.getType().getColor());
        // Стены — светлее цвета комнаты
        Color wallColor = roomColor.deriveColor(0, 0.7, 1.4, 0.45);
        wallMaterial = new PhongMaterial(wallColor);
        wallMaterial.setSpecularColor(Color.WHITE);

        wallHighlightMaterial = new PhongMaterial(roomColor.brighter());
        wallHighlightMaterial.setSpecularColor(Color.WHITE);

        buildRoom();
        addDevices();
        addLabel();
    }

    private void buildRoom() {
        double floorW = room.getWidth();
        double floorD = room.getHeight();

        // Пол
        // Причина: однотонный пол выглядит пусто и не передаёт тип комнаты.
        // Следствие: процедурная текстура из RoomTextureFactory накладывается
        //            через PhongMaterial.setDiffuseMap() — зависит от RoomType.
        Box floor = new Box(floorW, 2, floorD);
        PhongMaterial floorMaterial = createFloorMaterial();
        floor.setMaterial(floorMaterial);

        // Стены: front (+Z), back (-Z), left (-X), right (+X)
        walls[0] = new Box(floorW, WALL_H, WALL_THICKNESS);
        walls[0].setTranslateZ(floorD / 2);
        walls[0].setTranslateY(-WALL_H / 2);

        walls[1] = new Box(floorW, WALL_H, WALL_THICKNESS);
        walls[1].setTranslateZ(-floorD / 2);
        walls[1].setTranslateY(-WALL_H / 2);

        walls[2] = new Box(WALL_THICKNESS, WALL_H, floorD);
        walls[2].setTranslateX(-floorW / 2);
        walls[2].setTranslateY(-WALL_H / 2);

        walls[3] = new Box(WALL_THICKNESS, WALL_H, floorD);
        walls[3].setTranslateX(floorW / 2);
        walls[3].setTranslateY(-WALL_H / 2);

        for (Box wall : walls) {
            wall.setMaterial(wallMaterial);
        }

        getChildren().add(floor);
        getChildren().addAll(walls);
    }

    private void addDevices() {
        double floorW = room.getWidth();
        double floorD = room.getHeight();
        double startX = -floorW / 2 + 20;
        double startZ = -floorD / 2 + 20;
        double spacing = 38;
        int maxCols = Math.max(1, (int) ((floorW - 30) / spacing));

        int i = 0;
        for (Device device : room.getDevices()) {
            double dx = startX + (i % maxCols) * spacing;
            double dz = startZ + (i / maxCols) * spacing;

            Group deviceModel = Device3DModel.createModel(device.getType(), device.isOn());
            deviceModel.setTranslateX(dx);
            deviceModel.setTranslateZ(dz);

            deviceModels.add(deviceModel);
            // Причина: нужен доступ к Group по deviceId для toggle-анимации.
            // Следствие: регистрируем в маппинге при создании.
            deviceModelById.put(device.getId(), deviceModel);
            getChildren().add(deviceModel);
            i++;
        }
    }

    /** Возвращает список 3D-групп устройств (для drag в orbit-режиме) */
    public List<Group> getDeviceModels() {
        return deviceModels;
    }

    /** Возвращает 3D-группу по ID устройства (для toggle-анимации, Шаг 6) */
    public Group getDeviceModelById(String deviceId) {
        return deviceModelById.get(deviceId);
    }

    private void addLabel() {
        Text label = new Text(room.getName());
        label.setFont(Font.font("Arial", 13));
        label.setFill(Color.WHITE);
        // Горизонтально лежащий текст над комнатой
        label.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        label.setTranslateX(-room.getWidth() / 2 + 4);
        label.setTranslateY(-WALL_H - 8);
        getChildren().add(label);
    }

    /** Переключает подсветку стен (при выборе комнаты) */
    public void setHighlighted(boolean highlighted) {
        PhongMaterial mat = highlighted ? wallHighlightMaterial : wallMaterial;
        for (Box wall : walls) {
            wall.setMaterial(mat);
        }
    }

    /**
     * Создаёт материал пола с процедурной текстурой из RoomTextureFactory.
     *
     * Причина: PhongMaterial.setDiffuseMap() принимает Image — WritableImage подходит.
     * Следствие: каждый RoomType получает визуально отличимую поверхность
     *            (дерево, плитка, ковёр, бетон) без внешних PNG-файлов.
     */
    private PhongMaterial createFloorMaterial() {
        WritableImage texture = RoomTextureFactory.getFloorTexture(room.getType());
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseMap(texture);
        // Лёгкий бликовый эффект — поверхность выглядит отполированной
        mat.setSpecularColor(Color.web("#ffffff40"));
        mat.setSpecularPower(20);
        return mat;
    }

}
