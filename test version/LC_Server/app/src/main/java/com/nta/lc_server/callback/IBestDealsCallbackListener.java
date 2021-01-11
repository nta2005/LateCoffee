package com.nta.lc_server.callback;

import com.nta.lc_server.model.BestDealsModel;

import java.util.List;

public interface IBestDealsCallbackListener {

    void onListBestDealsLoadSuccess(List<BestDealsModel> bestDealsModels);
    void onListBestDealsLoadFailed(String message);
}
