package com.smarthome.model.device;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock-драйвер колонки.
 */
public class MockSpeakerDriver implements DeviceDriver {

    private boolean on = false;
    private int volume = 50;  // 0-100
    private boolean playing = false;

    @Override
    public void turnOn() { this.on = true; }

    @Override
    public void turnOff() { this.on = false; this.playing = false; }

    @Override
    public boolean isOn() { return on; }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("volume", volume);
        params.put("playing", playing);
        return params;
    }

    @Override
    public void setParameter(String key, Object value) {
        switch (key) {
            case "volume" -> volume = ((Number) value).intValue();
            case "playing" -> playing = (Boolean) value;
        }
    }

    @Override
    public boolean isConnected() { return true; }

    @Override
    public DeviceType getDeviceType() { return DeviceType.SPEAKER; }
}
