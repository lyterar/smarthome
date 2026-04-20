package com.smarthome.view.dialog;

import com.smarthome.model.device.Device;
import com.smarthome.model.device.DeviceType;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Диалог создания группы устройств (Composite).
 * Показывает устройства комнаты с чекбоксами — пользователь выбирает
 * какие объединить в группу.
 */
public class CreateGroupDialog extends Dialog<CreateGroupDialog.Result> {

    public record Result(String groupName, DeviceType groupType, List<String> deviceIds) {}

    public CreateGroupDialog(List<Device> roomDevices) {
        setTitle("Создать группу устройств");
        setHeaderText("Выберите устройства для объединения в группу");

        ButtonType createButton = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        // Поле имени группы
        TextField nameField = new TextField();
        nameField.setPromptText("Название группы");
        nameField.setText("Группа устройств");

        // Список устройств с чекбоксами
        ListView<CheckBox> checkList = new ListView<>();
        checkList.setPrefHeight(200);

        for (Device device : roomDevices) {
            CheckBox cb = new CheckBox(device.getType().getIcon() + " " + device.getName());
            cb.setUserData(device);
            checkList.getItems().add(cb);
        }

        // Авто-заполнение имени при первом выборе
        for (CheckBox cb : checkList.getItems()) {
            cb.selectedProperty().addListener((obs, was, now) -> {
                if (now && nameField.getText().equals("Группа устройств")) {
                    Device d = (Device) cb.getUserData();
                    nameField.setText("Группа " + d.getType().getDisplayName());
                }
            });
        }

        VBox content = new VBox(10,
                new Label("Название:"), nameField,
                new Label("Устройства в группе:"), checkList
        );
        content.setPadding(new Insets(20));
        getDialogPane().setContent(content);
        getDialogPane().setPrefWidth(360);

        // Кнопка "Создать" активна только если выбрано хотя бы одно устройство
        javafx.scene.Node createBtn = getDialogPane().lookupButton(createButton);
        createBtn.setDisable(true);
        for (CheckBox cb : checkList.getItems()) {
            cb.selectedProperty().addListener((obs, was, now) -> {
                boolean anySelected = checkList.getItems().stream().anyMatch(CheckBox::isSelected);
                createBtn.setDisable(!anySelected);
            });
        }

        setResultConverter(buttonType -> {
            if (buttonType != createButton) return null;

            List<Device> selected = checkList.getItems().stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> (Device) cb.getUserData())
                    .collect(Collectors.toList());

            if (selected.isEmpty()) return null;

            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Группа устройств";

            // Тип группы — по первому выбранному устройству
            DeviceType type = selected.get(0).getType();

            List<String> ids = selected.stream().map(Device::getId).collect(Collectors.toList());
            return new Result(name, type, ids);
        });
    }
}