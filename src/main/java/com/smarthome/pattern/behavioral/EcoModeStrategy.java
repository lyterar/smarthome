package com.smarthome.pattern.behavioral;

import com.smarthome.model.device.Device;
import com.smarthome.model.device.DeviceType;
import com.smarthome.model.room.Room;

/**
 * Стратегия «Эко режим»:
 * - Все лампы выключены
 * - Термостат на 18°C
 * - Камеры выключены
 */
public class EcoModeStrategy implements AutomationStrategy {

    @Override
    public String getName() {
        return "Эко режим";
    }

    @Override
    public void execute(Room room) {
        for (Device device : room) {
            switch (device.getType()) {
                case LIGHT -> device.turnOff();
                case THERMOSTAT -> {
                    device.turnOn();
                    device.setParameter("targetTemp", 18.0);
                }
                case CAMERA -> device.turnOff();
                default -> { /* без изменений */ }
            }
        }
    }
}
