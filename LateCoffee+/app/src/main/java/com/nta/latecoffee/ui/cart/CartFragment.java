package com.nta.latecoffee.ui.cart;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nta.latecoffee.R;
import com.nta.latecoffee.activity.ScanQRActivity;
import com.nta.latecoffee.adapter.MyCartAdapter;
import com.nta.latecoffee.callback.ILoadTimeFromFirebaseListener;
import com.nta.latecoffee.callback.ISearchCategoryCallbackListener;
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
import com.nta.latecoffee.model.AddonModel;
import com.nta.latecoffee.model.CategoryModel;
import com.nta.latecoffee.model.DiscountModel;
import com.nta.latecoffee.model.FCMSendData;
import com.nta.latecoffee.model.FoodModel;
import com.nta.latecoffee.model.OrderModel;
import com.nta.latecoffee.model.SizeModel;
import com.nta.latecoffee.remote.IFCMService;
import com.nta.latecoffee.remote.RetrofitFCMClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

//Sử dụng RxJava2: một Reactive dựa trên Java framework, thực hiện các tác vụ đồng bộ hoặc không đồng bộ trong chương trình.
// subscribeOn() Quyết định request trên thực hiện trên thread nào (thread được cung cấp bởi Scheduler).
// observeOn() Chỉ định thread mà operator hoặc subscriber được gọi sau nó.
// subscribe() sẽ sử dụng những item trên thread mà observeOn quyết định ở trên.

public class CartFragment extends Fragment implements ILoadTimeFromFirebaseListener, ISearchCategoryCallbackListener, TextWatcher {

    private static final int SCAN_QR_PERMISSION = 7171;
    private final List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    //Observable là nơi phát ra các phần tử 1 cách tuần tự.
    //Observer nhận các phần tử mà Observable chuyển đến.
    //*Disposable là sự liên kết giữa Observable và Observer.
    private final CompositeDisposable compositeDisposable = new CompositeDisposable(); //Tập hợp các Disposable
    //Callback
    IFCMService ifcmService;
    ILoadTimeFromFirebaseListener listener;
    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;

    @BindView(R.id.edt_discount_code)
    EditText edt_discount_code;

    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;
    View view;
    MySwipeHelper swipeButton;
    private BottomSheetDialog addonBottomSheetDialog;
    private ChipGroup chip_group_addon, chip_group_user_selected_addon;
    private EditText edt_search;
    private ISearchCategoryCallbackListener searchCategoryCallbackListener;
    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;
    private MyCartAdapter adapter;
    private Unbinder unbinder;
    private CartViewModel cartViewModel;

    //Scan QRCode
    @OnClick(R.id.img_scan)
    void onScanQRCode() {
        startActivityForResult(new Intent(requireContext(), ScanQRActivity.class), SCAN_QR_PERMISSION);
    }

