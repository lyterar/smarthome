package com.smarthome.controller;

import com.smarthome.event.DeviceEvent;
import com.smarthome.model.device.Device;
import com.smarthome.model.house.House;
import com.smarthome.model.room.Room;
import com.smarthome.pattern.behavioral.AutomationStrategy;
import com.smarthome.pattern.behavioral.CommandHistory;
import com.smarthome.pattern.behavioral.ToggleDeviceCommand;
import com.smarthome.pattern.structural.SmartHomeFacade;
import com.smarthome.service.AutomationService;
import com.smarthome.service.HouseSaveService;
import com.smarthome.pattern.creational.SmartHomeEngine;
import com.smarthome.view.component.Room3DView;
import com.smarthome.view.component.RoomCanvas;
import com.smarthome.view.dialog.AddDeviceDialog;
import com.smarthome.view.dialog.AddRoomDialog;
import com.smarthome.view.dialog.CreateGroupDialog;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Главный контроллер приложения.
 * Связывает UI (FXML) с логикой (Facade).
 */
public class MainController {

    // --- FXML элементы ---
    @FXML private Room3DView room3DView;
    @FXML private RoomCanvas roomCanvas;
    @FXML private ListView<Room> roomListView;
    @FXML private ListView<Device> deviceListView;
    @FXML private Label statusLabel;
    @FXML private Label houseInfoLabel;
    @FXML private ComboBox<AutomationStrategy> automationCombo;
    @FXML private Button undoButton;
    @FXML private Button redoButton;

    // --- Сервисы ---
    private SmartHomeFacade facade;
    private AutomationService automationService;
    private CommandHistory commandHistory;
    private HouseSaveService saveService;

    // --- Данные ---
    private ObservableList<Room> roomItems = FXCollections.observableArrayList();
    private ObservableList<Device> deviceItems = FXCollections.observableArrayList();
    private Room selectedRoom;

    @FXML
    public void initialize() {
        facade = new SmartHomeFacade();
        automationService = new AutomationService();
        commandHistory = new CommandHistory();
        saveService = new HouseSaveService(SmartHomeEngine.getInstance().getDeviceFactory());

        setupRoomList();
        setupDeviceList();
        setupAutomation();
        setupEventListeners();
        updateStatus("Готово");
        refreshAll();
    }

    // === Настройка UI ===

