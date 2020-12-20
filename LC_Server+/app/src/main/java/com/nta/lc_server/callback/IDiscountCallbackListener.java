package com.nta.lc_server.callback;

import com.nta.lc_server.model.DiscountModel;

import java.util.List;

public interface IDiscountCallbackListener {
    void onListDiscountLoadSuccess(List<DiscountModel> discountModelList);

    void onListDiscountLoadFailed(String message);
}
