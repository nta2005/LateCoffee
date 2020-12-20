package com.nta.lc_shipper.services;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nta.lc_shipper.common.Common;
import com.nta.lc_shipper.eventbus.UpdateShippingOrderEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;
import java.util.Random;

public class MyFCMServices extends FirebaseMessagingService {
    //Copy code from Client app
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> dataRecv = remoteMessage.getData();
        if (dataRecv != null) {
            Common.showNotification(this, new Random().nextInt(),
                    dataRecv.get(Common.NOTI_TITLE),
                    dataRecv.get(Common.NOTI_CONTENT),
                    null);

            EventBus.getDefault().postSticky(new UpdateShippingOrderEvent()); //Update order list when have new order need ship
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Common.updateToken(this, s,false,true); //Because we are in Shipper app so shipper = true
    }
}
