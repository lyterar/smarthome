package com.smarthome.view.dialog;

import com.smarthome.model.room.RoomType;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

/**
 * Диалог добавления комнаты.
 */
public class AddRoomDialog extends Dialog<AddRoomDialog.Result> {

    public record Result(String name, RoomType type) {}

    public AddRoomDialog() {
        setTitle("Добавить комнату");
        setHeaderText("Введите параметры комнаты");

        // Кнопки
        ButtonType addButton = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        // Форма
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Название комнаты");
        nameField.setText("Комната");

        ComboBox<RoomType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(RoomType.values());
        typeCombo.setValue(RoomType.LIVING_ROOM);
        typeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RoomType t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getDisplayName());
            }
        });
        typeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(RoomType t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getDisplayName());
            }
        });

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Тип:"), 0, 1);
        grid.add(typeCombo, 1, 1);

        getDialogPane().setContent(grid);

        // Фокус на поле имени
        nameField.requestFocus();

        // Конвертер результата
        setResultConverter(buttonType -> {
            if (buttonType == addButton) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = "Комната";
                return new Result(name, typeCombo.getValue());
            }
            return null;
        });
    }
}
