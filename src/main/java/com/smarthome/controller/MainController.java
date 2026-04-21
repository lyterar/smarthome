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
import com.smarthome.service.ThemeService;
import com.smarthome.pattern.creational.SmartHomeEngine;
import com.smarthome.view.component.Room3DView;
import com.smarthome.view.component.RoomCanvas;
import com.smarthome.view.dialog.AddDeviceDialog;
import com.smarthome.view.dialog.AddRoomDialog;
import com.smarthome.view.dialog.CreateGroupDialog;
import com.smarthome.view.window.AutomationWindow;
import com.smarthome.view.window.DeviceDetailWindow;
import com.smarthome.view.window.LogWindow;

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
 *
 * Шаг 3: добавлена поддержка смены тем через ThemeService (паттерн Strategy).
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

    // --- Кнопки тем (Шаг 3) ---
    // Причина: нужно визуально выделять активную тему
    // Следствие: при смене темы CSS-класс кнопок обновляется
    @FXML private Button btnThemeDark;
    @FXML private Button btnThemeLight;
    @FXML private Button btnThemeBlue;

    // --- Сервисы ---
    private SmartHomeFacade facade;
    private AutomationService automationService;
    private CommandHistory commandHistory;
    private HouseSaveService saveService;
    private ThemeService themeService;

    // --- Данные ---
    private ObservableList<Room> roomItems = FXCollections.observableArrayList();
    private ObservableList<Device> deviceItems = FXCollections.observableArrayList();
    private Room selectedRoom;

    // --- Немодальные окна (Шаг 4) ---
    // Причина: создаём один раз и переиспользуем — не создаём при каждом открытии
    // Следствие: окна сохраняют историю и состояние между открытиями
    private AutomationWindow automationWindow;
    private DeviceDetailWindow deviceDetailWindow;
    private LogWindow logWindow;

    @FXML
    public void initialize() {
        facade = new SmartHomeFacade();
        automationService = new AutomationService();
        commandHistory = new CommandHistory();
        saveService = new HouseSaveService(SmartHomeEngine.getInstance().getDeviceFactory());
        themeService = ThemeService.getInstance();

        setupRoomList();
        setupDeviceList();
        setupAutomation();
        setupEventListeners();
        initWindows();
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

    /**
     * Инициализируем немодальные окна (Шаг 4).
     * Причина: окна создаются один раз при старте.
     * Следствие: история и состояние окон сохраняются между открытиями.
     */
    private void initWindows() {
        automationWindow = new AutomationWindow(facade, automationService, commandHistory);
        deviceDetailWindow = new DeviceDetailWindow(facade);
        logWindow = new LogWindow(facade.getEventBus());
    }

    private void setupDeviceList() {
        deviceListView.setItems(deviceItems);
        // Причина: детали не видны в списке → двойной клик открывает DeviceDetailWindow
        // Следствие: немодальное окно с параметрами и управлением устройством
        deviceListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Device selected = deviceListView.getSelectionModel().getSelectedItem();
                if (selected != null && deviceDetailWindow != null) {
                    deviceDetailWindow.showDevice(selected);
                }
            }
        });
        deviceListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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

    // =========================================================
    //  Смена темы (Шаг 3 — паттерн Strategy)
    // =========================================================

    /**
     * Применяет тему и обновляет кнопки-индикаторы.
     *
     * Причина: повторяющийся код в каждом обработчике кнопки темы.
     * Следствие: единый метод applyTheme() — кнопки сбрасываются,
     *            нужная помечается как активная, SubScene получает новый фон.
     */
    private void applyTheme(ThemeService.Theme theme) {
        // Применяем CSS через ThemeService (Strategy)
        themeService.applyTheme(room3DView.getScene(), theme);

        // Синхронизируем фон 3D-сцены с новой темой
        // Причина: SubScene.fill не управляется CSS, нужен явный вызов
        // Следствие: 3D фон и UI-фон визуально совпадают
        room3DView.setSubSceneBackground(themeService.getSubSceneColor());

        // Обновляем стили кнопок-индикаторов
        btnThemeDark.getStyleClass().setAll(theme == ThemeService.Theme.DARK  ? "btn-theme-active" : "btn-theme");
        btnThemeLight.getStyleClass().setAll(theme == ThemeService.Theme.LIGHT ? "btn-theme-active" : "btn-theme");
        btnThemeBlue.getStyleClass().setAll(theme == ThemeService.Theme.BLUE  ? "btn-theme-active" : "btn-theme");

        updateStatus("Тема: " + themeService.getCurrentThemeName());
    }

    @FXML
    private void onThemeDark() {
        applyTheme(ThemeService.Theme.DARK);
    }

    @FXML
    private void onThemeLight() {
        applyTheme(ThemeService.Theme.LIGHT);
    }

    @FXML
    private void onThemeBlue() {
        applyTheme(ThemeService.Theme.BLUE);
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

        // Причина: toggle в 2D/3D виде не отражается без перерисовки.
        // Следствие: refreshAll() обновит 3D-модели; toggle-анимация
        //            запускается на соответствующей Group в Room3DView.
        room3DView.playToggleAnimation(device.getId(), device.isOn());
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

        if (selectedRoom \!= null) {
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
        if (file \!= null) {
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
        if (file \!= null) {
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
    /**
     * Открыть окно автоматизации (Шаг 4).
     * Причина: авто-режимы тесно сидят в главном окне.
     * Следствие: отдельное немодальное окно с историей команд.
     */
    @FXML
    private void onOpenAutomationWindow() {
        if (automationWindow != null) automationWindow.show();
    }

    /**
     * Открыть окно журнала событий (Шаг 4).
     * Причина: события EventBus нигде не отображаются — отладка затруднена.
     * Следствие: живой лог всех событий системы в реальном времени.
     */
    @FXML
    private void onOpenLogWindow() {
        if (logWindow != null) logWindow.show();
    }

    private void onExit() {
        Platform.exit();
    }

    // === Внутренние методы ===

    private void onRoomSelected(Room room) {
        selectedRoom = room;
        deviceItems.clear();
        if (room \!= null) {
            deviceItems.addAll(room.getDevices());
            room3DView.highlightRoom(room);
        }
    }

    private void refreshAll() {
        roomItems.setAll(facade.getRooms());
        if (selectedRoom \!= null) {
            // Обновляем ссылку на комнату (могла пересоздаться)
            selectedRoom = facade.getHouse().findRoomById(selectedRoom.getId());
            if (selectedRoom \!= null) {
                deviceItems.setAll(selectedRoom.getDevices());
            } else {
                deviceItems.clear();
            }
        }
        if (\!room3DView.isFpsMode()) room3DView.drawHouse(facade.getHouse());
        if (roomCanvas.isVisible()) roomCanvas.drawHouse(facade.getHouse());
        houseInfoLabel.setText(facade.getHouseSummary());
        updateUndoRedo();
    }

    private void updateUndoRedo() {
        undoButton.setDisable(\!commandHistory.canUndo());
        redoButton.setDisable(\!commandHistory.canRedo());
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
