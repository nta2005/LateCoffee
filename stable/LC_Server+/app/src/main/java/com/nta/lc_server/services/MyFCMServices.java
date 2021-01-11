package com.nta.lc_server.services;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nta.lc_server.activity.MainActivity;
import com.nta.lc_server.common.Common;

import java.util.Map;
import java.util.Random;

public class MyFCMServices extends FirebaseMessagingService {
    //Copy code from Client app
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> dataRecv = remoteMessage.getData();
        if (dataRecv != null) {
            if (dataRecv.get(Common.NOTI_TITLE).equals("Đơn hàng mới") || dataRecv.get(Common.NOTI_TITLE).equals("Huỷ đơn hàng")) {
                //Here we need call MainActivity because we must assign value for Common.currentUser
                //So we must call MainActivity to do that, if you direct call HomeActivity you will be crash
                //Because Common.currentUser only be assign in MainActivity AFTER LOGIN
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER, true); //Use extra to detect is app open from notification
                Common.showNotification(this, new Random().nextInt(),
                        dataRecv.get(Common.NOTI_TITLE),
                        dataRecv.get(Common.NOTI_CONTENT),
                        intent);
            } else
                Common.showNotification(this, new Random().nextInt(),
                        dataRecv.get(Common.NOTI_TITLE),
                        dataRecv.get(Common.NOTI_CONTENT),
                        null);
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Common.updateToken(this, s, true, false); //Because we are in Server app so server = true
    }
}
