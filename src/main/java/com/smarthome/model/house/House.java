package com.smarthome.model.house;

import com.smarthome.model.device.Device;
import com.smarthome.model.room.Room;

import java.util.ArrayList;
import java.util.List;

/**
 * Дом — корневой контейнер для комнат.
 */
public class House {

    private String name;
    private List<Room> rooms = new ArrayList<>();

    public House(String name) {
        this.name = name;
    }

    public void addRoom(Room room) {
        rooms.add(room);
    }

    public void removeRoom(Room room) {
        rooms.remove(room);
    }

    public Room findRoomById(String roomId) {
        return rooms.stream()
                .filter(r -> r.getId().equals(roomId))
                .findFirst()
                .orElse(null);
    }

    /** Найти устройство по ID среди всех комнат */
    public Device findDeviceById(String deviceId) {
        for (Room room : rooms) {
            Device device = room.findDeviceById(deviceId);
            if (device != null) return device;
        }
        return null;
    }

    /** Все устройства во всём доме */
    public List<Device> getAllDevices() {
        List<Device> all = new ArrayList<>();
        for (Room room : rooms) {
            all.addAll(room.getDevices());
        }
        return all;
    }

    public List<Room> getRooms() {
        return new ArrayList<>(rooms);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
