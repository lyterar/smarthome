package com.smarthome.pattern.creational;

import com.smarthome.model.device.Device;
import com.smarthome.model.room.Room;
import com.smarthome.model.room.RoomType;

/**
 * ПАТТЕРН: Builder
 *
 * Пошаговое конструирование комнаты с устройствами и геометрией.
 */
public class RoomBuilder {

    private String name;
    private RoomType type = RoomType.LIVING_ROOM;
    private double x = 0;
    private double y = 0;
    private double width = 200;
    private double height = 150;
    private java.util.List<Device> devices = new java.util.ArrayList<>();

    public RoomBuilder(String name) {
        this.name = name;
    }

    public RoomBuilder type(RoomType type) {
        this.type = type;
        return this;
    }

    public RoomBuilder position(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public RoomBuilder size(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public RoomBuilder addDevice(Device device) {
        this.devices.add(device);
        return this;
    }

    public Room build() {
        Room room = new Room(name, type);
        room.setX(x);
        room.setY(y);
        room.setWidth(width);
        room.setHeight(height);
        for (Device device : devices) {
            room.addDevice(device);
        }
        return room;
    }
}
