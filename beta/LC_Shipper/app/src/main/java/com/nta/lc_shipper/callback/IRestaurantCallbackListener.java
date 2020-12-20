package com.nta.lc_shipper.callback;

import com.nta.lc_shipper.model.RestaurantModel;

import java.util.List;

public interface IRestaurantCallbackListener {
    void onRestaurantLoadSuccess(List<RestaurantModel> restaurantModelList);

    void onRestaurantLoadFailed(String message);
}
