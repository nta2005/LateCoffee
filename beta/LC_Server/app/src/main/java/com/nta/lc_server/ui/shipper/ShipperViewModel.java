package com.nta.lc_server.ui.shipper;

import android.app.AlertDialog;
import android.widget.Button;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.lc_server.callback.IShipperLoadCallbackListener;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.model.OrderModel;
import com.nta.lc_server.model.ShipperModel;

import java.util.ArrayList;
import java.util.List;

public class ShipperViewModel extends ViewModel implements IShipperLoadCallbackListener {
    private MutableLiveData<String> messageError = new MutableLiveData<>();
    private MutableLiveData<List<ShipperModel>> shipperMutableList;
    private IShipperLoadCallbackListener shipperLoadCallbackListener;

    public ShipperViewModel() {
        shipperLoadCallbackListener = this;
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    public MutableLiveData<List<ShipperModel>> getShipperMutableList() {
        if (shipperMutableList == null) {
            shipperMutableList = new MutableLiveData<>();
            loadShipper();
        }
        return shipperMutableList;
    }

    private void loadShipper() {
        List<ShipperModel> tempList = new ArrayList<>();
        DatabaseReference shipperRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPER_REF);
        shipperRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot shipperSnapShot : dataSnapshot.getChildren()) {
                    ShipperModel shipperModel = shipperSnapShot.getValue(ShipperModel.class);
                    shipperModel.setKey(shipperSnapShot.getKey());
                    tempList.add(shipperModel);
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                shipperLoadCallbackListener.onShipperLoadFailed(databaseError.getMessage());
            }
        });
    }

    @Override
    public void onShipperLoadSuccess(List<ShipperModel> shipperModelList) {
        if (shipperMutableList != null) {
            shipperMutableList.setValue(shipperModelList);
        }
    }

    @Override
    public void onShipperLoadSuccess(int pos, OrderModel orderModel, List<ShipperModel> shipperModels, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        //Do nothing
    }

    @Override
    public void onShipperLoadFailed(String message) {
        messageError.setValue(message);
    }
}