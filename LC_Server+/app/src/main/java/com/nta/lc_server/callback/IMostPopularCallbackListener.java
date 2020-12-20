package com.nta.lc_server.callback;

import com.nta.lc_server.model.MostPopularModel;

import java.util.List;

public interface IMostPopularCallbackListener {
    void onListMostPopularLoadSuccess(List<MostPopularModel> mostPopularModels);

    void onListMostPopularLoadFailed(String message);
}
