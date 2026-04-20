package com.smarthome.pattern.behavioral;

import com.smarthome.model.device.Device;

/**
 * ПАТТЕРН: State
 *
 * Состояние устройства. Поведение зависит от текущего состояния.
 */
public interface DeviceState {

    /** Обработать включение */
    void handleTurnOn(Device device, DeviceStateContext context);

    /** Обработать выключение */
    void handleTurnOff(Device device, DeviceStateContext context);

    /** Название состояния */
    String getStateName();

    /** Цвет для UI */
    String getColor();
}
