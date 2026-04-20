package com.smarthome.pattern.behavioral;

import com.smarthome.model.device.Device;

/**
 * Команда изменения параметра устройства.
 */
public class SetParameterCommand implements DeviceCommand {

    private final Device device;
    private final String key;
    private final Object newValue;
    private Object oldValue;

    public SetParameterCommand(Device device, String key, Object newValue) {
        this.device = device;
        this.key = key;
        this.newValue = newValue;
    }

    @Override
    public void execute() {
        oldValue = device.getParameters().get(key);
        device.setParameter(key, newValue);
    }

    @Override
    public void undo() {
        if (oldValue != null) {
            device.setParameter(key, oldValue);
        }
    }

    @Override
    public String getDescription() {
        return "Изменить " + key + " -> " + newValue + " (" + device.getName() + ")";
    }
}
