package com.smarthome.pattern.structural;

import com.smarthome.model.device.DeviceDriver;
import com.smarthome.model.device.DeviceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ПАТТЕРН: Composite
 *
 * Группа устройств, которая ведёт себя как одно устройство.
 * Например: "Все лампы в гостиной" — включить/выключить одной командой.
 */
public class DeviceGroup implements DeviceDriver {

    private final String groupName;
    private final DeviceType groupType;
    private final List<DeviceDriver> children = new ArrayList<>();

    public DeviceGroup(String groupName, DeviceType groupType) {
        this.groupName = groupName;
        this.groupType = groupType;
    }

    public void addDevice(DeviceDriver device) {
        children.add(device);
    }

    public void removeDevice(DeviceDriver device) {
        children.remove(device);
    }

    public List<DeviceDriver> getChildren() {
        return new ArrayList<>(children);
    }

    @Override
    public void turnOn() {
        for (DeviceDriver child : children) {
            child.turnOn();
        }
    }

    @Override
    public void turnOff() {
        for (DeviceDriver child : children) {
            child.turnOff();
        }
    }

    @Override
    public boolean isOn() {
        // Группа "включена" только если все устройства включены —
        // тогда toggle предсказуем: не все включены → включить всех, все включены → выключить всех
        if (children.isEmpty()) return false;
        return children.stream().allMatch(DeviceDriver::isOn);
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("groupName", groupName);
        params.put("childCount", children.size());
        params.put("activeCount", children.stream().filter(DeviceDriver::isOn).count());
        return params;
    }

    @Override
    public void setParameter(String key, Object value) {
        // Устанавливаем параметр всем дочерним устройствам
        for (DeviceDriver child : children) {
            child.setParameter(key, value);
        }
    }

    @Override
    public boolean isConnected() {
        return children.stream().allMatch(DeviceDriver::isConnected);
    }

    @Override
    public DeviceType getDeviceType() {
        return groupType;
    }

    public String getGroupName() {
        return groupName;
    }
}
