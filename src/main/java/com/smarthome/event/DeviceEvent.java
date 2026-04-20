package com.smarthome.event;

import java.time.LocalDateTime;

/**
 * Событие системы умного дома.
 * Содержит тип события и ID объекта, к которому относится.
 */
public class DeviceEvent {

    private final String type;       // "device_added", "device_toggled", "room_added" и т.д.
    private final String targetId;   // ID устройства или комнаты
    private final LocalDateTime timestamp;
    private Object data;             // Дополнительные данные (опционально)

    public DeviceEvent(String type, String targetId) {
        this.type = type;
        this.targetId = targetId;
        this.timestamp = LocalDateTime.now();
    }

    public DeviceEvent(String type, String targetId, Object data) {
        this(type, targetId);
        this.data = data;
    }

    public String getType() { return type; }
    public String getTargetId() { return targetId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Object getData() { return data; }

    @Override
    public String toString() {
        return "[" + type + "] target=" + targetId + " at " + timestamp;
    }
}