    private void setupRoomList() {
        roomListView.setItems(roomItems);
        roomListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                setText(empty || room == null ? null : room.toString());
            }
        });
        roomListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> onRoomSelected(newVal));
    }

    private void setupDeviceList() {
        deviceListView.setItems(deviceItems);
        deviceListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        deviceListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Device device, boolean empty) {
                super.updateItem(device, empty);
                setText(empty || device == null ? null : device.toString());
            }
        });
    }

    private void setupAutomation() {
        automationCombo.getItems().addAll(automationService.getAvailableStrategies());
        automationCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AutomationStrategy s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : s.getName());
            }
        });
        automationCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AutomationStrategy s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "Выбрать режим" : s.getName());
            }
        });
    }

    private void setupEventListeners() {
        // Подписываемся на все события для обновления UI
        facade.getEventBus().subscribeAll(event ->
                Platform.runLater(this::refreshAll));
    }

    // === Обработчики кнопок (вызываются из FXML) ===

    @FXML
    private void onAddRoom() {
        AddRoomDialog dialog = new AddRoomDialog();
        dialog.showAndWait().ifPresent(result -> {
            facade.createRoom(result.name(), result.type(),
                    50 + roomItems.size() * 30, 50 + roomItems.size() * 30);
            updateStatus("Комната добавлена: " + result.name());
        });
    }

    @FXML
    private void onRemoveRoom() {
        Room room = roomListView.getSelectionModel().getSelectedItem();
        if (room == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить комнату \"" + room.getName() + "\"?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                facade.removeRoom(room.getId());
                updateStatus("Комната удалена");
            }
        });
    }

    @FXML
    private void onAddDevice() {
        if (selectedRoom == null) {
            showWarning("Сначала выберите комнату");
            return;
        }

        AddDeviceDialog dialog = new AddDeviceDialog();
        dialog.showAndWait().ifPresent(result -> {
            facade.addDeviceToRoom(selectedRoom.getId(), result.name(), result.type());
            updateStatus("Устройство добавлено: " + result.name());
        });
    }

    @FXML
    private void onRemoveDevice() {
        Device device = deviceListView.getSelectionModel().getSelectedItem();
        if (device == null || selectedRoom == null) return;

        facade.removeDeviceFromRoom(selectedRoom.getId(), device.getId());
        updateStatus("Устройство удалено");
    }

    @FXML
    private void onToggleDevice() {
        Device device = deviceListView.getSelectionModel().getSelectedItem();
        if (device == null) return;

        ToggleDeviceCommand cmd = new ToggleDeviceCommand(device);
        commandHistory.executeCommand(cmd);
        facade.getEventBus().publish(new DeviceEvent("device_toggled", device.getId()));
        updateUndoRedo();
        updateStatus(device.getName() + " " + (device.isOn() ? "включено" : "выключено"));
    }

    @FXML
    private void onEnableLogging() {
        Device device = deviceListView.getSelectionModel().getSelectedItem();
        if (device == null) {
            showWarning("Сначала выберите устройство");
            return;
        }
        facade.enableLogging(device.getId());
        updateStatus("Логирование включено: " + device.getName());
    }

    @FXML
    private void onCreateGroup() {
        if (selectedRoom == null) {
            showWarning("Сначала выберите комнату");
            return;
        }
        if (selectedRoom.getDevices().isEmpty()) {
            showWarning("В комнате нет устройств для объединения");
            return;
        }

        CreateGroupDialog dialog = new CreateGroupDialog(selectedRoom.getDevices());
        dialog.showAndWait().ifPresent(result -> {
            Device group = facade.createDeviceGroup(
                    selectedRoom.getId(), result.groupName(), result.groupType(), result.deviceIds());
            updateStatus("Группа создана: " + group.getName() + " (" + result.deviceIds().size() + " устройств)");
        });
    }

    @FXML
    private void onSwitch2D() {
        if (room3DView.isFpsMode()) room3DView.exitFpsMode();
        room3DView.setVisible(false);
        roomCanvas.setVisible(true);
        roomCanvas.drawHouse(facade.getHouse());
        updateStatus("Режим: 2D план");
    }

    @FXML
    private void onSwitch3D() {
        if (room3DView.isFpsMode()) room3DView.exitFpsMode();
        roomCanvas.setVisible(false);
        room3DView.setVisible(true);
        room3DView.drawHouse(facade.getHouse());
        updateStatus("Режим: 3D — ЛКМ вращение, ПКМ панорама, колёсико zoom, перетащи устройство мышью");
    }

    @FXML
    private void onSwitchFps() {
        Room room = roomListView.getSelectionModel().getSelectedItem();
        if (room == null) {
            showWarning("Выберите комнату для входа в FPS режим");
            return;
        }
        roomCanvas.setVisible(false);
        room3DView.setVisible(true);
        room3DView.drawHouse(facade.getHouse()); // убедимся что модели актуальны
        room3DView.enterFpsMode(room);
        updateStatus("FPS: " + room.getName() + " | WASD движение, мышь взгляд, ESC выход");
    }

    @FXML
    private void onApplyAutomation() {
        AutomationStrategy strategy = automationCombo.getValue();
        if (strategy == null) {
            showWarning("Выберите режим автоматизации");
            return;
        }

        if (selectedRoom != null) {
            automationService.applyStrategy(strategy, selectedRoom);
            updateStatus(strategy.getName() + " применён к " + selectedRoom.getName());
        } else {
            automationService.applyToAll(strategy);
            updateStatus(strategy.getName() + " применён ко всему дому");
        }
    }

    @FXML
    private void onUndo() {
        if (commandHistory.undo()) {
            refreshAll();
            updateUndoRedo();
            updateStatus("Отменено");
        }
    }

    @FXML
    private void onRedo() {
        if (commandHistory.redo()) {
            refreshAll();
            updateUndoRedo();
            updateStatus("Повторено");
        }
    }

    @FXML
    private void onSaveHouse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить дом");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON", "*.json"));
        chooser.setInitialFileName("my_house.json");
        File file = chooser.showSaveDialog(room3DView.getScene().getWindow());
        if (file != null) {
            try {
                saveService.save(facade.getHouse(), file.toPath());
                updateStatus("Сохранено: " + file.getName());
            } catch (Exception e) {
                showWarning("Ошибка сохранения: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onLoadHouse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Загрузить дом");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file = chooser.showOpenDialog(room3DView.getScene().getWindow());
        if (file != null) {
            try {
                House house = saveService.load(file.toPath());
                SmartHomeEngine.getInstance().setHouse(house);
                refreshAll();
                updateStatus("Загружено: " + file.getName());
            } catch (Exception e) {
                showWarning("Ошибка загрузки: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    // === Внутренние методы ===

    private void onRoomSelected(Room room) {
        selectedRoom = room;
        deviceItems.clear();
        if (room != null) {
            deviceItems.addAll(room.getDevices());
            room3DView.highlightRoom(room);
        }
    }

    private void refreshAll() {
        roomItems.setAll(facade.getRooms());
        if (selectedRoom != null) {
            // Обновляем ссылку на комнату (могла пересоздаться)
            selectedRoom = facade.getHouse().findRoomById(selectedRoom.getId());
            if (selectedRoom != null) {
                deviceItems.setAll(selectedRoom.getDevices());
            } else {
                deviceItems.clear();
            }
        }
        if (!room3DView.isFpsMode()) room3DView.drawHouse(facade.getHouse());
        if (roomCanvas.isVisible()) roomCanvas.drawHouse(facade.getHouse());
        houseInfoLabel.setText(facade.getHouseSummary());
        updateUndoRedo();
    }

    private void updateUndoRedo() {
        undoButton.setDisable(!commandHistory.canUndo());
        redoButton.setDisable(!commandHistory.canRedo());
        undoButton.setTooltip(new Tooltip(commandHistory.getUndoDescription()));
        redoButton.setTooltip(new Tooltip(commandHistory.getRedoDescription()));
    }

    private void updateStatus(String text) {
        statusLabel.setText(text);
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.showAndWait();
    }
}
