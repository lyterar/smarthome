package com.smarthome.model.room;

/**
 * Типы комнат.
 */
public enum RoomType {
    LIVING_ROOM("Гостиная", "#E8D5B7"),
    BEDROOM("Спальня", "#C5D5E8"),
    KITCHEN("Кухня", "#D5E8C5"),
    BATHROOM("Ванная", "#C5E8E8"),
    HALLWAY("Коридор", "#E8E8D5"),
    GARAGE("Гараж", "#D5D5D5"),
    OFFICE("Кабинет", "#E8D5E8");

    private final String displayName;
    private final String color;  // HEX цвет для отображения

    RoomType(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
}
