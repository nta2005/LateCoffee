package com.nta.latecoffee.callback;

import com.nta.latecoffee.model.FoodModel;

import java.util.List;

public interface IFoodListCallbackListener {
    void onFoodListLoadSuccess(List<FoodModel> foodModels);

    void onFoodListLoadFailed(String message);
}
