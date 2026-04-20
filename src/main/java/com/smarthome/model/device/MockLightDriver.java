package com.smarthome.model.device;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock-драйвер лампы. Хранит состояние в памяти.
 * 
 * Для замены на реальный — создать Esp32LightDriver,
 * который отправляет HTTP запросы на ESP32.
 */
public class MockLightDriver implements DeviceDriver {

    private boolean on = false;
    private int brightness = 100;    // 0-100
    private String color = "#FFFFFF";

    @Override
    public void turnOn() { this.on = true; }

    @Override
    public void turnOff() { this.on = false; }

    @Override
    public boolean isOn() { return on; }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("brightness", brightness);
        params.put("color", color);
        return params;
    }

    @Override
    public void setParameter(String key, Object value) {
        switch (key) {
            case "brightness" -> brightness = ((Number) value).intValue();
            case "color" -> color = value.toString();
        }
    }

    @Override
    public boolean isConnected() { return true; } // Mock всегда подключён

    @Override
    public DeviceType getDeviceType() { return DeviceType.LIGHT; }
}
