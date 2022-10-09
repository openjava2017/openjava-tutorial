package org.openjava.mongo.pojo;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public class ClassRoom {
    // MongoDB中_id默认为ObjectId类型，也可使用Long类型
    private ObjectId id;
    private String name;
    private String address;
    @BsonProperty("created_time")
    private LocalDateTime createdTime;

    public static ClassRoom of(String name, String address, LocalDateTime createdTime) {
        ClassRoom room = new ClassRoom();
        room.name = name;
        room.address = address;
        room.createdTime = createdTime;
        return room;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
