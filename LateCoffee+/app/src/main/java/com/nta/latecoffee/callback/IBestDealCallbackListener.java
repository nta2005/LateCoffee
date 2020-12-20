package com.nta.latecoffee.callback;

import com.nta.latecoffee.model.BestDealModel;

import java.util.List;

public interface IBestDealCallbackListener {
    void onBestDealLoadSuccess(List<BestDealModel> bestDealModels);

    void onBestDealLoadFailed(String message);
}
