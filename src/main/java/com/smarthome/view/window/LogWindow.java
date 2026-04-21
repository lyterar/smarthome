package com.smarthome.view.window;

import com.smarthome.event.DeviceEvent;
import com.smarthome.event.EventBus;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * ПАТТЕРН: Observer (подписка на EventBus)
 *
 * Причина: события системы (toggle, add, remove) нигде не видны — отладка затруднена.
 * Следствие: живой лог всех событий EventBus в реальном времени, в немодальном окне.
 *
 * Окно НЕ модальное: stage.initModality(Modality.NONE).
 * Подписывается на ВСЕ события через EventBus.subscribeAll().
 * При закрытии отписывается, чтобы не утекала память.
 */
public class LogWindow {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int MAX_ENTRIES = 500;

    private final Stage stage;
    private final EventBus eventBus;

    // Список записей лога (новые — сверху)
    private final ObservableList<LogEntry> logEntries = FXCollections.observableArrayList();

    // Подписчик — сохраняем ссылку для отписки
    private final Consumer<DeviceEvent> subscriber;

    // UI
    private ListView<LogEntry> logListView;
    private CheckBox chkAutoScroll;
    private Label countLabel;
    private boolean paused = false;

    /**
     * Причина: EventBus передаётся снаружи — окно не создаёт свою шину.
     * Следствие: один EventBus на приложение; LogWindow видит все события.
     */
    public LogWindow(EventBus eventBus) {
        this.eventBus = eventBus;
        // Причина: лямбда сохранена — иначе отписаться невозможно (GC удалит)
        // Следствие: subscriber живёт пока жив LogWindow
        this.subscriber = this::handleEvent;
        buildUI();
        // Подписываемся на все события сразу
        eventBus.subscribeAll(subscriber);
        // Отписываемся при закрытии окна
        stage.setOnCloseRequest(e -> eventBus.unsubscribeAll(subscriber));
    }

    private void buildUI() {
        stage.setTitle("Журнал событий");
        // Причина: немодальное окно не мешает работе в главном окне
        stage.initModality(Modality.NONE);
        stage.setResizable(true);

        VBox root = new VBox(8);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #1a1a1a;");

        // --- Заголовок ---
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label("Журнал событий");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");
        countLabel = new Label("0 событий");
        countLabel.setStyle("-fx-text-fill: #707070; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(title, spacer, countLabel);

        // --- Панель управления ---
        HBox controlBox = new HBox(8);
        controlBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        chkAutoScroll = new CheckBox("Авто-прокрутка");
        chkAutoScroll.setSelected(true);
        chkAutoScroll.setStyle("-fx-text-fill: #aaa;");

        Button btnPause = new Button("Пауза");
        btnPause.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-cursor: hand;");
        btnPause.setOnAction(e -> {
            paused = \!paused;
            btnPause.setText(paused ? "Продолжить" : "Пауза");
            btnPause.setStyle(paused
                ? "-fx-background-color: #a05000; -fx-text-fill: white; -fx-cursor: hand;"
                : "-fx-background-color: #555; -fx-text-fill: white; -fx-cursor: hand;");
        });

        Button btnClear = new Button("Очистить");
        btnClear.setStyle("-fx-background-color: #5a2a2a; -fx-text-fill: #ffaaaa; -fx-cursor: hand;");
        btnClear.setOnAction(e -> { logEntries.clear(); updateCount(); });

        controlBox.getChildren().addAll(chkAutoScroll, btnPause, btnClear);

        // --- Фильтр по типу ---
        HBox filterBox = new HBox(8);
        filterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblFilter = new Label("Фильтр:");
        lblFilter.setStyle("-fx-text-fill: #777;");
        TextField filterField = new TextField();
        filterField.setPromptText("Фильтр по типу события...");
        filterField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ddd; -fx-prompt-text-fill: #555;");
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterBox.getChildren().addAll(lblFilter, filterField);

        // --- Список событий ---
        logListView = new ListView<>(logEntries);
        logListView.setStyle("-fx-background-color: #111; -fx-background: #111;");
        VBox.setVgrow(logListView, Priority.ALWAYS);
        logListView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(LogEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                if (empty || entry == null) { setText(null); setStyle(""); return; }

                // Фильтрация по введённому тексту
                String filter = filterField.getText().trim().toLowerCase();
                if (\!filter.isEmpty() && \!entry.text.toLowerCase().contains(filter)) {
                    setText(null); setStyle("-fx-background-color: #111;"); return;
                }

                setText(entry.text);
                // Цветовая кодировка по типу события
                String color = colorForType(entry.eventType);
                setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; " +
                         "-fx-font-family: monospace; -fx-background-color: #111;");
            }
        });

        // Обновляем список при изменении фильтра
        filterField.textProperty().addListener((obs, o, n) -> logListView.refresh());

        root.getChildren().addAll(headerBox, controlBox, filterBox, logListView);

        Scene scene = new Scene(root, 620, 500);
        stage.setScene(scene);
        stage.setMinWidth(400);
        stage.setMinHeight(300);
    }

    // === Обработка событий ===

    /**
     * Причина: событие может прийти из фонового потока.
     * Следствие: Platform.runLater() гарантирует обновление UI в FX-потоке.
     */
    private void handleEvent(DeviceEvent event) {
        if (paused) return;
        String timeStr = event.getTimestamp().format(DT_FMT);
        String text = String.format("[%s] %-25s | target=%-10s%s",
                timeStr,
                event.getType(),
                event.getTargetId(),
                event.getData() \!= null ? " | data=" + event.getData() : "");

        Platform.runLater(() -> {
            logEntries.add(0, new LogEntry(event.getType(), text));
            // Ограничиваем размер лога
            if (logEntries.size() > MAX_ENTRIES) {
                logEntries.remove(logEntries.size() - 1);
            }
            updateCount();
            // Авто-прокрутка (к первому элементу, т.к. добавляем сверху)
            if (chkAutoScroll.isSelected() && \!logEntries.isEmpty()) {
                logListView.scrollTo(0);
            }
        });
    }

    private void updateCount() {
        countLabel.setText(logEntries.size() + " событий");
    }

    /**
     * Причина: разные типы событий хочется различать визуально.
     * Следствие: цвет строки зависит от типа события.
     */
    private String colorForType(String type) {
        if (type == null) return "#aaaaaa";
        return switch (type) {
            case "device_added"         -> "#80e080";
            case "device_removed"       -> "#e08080";
            case "device_toggled"       -> "#80d0ff";
            case "device_param_changed" -> "#ffd080";
            case "room_added"           -> "#a0e0a0";
            case "room_removed"         -> "#e0a0a0";
            case "automation_applied"   -> "#d0a0ff";
            default                     -> "#aaaaaa";
        };
    }

    // === Публичный API ===

    public void show() {
        stage.show();
        stage.toFront();
    }

    public boolean isShowing() { return stage.isShowing(); }

    // === Внутренние классы ===

    /** Запись лога для хранения типа события и текста */
    private record LogEntry(String eventType, String text) {}
}
