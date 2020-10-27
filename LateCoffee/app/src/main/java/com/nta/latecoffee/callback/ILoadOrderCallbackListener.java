package com.nta.latecoffee.callback;

import com.nta.latecoffee.model.OrderModel;

import java.util.List;

public interface ILoadOrderCallbackListener {
    void onLoadOrderSuccess(List<OrderModel> orderModelList);
    void onLoadOrderFailed(String message);
}
