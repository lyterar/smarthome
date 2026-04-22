package com.smarthome.view.window;

import com.smarthome.event.DeviceEvent;
import com.smarthome.model.device.Device;
import com.smarthome.model.device.DeviceType;
import com.smarthome.model.room.Room;
import com.smarthome.pattern.behavioral.CommandHistory;
import com.smarthome.pattern.behavioral.ToggleDeviceCommand;
import com.smarthome.pattern.structural.SmartHomeFacade;
import com.smarthome.view.dialog.AddDeviceDialog;

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

import java.util.function.Consumer;

/**
 * Окно управления устройствами.
 *
 * Причина: список устройств и кнопки управления занимали значительную часть
 *          главного окна и не масштабировались при увеличении числа устройств.
 * Следствие: отдельное немодальное окно с полноценным управлением:
 *            добавление, удаление, toggle, логирование, двойной клик → детали.
 *
 * GoF паттерн:
 *   Facade — все команды идут через SmartHomeFacade.
 *   Command — toggle выполняется через ToggleDeviceCommand + CommandHistory.
 *   Observer — окно подписано на selectedRoom через setRoom().
 */
public class DeviceWindow {

    private final Stage              stage;
    private final SmartHomeFacade    facade;
    private final CommandHistory     commandHistory;
    private final DeviceDetailWindow detailWindow;

    private final ObservableList<Device> deviceItems = FXCollections.observableArrayList();

    // Текущая комната — устанавливается из MainController при выборе комнаты
    private Room currentRoom;

    // Callback для уведомления MainController о toggle (для анимации)
    private Consumer<DeviceToggleEvent> onToggle;

    // UI элементы
    private ListView<Device> deviceListView;
    private Label            roomLabel;
    private Label            statusLabel;
    private Button           btnToggle;
    private Button           btnRemove;
    private Button           btnLogging;

    public record DeviceToggleEvent(String deviceId, boolean isOn) {}

    /**
     * Причина: DeviceDetailWindow передаётся снаружи — единственный экземпляр на приложение.
     * Следствие: детали устройства открываются в одном окне, а не создаются заново.
     */
    public DeviceWindow(SmartHomeFacade facade,
                        CommandHistory commandHistory,
                        DeviceDetailWindow detailWindow) {
        this.facade         = facade;
        this.commandHistory = commandHistory;
        this.detailWindow   = detailWindow;
        this.stage          = new Stage();
        buildUI();
    }

