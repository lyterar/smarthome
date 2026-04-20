package com.smarthome.pattern.behavioral;

/**
 * ПАТТЕРН: Command
 *
 * Команда для устройства с поддержкой отмены (undo).
 */
public interface DeviceCommand {

    /** Выполнить команду */
    void execute();

    /** Отменить команду */
    void undo();

    /** Описание для отображения в UI */
    String getDescription();
}
