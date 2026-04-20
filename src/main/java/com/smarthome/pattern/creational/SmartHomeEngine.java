package com.smarthome.pattern.creational;

import com.smarthome.model.house.House;
import com.smarthome.event.EventBus;

/**
 * ПАТТЕРН: Singleton
 *
 * Единственный экземпляр движка умного дома.
 * Хранит текущий дом, фабрику и шину событий.
 */
public class SmartHomeEngine {

    private static SmartHomeEngine instance;

    private House house;
    private final DeviceFactory deviceFactory;
    private final EventBus eventBus;

    private SmartHomeEngine() {
        this.house = new House("Мой дом");
        this.deviceFactory = new DeviceFactory();
        this.eventBus = new EventBus();
    }

    public static SmartHomeEngine getInstance() {
        if (instance == null) {
            instance = new SmartHomeEngine();
        }
        return instance;
    }

    /** Для тестов — сброс синглтона */
    public static void reset() {
        instance = null;
    }

    public House getHouse() { return house; }
    public void setHouse(House house) { this.house = house; }

    public DeviceFactory getDeviceFactory() { return deviceFactory; }
    public EventBus getEventBus() { return eventBus; }
}
