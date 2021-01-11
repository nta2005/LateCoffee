package com.nta.lc_server.callback;

import com.nta.lc_server.model.CategoryModel;

import java.util.List;

public interface ICategoryCallbackListener {
    void onCategoryLoadSuccess(List<CategoryModel> categoryModelList);

    void onCategoryLoadFailed(String message);
}
