package com.smarthome.model.device;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock-драйвер камеры.
 */
public class MockCameraDriver implements DeviceDriver {

    private boolean on = false;
    private boolean recording = false;
    private String resolution = "1080p";

    @Override
    public void turnOn() { this.on = true; }

    @Override
    public void turnOff() { this.on = false; this.recording = false; }

    @Override
    public boolean isOn() { return on; }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("recording", recording);
        params.put("resolution", resolution);
        return params;
    }

    @Override
    public void setParameter(String key, Object value) {
        switch (key) {
            case "recording" -> recording = (Boolean) value;
            case "resolution" -> resolution = value.toString();
        }
    }

    @Override
    public boolean isConnected() { return true; }

    @Override
    public DeviceType getDeviceType() { return DeviceType.CAMERA; }
}
