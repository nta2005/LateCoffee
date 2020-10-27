package com.nta.latecoffee.callback;

import com.nta.latecoffee.model.OrderModel;

public interface ILoadTimeFromFirebaseListener {
    void onLoadTimeSuccess(OrderModel orderModel, long estimateTimeInMs);
    void onLoadTimeFailed(String message);
}
