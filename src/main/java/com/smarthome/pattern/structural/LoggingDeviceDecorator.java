package com.smarthome.pattern.structural;

import com.smarthome.model.device.DeviceDriver;
import com.smarthome.model.device.DeviceType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ПАТТЕРН: Decorator
 *
 * Оборачивает любой DeviceDriver, добавляя логирование всех действий.
 
 */
public class LoggingDeviceDecorator implements DeviceDriver {

    private final DeviceDriver wrapped;
    private final List<String> log = new ArrayList<>();
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public LoggingDeviceDecorator(DeviceDriver wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void turnOn() {
        addLog("turnOn()");
        wrapped.turnOn();
    }

    @Override
    public void turnOff() {
        addLog("turnOff()");
        wrapped.turnOff();
    }

    @Override
    public boolean isOn() {
        return wrapped.isOn();
    }

    @Override
    public Map<String, Object> getParameters() {
        return wrapped.getParameters();
    }

    @Override
    public void setParameter(String key, Object value) {
        addLog("setParameter(" + key + ", " + value + ")");
        wrapped.setParameter(key, value);
    }

    @Override
    public boolean isConnected() {
        return wrapped.isConnected();
    }

    @Override
    public DeviceType getDeviceType() {
        return wrapped.getDeviceType();
    }

    /** Получить лог действий */
    public List<String> getLog() {
        return new ArrayList<>(log);
    }

    /** Последние N записей */
    public List<String> getRecentLog(int count) {
        int start = Math.max(0, log.size() - count);
        return new ArrayList<>(log.subList(start, log.size()));
    }

    private void addLog(String action) {
        String time = LocalDateTime.now().format(FORMAT);
        String entry = "[" + time + "] " + getDeviceType().getDisplayName() + ": " + action;
        log.add(entry);
        System.out.println(entry); // Также выводим в консоль
    }
}
