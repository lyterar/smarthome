package com.smarthome.model.device;

import java.util.Map;
import java.util.UUID;

/**
 * Устройство умного дома.
 * Содержит метаданные + драйвер для реального управления.
 * Реализует Prototype (clone).
 */
public class Device implements Cloneable {

    private String id;
    private String name;
    private DeviceType type;
    private DeviceDriver driver;

    // Позиция устройства на карте комнаты (в пикселях)
    private double x;
    private double y;

    public Device(String name, DeviceType type, DeviceDriver driver) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.type = type;
        this.driver = driver;
    }

    // --- Prototype pattern ---

    @Override
    public Device clone() {
        try {
            Device copy = (Device) super.clone();
            copy.id = UUID.randomUUID().toString().substring(0, 8);
            copy.name = this.name + " (копия)";
            // Драйвер не клонируется — каждое устройство получит свой через фабрику
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Clone failed", e);
        }
    }

    // --- Делегирование к драйверу ---

    public void turnOn() {
        driver.turnOn();
    }

    public void turnOff() {
        driver.turnOff();
    }

    public boolean isOn() {
        return driver.isOn();
    }

    public Map<String, Object> getParameters() {
        return driver.getParameters();
    }

    public void setParameter(String key, Object value) {
        driver.setParameter(key, value);
    }

    public boolean isConnected() {
        return driver.isConnected();
    }

    // --- Getters / Setters ---

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DeviceType getType() { return type; }

    public DeviceDriver getDriver() { return driver; }
    public void setDriver(DeviceDriver driver) { this.driver = driver; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    @Override
    public String toString() {
        return type.getIcon() + " " + name + " [" + (isOn() ? "ON" : "OFF") + "]";
    }
}
