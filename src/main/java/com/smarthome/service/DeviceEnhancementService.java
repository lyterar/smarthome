package com.smarthome.service;

import com.smarthome.model.device.Device;
import com.smarthome.model.device.DeviceConfig;
import com.smarthome.model.device.DeviceType;
import com.smarthome.model.house.House;
import com.smarthome.model.room.Room;
import com.smarthome.pattern.creational.DeviceFactory;
import com.smarthome.pattern.structural.DeviceGroup;
import com.smarthome.pattern.structural.DeviceProxy;
import com.smarthome.pattern.structural.LoggingDeviceDecorator;

import java.util.List;

/**
 * Сервис расширения устройств.
 *
 * Содержит логику создания Decorator, Composite и Proxy.
 * Является частью подсистемы — Facade делегирует сюда,
 * не содержа этой логики внутри себя.
 *
 * По GoF: подсистема не знает о Facade, Facade знает о подсистеме.
 */
public class DeviceEnhancementService {

    private final House house;
    private final DeviceFactory factory;

    public DeviceEnhancementService(House house, DeviceFactory factory) {
        this.house = house;
        this.factory = factory;
    }

    // === Decorator ===

    /**
     * Оборачивает драйвер устройства в LoggingDeviceDecorator.
     * Повторный вызов игнорируется — не дублирует декоратор.
     */
    public void enableLogging(String deviceId) {
        Device device = house.findDeviceById(deviceId);
        if (device == null) return;
        if (device.getDriver() instanceof LoggingDeviceDecorator) return;
        device.setDriver(new LoggingDeviceDecorator(device.getDriver()));
    }

    // === Composite ===

    /**
     * Создаёт группу устройств и добавляет её в комнату как одно устройство.
     * Возвращает null если комната не найдена.
     */
    public Device createDeviceGroup(String roomId, String groupName, DeviceType type, List<String> deviceIds) {
        Room room = house.findRoomById(roomId);
        if (room == null) return null;

        DeviceGroup group = new DeviceGroup(groupName, type);
        for (String id : deviceIds) {
            Device member = house.findDeviceById(id);
            if (member != null) {
                group.addDevice(member.getDriver());
            }
        }

        Device groupDevice = new Device(groupName, type, group);
        room.addDevice(groupDevice);
        return groupDevice;
    }

    // === Proxy ===

    /**
     * Добавляет устройство через Proxy — реальный драйвер создаётся
     * только при первом обращении (ленивая инициализация).
     */
    public Device addDeviceLazy(String roomId, String deviceName, DeviceType type) {
        Room room = house.findRoomById(roomId);
        if (room == null) return null;

        DeviceProxy proxy = new DeviceProxy(new DeviceConfig(deviceName, type), factory);
        Device device = new Device(deviceName, type, proxy);
        room.addDevice(device);
        return device;
    }
}