    //Apply Discount
    @OnClick(R.id.img_check)
    void onApplyDiscount() {
        if (!TextUtils.isEmpty(edt_discount_code.getText().toString())) {
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setCancelable(false)
                    .setMessage("Vui lòng đợi...")
                    .create();
            dialog.show();

            final DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
            final DatabaseReference discountRef = FirebaseDatabase.getInstance()
                    .getReference(Common.DISCOUNT_REF);

            offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    long offset = dataSnapshot.getValue(Long.class);
                    long estimatedServerTimeMs = System.currentTimeMillis() + offset;

                    //discountRef.child(edt_discount_code.getText().toString().toLowerCase()) //use if server use too
                    discountRef.child(edt_discount_code.getText().toString())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        DiscountModel discountModel = dataSnapshot.getValue(DiscountModel.class);
                                        discountModel.setKey(dataSnapshot.getKey());
                                        if (discountModel.getUntilDate() < estimatedServerTimeMs) {
                                            dialog.dismiss();
                                            listener.onLoadTimeFailed("Mã giảm giá đã hết hạn!");
                                        } else {
                                            dialog.dismiss();
                                            Common.discountApply = discountModel;
                                            sumAllItemInCart();
                                            Toast.makeText(getContext(), "Bạn được giảm " + discountModel.getPercent() + "%", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        dialog.dismiss();
                                        listener.onLoadTimeFailed("Mã giảm giá không hợp lệ");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    dialog.dismiss();
                                    listener.onLoadTimeFailed(databaseError.getMessage());
                                }
                            });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    dialog.dismiss();
                    listener.onLoadTimeFailed(databaseError.getMessage());
                }
            });
        }
    }

    //Nhấn vào nút Đặt hàng
    @OnClick(R.id.btn_place_order)
    void onPlaceOrderClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.one_more_step);
        if (view == null)
            view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_order, null);

        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }

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
        //Implement with Google API
        rdi_ship_to_this.setOnCheckedChangeListener((compoundButton, isChecked) -> {

            if (isChecked) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

            //Radio button COD Payment
            if (rdi_cod.isChecked())
                paymentCOD(edt_address.getText().toString(), edt_comment.getText().toString());

                //Radio button BrainTree
            else if (rdi_braintree.isChecked()) {
                //Project not billing so don't use BrainTree Payment
                Toast.makeText(getContext(), R.string.braintree_not_available, Toast.LENGTH_SHORT).show();
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
                                    //Nothing
                                }

                                @Override
                                public void onSuccess(@io.reactivex.annotations.NonNull Double totalPrice) {
                                    double finalPrice = totalPrice; //We will modify this formula for discount late
                                    OrderModel order = new OrderModel();
                                    order.setUserId(Common.currentUser.getUid());
                                    order.setUserName(Common.currentUser.getName());
                                    order.setUserPhone(Common.currentUser.getPhone());
                                    order.setShippingAddress(address);
                                    order.setComment(comment);

                                    if (currentLocation != null) {
                                        order.setLat(currentLocation.getLatitude());
                                        order.setLng(currentLocation.getLongitude());
                                    } else {
                                        order.setLat(-0.1f);
                                        order.setLng(-0.1f);
                                    }
                                    order.setCartItemList(cartItems);
                                    order.setTotalPayment(totalPrice);
                                    if (Common.discountApply != null) //update v103
                                        order.setDiscount(Common.discountApply.getPercent());
                                    else
                                        order.setDiscount(0);
                                    order.setFinalPayment(finalPrice);
                                    order.setCod(true);
                                    order.setTransactionId("Thanh toán khi giao hàng");

                                    syncLocalTimeWithGlobaltime(order);
                                }

                                @Override
                                public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                    if (!e.getMessage().contains("Query returned empty result set"))
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

                            Map<String, String> notiData = new HashMap<>();
                            notiData.put(Common.NOTI_TITLE, "Đơn hàng mới");
                            notiData.put(Common.NOTI_CONTENT, "Bạn có một đơn hàng mới từ " + Common.currentUser.getPhone());

                            FCMSendData sendData = new FCMSendData(Common.createTopicOrder(), notiData);

                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(fcmResponse -> {
                                        Toast.makeText(getContext(), R.string.order_placed_success, Toast.LENGTH_SHORT).show();
                                        //Update cart count number after submit view_orders
                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                    }, throwable -> {
                                        Toast.makeText(getContext(), "Đã gửi đơn hàng, nhưng gửi thông báo thất bại!", Toast.LENGTH_SHORT).show();
                                        //Update cart count number after submit view_orders
                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                    }));
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
                result = "Không tìm thấy địa chỉ này!";
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        return result;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);
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

                adapter = new MyCartAdapter(getContext(), cartItems);
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
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

        searchCategoryCallbackListener = this;

        initPlaceClient();

        setHasOptionsMenu(true); //Show Menu Clear all Cart
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        ////////////Ẩn fab button cart khi nhấn vào giỏ hàng
        EventBus.getDefault().postSticky(new HideFABCart(true));
        recycler_cart.setHasFixedSize(true);

        /////Set layout/////
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        ///////////Swipe Button////////////
        swipeButton = new MySwipeHelper(getContext(), recycler_cart, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Xoá", 35, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("XOÁ VẬT PHẨM")
                                    .setMessage("Bạn có muốn xoá vật phẩm này?")
                                    .setNegativeButton("Đóng", (dialogInterface, i) -> dialogInterface.dismiss())
                                    .setPositiveButton("Xoá", (dialog, which) -> {
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

                buf.add(new MyButton(getContext(), "Cập nhật", 35, 0, Color.parseColor("#5D4037"),
                        pos -> {
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            FirebaseDatabase.getInstance()
                                    .getReference(Common.CATEGORY_REF)
                                    .child(cartItem.getCategoryId())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                CategoryModel categoryModel = dataSnapshot.getValue(CategoryModel.class);
                                                searchCategoryCallbackListener.onSearchCategoryFound(categoryModel, cartItem);

                                            } else {
                                                searchCategoryCallbackListener.onSearchCategoryNotFound("Vật phẩm không tồn tại!");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            searchCategoryCallbackListener.onSearchCategoryNotFound(databaseError.getMessage());

                                        }
                                    });
                        }));
            }
        };

        //Sum all item in Cart after delete Cart
        sumAllItemInCart();

        //Addon
        addonBottomSheetDialog = new BottomSheetDialog(getContext(), R.style.DialogStyle);
        View layout_addon_display = getLayoutInflater().inflate(R.layout.layout_addon_display, null);
        chip_group_addon = layout_addon_display.findViewById(R.id.chip_group_addon);
        edt_search = layout_addon_display.findViewById(R.id.edt_search);
        addonBottomSheetDialog.setContentView(layout_addon_display);

        addonBottomSheetDialog.setOnDismissListener(dialogInterface -> {
            displayUserSelectedAddon(chip_group_user_selected_addon);
            calculateTotalPrice();
        });
    }

    private void displayUserSelectedAddon(ChipGroup chip_group_user_selected_addon) {
        if (Common.selectedFood.getUserSelectedAddon() != null && Common.selectedFood.getUserSelectedAddon().size() > 0) {
            chip_group_user_selected_addon.removeAllViews();
            for (AddonModel addonModel : Common.selectedFood.getUserSelectedAddon()) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_chip_with_delete_icon, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(")
                        .append(addonModel.getPrice()).append(" đ)"));
                chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                    if (isChecked) {
                        if (Common.selectedFood.getUserSelectedAddon() == null)
                            Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                        Common.selectedFood.getUserSelectedAddon().add(addonModel);
                    }
                });
                chip_group_user_selected_addon.addView(chip);
            }
        } else
            chip_group_user_selected_addon.removeAllViews();

    }

    private void initPlaceClient() {
        Places.initialize(getContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(getContext());
    }

    //Sum all item in Cart (update v103 apply discount)
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
                        //update v103
                        if (Common.discountApply != null) {
                            aDouble = aDouble - (aDouble * Common.discountApply.getPercent() / 100);
                            txt_total_price.setText(new StringBuilder("Tổng cộng: ").append(Common.formatPrice(aDouble)).append(" đ")
                                    .append("(- ")
                                    .append(Common.discountApply.getPercent())
                                    .append("%)"));
                        } else {
                            txt_total_price.setText(new StringBuilder("Tổng cộng: ").append(Common.formatPrice(aDouble)).append(" đ"));
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (!e.getMessage().contains("Query returned empty result set"))
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
                            //Nothing
                        }

                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull Integer integer) {
                            calculateTotalPrice();
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState); //Fix error refresh recycler after update
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            if (!e.getMessage().contains("Query returned empty result set"))
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
                        //Nothing
                    }

                    @Override
                    public void onSuccess(@io.reactivex.annotations.NonNull Double price) {
                        txt_total_price.setText(new StringBuilder("Tổng cộng: ")
                                //.append(Common.formatPrice(price)).append(" đ"));
                                .append(Common.formatPrice(price)).append(" đ"));
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        if (!e.getMessage().contains("Query returned empty result set"))
                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Result with Discount
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_QR_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                //edt_discount_code.setText(data.getStringExtra(Common.QR_CODE_TAG.toLowerCase())); // use if server too
                edt_discount_code.setText(data.getStringExtra(Common.QR_CODE_TAG));
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
    public void onLoadOnlyTimeSuccess(long estimateTimeInMs) {
        //Do nothing
    }

    @Override
    public void onLoadTimeFailed(String message) {
        Toast.makeText(getContext(), "" + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSearchCategoryFound(CategoryModel categoryModel, CartItem cartItem) {
        FoodModel foodModel = Common.findFoodInListById(categoryModel, cartItem.getFoodId());
        if (foodModel != null) {
            showUpdateDialog(cartItem, foodModel);
        } else
            Toast.makeText(getContext(), "ID vật phẩm không tồn tại!", Toast.LENGTH_SHORT).show();

    }

    private void showUpdateDialog(CartItem cartItem, FoodModel foodModel) {
        Common.selectedFood = foodModel;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_dialog_update_cart, null);
        builder.setView(itemView);

        //View
        Button btn_ok = itemView.findViewById(R.id.btn_ok);
        Button btn_cancel = itemView.findViewById(R.id.btn_cancel);

        RadioGroup rdi_group_size = itemView.findViewById(R.id.rdi_group_size);
        chip_group_user_selected_addon = itemView.findViewById(R.id.chip_group_user_selected_addon);
        ImageView img_add_on = itemView.findViewById(R.id.img_add_addon);
        img_add_on.setOnClickListener(view -> {
            if (foodModel.getAddon() != null) {
                displayAddonList();
                addonBottomSheetDialog.show();
            }
        });

        //Size
        if (foodModel.getSize() != null) {
            for (SizeModel sizeModel : foodModel.getSize()) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                    if (isChecked)
                        Common.selectedFood.setUserSelectedSize(sizeModel);
                    calculateTotalPrice();
                });

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.1f);
                radioButton.setLayoutParams(params);
                radioButton.setText(sizeModel.getName());
                radioButton.setTag(sizeModel.getPrice());

                rdi_group_size.addView(radioButton);
            }

            if (rdi_group_size.getChildCount() > 0) {
                RadioButton radioButton = (RadioButton) rdi_group_size.getChildAt(0); // Get first radio button
                radioButton.setChecked(true); //Set default at first radio button
            }
        }

        //Addon
        displayAlreadySelectedAddon(chip_group_user_selected_addon, cartItem);

        //ShowDialog
        AlertDialog dialog = builder.create();
        dialog.show();

        //Custom dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);

        //Event
        btn_ok.setOnClickListener(view -> {
            //First, delete item in cart
            cartDataSource.deleteCartItem(cartItem)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                            //Nothing
                        }

                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull Integer integer) {
                            //After that, update information and add new
                            //Update price and info

                            //Set Food Addon
                            if (Common.selectedFood.getUserSelectedAddon() != null)
                                cartItem.setFoodAddon(new Gson().toJson(Common.selectedFood.getUserSelectedAddon()));
                            else
                                cartItem.setFoodAddon("Mặc định");

                            //Set Food Size
                            if (Common.selectedFood.getUserSelectedSize() != null)
                                cartItem.setFoodSize(new Gson().toJson(Common.selectedFood.getUserSelectedSize()));
                            else
                                cartItem.setFoodSize("Mặc định");

                            //Set Food Extra Price
                            cartItem.setFoodExtraPrice(Common.calculateExtraPrice(Common.selectedFood.getUserSelectedSize(),
                                    Common.selectedFood.getUserSelectedAddon()));

                            //Insert new
                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(() -> {
                                        EventBus.getDefault().postSticky(new CounterCartEvent(true)); //Count cart again
                                        calculateTotalPrice();
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "Đã cập nhật giỏ hàng!", Toast.LENGTH_SHORT).show();

                                    }, throwable -> {
                                        Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    }));
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btn_cancel.setOnClickListener(view -> {
            dialog.dismiss();
        });
    }

    private void displayAlreadySelectedAddon(ChipGroup chip_group_user_selected_addon, CartItem cartItem) {
        //This function will display all addon we already select before add to cart and display on layout
        if (cartItem.getFoodAddon() != null && !cartItem.getFoodAddon().equals("Mặc định")) {
            List<AddonModel> addonModels = new Gson().fromJson(
                    cartItem.getFoodAddon(), new TypeToken<List<AddonModel>>() {
                    }.getType());

            Common.selectedFood.setUserSelectedAddon(addonModels);
            chip_group_user_selected_addon.removeAllViews();
            //Add all view
            for (AddonModel addonModel : addonModels) //Get Addon model from already what user have select in local cart
            {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_chip_with_delete_icon, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+")
                        .append(addonModel.getPrice()).append("đ)"));
                chip.setClickable(false);
                chip.setOnCloseIconClickListener(view -> {
                    //Remove when user select delete
                    chip_group_user_selected_addon.removeView(view);
                    Common.selectedFood.getUserSelectedAddon().remove(addonModel);
                    calculateTotalPrice();
                });
                chip_group_user_selected_addon.addView(chip);
            }
        }
    }

    private void displayAddonList() {
        if (Common.selectedFood.getAddon() != null && Common.selectedFood.getAddon().size() > 0) {
            chip_group_addon.clearCheck();
            chip_group_addon.removeAllViews();

            edt_search.addTextChangedListener(this);

            //Add all view
            for (AddonModel addonModel : Common.selectedFood.getAddon()) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_addon_item, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+")
                        .append(addonModel.getPrice()).append("đ)"));
                chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                    if (isChecked) {
                        if (Common.selectedFood.getUserSelectedAddon() == null)
                            Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                        Common.selectedFood.getUserSelectedAddon().add(addonModel);
                    }
                });
                chip_group_addon.addView(chip);
            }
        }
    }

    @Override
    public void onSearchCategoryNotFound(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        //Nothing
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        chip_group_addon.clearCheck();
        chip_group_addon.removeAllViews();
        for (AddonModel addonModel : Common.selectedFood.getAddon()) {
            if (addonModel.getName().toLowerCase().contains(charSequence.toString().toLowerCase())) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_addon_item, null);
                chip.setText(new StringBuilder(addonModel.getName()).append("(+")
                        .append(addonModel.getPrice()).append("đ)"));
                chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                    if (isChecked) {
                        if (Common.selectedFood.getUserSelectedAddon() == null)
                            Common.selectedFood.setUserSelectedAddon(new ArrayList<>());
                        Common.selectedFood.getUserSelectedAddon().add(addonModel);
                    }
                });
                chip_group_addon.addView(chip);
            }
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        //Nothing
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().removeAllStickyEvents();
        EventBus.getDefault().postSticky(new HideFABCart(false));
        EventBus.getDefault().postSticky(new CounterCartEvent(false));
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
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
        EventBus.getDefault().postSticky(new HideFABCart(true));
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        EventBus.getDefault().postSticky(new HideFABCart(false));
        EventBus.getDefault().postSticky(new CounterCartEvent(true));
        edt_discount_code.setText("");
        Common.discountApply = null;
        onApplyDiscount();
        calculateTotalPrice();
        super.onDestroy();
    }

}