package com.smarthome.pattern.behavioral;

import com.smarthome.model.device.Device;

/**
 * Инициализация mock-устройства (Template Method).
 */
public class MockDeviceInit extends DeviceInitTemplate {

    @Override
    protected void checkConnection(Device device) {
        System.out.println("  [Mock] Подключение: всегда OK");
    }

    @Override
    protected void loadConfiguration(Device device) {
        System.out.println("  [Mock] Конфигурация: значения по умолчанию");
    }

    @Override
    protected void applyDefaults(Device device) {
        // Mock-устройства начинают выключенными
        device.turnOff();
        System.out.println("  [Mock] Устройство выключено по умолчанию");
    }
}
