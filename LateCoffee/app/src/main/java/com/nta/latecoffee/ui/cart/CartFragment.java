package com.nta.latecoffee.ui.cart;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.latecoffee.R;
import com.nta.latecoffee.adapter.MyCartAdaper;
import com.nta.latecoffee.callback.ILoadTimeFromFirebaseListener;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.common.MySwipeHelper;
import com.nta.latecoffee.database.CartDataSource;
import com.nta.latecoffee.database.CartDatabase;
import com.nta.latecoffee.database.CartItem;
import com.nta.latecoffee.database.LocalCartDataSource;
import com.nta.latecoffee.eventbus.CounterCartEvent;
import com.nta.latecoffee.eventbus.HideFABCart;
import com.nta.latecoffee.eventbus.MenuItemBack;
import com.nta.latecoffee.eventbus.UpdateItemInCart;
import com.nta.latecoffee.model.OrderModel;
import com.nta.latecoffee.remote.ICloudFunctions;
import com.nta.latecoffee.remote.RetrofitICloudClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CartFragment extends Fragment implements ILoadTimeFromFirebaseListener {

    private static final int REQUEST_BRAINTREE_CODE = 7777;

    //Callback
    ICloudFunctions cloudFunctions;
    ILoadTimeFromFirebaseListener listener;

    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;

    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;
    String address, comment;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;
    private MyCartAdaper adapter;
    private Unbinder unbinder;
    private CartViewModel cartViewModel;

    //Khi nhấn vào nút Đặt hàng
    @OnClick(R.id.btn_place_order)
    void onPlaceOrderClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.one_more_step);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_order, null);

        EditText edt_address = view.findViewById(R.id.edt_address);
        EditText edt_comment = view.findViewById(R.id.edt_comment);

        TextView txt_address = view.findViewById(R.id.txt_address_detail);

        RadioButton rdi_home = view.findViewById(R.id.rdi_home_address);
        RadioButton rdi_other_address = view.findViewById(R.id.rdi_other_address);
        RadioButton rdi_ship_to_this = view.findViewById(R.id.rdi_ship_this_address);
        RadioButton rdi_cod = view.findViewById(R.id.rdi_cod);
        RadioButton rdi_braintree = view.findViewById(R.id.rdi_braintree);

        //Project not billing so don't use BrainTree Payment
        rdi_braintree.setOnClickListener(v -> Toast.makeText(getContext(), R.string.braintree_not_available, Toast.LENGTH_SHORT).show());

        //Data
        edt_address.setText(Common.currentUser.getAddress()); //By default we select home address, so user's address will display

        //Event

        //Radio button Home
        rdi_home.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                edt_address.setText(Common.currentUser.getAddress());
                txt_address.setVisibility(View.GONE);
            }
        });

        //Radio button other address
        rdi_other_address.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                edt_address.setText(""); //Clear
                //edt_address.setHint("Enter your address");
                txt_address.setVisibility(View.GONE);
            }
        });

        //Radio button ship to this
        //Implement late with Google API
        rdi_ship_to_this.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                //Toast.makeText(getContext(), "Implement late with Google API", Toast.LENGTH_SHORT).show();

                //Permission Check
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                fusedLocationProviderClient.getLastLocation()
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            txt_address.setVisibility(View.GONE);
                        })
                        .addOnCompleteListener(task -> {
                            String coordinates = new StringBuilder()
                                    .append(task.getResult().getLatitude())
                                    .append("/")
                                    .append(task.getResult().getLongitude()).toString();

                            Single<String> singleAddress = Single.just(getAddressFromLatLng(task.getResult().getLatitude(),
                                    task.getResult().getLongitude()));

                            Disposable disposable = singleAddress.subscribeWith(new DisposableSingleObserver<String>() {

                                @Override
                                public void onSuccess(@io.reactivex.annotations.NonNull String s) {
                                    edt_address.setText(coordinates);
                                    txt_address.setText(s);
                                    txt_address.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                    edt_address.setText(coordinates);
                                    txt_address.setText(e.getMessage());
                                    txt_address.setVisibility(View.VISIBLE);
                                }
                            });
                        });
            }
        });

        builder.setView(view);
        builder.setNegativeButton(R.string.place_order_cancel, (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        builder.setPositiveButton(R.string.place_order_ok, (dialogInterface, i) -> {
            //Toast.makeText(getContext(), "Implement late!", Toast.LENGTH_SHORT).show();

            //Radio button COD Payment
            if (rdi_cod.isChecked())
                paymentCOD(edt_address.getText().toString(), edt_comment.getText().toString());

                //Radio button BrainTree
            else if (rdi_braintree.isChecked()) {

                //Project not billing so don't use BrainTree Payment
                Toast.makeText(getContext(), R.string.braintree_not_available, Toast.LENGTH_SHORT).show();

                //Payment with BrainTree
//                address = edt_address.getText().toString();
//                comment = edt_comment.getText().toString();
//                if (!TextUtils.isEmpty(Common.currentToken)) {
//                    DropInRequest dropInRequest = new DropInRequest().clientToken(Common.currentToken);
//                    startActivityForResult(dropInRequest.getIntent(getContext()), REQUEST_BRAINTREE_CODE);
//                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //Payment with COD
    private void paymentCOD(String address, String comment) {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cartItems -> {
                    //When we have all cartItems, we will get total price
                    cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Double>() {
                                @Override
                                public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                                }

                                @Override
                                public void onSuccess(@io.reactivex.annotations.NonNull Double totalPrice) {
                                    double finalPrice = totalPrice; //We will modify this formula for discount late
                                    OrderModel orderModel = new OrderModel();
                                    orderModel.setUserId(Common.currentUser.getUid());
                                    orderModel.setUserName(Common.currentUser.getName());
                                    orderModel.setUserPhone(Common.currentUser.getPhone());
                                    orderModel.setShippingAddress(address);
                                    orderModel.setComment(comment);

                                    if (currentLocation != null) {
                                        orderModel.setLat(currentLocation.getLatitude());
                                        orderModel.setLng(currentLocation.getLongitude());


                                    } else {
                                        orderModel.setLat(-0.1f);
                                        orderModel.setLng(-0.1f);
                                    }
                                    orderModel.setCartItemList(cartItems);
                                    orderModel.setTotalPayment(totalPrice);
                                    orderModel.setDiscount(0); //Modify with discount late
                                    orderModel.setFinalPayment(finalPrice);
                                    orderModel.setCod(true);
                                    orderModel.setTransactionId("Cash On Delivery");

                                    //Submit this view_orders object to Firebase
                                    //writeOrderToFirebase(view_orders);
                                    syncLocalTimeWithGlobaltime(orderModel);
                                }

                                @Override
                                public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }, throwable -> {
                    Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));
    }

    //Sync Local Time with Global time
    private void syncLocalTimeWithGlobaltime(OrderModel orderModel) {
        final DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long offset = dataSnapshot.getValue(Long.class);
                long estimatedServerTimeMs = System.currentTimeMillis() + offset; //offset is missing time between your local time and server time
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                Date resultDate = new Date(estimatedServerTimeMs);
                Log.d("TEST_DATE", "" + sdf.format(resultDate));
                listener.onLoadTimeSuccess(orderModel, estimatedServerTimeMs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onLoadTimeFailed(databaseError.getMessage());
            }
        });
    }

    //Submit this view_orders object to Firebase
    private void writeOrderToFirebase(OrderModel orderModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.ORDER_REF)
                .child(Common.createOrderNumber()) //Create view_orders number with only digit
                .setValue(orderModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task -> {

            //Write success
            cartDataSource.clearCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                        }

                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull Integer integer) {
                            //Clean success
                            Toast.makeText(getContext(), R.string.order_placed_success, Toast.LENGTH_SHORT).show();
                            //Update cart count number after submit view_orders
                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                            sumAllItemInCart();
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    //Use class Geocoder implement address
    private String getAddressFromLatLng(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        String result = "";
        try {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0); //always get first item
                StringBuilder sb = new StringBuilder(address.getAddressLine(0));
                result = sb.toString();
            } else {
                result = "Address not found";
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        return result;
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        cartViewModel =
                new ViewModelProvider(this).get(CartViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        cloudFunctions = RetrofitICloudClient.getInstance().create(ICloudFunctions.class);
        listener = this;

        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItems().observe(getViewLifecycleOwner(), cartItems -> {
            if (cartItems == null || cartItems.isEmpty()) {
                recycler_cart.setVisibility(View.GONE);
                group_place_holder.setVisibility(View.GONE);
                txt_empty_cart.setVisibility(View.VISIBLE);
            } else {
                recycler_cart.setVisibility(View.VISIBLE);
                group_place_holder.setVisibility(View.VISIBLE);
                txt_empty_cart.setVisibility(View.GONE);

                adapter = new MyCartAdaper(getContext(), cartItems);
                recycler_cart.setAdapter(adapter);
            }
        });
        unbinder = ButterKnife.bind(this, root);
        initViews();
        intitLocation();
        return root;
    }

    private void intitLocation() {
        buildLocationRequest();
        buildLocationCallback();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

        //Permission Check
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
            }
        };

    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);

    }

    private void initViews() {
        setHasOptionsMenu(true); //Show Menu Clear all Cart
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        //Ẩn fab button cart khi nhấn vào giỏ hàng
        EventBus.getDefault().postSticky(new HideFABCart(true));
        recycler_cart.setHasFixedSize(true);

        //Set layout
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        //Swipe Delete Cart
        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_cart, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 35, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("DELETE")
                                    .setMessage("Do you want to delete this item?")
                                    .setNegativeButton("CANCEL", (dialogInterface, i) -> dialogInterface.dismiss())
                                    .setPositiveButton("DELETE", (dialog, which) -> {
                                        cartDataSource.deleteCartItem(cartItem)
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe(new SingleObserver<Integer>() {
                                                    @Override
                                                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                                                    }

                                                    @Override
                                                    public void onSuccess(@io.reactivex.annotations.NonNull Integer integer) {
                                                        adapter.notifyItemRemoved(pos);
                                                        sumAllItemInCart(); //Update sum price
                                                        EventBus.getDefault().postSticky(new CounterCartEvent(true)); //Update FAB
                                                        Toast.makeText(getContext(), R.string.toast_clear_cart, Toast.LENGTH_SHORT).show();
                                                    }

                                                    @Override
                                                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    });
                            AlertDialog deteteDialog = builder.create();
                            deteteDialog.show();
                        }));
            }
        };
        //Sum all item in Cart after delete Cart
        sumAllItemInCart();
    }

    //Sum all item in Cart
    private void sumAllItemInCart() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull Double aDouble) {
                        txt_total_price.setText(new StringBuilder("Total: $").append(aDouble));
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (!e.getMessage().contains("Query returned empty"))
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false); //Hide Home menu already inflate
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.cart_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    //Clear all Cart
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_cart) {
            cartDataSource.clearCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                        }

                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull Integer integer) {
                            Toast.makeText(getContext(), R.string.toast_clearAll_cart, Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                            sumAllItemInCart();
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().postSticky(new HideFABCart(false));
        cartViewModel.onStop();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);

        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        compositeDisposable.clear();

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fusedLocationProviderClient != null) {
            //Permission Check
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateItemInCartEvent(UpdateItemInCart event) {
        if (event.getCartItem() != null) {
            // First, save state of Recycler view
            recyclerViewState = recycler_cart.getLayoutManager().onSaveInstanceState();
            cartDataSource.updateCartItems(event.getCartItem())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                        }

                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull Integer integer) {
                            calculateTotalPrice();
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState); //Fix error refresh recycler after update
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            Toast.makeText(getContext(), "[UPDATE CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }

    }

    //Calculate Total Price
    private void calculateTotalPrice() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull Double price) {
                        txt_total_price.setText(new StringBuilder("Total: $")
                                .append(Common.formatPrice(price)));
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (!e.getMessage().contains("Query returned empty"))
                            Toast.makeText(getContext(), "[SUM CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Payment with BrainTree
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BRAINTREE_CODE) {
            if (requestCode == Activity.RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                PaymentMethodNonce nonce = result.getPaymentMethodNonce();

                //calculate sum cart
                cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<Double>() {
                            @Override
                            public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                            }

                            @Override
                            public void onSuccess(@io.reactivex.annotations.NonNull Double totalPrice) {
                                //Get all item in cart to create view_orders
                                compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid())
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(cartItems -> {
                                            //Submit Payment
                                            Map<String, String> headers = new HashMap<>();
                                            headers.put("Authorization", Common.buildToken(Common.authorizeKey));
                                            compositeDisposable.add(cloudFunctions.submitPayment(
                                                    headers,
                                                    totalPrice,
                                                    nonce.getNonce())
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(braintreeTransaction -> {
                                                        if (braintreeTransaction.isSuccess()) {
                                                            double finalPrice = totalPrice; //We will modify this formula for discount late
                                                            OrderModel orderModel = new OrderModel();
                                                            orderModel.setUserId(Common.currentUser.getUid());
                                                            orderModel.setUserName(Common.currentUser.getName());
                                                            orderModel.setUserPhone(Common.currentUser.getPhone());
                                                            orderModel.setShippingAddress(address);
                                                            orderModel.setComment(comment);

                                                            if (currentLocation != null) {
                                                                orderModel.setLat(currentLocation.getLatitude());
                                                                orderModel.setLng(currentLocation.getLongitude());


                                                            } else {
                                                                orderModel.setLat(-0.1f);
                                                                orderModel.setLng(-0.1f);
                                                            }
                                                            orderModel.setCartItemList(cartItems);
                                                            orderModel.setTotalPayment(totalPrice);
                                                            orderModel.setDiscount(0); //Modify with discount late
                                                            orderModel.setFinalPayment(finalPrice);
                                                            orderModel.setCod(false);
                                                            orderModel.setTransactionId(braintreeTransaction.getTransaction().getId());


                                                            //Submit this view_orders object to Firebase
                                                            //writeOrderToFirebase(view_orders);
                                                            syncLocalTimeWithGlobaltime(orderModel);
                                                        }
                                                    }, throwable -> {
                                                        Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                    })
                                            );
                                        }, throwable -> {
                                            Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                        }));
                            }

                            @Override
                            public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    //Callback Load Time from Firebase
    @Override
    public void onLoadTimeSuccess(OrderModel orderModel, long estimateTimeInMs) {
        orderModel.setCreateDate(estimateTimeInMs);
        orderModel.setOrderStatus(0);
        writeOrderToFirebase(orderModel);
    }

    @Override
    public void onLoadTimeFailed(String message) {
        Toast.makeText(getContext(), "" + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();
    }
}