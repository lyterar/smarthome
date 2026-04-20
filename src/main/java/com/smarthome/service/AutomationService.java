package com.smarthome.service;

import com.smarthome.event.DeviceEvent;
import com.smarthome.model.room.Room;
import com.smarthome.pattern.behavioral.AutomationStrategy;
import com.smarthome.pattern.behavioral.EcoModeStrategy;
import com.smarthome.pattern.behavioral.NightModeStrategy;
import com.smarthome.pattern.creational.SmartHomeEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис автоматизации.
 * Позволяет применять стратегии к комнатам.
 */
public class AutomationService {

    private final List<AutomationStrategy> availableStrategies = new ArrayList<>();

    public AutomationService() {
        // Регистрируем доступные стратегии
        availableStrategies.add(new NightModeStrategy());
        availableStrategies.add(new EcoModeStrategy());
    }

    /** Применить стратегию к комнате */
    public void applyStrategy(AutomationStrategy strategy, Room room) {
        strategy.execute(room);
        SmartHomeEngine.getInstance().getEventBus()
                .publish(new DeviceEvent("automation_applied", room.getId(), strategy.getName()));
    }

    /** Применить стратегию ко всем комнатам */
    public void applyToAll(AutomationStrategy strategy) {
        for (Room room : SmartHomeEngine.getInstance().getHouse().getRooms()) {
            strategy.execute(room);
        }
        SmartHomeEngine.getInstance().getEventBus()
                .publish(new DeviceEvent("automation_applied", "all", strategy.getName()));
    }

    public List<AutomationStrategy> getAvailableStrategies() {
        return availableStrategies;
    }

    public void addStrategy(AutomationStrategy strategy) {
        availableStrategies.add(strategy);
    }
}
