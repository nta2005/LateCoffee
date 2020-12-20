package com.nta.lc_server.callback;

import com.nta.lc_server.model.OrderModel;

public interface ILoadTimeFromFirebaseListener {
    void onLoadTimeSuccess(OrderModel orderModel, long estimateTimeInMs);
    void onLoadOnlyTimeSuccess(long estimateTimeInMs);
    void onLoadTimeFailed(String message);
}
