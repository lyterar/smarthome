package com.smarthome.view.window;

import com.smarthome.model.device.Device;
import com.smarthome.pattern.structural.SmartHomeFacade;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;

/**
 * ПАТТЕРН: Facade + Observer
 *
 * Причина: детали устройства (параметры, состояние, ID) не видны в списке главного окна.
 * Следствие: двойной клик по устройству открывает немодальное окно с полной информацией
 *            и возможностью включить/выключить или изменить параметры.
 *
 * Окно НЕ модальное: stage.initModality(Modality.NONE).
 */
public class DeviceDetailWindow {

    private final Stage stage;
    private final SmartHomeFacade facade;

    // Текущее устройство
    private Device device;

    // UI-элементы для обновления
    private Label lblName;
    private Label lblType;
    private Label lblId;
    private Label lblState;
    private Label lblConnected;
    private VBox paramsBox;
    private Button btnToggle;
    private Label statusLabel;

    public DeviceDetailWindow(SmartHomeFacade facade) {
        this.facade = facade;
        this.stage = new Stage();
        buildUI();
    }

    private void buildUI() {
        stage.setTitle("Детали устройства");
        // Причина: Modality.NONE — не блокирует главное окно
        // Следствие: пользователь видит детали и продолжает работу
        stage.initModality(Modality.NONE);
        stage.setResizable(true);

        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #2b2b2b;");

        // --- Заголовок ---
        Label title = new Label("Детали устройства");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        // --- Основная информация ---
        GridPane info = new GridPane();
        info.setHgap(12);
        info.setVgap(6);
        info.setStyle("-fx-background-color: #3a3a3a; -fx-padding: 10; -fx-background-radius: 4;");

        lblName      = makeValueLabel("—");
        lblType      = makeValueLabel("—");
        lblId        = makeValueLabel("—");
        lblState     = makeValueLabel("—");
        lblConnected = makeValueLabel("—");

        info.add(makeKeyLabel("Имя:"),       0, 0); info.add(lblName,      1, 0);
        info.add(makeKeyLabel("Тип:"),       0, 1); info.add(lblType,      1, 1);
        info.add(makeKeyLabel("ID:"),        0, 2); info.add(lblId,        1, 2);
        info.add(makeKeyLabel("Состояние:"), 0, 3); info.add(lblState,     1, 3);
        info.add(makeKeyLabel("Связь:"),     0, 4); info.add(lblConnected, 1, 4);

        // --- Кнопка вкл/выкл ---
        btnToggle = new Button("Включить / Выключить");
        btnToggle.setMaxWidth(Double.MAX_VALUE);
        btnToggle.setStyle(
            "-fx-background-color: #4a90d9; -fx-text-fill: white; " +
            "-fx-font-size: 13px; -fx-cursor: hand;");
        btnToggle.setOnAction(e -> toggleDevice());

        // --- Параметры устройства ---
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #555;");
        Label lblParams = new Label("Параметры:");
        lblParams.setStyle("-fx-text-fill: #a0a0a0; -fx-font-weight: bold;");

        paramsBox = new VBox(4);
        paramsBox.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 8; -fx-background-radius: 4;");

        // --- Кнопка обновить ---
        Button btnRefresh = new Button("Обновить");
        btnRefresh.setMaxWidth(Double.MAX_VALUE);
        btnRefresh.setStyle("-fx-background-color: #444; -fx-text-fill: #ccc; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> refreshData());

        // --- Статус ---
        statusLabel = new Label("Выберите устройство");
        statusLabel.setStyle("-fx-text-fill: #80c080; -fx-font-size: 11px;");

        root.getChildren().addAll(
                title, info, btnToggle,
                sep, lblParams, paramsBox,
                btnRefresh, statusLabel
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #2b2b2b; -fx-background: #2b2b2b;");

        Scene scene = new Scene(scroll, 380, 500);
        stage.setScene(scene);
        stage.setMinWidth(300);
        stage.setMinHeight(360);
    }

    // === Логика ===

    /**
     * Причина: окно переиспользуется для разных устройств.
     * Следствие: вызов showDevice() обновляет UI под текущее устройство.
     */
    public void showDevice(Device device) {
        this.device = device;
        refreshData();
        stage.setTitle("Устройство: " + device.getName());
        stage.show();
        stage.toFront();
    }

    private void refreshData() {
        if (device == null) return;

        Platform.runLater(() -> {
            lblName.setText(device.getName());
            lblType.setText(device.getType().name());
            lblId.setText(device.getId());

            boolean on = device.isOn();
            lblState.setText(on ? "Включено" : "Выключено");
            lblState.setStyle(on
                    ? "-fx-text-fill: #80e080; -fx-font-weight: bold;"
                    : "-fx-text-fill: #e08080; -fx-font-weight: bold;");

            lblConnected.setText(device.isConnected() ? "Подключено" : "Нет связи");

            // Обновляем параметры
            paramsBox.getChildren().clear();
            Map<String, Object> params = device.getParameters();
            if (params == null || params.isEmpty()) {
                Label noParams = new Label("(нет параметров)");
                noParams.setStyle("-fx-text-fill: #777; -fx-font-size: 11px;");
                paramsBox.getChildren().add(noParams);
            } else {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    HBox row = new HBox(8);
                    Label key = makeKeyLabel(entry.getKey() + ":");
                    Label val = makeValueLabel(String.valueOf(entry.getValue()));
                    val.setStyle("-fx-text-fill: #ffda80;");
                    row.getChildren().addAll(key, val);
                    paramsBox.getChildren().add(row);
                }
            }

            // Обновляем кнопку
            btnToggle.setText(on ? "Выключить" : "Включить");
            btnToggle.setStyle(on
                    ? "-fx-background-color: #b04040; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;"
                    : "-fx-background-color: #40a040; -fx-text-fill: white; -fx-font-size: 13px; -fx-cursor: hand;");
        });
    }

    private void toggleDevice() {
        if (device == null) return;
        facade.toggleDevice(device.getId());
        refreshData();
        statusLabel.setText("Переключено: " + device.getName());
    }

    // === Утилиты ===

    private Label makeKeyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #a0a0a0; -fx-min-width: 90;");
        return l;
    }

    private Label makeValueLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #e0e0e0;");
        return l;
    }

    public boolean isShowing() { return stage.isShowing(); }
}
