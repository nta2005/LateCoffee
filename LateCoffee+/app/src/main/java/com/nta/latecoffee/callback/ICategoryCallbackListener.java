package com.nta.latecoffee.callback;

import com.nta.latecoffee.model.CategoryModel;

import java.util.List;

public interface ICategoryCallbackListener {
    void onCategoryLoadSuccess(List<CategoryModel> categoryModelList);

    void onCategoryLoadFailed(String message);
}
