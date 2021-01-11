package com.nta.latecoffee.ui.view_orders;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nta.latecoffee.model.OrderModel;

import java.util.List;

public class ViewOrderViewModel extends ViewModel {

    private final MutableLiveData<List<OrderModel>> mutableLiveDataOrderList;

    public ViewOrderViewModel() {
        mutableLiveDataOrderList = new MutableLiveData<>();
    }

    public MutableLiveData<List<OrderModel>> getMutableLiveDataOrderList() {
        return mutableLiveDataOrderList;
    }

    public void setMutableLiveDataOrderList(List<OrderModel> orderModelList) {
        mutableLiveDataOrderList.setValue(orderModelList);
    }
}