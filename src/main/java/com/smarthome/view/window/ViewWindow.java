package com.smarthome.view.window;

import com.smarthome.event.EventBus;
import com.smarthome.model.room.Room;
import com.smarthome.pattern.structural.SmartHomeFacade;
import com.smarthome.view.component.Room3DView;
import com.smarthome.view.component.RoomCanvas;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Окно просмотра — 2D/3D/FPS вид в отдельном немодальном окне.
 *
 * Причина: 2D/3D вид занимал половину главного окна — перегружено.
 * Следствие: вид вынесен в отдельное окно (Modality.NONE); можно держать
 *            на втором мониторе и работать с главным окном параллельно.
 *
 * GoF: Facade — все обращения к модели идут через SmartHomeFacade.
 */
public class ViewWindow {

    private final Stage           stage;
    private final SmartHomeFacade facade;
    private final Room3DView      room3DView;
    private final RoomCanvas      roomCanvas;
    private final Label           statusLabel;

    private Room    currentRoom;
    private final Button btn2D;
    private final Button btn3D;
    private final Button btnFPS;

    public ViewWindow(SmartHomeFacade facade, EventBus eventBus) {
        this.facade      = facade;
        this.room3DView  = new Room3DView();
        this.roomCanvas  = new RoomCanvas();
        this.statusLabel = new Label("Режим: 3D");
        this.stage       = new Stage();
        this.btn2D  = new Button("2D");
        this.btn3D  = new Button("3D");
        this.btnFPS = new Button("FPS");
        buildUI();
        // Причина: при изменении модели из любого окна — вид сам обновляется
        eventBus.subscribeAll(e -> Platform.runLater(this::refresh));
        switch3D();
    }

    private void buildUI() {
        stage.setTitle("Вид — Умный Дом 3D");
        stage.initModality(Modality.NONE);
        stage.setResizable(true);
        stage.setMinWidth(600);
        stage.setMinHeight(500);

        applyToolbarStyle(btn2D);
        applyToolbarStyle(btn3D);
        applyToolbarStyle(btnFPS);
        btn2D.setOnAction(e  -> switch2D());
        btn3D.setOnAction(e  -> switch3D());
        btnFPS.setOnAction(e -> switchFPS());

        Label titleLabel = new Label("Просмотр");
        titleLabel.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#e0e0e0;");

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(8, 12, 8, 12));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color:#1a2035;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        toolbar.getChildren().addAll(titleLabel, new Separator(), btn2D, btn3D, btnFPS);

        statusLabel.setStyle("-fx-text-fill:#aaaaaa; -fx-font-size:12px;");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(4, 10, 4, 10));
        statusBar.setStyle("-fx-background-color:#111827;");

        roomCanvas.setVisible(false);
        StackPane viewPane = new StackPane(roomCanvas, room3DView);
        VBox.setVgrow(viewPane, Priority.ALWAYS);

        VBox root = new VBox(toolbar, viewPane, statusBar);
        root.setStyle("-fx-background-color:#0d1117;");
        stage.setScene(new Scene(root, 920, 680));
    }

    private void applyToolbarStyle(Button b) {
        b.setStyle("-fx-background-color:#2d3748; -fx-text-fill:#e0e0e0;" +
                   "-fx-border-radius:4; -fx-background-radius:4;" +
                   "-fx-padding:5 14 5 14; -fx-cursor:hand;");
    }

    private void setActiveBtn(Button active) {
        for (Button b : new Button[]{btn2D, btn3D, btnFPS}) {
            if (b == active) {
                b.setStyle("-fx-background-color:#3b82f6; -fx-text-fill:white;" +
                           "-fx-border-radius:4; -fx-background-radius:4;" +
                           "-fx-padding:5 14 5 14; -fx-cursor:hand;");
            } else {
                applyToolbarStyle(b);
            }
        }
    }

    // ─── Переключение режимов ─────────────────────────────────────────────────

    private void switch2D() {
        if (room3DView.isFpsMode()) room3DView.exitFpsMode();
        room3DView.setVisible(false);
        roomCanvas.setVisible(true);
        roomCanvas.drawHouse(facade.getHouse());
        setActiveBtn(btn2D);
        statusLabel.setText("Режим: 2D план");
    }

    private void switch3D() {
        if (room3DView.isFpsMode()) room3DView.exitFpsMode();
        roomCanvas.setVisible(false);
        room3DView.setVisible(true);
        room3DView.drawHouse(facade.getHouse());
        setActiveBtn(btn3D);
        statusLabel.setText("Режим: 3D  |  ЛКМ вращение  |  колёсико zoom  |  перетащи устройство");
    }

    private void switchFPS() {
        if (currentRoom == null) {
            statusLabel.setText("Выберите комнату в главном окне, затем нажмите FPS");
            return;
        }
        roomCanvas.setVisible(false);
        room3DView.setVisible(true);
        room3DView.drawHouse(facade.getHouse());
        room3DView.enterFpsMode(currentRoom);
        setActiveBtn(btnFPS);
        statusLabel.setText("FPS: " + currentRoom.getName() +
                            "  |  WASD — движение  |  мышь — взгляд  |  ESC — выход");
    }

    // ─── Публичный API ────────────────────────────────────────────────────────

    public void show() {
        stage.show();
        stage.toFront();
        refresh();
    }

    /** Вызывается из MainController при выборе комнаты. */
    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
        if (room \!= null) room3DView.highlightRoom(room);
    }

    /** Вызывается при toggle устройства — запускает анимацию. */
    public void playToggleAnimation(String deviceId, boolean isOn) {
        room3DView.playToggleAnimation(deviceId, isOn);
    }

    /** Синхронизировать фон 3D с активной темой. */
    public void setSubSceneBackground(javafx.scene.paint.Color color) {
        room3DView.setSubSceneBackground(color);
    }

    private void refresh() {
        if (\!room3DView.isFpsMode()) room3DView.drawHouse(facade.getHouse());
        if (roomCanvas.isVisible())  roomCanvas.drawHouse(facade.getHouse());
    }

    public Stage getStage() { return stage; }
}
