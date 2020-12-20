package com.nta.latecoffee.model;

public class BestDealModel {
    private String menu_id, food_id, name, image;

    public BestDealModel(String menu_id, String food_id, String name, String image) {
        this.menu_id = menu_id;
        this.food_id = food_id;
        this.name = name;
        this.image = image;
    }

    public BestDealModel() {
    }

    public String getMenu_id() {
        return menu_id;
    }

    public BestDealModel setMenu_id(String menu_id) {
        this.menu_id = menu_id;
        return this;
    }

    public String getFood_id() {
        return food_id;
    }

    public BestDealModel setFood_id(String food_id) {
        this.food_id = food_id;
        return this;
    }

    public String getName() {
        return name;
    }

    public BestDealModel setName(String name) {
        this.name = name;
        return this;
    }

    public String getImage() {
        return image;
    }

    public BestDealModel setImage(String image) {
        this.image = image;
        return this;
    }
}
