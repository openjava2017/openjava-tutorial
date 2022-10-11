package org.openjava.jackson.pojo;

import java.time.LocalDateTime;

/**
 * @author: brenthuang
 * @date: 2022/10/10
 */
public class Student {
    private Long id;
    private String name;
    private LocalDateTime birthday;
    private String gender;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDateTime birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
