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
import com.smarthome.view.window.AutomationWindow;
import com.smarthome.view.window.DeviceDetailWindow;
import com.smarthome.view.window.DeviceWindow;
import com.smarthome.view.window.LogWindow;
import com.smarthome.view.window.ViewWindow;
import com.smarthome.view.dialog.AddRoomDialog;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Главный контроллер — роль «диспетчера».
 *
 * Причина: в одном окне было всё — комнаты, устройства, автоматизация, 3D.
 * Следствие: главное окно стало «диспетчером», каждая функция — в своём окне:
 *   ViewWindow       — 2D/3D/FPS просмотр
 *   DeviceWindow     — управление устройствами
 *   AutomationWindow — стратегии автоматизации + история
 *   LogWindow        — живой лог EventBus
 *   DeviceDetailWindow — детали устройства по двойному клику
 *
 * GoF: Facade — все операции с моделью идут через SmartHomeFacade.
 */
public class MainController {

    // --- FXML: только то, что остаётся в главном окне ---
    @FXML private ListView<Room> roomListView;
    @FXML private Label          statusLabel;
    @FXML private Label          houseInfoLabel;
    @FXML private Button         btnThemeDark;
    @FXML private Button         btnThemeLight;
    @FXML private Button         btnThemeBlue;
    @FXML private Button         undoButton;
    @FXML private Button         redoButton;

    // --- Сервисы ---
    private SmartHomeFacade    facade;
    private AutomationService  automationService;
    private CommandHistory     commandHistory;
    private HouseSaveService   saveService;
    private ThemeService       themeService;

    // --- Данные ---
    private final ObservableList<Room> roomItems = FXCollections.observableArrayList();
    private Room selectedRoom;

    // ─── Пять немодальных окон ───────────────────────────────────────────────
    // Причина: создаём один раз — окна сохраняют историю и состояние
    // Следствие: каждое окно живёт параллельно, без блокировки главного
    private ViewWindow         viewWindow;
    private DeviceWindow       deviceWindow;
    private AutomationWindow   automationWindow;
    private LogWindow          logWindow;
    private DeviceDetailWindow deviceDetailWindow;

    @FXML
    public void initialize() {
        facade            = new SmartHomeFacade();
        automationService = new AutomationService();
        commandHistory    = new CommandHistory();
        saveService       = new HouseSaveService(SmartHomeEngine.getInstance().getDeviceFactory());
        themeService      = ThemeService.getInstance();

        setupRoomList();
        initWindows();
        setupEventListeners();

        updateStatus("Умный Дом готов — откройте нужные окна");
        refreshAll();
    }

    // ─── Инициализация пяти окон ──────────────────────────────────────────────

    /**
     * Причина: все окна создаются один раз при старте приложения.
     * Следствие: состояние (история лога, история команд) сохраняется
     *            между открытиями окон.
     */
    private void initWindows() {
        // 1. Окно просмотра (2D/3D/FPS) — основная рабочая область
        viewWindow = new ViewWindow(facade, facade.getEventBus());

        // 2. Окно деталей устройства — создаём до DeviceWindow (передаём внутрь)
        deviceDetailWindow = new DeviceDetailWindow(facade);

        // 3. Окно устройств — управление, toggle, добавление/удаление
        deviceWindow = new DeviceWindow(facade, commandHistory, deviceDetailWindow);

        // Причина: toggle из DeviceWindow должен запускать анимацию в ViewWindow
        // Следствие: колбэк передаёт deviceId и статус в ViewWindow.playToggleAnimation()
        deviceWindow.setOnToggle(evt ->
            Platform.runLater(() -> viewWindow.playToggleAnimation(evt.deviceId(), evt.isOn()))
        );

        // 4. Окно автоматизации — стратегии + история команд
        automationWindow = new AutomationWindow(facade, automationService, commandHistory);

        // 5. Окно журнала событий — живой лог EventBus
        logWindow = new LogWindow(facade.getEventBus());

        // Сразу открываем окно просмотра — оно главное
        Platform.runLater(() -> viewWindow.show());
    }

    // ─── Настройка главного окна ──────────────────────────────────────────────