    private void buildUI() {
        stage.setTitle("Устройства");
        stage.initModality(Modality.NONE);
        stage.setResizable(true);
        stage.setMinWidth(340);
        stage.setMinHeight(400);

        // --- Заголовок с названием комнаты ---
        Label title = new Label("Устройства");
        title.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#e0e0e0;");

        roomLabel = new Label("Комната не выбрана");
        roomLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#94a3b8;");

        VBox headerBox = new VBox(2, title, roomLabel);
        headerBox.setPadding(new Insets(10, 12, 10, 12));
        headerBox.setStyle("-fx-background-color:#1a2035;");

        // --- Список устройств ---
        deviceListView = new ListView<>(deviceItems);
        deviceListView.setStyle("-fx-background-color:#1e293b; -fx-border-color:#334155;");
        deviceListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Device d, boolean empty) {
                super.updateItem(d, empty);
                if (empty || d == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // Причина: иконка типа + статус "вкл/выкл" делает список информативным
                    String icon = iconForType(d.getType());
                    String status = d.isOn() ? "✅" : "⬜";
                    setText(icon + " " + d.getName() + "  " + status);
                    setStyle("-fx-text-fill:#e2e8f0; -fx-font-size:13px;" +
                             "-fx-background-color:transparent;");
                }
            }
        });

        // Двойной клик → окно деталей
        // Причина: список показывает краткую информацию; детали нужны отдельно
        deviceListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Device sel = deviceListView.getSelectionModel().getSelectedItem();
                if (sel \!= null && detailWindow \!= null) {
                    detailWindow.showDevice(sel);
                }
            }
        });
        VBox.setVgrow(deviceListView, Priority.ALWAYS);

        // --- Кнопки действий ---
        Button btnAdd  = makeBtn("+ Добавить", "#2563eb");
        btnRemove      = makeBtn("Удалить",    "#dc2626");
        btnToggle      = makeBtn("Вкл / Выкл", "#d97706");
        btnLogging     = makeBtn("Логирование","#4b5563");
        Button btnDetail = makeBtn("Детали",   "#7c3aed");

        btnAdd.setOnAction(e    -> onAddDevice());
        btnRemove.setOnAction(e -> onRemoveDevice());
        btnToggle.setOnAction(e -> onToggleDevice());
        btnLogging.setOnAction(e -> onLogging());
        btnDetail.setOnAction(e  -> onOpenDetail());

        HBox row1 = makeRow(btnAdd, btnRemove);
        HBox row2 = makeRow(btnToggle, btnDetail);
        HBox row3 = makeRow(btnLogging);

        // Подсказка о двойном клике
        Label hint = new Label("Двойной клик на устройство → детали");
        hint.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");
        hint.setAlignment(Pos.CENTER);

        // --- Статусная строка ---
        statusLabel = new Label("Выберите комнату в главном окне");
        statusLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color:#0f172a;");

        VBox content = new VBox(8,
            deviceListView,
            new Separator(),
            row1, row2, row3,
            hint
        );
        content.setPadding(new Insets(10, 12, 10, 12));
        content.setStyle("-fx-background-color:#111827;");

        VBox root = new VBox(headerBox, content, statusBar);
        VBox.setVgrow(content, Priority.ALWAYS);
        root.setStyle("-fx-background-color:#111827;");

        stage.setScene(new Scene(root, 360, 520));
    }

    // ─── Обработчики ─────────────────────────────────────────────────────────

    private void onAddDevice() {
        if (currentRoom == null) {
            setStatus("Выберите комнату в главном окне");
            return;
        }
        AddDeviceDialog dlg = new AddDeviceDialog();
        dlg.showAndWait().ifPresent(r -> {
            facade.addDeviceToRoom(currentRoom.getId(), r.name(), r.type());
            setStatus("Добавлено: " + r.name());
            refreshDevices();
        });
    }

    private void onRemoveDevice() {
        Device sel = deviceListView.getSelectionModel().getSelectedItem();
        if (sel == null || currentRoom == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить устройство \"" + sel.getName() + "\"?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                facade.removeDeviceFromRoom(currentRoom.getId(), sel.getId());
                setStatus("Удалено: " + sel.getName());
                refreshDevices();
            }
        });
    }

    private void onToggleDevice() {
        Device sel = deviceListView.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Выберите устройство"); return; }

        ToggleDeviceCommand cmd = new ToggleDeviceCommand(sel);
        commandHistory.executeCommand(cmd);
        facade.getEventBus().publish(new DeviceEvent("device_toggled", sel.getId()));

        // Уведомляем главный контроллер для анимации в ViewWindow
        if (onToggle \!= null) {
            onToggle.accept(new DeviceToggleEvent(sel.getId(), sel.isOn()));
        }
        setStatus(sel.getName() + " " + (sel.isOn() ? "включено" : "выключено"));
        refreshDevices();
    }

    private void onLogging() {
        Device sel = deviceListView.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Выберите устройство"); return; }
        facade.enableLogging(sel.getId());
        setStatus("Логирование включено: " + sel.getName());
    }

    private void onOpenDetail() {
        Device sel = deviceListView.getSelectionModel().getSelectedItem();
        if (sel == null) { setStatus("Выберите устройство"); return; }
        if (detailWindow \!= null) detailWindow.showDevice(sel);
    }

    // ─── Публичный API ────────────────────────────────────────────────────────

    public void show() {
        stage.show();
        stage.toFront();
    }

    /**
     * Вызывается из MainController при выборе комнаты.
     * Причина: список устройств зависит от выбранной комнаты.
     * Следствие: окно автоматически показывает устройства новой комнаты.
     */
    public void setRoom(Room room) {
        this.currentRoom = room;
        if (room \!= null) {
            roomLabel.setText("Комната: " + room.getName());
            stage.setTitle("Устройства — " + room.getName());
        } else {
            roomLabel.setText("Комната не выбрана");
            stage.setTitle("Устройства");
        }
        refreshDevices();
    }

    /** Обновить список устройств из текущей комнаты. */
    public void refreshDevices() {
        Platform.runLater(() -> {
            deviceItems.clear();
            if (currentRoom \!= null) {
                // Получаем свежую ссылку на комнату
                Room fresh = facade.getHouse().findRoomById(currentRoom.getId());
                if (fresh \!= null) {
                    currentRoom = fresh;
                    deviceItems.addAll(fresh.getDevices());
                }
            }
        });
    }

    /** Установить callback для уведомления о toggle (для анимаций). */
    public void setOnToggle(Consumer<DeviceToggleEvent> callback) {
        this.onToggle = callback;
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    private void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private Button makeBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white;" +
                     "-fx-border-radius:4; -fx-background-radius:4;" +
                     "-fx-padding:6 10 6 10; -fx-cursor:hand;");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private HBox makeRow(Button... buttons) {
        HBox row = new HBox(6, buttons);
        for (Button b : buttons) HBox.setHgrow(b, Priority.ALWAYS);
        return row;
    }

    private String iconForType(DeviceType type) {
        if (type == null) return "📦";
        return switch (type) {
            case LIGHT       -> "💡";
            case THERMOSTAT  -> "🌡";
            case CAMERA      -> "📷";
            case LOCK        -> "🔒";
            case SENSOR      -> "📡";
            case SPEAKER     -> "🔊";
            default          -> "📦";
        };
    }

    public Stage getStage() { return stage; }
}
