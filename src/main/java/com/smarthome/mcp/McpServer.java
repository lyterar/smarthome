package com.smarthome.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * MCP (Model Context Protocol) Сервер.
 *
 * Встроенный HTTP-сервер с JSON-RPC API.
 * Предоставляет инструменты для управления умным домом
 * через внешние приложения (Claude, скрипты и т.д.)
 *
 * Эндпоинты:
 *   POST /mcp  — JSON-RPC запросы
 *   GET  /health — проверка работоспособности
 */
public class McpServer {

    private HttpServer server;
    private final int port;
    private final Gson gson = new Gson();

    // Зарегистрированные инструменты: имя -> обработчик
    private final Map<String, McpTool> tools = new HashMap<>();

    public McpServer(int port) {
        this.port = port;
    }

    /**
     * Зарегистрировать инструмент.
     */
    public void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * Запустить сервер.
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // JSON-RPC эндпоинт
        server.createContext("/mcp", this::handleMcpRequest);

        // Health check
        server.createContext("/health", exchange -> {
            String response = "{\"status\":\"ok\"}";
            sendResponse(exchange, 200, response);
        });

        server.setExecutor(null); // Дефолтный executor
        server.start();
        System.out.println("[MCP] Сервер запущен на порту " + port);
    }

    /**
     * Остановить сервер.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("[MCP] Сервер остановлен");
        }
    }

    private void handleMcpRequest(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        // Читаем тело запроса
        String body = readBody(exchange);

        try {
            JsonObject request = JsonParser.parseString(body).getAsJsonObject();
            String method = request.get("method").getAsString();

            String result = switch (method) {
                case "tools/list" -> handleToolsList();
                case "tools/call" -> handleToolCall(request);
                default -> errorResponse("Unknown method: " + method);
            };

            sendResponse(exchange, 200, result);

        } catch (Exception e) {
            sendResponse(exchange, 400, errorResponse(e.getMessage()));
        }
    }

    private String handleToolsList() {
        var toolList = tools.values().stream()
                .map(tool -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("name", tool.getName());
                    info.put("description", tool.getDescription());
                    info.put("parameters", tool.getParameterSchema());
                    return info;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("tools", toolList);
        return gson.toJson(response);
    }

    private String handleToolCall(JsonObject request) {
        JsonObject params = request.getAsJsonObject("params");
        String toolName = params.get("name").getAsString();

        McpTool tool = tools.get(toolName);
        if (tool == null) {
            return errorResponse("Tool not found: " + toolName);
        }

        JsonObject arguments = params.has("arguments")
                ? params.getAsJsonObject("arguments")
                : new JsonObject();

        try {
            Object result = tool.execute(arguments);
            Map<String, Object> response = new HashMap<>();
            response.put("content", result);
            return gson.toJson(response);
        } catch (Exception e) {
            return errorResponse("Tool error: " + e.getMessage());
        }
    }

    private String errorResponse(String message) {
        return "{\"error\":\"" + message.replace("\"", "'") + "\"}";
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public int getPort() { return port; }
    public Map<String, McpTool> getTools() { return tools; }
}
