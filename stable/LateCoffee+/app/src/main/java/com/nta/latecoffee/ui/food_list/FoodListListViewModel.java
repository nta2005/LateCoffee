package com.nta.latecoffee.ui.food_list;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.latecoffee.callback.IFoodListCallbackListener;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.model.FoodModel;

import java.util.ArrayList;
import java.util.List;

public class FoodListListViewModel extends ViewModel implements IFoodListCallbackListener {

    private final IFoodListCallbackListener listener;
    private MutableLiveData<List<FoodModel>> mutableLiveDataFoodList;
    private MutableLiveData<String> messageError;

    public FoodListListViewModel() {
        listener = this;
    }

    public MutableLiveData<List<FoodModel>> getMutableLiveDataFoodList() {
        if (mutableLiveDataFoodList == null) {
            mutableLiveDataFoodList = new MutableLiveData<>();
            //mutableLiveDataFoodList.setValue(Common.categorySelected.getFoods());
            messageError = new MutableLiveData<>();
            loadFoodList();
        }
        return mutableLiveDataFoodList;
    }

    public void loadFoodList() {
        List<FoodModel> tempList = new ArrayList<>();
        DatabaseReference foodRef = FirebaseDatabase.getInstance().getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())
                .child("foods");
        foodRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot itemSnapShot : dataSnapshot.getChildren()) {
                    FoodModel model = itemSnapShot.getValue(FoodModel.class);
                    tempList.add(model);
                }
                listener.onFoodListLoadSuccess(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onFoodListLoadFailed(databaseError.getMessage());
            }
        });
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    @Override
    public void onFoodListLoadSuccess(List<FoodModel> foodModels) {
        mutableLiveDataFoodList.setValue(foodModels);
    }

    @Override
    public void onFoodListLoadFailed(String message) {
        messageError.setValue(message);
    }
}