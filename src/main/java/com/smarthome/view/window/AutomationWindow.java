package com.smarthome.view.window;

import com.smarthome.model.room.Room;
import com.smarthome.pattern.behavioral.AutomationStrategy;
import com.smarthome.pattern.behavioral.CommandHistory;
import com.smarthome.pattern.structural.SmartHomeFacade;
import com.smarthome.service.AutomationService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ПАТТЕРН: Observer (EventBus) + Strategy (AutomationStrategy)
 *
 * Причина: вся автоматизация в главном окне — тесно и неудобно.
 * Следствие: отдельное немодальное окно с выбором стратегии и историей применений,
 *            работает параллельно с главным окном (Modality.NONE).
 */
public class AutomationWindow {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Stage stage;
    private final SmartHomeFacade facade;
    private final AutomationService automationService;
    private final CommandHistory commandHistory;

    private final ObservableList<String> historyItems = FXCollections.observableArrayList();
    private final ObservableList<String> roomItems = FXCollections.observableArrayList();

    private ComboBox<AutomationStrategy> strategyCombo;
    private ComboBox<String> roomCombo;
    private Label statusLabel;
    private Button btnUndo;
    private Button btnRedo;

    public AutomationWindow(SmartHomeFacade facade,
                             AutomationService automationService,
                             CommandHistory commandHistory) {
        this.facade = facade;
        this.automationService = automationService;
        this.commandHistory = commandHistory;
        this.stage = new Stage();
        buildUI();
    }

    private void buildUI() {
        stage.setTitle("Автоматизация");
        // Причина: немодальное окно — пользователь управляет обоими окнами сразу
        // Следствие: Modality.NONE разрешает ввод в любом окне
        stage.initModality(Modality.NONE);
        stage.setResizable(true);

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #2b2b2b;");

        Label title = new Label("Стратегии автоматизации");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        Label lblStrategy = new Label("Стратегия:");
        lblStrategy.setStyle("-fx-text-fill: #a0a0a0;");

        strategyCombo = new ComboBox<>(
                FXCollections.observableArrayList(automationService.getAvailableStrategies()));
        strategyCombo.setMaxWidth(Double.MAX_VALUE);
        strategyCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(AutomationStrategy s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : s.getName());
            }
        });
        strategyCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(AutomationStrategy s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "Выберите стратегию..." : s.getName());
            }
        });
        if (\!strategyCombo.getItems().isEmpty()) {
            strategyCombo.getSelectionModel().selectFirst();
        }

        Label lblRoom = new Label("Комната (пусто = все комнаты):");
        lblRoom.setStyle("-fx-text-fill: #a0a0a0;");
        refreshRoomList();
        roomCombo = new ComboBox<>(roomItems);
        roomCombo.setMaxWidth(Double.MAX_VALUE);
        roomCombo.setPromptText("Все комнаты");

        Button btnApply = new Button("Применить стратегию");
        btnApply.setMaxWidth(Double.MAX_VALUE);
        btnApply.setStyle(
            "-fx-background-color: #4a90d9; -fx-text-fill: white; " +
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnApply.setOnAction(e -> applyStrategy());

        HBox undoRedoBox = new HBox(8);
        undoRedoBox.setAlignment(Pos.CENTER);
        btnUndo = new Button("Отменить");
        btnRedo = new Button("Повторить");
        for (Button b : new Button[]{btnUndo, btnRedo}) {
            b.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-cursor: hand;");
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
        }
        btnUndo.setOnAction(e -> undoLast());
        btnRedo.setOnAction(e -> redoLast());
        undoRedoBox.getChildren().addAll(btnUndo, btnRedo);
        updateUndoRedo();

        Button btnRefresh = new Button("Обновить комнаты");
        btnRefresh.setMaxWidth(Double.MAX_VALUE);
        btnRefresh.setStyle("-fx-background-color: #444; -fx-text-fill: #ccc; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> refreshRoomList());

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #555;");

        Label lblHistory = new Label("История применений:");
        lblHistory.setStyle("-fx-text-fill: #a0a0a0; -fx-font-weight: bold;");

        ListView<String> historyList = new ListView<>(historyItems);
        historyList.setPrefHeight(180);
        historyList.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #c0c0c0;");
        historyList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); }
                else { setText(s); setStyle("-fx-text-fill: #b0d0ff; -fx-font-size: 11px; -fx-background-color: #1e1e1e;"); }
            }
        });

        Button btnClear = new Button("Очистить историю");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setStyle("-fx-background-color: #5a2a2a; -fx-text-fill: #ffaaaa; -fx-cursor: hand;");
        btnClear.setOnAction(e -> historyItems.clear());

        statusLabel = new Label("Готово");
        statusLabel.setStyle("-fx-text-fill: #80c080; -fx-font-size: 11px;");

        root.getChildren().addAll(
                title, sep,
                lblStrategy, strategyCombo,
                lblRoom, roomCombo,
                btnApply, undoRedoBox, btnRefresh,
                new Separator(),
                lblHistory, historyList, btnClear,
                statusLabel
        );

        Scene scene = new Scene(root, 400, 580);
        stage.setScene(scene);
        stage.setMinWidth(320);
        stage.setMinHeight(450);
    }

    private void applyStrategy() {
        AutomationStrategy strategy = strategyCombo.getValue();
        if (strategy == null) { setStatus("Выберите стратегию"); return; }

        String selectedRoomName = roomCombo.getValue();
        String timeStr = LocalDateTime.now().format(TIME_FMT);

        if (selectedRoomName == null || selectedRoomName.isEmpty()) {
            automationService.applyToAll(strategy);
            addHistory(timeStr + " | " + strategy.getName() + " — все комнаты");
            setStatus(strategy.getName() + " применён ко всему дому");
        } else {
            Room target = facade.getRooms().stream()
                    .filter(r -> r.getName().equals(selectedRoomName))
                    .findFirst().orElse(null);
            if (target == null) { setStatus("Комната не найдена"); return; }
            automationService.applyStrategy(strategy, target);
            addHistory(timeStr + " | " + strategy.getName() + " — " + target.getName());
            setStatus(strategy.getName() + " применён к " + target.getName());
        }
        updateUndoRedo();
    }

    private void undoLast() {
        if (commandHistory.undo()) {
            addHistory(LocalDateTime.now().format(TIME_FMT) + " | Отменено");
            setStatus("Действие отменено");
            updateUndoRedo();
        } else { setStatus("Нечего отменять"); }
    }

    private void redoLast() {
        if (commandHistory.redo()) {
            addHistory(LocalDateTime.now().format(TIME_FMT) + " | Повторено");
            setStatus("Действие повторено");
            updateUndoRedo();
        } else { setStatus("Нечего повторять"); }
    }

    private void refreshRoomList() {
        roomItems.clear();
        facade.getRooms().forEach(r -> roomItems.add(r.getName()));
        if (roomCombo \!= null) roomCombo.setValue(null);
    }

    private void addHistory(String record) {
        Platform.runLater(() -> {
            historyItems.add(0, record);
            if (historyItems.size() > 100) historyItems.remove(historyItems.size() - 1);
        });
    }

    private void updateUndoRedo() {
        if (btnUndo == null) return;
        btnUndo.setDisable(\!commandHistory.canUndo());
        btnRedo.setDisable(\!commandHistory.canRedo());
    }

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    public void show() {
        refreshRoomList();
        if (roomCombo \!= null) roomCombo.setValue(null);
        stage.show();
        stage.toFront();
    }

    public boolean isShowing() { return stage.isShowing(); }
}
