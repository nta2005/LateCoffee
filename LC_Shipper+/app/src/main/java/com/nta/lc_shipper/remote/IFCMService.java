package com.nta.lc_shipper.remote;


import com.nta.lc_shipper.model.FCMResponse;
import com.nta.lc_shipper.model.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAA5KONDmg:APA91bFwSJYZTIx5Wngpz4DE3E9s2kF3pw4RefNG14CcMNSnVesyjLGRpFtsJ4-NMsdYVxWsnX5XwuJMAuwzYh_avMafwxHZgTT3LSp3C7iGP15HCisatRCs03suTV8idMdmlQsrTotV"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
