package com.smarthome.pattern.structural;

import com.smarthome.model.device.DeviceDriver;
import com.smarthome.model.device.DeviceType;

import java.util.Map;

/**
 * ПАТТЕРН: Adapter
 *
 * Адаптирует внешний API устройства к нашему интерфейсу DeviceDriver.
 * Пример: устройство стороннего производителя с другим API.
 *
 * Использование:
 *   ExternalApi api = new SomeVendorApi("192.168.1.10");
 *   DeviceDriver adapted = new DeviceAdapter(api, DeviceType.LIGHT);
 */
public class DeviceAdapter implements DeviceDriver {

    /**
     * Интерфейс внешнего устройства (сторонний API).
     * Замените на реальный API при интеграции.
     */
    public interface ExternalDeviceApi {
        void powerOn();
        void powerOff();
        int getPowerState();  // 1 = on, 0 = off
        String readStatus();
        void writeConfig(String key, String value);
    }

    private final ExternalDeviceApi externalApi;
    private final DeviceType deviceType;

    public DeviceAdapter(ExternalDeviceApi externalApi, DeviceType deviceType) {
        this.externalApi = externalApi;
        this.deviceType = deviceType;
    }

    @Override
    public void turnOn() {
        externalApi.powerOn();
    }

    @Override
    public void turnOff() {
        externalApi.powerOff();
    }

    @Override
    public boolean isOn() {
        return externalApi.getPowerState() == 1;
    }

    @Override
    public Map<String, Object> getParameters() {
        // Парсим строку статуса во внутренний формат
        String raw = externalApi.readStatus();
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("rawStatus", raw);
        return params;
    }

    @Override
    public void setParameter(String key, Object value) {
        externalApi.writeConfig(key, value.toString());
    }

    @Override
    public boolean isConnected() {
        try {
            externalApi.getPowerState();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public DeviceType getDeviceType() {
        return deviceType;
    }
}
