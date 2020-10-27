package com.nta.latecoffee.eventbus;

import com.nta.latecoffee.model.FoodModel;

public class FoodItemClick {
    private boolean success;
    private FoodModel foodModel;

    public FoodItemClick(boolean success, FoodModel foodModel) {
        this.success = success;
        this.foodModel = foodModel;
    }

    public boolean isSuccess() {
        return success;
    }

    public FoodItemClick setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public FoodModel getFoodModel() {
        return foodModel;
    }

    public FoodItemClick setFoodModel(FoodModel foodModel) {
        this.foodModel = foodModel;
        return this;
    }
}