    private void setupRoomList() {
        roomListView.setItems(roomItems);
        roomListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                setText(empty || room == null ? null :
                    "🏠 " + room.getName() + "  [" + room.getDevices().size() + "]");
            }
        });
        // При выборе комнаты — обновляем все окна
        // Причина: DeviceWindow и ViewWindow зависят от выбранной комнаты
        roomListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, newRoom) -> onRoomSelected(newRoom)
        );
    }

    private void setupEventListeners() {
        // Причина: любое изменение модели (из любого окна) должно обновить список комнат
        facade.getEventBus().subscribeAll(e ->
            Platform.runLater(this::refreshAll)
        );
    }

    // ─── Выбор комнаты ────────────────────────────────────────────────────────

    private void onRoomSelected(Room room) {
        selectedRoom = room;
        // Синхронизируем все окна с выбранной комнатой
        viewWindow.setCurrentRoom(room);
        deviceWindow.setRoom(room);
        if (room \!= null) {
            updateStatus("Выбрана: " + room.getName() +
                         " (" + room.getDevices().size() + " устройств)");
        }
    }

    // ─── Кнопки открытия окон (FXML) ─────────────────────────────────────────

    /** Причина: вид нужен отдельно — пользователь хочет видеть 3D и управлять. */
    @FXML
    private void onOpenViewWindow() {
        viewWindow.show();
    }

    /** Причина: устройства требуют своего пространства для полного списка. */
    @FXML
    private void onOpenDeviceWindow() {
        deviceWindow.show();
    }

    /**
     * Причина: автоматизация отдельно от главного окна — не мешает повседневной работе.
     * Следствие: окно с историей применённых стратегий всегда под рукой.
     */
    @FXML
    private void onOpenAutomationWindow() {
        automationWindow.show();
    }

    /**
     * Причина: журнал событий нужен для отладки и мониторинга — не для ежедневной работы.
     * Следствие: живой лог в отдельном окне, не засоряет главное.
     */
    @FXML
    private void onOpenLogWindow() {
        logWindow.show();
    }

    // ─── Управление комнатами (остаётся в главном окне) ──────────────────────

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

    // ─── Undo / Redo ──────────────────────────────────────────────────────────

    @FXML
    private void onUndo() {
        if (commandHistory.undo()) {
            refreshAll();
            deviceWindow.refreshDevices();
            updateStatus("Отменено");
        }
        updateUndoRedo();
    }

    @FXML
    private void onRedo() {
        if (commandHistory.redo()) {
            refreshAll();
            deviceWindow.refreshDevices();
            updateStatus("Повторено");
        }
        updateUndoRedo();
    }

    // ─── Сохранение / Загрузка ────────────────────────────────────────────────

    @FXML
    private void onSaveHouse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить дом");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        chooser.setInitialFileName("my_house.json");
        File file = chooser.showSaveDialog(
            roomListView.getScene() \!= null ? roomListView.getScene().getWindow() : null);
        if (file \!= null) {
            try {
                saveService.save(facade.getHouse(), file.toPath());
                updateStatus("Сохранено: " + file.getName());
            } catch (Exception e) {
                new Alert(Alert.AlertType.WARNING, "Ошибка: " + e.getMessage()).showAndWait();
            }
        }
    }

    @FXML
    private void onLoadHouse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Загрузить дом");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file = chooser.showOpenDialog(
            roomListView.getScene() \!= null ? roomListView.getScene().getWindow() : null);
        if (file \!= null) {
            try {
                House house = saveService.load(file.toPath());
                SmartHomeEngine.getInstance().setHouse(house);
                refreshAll();
                updateStatus("Загружено: " + file.getName());
            } catch (Exception e) {
                new Alert(Alert.AlertType.WARNING, "Ошибка: " + e.getMessage()).showAndWait();
            }
        }
    }

    // ─── Темы (Strategy паттерн) ──────────────────────────────────────────────

    /**
     * Причина: одна точка входа для смены темы — не дублировать код.
     * Следствие: тема применяется к главному окну и к 3D-фону ViewWindow.
     */
    private void applyTheme(ThemeService.Theme theme) {
        if (roomListView.getScene() \!= null) {
            themeService.applyTheme(roomListView.getScene(), theme);
        }
        // Синхронизировать фон 3D-сцены в ViewWindow
        viewWindow.setSubSceneBackground(themeService.getSubSceneColor());

        btnThemeDark.getStyleClass().setAll(
            theme == ThemeService.Theme.DARK  ? "btn-theme-active" : "btn-theme");
        btnThemeLight.getStyleClass().setAll(
            theme == ThemeService.Theme.LIGHT ? "btn-theme-active" : "btn-theme");
        btnThemeBlue.getStyleClass().setAll(
            theme == ThemeService.Theme.BLUE  ? "btn-theme-active" : "btn-theme");
        updateStatus("Тема: " + themeService.getCurrentThemeName());
    }

    @FXML private void onThemeDark()  { applyTheme(ThemeService.Theme.DARK);  }
    @FXML private void onThemeLight() { applyTheme(ThemeService.Theme.LIGHT); }
    @FXML private void onThemeBlue()  { applyTheme(ThemeService.Theme.BLUE);  }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    private void refreshAll() {
        roomItems.setAll(facade.getRooms());
        if (selectedRoom \!= null) {
            selectedRoom = facade.getHouse().findRoomById(selectedRoom.getId());
        }
        houseInfoLabel.setText(facade.getHouseSummary());
        updateUndoRedo();
    }

    private void updateUndoRedo() {
        if (undoButton \!= null) undoButton.setDisable(\!commandHistory.canUndo());
        if (redoButton \!= null) redoButton.setDisable(\!commandHistory.canRedo());
    }

    private void updateStatus(String text) {
        if (statusLabel \!= null) statusLabel.setText(text);
    }
}
