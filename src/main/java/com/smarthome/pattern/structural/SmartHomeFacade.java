package com.smarthome.pattern.structural;

import com.smarthome.event.DeviceEvent;
import com.smarthome.event.EventBus;
import com.smarthome.model.device.Device;
import com.smarthome.model.device.DeviceType;
import com.smarthome.model.house.House;
import com.smarthome.model.room.Room;
import com.smarthome.model.room.RoomType;
import com.smarthome.pattern.creational.DeviceFactory;
import com.smarthome.pattern.creational.RoomBuilder;
import com.smarthome.pattern.creational.SmartHomeEngine;
import com.smarthome.service.DeviceEnhancementService;

import java.util.List;

/**
 * ПАТТЕРН: Facade
 *
 * Упрощённый API для всей системы умного дома.
 * UI вызывает только методы фасада, не работая напрямую
 * с фабриками, билдерами, событиями и т.д.
 */
public class SmartHomeFacade {

    private final SmartHomeEngine engine;
    // Подсистема: логика Decorator/Composite/Proxy вынесена сюда
    private final DeviceEnhancementService enhancementService;

    public SmartHomeFacade() {
        this.engine = SmartHomeEngine.getInstance();
        this.enhancementService = new DeviceEnhancementService(
                engine.getHouse(), engine.getDeviceFactory());
    }

    // === Комнаты ===

    public Room createRoom(String name, RoomType type, double x, double y) {
        Room room = new RoomBuilder(name)
                .type(type)
                .position(x, y)
                .build();
        engine.getHouse().addRoom(room);
        engine.getEventBus().publish(new DeviceEvent("room_added", room.getId()));
        return room;
    }

    public void removeRoom(String roomId) {
        Room room = engine.getHouse().findRoomById(roomId);
        if (room != null) {
            engine.getHouse().removeRoom(room);
            engine.getEventBus().publish(new DeviceEvent("room_removed", roomId));
        }
    }

    public List<Room> getRooms() {
        return engine.getHouse().getRooms();
    }

    // === Устройства ===

    public Device addDeviceToRoom(String roomId, String deviceName, DeviceType type) {
        Room room = engine.getHouse().findRoomById(roomId);
        if (room == null) return null;

        DeviceFactory factory = engine.getDeviceFactory();
        Device device = factory.createDevice(deviceName, type);
        room.addDevice(device);

        engine.getEventBus().publish(new DeviceEvent("device_added", device.getId()));
        return device;
    }

    public void removeDeviceFromRoom(String roomId, String deviceId) {
        Room room = engine.getHouse().findRoomById(roomId);
        if (room == null) return;

        Device device = room.findDeviceById(deviceId);
        if (device != null) {
            room.removeDevice(device);
            engine.getEventBus().publish(new DeviceEvent("device_removed", deviceId));
        }
    }

    public void toggleDevice(String deviceId) {
        Device device = engine.getHouse().findDeviceById(deviceId);
        if (device == null) return;

        if (device.isOn()) {
            device.turnOff();
        } else {
            device.turnOn();
        }
        engine.getEventBus().publish(
                new DeviceEvent("device_toggled", deviceId));
    }

    public void setDeviceParameter(String deviceId, String key, Object value) {
        Device device = engine.getHouse().findDeviceById(deviceId);
        if (device != null) {
            device.setParameter(key, value);
            engine.getEventBus().publish(
                    new DeviceEvent("device_param_changed", deviceId));
        }
    }

    public List<Device> getAllDevices() {
        return engine.getHouse().getAllDevices();
    }

    // === Общее ===

    public House getHouse() {
        return engine.getHouse();
    }

    public EventBus getEventBus() {
        return engine.getEventBus();
    }

    public String getHouseSummary() {
        House house = engine.getHouse();
        int rooms = house.getRooms().size();
        int devices = house.getAllDevices().size();
        long activeDevices = house.getAllDevices().stream()
                .filter(Device::isOn).count();
        return String.format("%s: %d комнат, %d устройств (%d активных)",
                house.getName(), rooms, devices, activeDevices);
    }

    // === Decorator ===

    /** Делегирует в DeviceEnhancementService — логика там */
    public void enableLogging(String deviceId) {
        enhancementService.enableLogging(deviceId);
        engine.getEventBus().publish(new DeviceEvent("device_logging_enabled", deviceId));
    }

    // === Composite ===

    /** Делегирует в DeviceEnhancementService — логика там */
    public Device createDeviceGroup(String roomId, String groupName, DeviceType type, List<String> deviceIds) {
        Device group = enhancementService.createDeviceGroup(roomId, groupName, type, deviceIds);
        if (group != null) {
            engine.getEventBus().publish(new DeviceEvent("device_added", group.getId()));
        }
        return group;
    }

    // === Proxy ===

    /** Делегирует в DeviceEnhancementService — логика там */
    public Device addDeviceLazy(String roomId, String deviceName, DeviceType type) {
        Device device = enhancementService.addDeviceLazy(roomId, deviceName, type);
        if (device != null) {
            engine.getEventBus().publish(new DeviceEvent("device_added", device.getId()));
        }
        return device;
    }
}
