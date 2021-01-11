package com.nta.lc_server.model;

public class ServerUserModel {
    private String uid, name, phone, restaurant;
    private boolean active;

    public ServerUserModel() {
    }

    public ServerUserModel(String uid, String name, String phone, boolean active) {
        this.uid = uid;
        this.name = name;
        this.phone = phone;
        this.active = active;
    }

    public String getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(String restaurant) {
        this.restaurant = restaurant;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
