package com.smarthome.pattern.behavioral;

import com.smarthome.model.device.Device;
import com.smarthome.model.device.DeviceType;
import com.smarthome.model.room.Room;

/**
 * Стратегия «Ночной режим»:
 * - Лампы на 10% яркости
 * - Замки заблокированы
 * - Колонки выключены
 */
public class NightModeStrategy implements AutomationStrategy {

    @Override
    public String getName() {
        return "Ночной режим";
    }

    @Override
    public void execute(Room room) {
        for (Device device : room) {
            switch (device.getType()) {
                case LIGHT -> {
                    device.turnOn();
                    device.setParameter("brightness", 10);
                }
                case LOCK -> {
                    device.turnOn(); // on = locked
                }
                case SPEAKER -> {
                    device.turnOff();
                }
                default -> { /* остальные без изменений */ }
            }
        }
    }
}
