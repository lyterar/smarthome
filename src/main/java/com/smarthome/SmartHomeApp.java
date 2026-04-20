package com.smarthome;

import com.smarthome.mcp.McpServer;
import com.smarthome.mcp.McpTools;
import com.smarthome.mcp.McpPatternReferenceTools;
import com.smarthome.pattern.structural.SmartHomeFacade;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Точка входа приложения Smart Home Constructor.
 *
 * Запускает:
 * 1. JavaFX UI (конструктор комнат)
 * 2. MCP сервер (порт 3001) для внешнего управления
 */
public class SmartHomeApp extends Application {

    private McpServer mcpServer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Загружаем FXML
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();

        // Настраиваем сцену
        Scene scene = new Scene(root, 1100, 700);

        primaryStage.setTitle("Smart Home Constructor");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();

        // Запускаем MCP сервер в фоне
        startMcpServer();
    }

    private void startMcpServer() {
        try {
            SmartHomeFacade facade = new SmartHomeFacade();
            mcpServer = new McpServer(3001);
            new McpTools(facade).registerAll(mcpServer);
            new McpPatternReferenceTools().registerAll(mcpServer);
            mcpServer.start();
            System.out.println("[MCP] Зарегистрировано инструментов: " + mcpServer.getTools().size());
        } catch (Exception e) {
            System.err.println("Не удалось запустить MCP сервер: " + e.getMessage());
            // Приложение продолжает работать без MCP
        }
    }

    @Override
    public void stop() {
        // Останавливаем MCP при закрытии
        if (mcpServer != null) {
            mcpServer.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
