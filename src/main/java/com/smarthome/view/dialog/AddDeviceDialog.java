package com.smarthome.view.dialog;

import com.smarthome.model.device.DeviceType;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

/**
 * Диалог добавления устройства.
 */
public class AddDeviceDialog extends Dialog<AddDeviceDialog.Result> {

    public record Result(String name, DeviceType type) {}

    public AddDeviceDialog() {
        setTitle("Добавить устройство");
        setHeaderText("Выберите тип и имя устройства");

        ButtonType addButton = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Название устройства");

        ComboBox<DeviceType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(DeviceType.values());
        typeCombo.setValue(DeviceType.LIGHT);
        typeCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DeviceType t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getIcon() + " " + t.getDisplayName());
            }
        });
        typeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DeviceType t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : t.getIcon() + " " + t.getDisplayName());
            }
        });

        // Автозаполнение имени при выборе типа
        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && nameField.getText().isEmpty()) {
                nameField.setText(newVal.getDisplayName());
            }
        });
        nameField.setText("Лампа"); // по умолчанию

        grid.add(new Label("Тип:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Название:"), 0, 1);
        grid.add(nameField, 1, 1);

        getDialogPane().setContent(grid);
        nameField.requestFocus();

        setResultConverter(buttonType -> {
            if (buttonType == addButton) {
                String name = nameField.getText().trim();
                if (name.isEmpty()) name = typeCombo.getValue().getDisplayName();
                return new Result(name, typeCombo.getValue());
            }
            return null;
        });
    }
}
