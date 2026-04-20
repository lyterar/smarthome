package com.smarthome.pattern.behavioral;

import com.smarthome.model.device.Device;
import com.smarthome.model.room.Room;

import java.util.List;

/**
 * ПАТТЕРН: Strategy
 *
 * Стратегия автоматизации — определяет логику автоматических действий.
 * Можно менять стратегию на лету.
 */
public interface AutomationStrategy {

    /** Название стратегии для UI */
    String getName();

    /** Выполнить стратегию для комнаты */
    void execute(Room room);
}
