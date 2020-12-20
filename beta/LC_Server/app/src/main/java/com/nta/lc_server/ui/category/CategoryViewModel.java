package com.nta.lc_server.ui.category;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.lc_server.callback.ICategoryCallbackListener;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.model.CategoryModel;

import java.util.ArrayList;
import java.util.List;

public class CategoryViewModel extends ViewModel implements ICategoryCallbackListener {

    private MutableLiveData<List<CategoryModel>> categoryListMultable;
    private MutableLiveData<String> messageError = new MutableLiveData<>();
    public static ICategoryCallbackListener categoryCallbackListener;


    public CategoryViewModel() {
        categoryCallbackListener = this;
    }


    public MutableLiveData<List<CategoryModel>> getCategoryListMultable() {
        if (categoryListMultable == null) {
            categoryListMultable = new MutableLiveData<>();
            messageError = new MutableLiveData<>();
            loadCategories();
        }
        return categoryListMultable;
    }

    public static void loadCategories() {
        List<CategoryModel> tempList = new ArrayList<>();
        DatabaseReference categoryRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                //.child("restauranta") //Nếu user chưa có restaurant
                .child(Common.currentServerUser.getRestaurant()) //User đã có restaurant
                .child(Common.CATEGORY_REF);
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot itemSnapShot : dataSnapshot.getChildren()) {
                        CategoryModel categoryModel = itemSnapShot.getValue(CategoryModel.class);
                        categoryModel.setMenu_id(itemSnapShot.getKey());
                        tempList.add(categoryModel);
                    }
                    if (tempList.size() > 0)
                        categoryCallbackListener.onCategoryLoadSuccess(tempList);
                    else
                        categoryCallbackListener.onCategoryLoadFailed("Category empty!");
                } else
                    categoryCallbackListener.onCategoryLoadFailed("Category not exists!");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                categoryCallbackListener.onCategoryLoadFailed(databaseError.getMessage());
            }
        });
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    @Override
    public void onCategoryLoadSuccess(List<CategoryModel> categoryModelList) {
        categoryListMultable.setValue(categoryModelList);
    }

    @Override
    public void onCategoryLoadFailed(String message) {
        messageError.setValue(message);
    }
}