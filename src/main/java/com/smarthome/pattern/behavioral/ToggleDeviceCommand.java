package com.smarthome.pattern.behavioral;

import com.smarthome.model.device.Device;

/**
 * Команда переключения устройства (вкл/выкл).
 */
public class ToggleDeviceCommand implements DeviceCommand {

    private final Device device;
    private boolean wasOn;

    public ToggleDeviceCommand(Device device) {
        this.device = device;
    }

    @Override
    public void execute() {
        wasOn = device.isOn();
        if (wasOn) {
            device.turnOff();
        } else {
            device.turnOn();
        }
    }

    @Override
    public void undo() {
        if (wasOn) {
            device.turnOn();
        } else {
            device.turnOff();
        }
    }

    @Override
    public String getDescription() {
        return "Переключить " + device.getName();
    }
}
