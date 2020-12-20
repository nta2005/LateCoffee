package com.nta.lc_server.callback;

import com.nta.lc_server.model.ShippingOrderModel;

public interface ISingleShippingOrderCallbackListener {
    void onSingleShippingOrderLoadSuccess(ShippingOrderModel shippingOrderModel);

}
