package com.nta.lc_server.model;

import java.util.List;

public class FoodModel {
    private String key;
    private String name, image, id, description;
    private Long price;
    private List<AddonModel> addon;
    private List<SizeModel> size;
    private Double ratingValue;
    private Long ratingCount;

    //For Cart
    private List<AddonModel> userSelectedAddon;
    private SizeModel userSelectedSize;

    //For Search
    //Save index of food in list after searched (add property to Food Model)
    private int positionInList = -1;


    public FoodModel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public List<AddonModel> getAddon() {
        return addon;
    }

    public void setAddon(List<AddonModel> addon) {
        this.addon = addon;
    }

    public List<SizeModel> getSize() {
        return size;
    }

    public void setSize(List<SizeModel> size) {
        this.size = size;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Double getRatingValue() {
        return ratingValue;
    }

    public void setRatingValue(Double ratingValue) {
        this.ratingValue = ratingValue;
    }

    public Long getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Long ratingCount) {
        this.ratingCount = ratingCount;
    }

    public List<AddonModel> getUserSelectedAddon() {
        return userSelectedAddon;
    }

    public void setUserSelectedAddon(List<AddonModel> userSelectedAddon) {
        this.userSelectedAddon = userSelectedAddon;
    }

    public SizeModel getUserSelectedSize() {
        return userSelectedSize;
    }

    public void setUserSelectedSize(SizeModel userSelectedSize) {
        this.userSelectedSize = userSelectedSize;
    }

    public int getPositionInList() {
        return positionInList;
    }

    public void setPositionInList(int positionInList) {
        this.positionInList = positionInList;
    }
}
