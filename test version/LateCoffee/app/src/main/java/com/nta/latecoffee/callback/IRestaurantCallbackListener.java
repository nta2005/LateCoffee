package com.nta.latecoffee.callback;

import com.nta.latecoffee.model.RestaurantModel;

import java.util.List;

public interface IRestaurantCallbackListener {
    void onRestaurantLoadSuccess(List<RestaurantModel> restaurantModelList);

    void onRestaurantLoadFailed(String message);
}
