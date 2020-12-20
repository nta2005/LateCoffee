package com.nta.latecoffee.services;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nta.latecoffee.common.Common;

import java.util.Map;
import java.util.Random;

public class MyFCMServices extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> dataRecv = remoteMessage.getData();
        if (dataRecv != null) {
            if (dataRecv.get(Common.IS_SEND_IMAGE) != null &&
                    dataRecv.get(Common.IS_SEND_IMAGE).equals("true")) {
                Glide.with(this)
                        .asBitmap() //Remember it!
                        .load(dataRecv.get(Common.IMAGE_URL))
                        .into(new CustomTarget<Bitmap>() {

                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                Common.showNotificationBigStyle(MyFCMServices.this, new Random().nextInt(),
                                        dataRecv.get(Common.NOTI_TITLE),
                                        dataRecv.get(Common.NOTI_CONTENT),
                                        resource,
                                        null);
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });
            } else {
                Common.showNotification(this, new Random().nextInt(),
                        dataRecv.get(Common.NOTI_TITLE),
                        dataRecv.get(Common.NOTI_CONTENT),
                        null);
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Common.updateToken(this, s);
    }
}
