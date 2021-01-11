package com.nta.lc_server.ui.most_popular;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.lc_server.callback.IMostPopularCallbackListener;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.model.MostPopularModel;

import java.util.ArrayList;
import java.util.List;

public class MostPopularViewModel extends ViewModel implements IMostPopularCallbackListener {
    private MutableLiveData<String> messageError = new MutableLiveData<>();
    private MutableLiveData<List<MostPopularModel>> mostPopularListMutable;
    private IMostPopularCallbackListener mostPopularCallbackListener;

    public MostPopularViewModel() {
        mostPopularCallbackListener = this;
    }

    public MutableLiveData<List<MostPopularModel>> getMostPopularListMutable() {
        if (mostPopularListMutable == null)
            mostPopularListMutable = new MutableLiveData<>();
        loadMostPopular();
        return mostPopularListMutable;

    }

    public void loadMostPopular() {
        List<MostPopularModel> temp = new ArrayList<>();
        DatabaseReference mostPopularRef = FirebaseDatabase.getInstance()
                .getReference(Common.MOST_POPULAR);
        mostPopularRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot mostPopularSnapShot : dataSnapshot.getChildren()) {
                    MostPopularModel mostPopularModel = mostPopularSnapShot.getValue(MostPopularModel.class);
                    mostPopularModel.setKey(mostPopularSnapShot.getKey());
                    temp.add(mostPopularModel);
                }
                mostPopularCallbackListener.onListMostPopularLoadSuccess(temp);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                mostPopularCallbackListener.onListMostPopularLoadFailed(databaseError.getMessage());
            }
        });
    }

    public MutableLiveData<String> getMessageError() {
        return messageError;
    }

    @Override
    public void onListMostPopularLoadSuccess(List<MostPopularModel> mostPopularModels) {
        mostPopularListMutable.setValue(mostPopularModels);
    }

    @Override
    public void onListMostPopularLoadFailed(String message) {
        messageError.setValue(message);
    }
}