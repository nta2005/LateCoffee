package com.nta.latecoffee.callback;

import com.nta.latecoffee.database.CartItem;
import com.nta.latecoffee.model.CategoryModel;

public interface ISearchCategoryCallbackListener {
    void onSearchCategoryFound(CategoryModel categoryModel, CartItem cartItem);

    void onSearchCategoryNotFound(String message);
}
