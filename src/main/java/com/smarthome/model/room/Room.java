package com.smarthome.model.room;

import com.smarthome.model.device.Device;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Комната дома. Содержит список устройств и геометрию для отрисовки.
 * Реализует Iterable для паттерна Iterator.
 */
public class Room implements Iterable<Device> {

    private String id;
    private String name;
    private RoomType type;
    private List<Device> devices = new ArrayList<>();

    // Позиция и размер на плане дома (в пикселях)
    private double x;
    private double y;
    private double width = 200;
    private double height = 150;

    public Room(String name, RoomType type) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.type = type;
    }

    // --- Управление устройствами ---

    public void addDevice(Device device) {
        devices.add(device);
    }

    public void removeDevice(Device device) {
        devices.remove(device);
    }

    public Device findDeviceById(String deviceId) {
        return devices.stream()
                .filter(d -> d.getId().equals(deviceId))
                .findFirst()
                .orElse(null);
    }

    public List<Device> getDevices() {
        return new ArrayList<>(devices); // Защитная копия
    }

    public int getDeviceCount() {
        return devices.size();
    }

    // --- Iterator pattern ---

    @Override
    public Iterator<Device> iterator() {
        return devices.iterator();
    }

    // --- Getters / Setters ---

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    @Override
    public String toString() {
        return name + " (" + type.getDisplayName() + ") — " + devices.size() + " устройств";
    }
}
