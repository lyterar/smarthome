package com.smarthome.model.device;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock-драйвер замка.
 */
public class MockLockDriver implements DeviceDriver {

    private boolean on = false;  // on = locked
    private boolean locked = true;

    @Override
    public void turnOn() { this.on = true; this.locked = true; }

    @Override
    public void turnOff() { this.on = false; this.locked = false; }

    @Override
    public boolean isOn() { return on; }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("locked", locked);
        return params;
    }

    @Override
    public void setParameter(String key, Object value) {
        if ("locked".equals(key)) {
            locked = (Boolean) value;
            on = locked;
        }
    }

    @Override
    public boolean isConnected() { return true; }

    @Override
    public DeviceType getDeviceType() { return DeviceType.LOCK; }
}
