package com.nta.lc_server.activity;

import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.lc_server.R;
import com.nta.lc_server.callback.ISingleShippingOrderCallbackListener;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.model.ShippingOrderModel;
import com.nta.lc_server.remote.IGoogleAPI;
import com.nta.lc_server.remote.RetrofitGoogleAPIClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class TrackingOrderActivity extends FragmentActivity implements OnMapReadyCallback, ISingleShippingOrderCallbackListener, ValueEventListener {

    private GoogleMap mMap;
    private ISingleShippingOrderCallbackListener iSingleShippingOrderCallbackListener;

    //Route
    private Marker shipperMarker;

    private PolylineOptions polylineOptions, blackPolylineOptions;

    private List<LatLng> polylineList;

    private Polyline yellowPolyline, grayPolyline, blackPolyline;

    private IGoogleAPI iGoogleAPI;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ShippingOrderModel currentShippingOrder;
    private DatabaseReference shipperRef;

    //Move marker
    private Handler handler;
    private int index, next;
    private LatLng start, end;
    private float v;
    private double lat, lng;

    private boolean isInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking_order);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initViews();
    }

    private void initViews() {
        iSingleShippingOrderCallbackListener = this;

        iGoogleAPI = RetrofitGoogleAPIClient.getInstance().create(IGoogleAPI.class);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,
                    R.raw.uber_light_with_label));
            if (!success)
                Log.e("Aaron", "Style parsing failed");

        } catch (Resources.NotFoundException ex) {
            Log.e("Aaron", "Resource not found");
        }

        checkOrderFromFirebase();

    }

    private void checkOrderFromFirebase() {
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPING_ORDER_REF)
                .child(Common.currentOrderSelected.getOrderNumber())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            ShippingOrderModel shippingOrderModel = dataSnapshot.getValue(ShippingOrderModel.class);
                            shippingOrderModel.setKey(dataSnapshot.getKey());

                            iSingleShippingOrderCallbackListener.onSingleShippingOrderLoadSuccess(shippingOrderModel);
                        } else {
                            Toast.makeText(TrackingOrderActivity.this, "Order not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(TrackingOrderActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onSingleShippingOrderLoadSuccess(ShippingOrderModel shippingOrderModel) {

        currentShippingOrder = shippingOrderModel;
        subscribeShipperMove(currentShippingOrder);

        //Copy code form method 'drawRoutes' of Client app with some modification
        LatLng locationOrder = new LatLng(shippingOrderModel.getOrderModel().getLat(),
                shippingOrderModel.getOrderModel().getLng());

        LatLng locationShipper = new LatLng(shippingOrderModel.getCurrentLat(),
                shippingOrderModel.getCurrentLng());

        //Add box
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                .title(shippingOrderModel.getOrderModel().getUserName())
                .snippet(shippingOrderModel.getOrderModel().getShippingAddress())
                .position(locationOrder));

        //Add Shipper
        if (shipperMarker == null) {
            int height, width;
            height = width = 80;
            BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat
                    .getDrawable(TrackingOrderActivity.this, R.drawable.shippernew);
            Bitmap resized = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), width, height, false);

            shipperMarker = mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(resized))
                    .title(shippingOrderModel.getShipperName())
                    .snippet(shippingOrderModel.getShipperPhone())
                    .position(locationShipper));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18));

        } else {
            shipperMarker.setPosition(locationShipper);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18));
        }

        //Draw routes
        String to = new StringBuilder()
                .append(shippingOrderModel.getOrderModel().getLat())
                .append(",")
                .append(shippingOrderModel.getOrderModel().getLng())
                .toString();
        String from = new StringBuilder()
                .append(shippingOrderModel.getCurrentLat())
                .append(",")
                .append(shippingOrderModel.getCurrentLng())
                .toString();

        compositeDisposable.add(iGoogleAPI.getDirections("driving",
                "les_driving",
                from, to,
                getString(R.string.google_maps_key))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    try {
                        JSONObject jsonObject = new JSONObject(s);
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polylineList = Common.decodePoly(polyline);

                        }

                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.YELLOW);
                        polylineOptions.width(12);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polylineList);
                        yellowPolyline = mMap.addPolyline(polylineOptions);

                    } catch (Exception e) {
                        Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }, throwable -> {
                    Toast.makeText(this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    private void subscribeShipperMove(ShippingOrderModel currentShippingOrder) {
        shipperRef = FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPING_ORDER_REF)
                .child(currentShippingOrder.getKey());
        shipperRef.addValueEventListener(this);
    }

    @Override
    protected void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists())
        {
            //Save old position
            String from = new StringBuilder()
                    .append(currentShippingOrder.getCurrentLat())
                    .append(",")
                    .append(currentShippingOrder.getCurrentLng())
                    .toString();

            //Update position
            currentShippingOrder = dataSnapshot.getValue(ShippingOrderModel.class);
            currentShippingOrder.setKey(dataSnapshot.getKey());
            //Save new position
            String to = new StringBuilder()
                    .append(currentShippingOrder.getCurrentLat())
                    .append(",")
                    .append(currentShippingOrder.getCurrentLng())
                    .toString();

            if (isInit)
                moveMakerAnimation(shipperMarker,from,to);
            else isInit = true;
        }
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {
        Toast.makeText(this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void moveMakerAnimation(Marker shipperMarker, String from, String to) {
        compositeDisposable.add(iGoogleAPI.getDirections("driving", "less_driving",
                from, to,
                getString(R.string.google_maps_key))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(returnResult -> {
                    try {
                        JSONObject jsonObject = new JSONObject(returnResult);
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polylineList = Common.decodePoly(polyline);

                        }

                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.GRAY);
                        polylineOptions.width(12);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polylineList);
                        grayPolyline = mMap.addPolyline(polylineOptions);


                        blackPolylineOptions = new PolylineOptions();
                        blackPolylineOptions.color(Color.BLACK);
                        blackPolylineOptions.width(5);
                        blackPolylineOptions.startCap(new SquareCap());
                        blackPolylineOptions.jointType(JointType.ROUND);
                        blackPolylineOptions.addAll(polylineList);
                        blackPolyline = mMap.addPolyline(blackPolylineOptions);


                        //Animator
                        ValueAnimator polylineAnimator = ValueAnimator.ofInt(0, 100);
                        polylineAnimator.setDuration(2000);
                        polylineAnimator.setInterpolator(new LinearInterpolator());
                        polylineAnimator.addUpdateListener(valueAnimator -> {
                            List<LatLng> points = grayPolyline.getPoints();
                            int percentValue = (int) valueAnimator.getAnimatedValue();
                            int size = points.size();
                            int newPoints = (int) (size * (percentValue / 100.0f));
                            List<LatLng> p = points.subList(0, newPoints);
                            blackPolyline.setPoints(p);
                        });
                        polylineAnimator.start();

                        //Bike moving
                        handler = new Handler();
                        index = -1;
                        next = 1;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (index < polylineList.size() - 1) {
                                    index++;
                                    next = index + 1;
                                    start = polylineList.get(index);
                                    end = polylineList.get(next);
                                }

                                ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 1);
                                valueAnimator.setDuration(1500);
                                valueAnimator.setInterpolator(new LinearInterpolator());
                                valueAnimator.addUpdateListener(valueAnimator1 -> {
                                    v = valueAnimator1.getAnimatedFraction();
                                    lng = v * end.longitude + (1 - v) * start.longitude;
                                    lat = v * end.latitude + (1 - v) * start.latitude;
                                    LatLng newPos = new LatLng(lat,lng);
                                    shipperMarker.setPosition(newPos);
                                    shipperMarker.setAnchor(0.5f,0.5f);
                                    shipperMarker.setRotation(Common.getBearing(start,newPos));

                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(newPos));
                                });
                                valueAnimator.start();
                                if (index<polylineList.size()-2) //Reach destination
                                    handler.postDelayed(this,1500);
                            }
                        }, 1500);

                    } catch (Exception e) {
                        Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }, throwable -> {
                    Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    @Override
    protected void onDestroy() {
        shipperRef.removeEventListener(this);
        isInit=false;
        super.onDestroy();
    }
}