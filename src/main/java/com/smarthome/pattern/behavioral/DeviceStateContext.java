package com.smarthome.pattern.behavioral;

import com.smarthome.model.device.Device;

/**
 * Контекст состояния устройства.
 * Хранит текущее состояние и управляет переходами.
 */
public class DeviceStateContext {

    private DeviceState currentState;
    private final Device device;

    // Предопределённые состояния
    public static final DeviceState OFF_STATE = new OffState();
    public static final DeviceState ON_STATE = new OnState();
    public static final DeviceState ERROR_STATE = new ErrorState();

    public DeviceStateContext(Device device) {
        this.device = device;
        this.currentState = OFF_STATE;
    }

    public void turnOn() {
        currentState.handleTurnOn(device, this);
    }

    public void turnOff() {
        currentState.handleTurnOff(device, this);
    }

    public void setState(DeviceState state) {
        this.currentState = state;
    }

    public DeviceState getState() {
        return currentState;
    }

    // --- Конкретные состояния ---

    private static class OffState implements DeviceState {
        @Override
        public void handleTurnOn(Device device, DeviceStateContext context) {
            device.turnOn();
            context.setState(ON_STATE);
            System.out.println(device.getName() + ": OFF -> ON");
        }

        @Override
        public void handleTurnOff(Device device, DeviceStateContext context) {
            // Уже выключено — ничего не делаем
        }

        @Override
        public String getStateName() { return "Выключено"; }

        @Override
        public String getColor() { return "#999999"; }
    }

    private static class OnState implements DeviceState {
        @Override
        public void handleTurnOn(Device device, DeviceStateContext context) {
            // Уже включено
        }

        @Override
        public void handleTurnOff(Device device, DeviceStateContext context) {
            device.turnOff();
            context.setState(OFF_STATE);
            System.out.println(device.getName() + ": ON -> OFF");
        }

        @Override
        public String getStateName() { return "Включено"; }

        @Override
        public String getColor() { return "#4CAF50"; }
    }

    private static class ErrorState implements DeviceState {
        @Override
        public void handleTurnOn(Device device, DeviceStateContext context) {
            System.out.println(device.getName() + ": ОШИБКА — попробуйте перезагрузить");
        }

        @Override
        public void handleTurnOff(Device device, DeviceStateContext context) {
            device.turnOff();
            context.setState(OFF_STATE);
            System.out.println(device.getName() + ": ERROR -> OFF (сброс)");
        }

        @Override
        public String getStateName() { return "Ошибка"; }

        @Override
        public String getColor() { return "#F44336"; }
    }
}
