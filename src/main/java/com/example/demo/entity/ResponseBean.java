package com.example.demo.entity;

public class ResponseBean {
    private String message;
    private String order;
    private String keyRole_id;

    public ResponseBean(String message, String order, String keyRole_id) {
        this.message = message;
        this.order = order;
        this.keyRole_id = keyRole_id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getKeyRole_id() {
        return keyRole_id;
    }

    public void setKeyRole_id(String keyRole_id) {
        this.keyRole_id = keyRole_id;
    }
}
