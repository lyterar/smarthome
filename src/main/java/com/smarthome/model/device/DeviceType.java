package com.smarthome.model.device;

/**
 * Типы устройств умного дома.
 * При добавлении нового типа — добавить сюда и в DeviceFactory.
 */
public enum DeviceType {
    LIGHT("Лампа", "💡"),
    THERMOSTAT("Термостат", "🌡"),
    SENSOR("Датчик", "📡"),
    LOCK("Замок", "🔒"),
    CAMERA("Камера", "📷"),
    SPEAKER("Колонка", "🔊");

    private final String displayName;
    private final String icon;

    DeviceType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}
