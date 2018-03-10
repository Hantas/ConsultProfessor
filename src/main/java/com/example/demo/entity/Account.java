package com.example.demo.entity;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Created by 10742 on 2018/2/8.
 */
public class Account {
    private Long user_id;
    @JSONField(name = "name")
    private String name;
    @JSONField(name = "role")
    private String role;
    @JSONField(name = "area")
    private String area;

    public Long getUser_id() {
        return user_id;
    }

    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }
}
