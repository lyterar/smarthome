package com.smarthome.model.device;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Mock-драйвер датчика. Генерирует случайные показания.
 */
public class MockSensorDriver implements DeviceDriver {

    private boolean on = false;
    private final Random random = new Random();
    private String sensorType = "temperature"; // temperature, humidity, motion

    @Override
    public void turnOn() { this.on = true; }

    @Override
    public void turnOff() { this.on = false; }

    @Override
    public boolean isOn() { return on; }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("sensorType", sensorType);
        if (on) {
            params.put("value", generateReading());
        }
        return params;
    }

    @Override
    public void setParameter(String key, Object value) {
        if ("sensorType".equals(key)) {
            sensorType = value.toString();
        }
    }

    @Override
    public boolean isConnected() { return true; }

    @Override
    public DeviceType getDeviceType() { return DeviceType.SENSOR; }

    private double generateReading() {
        return switch (sensorType) {
            case "temperature" -> 18 + random.nextDouble() * 10;  // 18-28°C
            case "humidity" -> 30 + random.nextDouble() * 40;     // 30-70%
            case "motion" -> random.nextBoolean() ? 1.0 : 0.0;
            default -> 0.0;
        };
    }
}
