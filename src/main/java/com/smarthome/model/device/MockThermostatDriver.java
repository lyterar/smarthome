package com.smarthome.model.device;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock-драйвер термостата.
 */
public class MockThermostatDriver implements DeviceDriver {

    private boolean on = false;
    private double targetTemp = 22.0;
    private double currentTemp = 20.0;

    @Override
    public void turnOn() { this.on = true; }

    @Override
    public void turnOff() { this.on = false; }

    @Override
    public boolean isOn() { return on; }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("targetTemp", targetTemp);
        params.put("currentTemp", currentTemp);
        return params;
    }

    @Override
    public void setParameter(String key, Object value) {
        switch (key) {
            case "targetTemp" -> targetTemp = ((Number) value).doubleValue();
            case "currentTemp" -> currentTemp = ((Number) value).doubleValue();
        }
    }

    @Override
    public boolean isConnected() { return true; }

    @Override
    public DeviceType getDeviceType() { return DeviceType.THERMOSTAT; }
}
