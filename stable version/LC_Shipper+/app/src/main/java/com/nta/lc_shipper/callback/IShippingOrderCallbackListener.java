package com.nta.lc_shipper.callback;

import com.nta.lc_shipper.model.ShippingOrderModel;

import java.util.List;

public interface IShippingOrderCallbackListener {
    void onShippingOrderLoadSuccess(List<ShippingOrderModel> shippingOrderModelList);

    void onShippingOrderLoadFailed(String message);
}
