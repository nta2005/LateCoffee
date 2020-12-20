package com.nta.lc_server.ui.discount;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.lc_server.callback.IDiscountCallbackListener;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.model.DiscountModel;

import java.util.ArrayList;
import java.util.List;

public class DiscountViewModel extends ViewModel implements IDiscountCallbackListener {

    private MutableLiveData<String> messageError = new MutableLiveData<>();
    private MutableLiveData<List<DiscountModel>> discountMutableLiveData;
    private IDiscountCallbackListener discountCallbackListener;

    public DiscountViewModel() {
        discountCallbackListener = this;
    }

    public MutableLiveData<List<DiscountModel>> getDiscountMutableLiveData() {
        if (discountMutableLiveData == null) discountMutableLiveData = new MutableLiveData<>();
        loadDiscount();
        return discountMutableLiveData;
    }

    public void loadDiscount() {
        List<DiscountModel> temp = new ArrayList<>();
        DatabaseReference discountRef = FirebaseDatabase.getInstance()
                .getReference(Common.DISCOUNT_REF);
        discountRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildren().iterator().hasNext()) {
                    for (DataSnapshot discountSnapshot : dataSnapshot.getChildren()) {
                        DiscountModel discountModel = discountSnapshot.getValue(DiscountModel.class);
                        discountModel.setKey(discountSnapshot.getKey());
                        temp.add(discountModel);
                    }
                    discountCallbackListener.onListDiscountLoadSuccess(temp);
                } else
                    discountCallbackListener.onListDiscountLoadFailed("Dữ liệu trống");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                discountCallbackListener.onListDiscountLoadFailed(databaseError.getMessage());
            }
        });
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    @Override
    public void onListDiscountLoadSuccess(List<DiscountModel> discountModelList) {
        discountMutableLiveData.setValue(discountModelList);
    }

    @Override
    public void onListDiscountLoadFailed(String message) {
        if (message.equals("Dữ liệu trống"))
            discountMutableLiveData.setValue(null);
        messageError.setValue(message);
    }
}