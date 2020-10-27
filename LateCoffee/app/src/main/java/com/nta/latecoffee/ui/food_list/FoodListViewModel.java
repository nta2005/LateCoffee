package com.nta.latecoffee.ui.food_list;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.model.FoodModel;

import java.util.List;

public class FoodListViewModel extends ViewModel {

    private MutableLiveData<List<FoodModel>> mutableLiveDataFoodList;

    public FoodListViewModel() {

    }

    public MutableLiveData<List<FoodModel>> getMutableLiveDataFoodList() {
        if (mutableLiveDataFoodList == null) {
            mutableLiveDataFoodList = new MutableLiveData<>();
            mutableLiveDataFoodList.setValue(Common.categorySelected.getFoods());
        }
        return mutableLiveDataFoodList;
    }
}