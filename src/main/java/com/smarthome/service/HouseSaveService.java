package com.smarthome.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.smarthome.model.device.Device;
import com.smarthome.model.device.DeviceType;
import com.smarthome.model.house.House;
import com.smarthome.model.room.Room;
import com.smarthome.model.room.RoomType;
import com.smarthome.pattern.creational.DeviceFactory;
import com.smarthome.pattern.creational.RoomBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Сервис сохранения/загрузки конфигурации дома в JSON.
 */
public class HouseSaveService {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final DeviceFactory factory;

    public HouseSaveService(DeviceFactory factory) {
        this.factory = factory;
    }

    /** Сохранить дом в файл */
    public void save(House house, Path path) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("name", house.getName());

        JsonArray roomsArray = new JsonArray();
        for (Room room : house.getRooms()) {
            JsonObject roomObj = new JsonObject();
            roomObj.addProperty("name", room.getName());
            roomObj.addProperty("type", room.getType().name());
            roomObj.addProperty("x", room.getX());
            roomObj.addProperty("y", room.getY());
            roomObj.addProperty("width", room.getWidth());
            roomObj.addProperty("height", room.getHeight());

            JsonArray devicesArray = new JsonArray();
            for (Device device : room.getDevices()) {
                JsonObject devObj = new JsonObject();
                devObj.addProperty("name", device.getName());
                devObj.addProperty("type", device.getType().name());
                devObj.addProperty("x", device.getX());
                devObj.addProperty("y", device.getY());
                devicesArray.add(devObj);
            }
            roomObj.add("devices", devicesArray);
            roomsArray.add(roomObj);
        }
        root.add("rooms", roomsArray);

        Files.writeString(path, gson.toJson(root));
        System.out.println("Дом сохранён в " + path);
    }

    /** Загрузить дом из файла */
    public House load(Path path) throws IOException {
        String json = Files.readString(path);
        JsonObject root = gson.fromJson(json, JsonObject.class);

        House house = new House(root.get("name").getAsString());

        JsonArray roomsArray = root.getAsJsonArray("rooms");
        for (var roomElement : roomsArray) {
            JsonObject roomObj = roomElement.getAsJsonObject();

            RoomType roomType = RoomType.valueOf(roomObj.get("type").getAsString());
            Room room = new RoomBuilder(roomObj.get("name").getAsString())
                    .type(roomType)
                    .position(roomObj.get("x").getAsDouble(), roomObj.get("y").getAsDouble())
                    .size(roomObj.get("width").getAsDouble(), roomObj.get("height").getAsDouble())
                    .build();

            JsonArray devicesArray = roomObj.getAsJsonArray("devices");
            if (devicesArray != null) {
                for (var devElement : devicesArray) {
                    JsonObject devObj = devElement.getAsJsonObject();
                    DeviceType devType = DeviceType.valueOf(devObj.get("type").getAsString());
                    Device device = factory.createDevice(devObj.get("name").getAsString(), devType);
                    device.setX(devObj.get("x").getAsDouble());
                    device.setY(devObj.get("y").getAsDouble());
                    room.addDevice(device);
                }
            }

            house.addRoom(room);
        }

        System.out.println("Дом загружен из " + path);
        return house;
    }
}
