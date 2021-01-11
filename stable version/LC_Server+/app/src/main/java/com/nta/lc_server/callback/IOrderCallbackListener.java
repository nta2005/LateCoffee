package com.nta.lc_server.callback;

import com.nta.lc_server.model.OrderModel;

import java.util.List;

public interface IOrderCallbackListener {
    void onOrderLoadSuccess(List<OrderModel> orderModelList);

    void onOrderLoadFailed(String message);
}
