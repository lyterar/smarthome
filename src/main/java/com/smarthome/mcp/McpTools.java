package com.smarthome.mcp;

import com.google.gson.JsonObject;
import com.smarthome.model.device.Device;
import com.smarthome.model.room.Room;
import com.smarthome.pattern.creational.SmartHomeEngine;
import com.smarthome.pattern.structural.SmartHomeFacade;

import java.util.*;

/**
 * Набор встроенных MCP инструментов.
 * Регистрируются в McpServer при старте.
 */
public class McpTools {

    private final SmartHomeFacade facade;

    public McpTools(SmartHomeFacade facade) {
        this.facade = facade;
    }

    /** Зарегистрировать все инструменты на сервере */
    public void registerAll(McpServer server) {
        server.registerTool(new GetPatternsTool());
        server.registerTool(new ListDevicesTool());
        server.registerTool(new ControlDeviceTool());
        server.registerTool(new ListRoomsTool());
        server.registerTool(new GetHouseStatusTool());
    }

    // ====== Инструмент: get_patterns ======

    private class GetPatternsTool implements McpTool {
        @Override
        public String getName() { return "get_patterns"; }

        @Override
        public String getDescription() {
            return "Получить список GoF паттернов, используемых в проекте";
        }

        @Override
        public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of());
        }

        @Override
        public Object execute(JsonObject arguments) {
            List<Map<String, String>> patterns = new ArrayList<>();
            patterns.add(pattern("Singleton", "SmartHomeEngine", "creational"));
            patterns.add(pattern("Factory Method", "DeviceFactory", "creational"));
            patterns.add(pattern("Builder", "RoomBuilder", "creational"));
            patterns.add(pattern("Prototype", "Device.clone()", "creational"));
            patterns.add(pattern("Adapter", "DeviceAdapter", "structural"));
            patterns.add(pattern("Decorator", "LoggingDeviceDecorator", "structural"));
            patterns.add(pattern("Composite", "DeviceGroup", "structural"));
            patterns.add(pattern("Facade", "SmartHomeFacade", "structural"));
            patterns.add(pattern("Proxy", "DeviceProxy", "structural"));
            patterns.add(pattern("Observer", "EventBus", "behavioral"));
            patterns.add(pattern("Strategy", "AutomationStrategy", "behavioral"));
            patterns.add(pattern("Command", "DeviceCommand + CommandHistory", "behavioral"));
            patterns.add(pattern("State", "DeviceStateContext", "behavioral"));
            patterns.add(pattern("Iterator", "Room implements Iterable", "behavioral"));
            patterns.add(pattern("Template Method", "DeviceInitTemplate", "behavioral"));
            return Map.of("patterns", patterns, "total", patterns.size());
        }

        private Map<String, String> pattern(String name, String location, String category) {
            Map<String, String> p = new HashMap<>();
            p.put("name", name);
            p.put("implementation", location);
            p.put("category", category);
            return p;
        }
    }

    // ====== Инструмент: list_devices ======

    private class ListDevicesTool implements McpTool {
        @Override
        public String getName() { return "list_devices"; }

        @Override
        public String getDescription() {
            return "Получить список всех устройств в доме";
        }

        @Override
        public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of(
                    "room_id", Map.of("type", "string", "description", "ID комнаты (опционально)")
            ));
        }

        @Override
        public Object execute(JsonObject arguments) {
            List<Device> devices;
            if (arguments.has("room_id")) {
                String roomId = arguments.get("room_id").getAsString();
                Room room = SmartHomeEngine.getInstance().getHouse().findRoomById(roomId);
                devices = (room != null) ? room.getDevices() : List.of();
            } else {
                devices = facade.getAllDevices();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Device d : devices) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", d.getId());
                info.put("name", d.getName());
                info.put("type", d.getType().name());
                info.put("isOn", d.isOn());
                info.put("connected", d.isConnected());
                info.put("parameters", d.getParameters());
                result.add(info);
            }
            return Map.of("devices", result, "count", result.size());
        }
    }

    // ====== Инструмент: control_device ======

    private class ControlDeviceTool implements McpTool {
        @Override
        public String getName() { return "control_device"; }

        @Override
        public String getDescription() {
            return "Управление устройством: включить, выключить, изменить параметр";
        }

        @Override
        public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object",
                    "properties", Map.of(
                            "device_id", Map.of("type", "string", "description", "ID устройства"),
                            "action", Map.of("type", "string", "description", "toggle | set_param"),
                            "param_key", Map.of("type", "string", "description", "Ключ параметра (для set_param)"),
                            "param_value", Map.of("type", "string", "description", "Значение (для set_param)")
                    ),
                    "required", List.of("device_id", "action")
            );
        }

        @Override
        public Object execute(JsonObject arguments) {
            String deviceId = arguments.get("device_id").getAsString();
            String action = arguments.get("action").getAsString();

            switch (action) {
                case "toggle" -> {
                    facade.toggleDevice(deviceId);
                    return Map.of("status", "ok", "action", "toggled");
                }
                case "set_param" -> {
                    String key = arguments.get("param_key").getAsString();
                    String value = arguments.get("param_value").getAsString();
                    facade.setDeviceParameter(deviceId, key, value);
                    return Map.of("status", "ok", "action", "param_set", "key", key, "value", value);
                }
                default -> {
                    return Map.of("status", "error", "message", "Unknown action: " + action);
                }
            }
        }
    }

    // ====== Инструмент: list_rooms ======

    private class ListRoomsTool implements McpTool {
        @Override
        public String getName() { return "list_rooms"; }

        @Override
        public String getDescription() {
            return "Получить список комнат дома";
        }

        @Override
        public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of());
        }

        @Override
        public Object execute(JsonObject arguments) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Room room : facade.getRooms()) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", room.getId());
                info.put("name", room.getName());
                info.put("type", room.getType().name());
                info.put("deviceCount", room.getDeviceCount());
                info.put("x", room.getX());
                info.put("y", room.getY());
                info.put("width", room.getWidth());
                info.put("height", room.getHeight());
                result.add(info);
            }
            return Map.of("rooms", result, "count", result.size());
        }
    }

    // ====== Инструмент: get_house_status ======

    private class GetHouseStatusTool implements McpTool {
        @Override
        public String getName() { return "get_house_status"; }

        @Override
        public String getDescription() {
            return "Общая информация о доме: количество комнат, устройств, активных устройств";
        }

        @Override
        public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of());
        }

        @Override
        public Object execute(JsonObject arguments) {
            return Map.of("summary", facade.getHouseSummary());
        }
    }
}
