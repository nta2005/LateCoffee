package com.nta.latecoffee.remote;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitICloudClient {
    private static Retrofit instance;

    //paymentUrl = "https://us-central1-late-coffee-d11bc.cloudfunctions.net/widgets/";

    public static Retrofit getInstance() {
        if (instance == null) {
            instance = new Retrofit.Builder()
                    .baseUrl("https://us-central1-late-coffee-d11bc.cloudfunctions.net/widgets/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build();

        }
        return instance;
    }

//    public static Retrofit getInstance(String paymentUrl) {
//        if (instance == null) {
//            instance = new Retrofit.Builder()
//                    .baseUrl(paymentUrl)
//                    .addConverterFactory(GsonConverterFactory.create())
//                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//                    .build();
//
//        }
//        return instance;
//    }

}
