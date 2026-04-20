package com.smarthome.mcp;

import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Инструмент MCP сервера.
 * Каждый инструмент имеет имя, описание и может быть вызван с параметрами.
 */
public interface McpTool {

    /** Уникальное имя инструмента */
    String getName();

    /** Описание для LLM */
    String getDescription();

    /** JSON Schema параметров */
    Map<String, Object> getParameterSchema();

    /** Выполнить инструмент */
    Object execute(JsonObject arguments);
}
