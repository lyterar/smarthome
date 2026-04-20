package com.smarthome.model.device;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация для создания устройства.
 * Передаётся в фабрику при создании.
 * Для реальных устройств тут будет адрес, порт, протокол и т.д.
 */
public class DeviceConfig {

    private String name;
    private DeviceType type;
    private String address;        // IP или MAC для реальных устройств
    private int port;
    private Map<String, String> extra = new HashMap<>();

    public DeviceConfig(String name, DeviceType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DeviceType getType() { return type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public Map<String, String> getExtra() { return extra; }
    public void putExtra(String key, String value) { extra.put(key, value); }
}
