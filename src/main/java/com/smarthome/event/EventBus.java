package com.smarthome.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * ПАТТЕРН: Observer
 *
 * Шина событий — подписка на события по типу.
 * UI подписывается на изменения и обновляет отображение.
 */
public class EventBus {

    // Подписчики, сгруппированные по типу события
    private final Map<String, List<Consumer<DeviceEvent>>> listeners = new HashMap<>();

    // Подписчики на ВСЕ события
    private final List<Consumer<DeviceEvent>> globalListeners = new ArrayList<>();

    /**
     * Подписаться на конкретный тип события.
     *
     * Пример: eventBus.subscribe("device_toggled", event -> updateUI());
     */
    public void subscribe(String eventType, Consumer<DeviceEvent> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    /**
     * Подписаться на все события.
     */
    public void subscribeAll(Consumer<DeviceEvent> listener) {
        globalListeners.add(listener);
    }

    /**
     * Отписаться от конкретного типа.
     */
    public void unsubscribe(String eventType, Consumer<DeviceEvent> listener) {
        List<Consumer<DeviceEvent>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Опубликовать событие — уведомить всех подписчиков.
     */
    public void publish(DeviceEvent event) {
        // Уведомляем подписчиков конкретного типа
        List<Consumer<DeviceEvent>> specific = listeners.get(event.getType());
        if (specific != null) {
            for (Consumer<DeviceEvent> listener : specific) {
                listener.accept(event);
            }
        }

        // Уведомляем глобальных подписчиков
        for (Consumer<DeviceEvent> listener : globalListeners) {
            listener.accept(event);
        }
    }

    /**
     * Очистить все подписки (при закрытии).
     */
    public void clear() {
        listeners.clear();
        globalListeners.clear();
    }
}
