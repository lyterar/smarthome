module com.smarthome {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires com.google.gson;
    requires jdk.httpserver;

    opens com.smarthome to javafx.fxml;
    opens com.smarthome.controller to javafx.fxml;
    opens com.smarthome.view.component to javafx.fxml;
    opens com.smarthome.view.dialog to javafx.fxml;
    opens com.smarthome.view.window to javafx.fxml;
    opens com.smarthome.model.device to com.google.gson;
    opens com.smarthome.model.room to com.google.gson;
    opens com.smarthome.model.house to com.google.gson;

    exports com.smarthome;
    exports com.smarthome.model.device;
    exports com.smarthome.model.room;
    exports com.smarthome.model.house;
    exports com.smarthome.controller;
    exports com.smarthome.view.component;
    exports com.smarthome.view.dialog;
    exports com.smarthome.view.window;
    exports com.smarthome.pattern.creational;
    exports com.smarthome.pattern.structural;
    exports com.smarthome.pattern.behavioral;
    exports com.smarthome.mcp;
    exports com.smarthome.event;
    exports com.smarthome.service;
}
