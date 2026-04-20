package com.smarthome.model.device;

import java.util.Map;

/**
 * Интерфейс драйвера устройства.
 *
 * Это ГЛАВНАЯ абстракция для замены mock на реальные устройства.
 * Mock-реализации просто хранят состояние в памяти.
 * Реальные реализации будут отправлять команды по HTTP/MQTT к ESP32.
 */
public interface DeviceDriver {

    /** Включить устройство */
    void turnOn();

    /** Выключить устройство */
    void turnOff();

    /** Устройство включено? */
    boolean isOn();

    /** Получить текущие параметры (яркость, температура и т.д.) */
    Map<String, Object> getParameters();

    /** Установить параметр */
    void setParameter(String key, Object value);

    /** Получить статус подключения */
    boolean isConnected();

    /** Тип устройства */
    DeviceType getDeviceType();
}
