package org.openjava.mongo.pojo;

import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.codecs.pojo.annotations.BsonRepresentation;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.List;

public class Student {
    // MongoDB中_id默认为ObjectId类型，也可使用Long类型
    private ObjectId id;
    private String name;
    private Integer age;
    private Boolean gender;
    private List<String> roles;
    // 可以避免使用ObjectId类型的属性
    @BsonProperty("class_id")
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String classId;
    private Integer score;
    private Feature feature;
    @BsonProperty("created_time")
    private LocalDateTime createdTime;

    public static Student of(String name, Integer age, Boolean gender, List<String> roles, String cid,
                             Integer score, Feature feature, LocalDateTime createdTime) {
        Student student = new Student();
        student.name = name;
        student.age = age;
        student.gender = gender;
        student.roles = roles;
        student.classId = cid;
        student.score = score;
        student.feature = feature;
        student.createdTime = createdTime;
        return student;
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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getGender() {
        return gender;
    }

    public void setGender(Boolean gender) {
        this.gender = gender;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}
