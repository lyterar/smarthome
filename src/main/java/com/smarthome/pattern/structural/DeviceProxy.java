package com.smarthome.pattern.structural;

import com.smarthome.model.device.DeviceConfig;
import com.smarthome.model.device.DeviceDriver;
import com.smarthome.model.device.DeviceType;
import com.smarthome.pattern.creational.DeviceFactory;

import java.util.Map;

/**
 * ПАТТЕРН: Proxy
 *
 * Ленивая инициализация драйвера устройства.
 * Реальный драйвер создаётся только при первом обращении.
 * Полезно для реальных устройств — не подключаемся пока не нужно.
 */
public class DeviceProxy implements DeviceDriver {

    private final DeviceConfig config;
    private final DeviceFactory factory;
    private DeviceDriver realDriver;  // null пока не инициализирован

    public DeviceProxy(DeviceConfig config, DeviceFactory factory) {
        this.config = config;
        this.factory = factory;
    }

    /** Ленивая инициализация реального драйвера */
    private DeviceDriver getReal() {
        if (realDriver == null) {
            System.out.println("[Proxy] Инициализация драйвера: " + config.getName());
            // Создаём устройство через фабрику и берём его драйвер
            realDriver = factory.createDevice(config).getDriver();
        }
        return realDriver;
    }

    @Override
    public void turnOn() { getReal().turnOn(); }

    @Override
    public void turnOff() { getReal().turnOff(); }

    @Override
    public boolean isOn() { return getReal().isOn(); }

    @Override
    public Map<String, Object> getParameters() { return getReal().getParameters(); }

    @Override
    public void setParameter(String key, Object value) { getReal().setParameter(key, value); }

    @Override
    public boolean isConnected() {
        // Если ещё не инициализирован — не подключён
        if (realDriver == null) return false;
        return realDriver.isConnected();
    }

    @Override
    public DeviceType getDeviceType() { return config.getType(); }

    /** Принудительно инициализировать драйвер */
    public void initialize() {
        getReal();
    }

    /** Проверить инициализирован ли драйвер */
    public boolean isInitialized() {
        return realDriver != null;
    }
}
