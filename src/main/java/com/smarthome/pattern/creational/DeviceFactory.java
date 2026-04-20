package com.smarthome.pattern.creational;

import com.smarthome.model.device.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ПАТТЕРН: Factory Method
 *
 * Создаёт устройства по типу. Для замены mock на реальные —
 * просто перерегистрируйте создатель для нужного типа.
 */
public class DeviceFactory {

    private final Map<DeviceType, Function<DeviceConfig, DeviceDriver>> creators = new HashMap<>();

    public DeviceFactory() {
        // Регистрируем mock-драйверы по умолчанию
        registerMockDrivers();
    }

    private void registerMockDrivers() {
        creators.put(DeviceType.LIGHT, config -> new MockLightDriver());
        creators.put(DeviceType.THERMOSTAT, config -> new MockThermostatDriver());
        creators.put(DeviceType.SENSOR, config -> new MockSensorDriver());
        creators.put(DeviceType.LOCK, config -> new MockLockDriver());
        creators.put(DeviceType.CAMERA, config -> new MockCameraDriver());
        creators.put(DeviceType.SPEAKER, config -> new MockSpeakerDriver());
    }

    /**
     * Зарегистрировать создатель драйвера для типа.
     * Используйте это для замены mock на реальный драйвер:
     *
     *   factory.register(DeviceType.LIGHT, cfg -> new Esp32LightDriver(cfg.getAddress()));
     */
    public void register(DeviceType type, Function<DeviceConfig, DeviceDriver> creator) {
        creators.put(type, creator);
    }

    /**
     * Создать устройство по конфигурации.
     */
    public Device createDevice(DeviceConfig config) {
        Function<DeviceConfig, DeviceDriver> creator = creators.get(config.getType());
        if (creator == null) {
            throw new IllegalArgumentException("Нет создателя для типа: " + config.getType());
        }
        DeviceDriver driver = creator.apply(config);
        return new Device(config.getName(), config.getType(), driver);
    }

    /**
     * Быстрое создание устройства по типу и имени.
     */
    public Device createDevice(String name, DeviceType type) {
        return createDevice(new DeviceConfig(name, type));
    }
}
