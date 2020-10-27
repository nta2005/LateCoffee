package com.nta.latecoffee.model;

public class UserModel {
    private String uid, name, address, phone;

    public UserModel() {
        this.uid = uid;
        this.name = name;
        this.address = address;
        this.phone = phone;
    }

    public String getUid() {
        return uid;
    }

    public UserModel setUid(String uid) {
        this.uid = uid;
        return this;
    }

    public String getName() {
        return name;
    }

    public UserModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public UserModel setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public UserModel setPhone(String phone) {
        this.phone = phone;
        return this;
    }
}
