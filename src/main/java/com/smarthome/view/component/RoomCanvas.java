package com.smarthome.view.component;

import com.smarthome.model.device.Device;
import com.smarthome.model.house.House;
import com.smarthome.model.room.Room;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Канвас для отрисовки плана дома с комнатами и устройствами.
 * Комнаты — цветные прямоугольники. Устройства — иконки внутри комнат.
 */
public class RoomCanvas extends Canvas {

    private House currentHouse;
    private Room highlightedRoom;

    public RoomCanvas() {
        // Размер по умолчанию
        setWidth(700);
        setHeight(500);

        // Перерисовка при изменении размера
        widthProperty().addListener(e -> redraw());
        heightProperty().addListener(e -> redraw());
    }

    /** Нарисовать весь дом */
    public void drawHouse(House house) {
        this.currentHouse = house;
        redraw();
    }

    /** Подсветить выбранную комнату */
    public void highlightRoom(Room room) {
        this.highlightedRoom = room;
        redraw();
    }

    private void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // Фон
        gc.setFill(Color.web("#F5F5F5"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        // Сетка
        drawGrid(gc);

        if (currentHouse == null) {
            gc.setFill(Color.GRAY);
            gc.setFont(Font.font(16));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Добавьте комнату для начала", getWidth() / 2, getHeight() / 2);
            return;
        }

        // Рисуем комнаты
        for (Room room : currentHouse.getRooms()) {
            drawRoom(gc, room);
        }
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.web("#E0E0E0"));
        gc.setLineWidth(0.5);
        for (double x = 0; x < getWidth(); x += 50) {
            gc.strokeLine(x, 0, x, getHeight());
        }
        for (double y = 0; y < getHeight(); y += 50) {
            gc.strokeLine(0, y, getWidth(), y);
        }
    }

    private void drawRoom(GraphicsContext gc, Room room) {
        double x = room.getX();
        double y = room.getY();
        double w = room.getWidth();
        double h = room.getHeight();

        // Заливка комнаты
        Color baseColor = Color.web(room.getType().getColor());
        gc.setFill(baseColor);
        gc.fillRect(x, y, w, h);

        // Рамка (толще для выделенной комнаты)
        boolean isHighlighted = highlightedRoom != null
                && highlightedRoom.getId().equals(room.getId());
        gc.setStroke(isHighlighted ? Color.web("#2196F3") : Color.web("#666666"));
        gc.setLineWidth(isHighlighted ? 3 : 1.5);
        gc.strokeRect(x, y, w, h);

        // Название комнаты
        gc.setFill(Color.web("#333333"));
        gc.setFont(Font.font("System", isHighlighted ? 14 : 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(room.getName(), x + w / 2, y + 20);

        // Устройства внутри комнаты
        drawDevices(gc, room);
    }

    private void drawDevices(GraphicsContext gc, Room room) {
        double startX = room.getX() + 15;
        double startY = room.getY() + 40;
        double spacing = 30;
        int col = 0;
        int maxCols = (int) ((room.getWidth() - 30) / spacing);
        if (maxCols < 1) maxCols = 1;

        gc.setFont(Font.font(16));
        gc.setTextAlign(TextAlignment.LEFT);

        for (Device device : room.getDevices()) {
            double dx = startX + (col % maxCols) * spacing;
            double dy = startY + (col / maxCols) * spacing;

            // Иконка устройства
            String icon = device.getType().getIcon();
            gc.setFill(device.isOn() ? Color.web("#4CAF50") : Color.web("#999999"));
            gc.fillText(icon, dx, dy);

            col++;
        }
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return 700;
    }

    @Override
    public double prefHeight(double width) {
        return 500;
    }
}
