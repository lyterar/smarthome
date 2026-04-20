package com.smarthome.pattern.behavioral;

import com.smarthome.model.device.Device;

/**
 * ПАТТЕРН: Template Method
 *
 * Шаблон инициализации устройства.
 * Определяет общий порядок шагов, подклассы переопределяют конкретные шаги.
 */
public abstract class DeviceInitTemplate {

    /**
     * Шаблонный метод — порядок инициализации фиксирован.
     * final — подклассы не могут менять порядок.
     */
    public final void initialize(Device device) {
        System.out.println("--- Инициализация: " + device.getName() + " ---");
        checkConnection(device);
        loadConfiguration(device);
        applyDefaults(device);
        runSelfTest(device);
        System.out.println("--- Готово: " + device.getName() + " ---");
    }

    /** Проверить подключение */
    protected abstract void checkConnection(Device device);

    /** Загрузить конфигурацию */
    protected abstract void loadConfiguration(Device device);

    /** Применить настройки по умолчанию */
    protected void applyDefaults(Device device) {
        // По умолчанию — ничего. Подклассы могут переопределить.
    }

    /** Самотест */
    protected void runSelfTest(Device device) {
        System.out.println("  Самотест: " + (device.isConnected() ? "OK" : "FAIL"));
    }
}
