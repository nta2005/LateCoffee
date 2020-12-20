package com.nta.lc_server.ui.order;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.nta.lc_server.R;
import com.nta.lc_server.activity.TrackingOrderActivity;
import com.nta.lc_server.adapter.MyOrderAdapter;
import com.nta.lc_server.adapter.MyShipperSelectionAdapter;
import com.nta.lc_server.callback.IShipperLoadCallbackListener;
import com.nta.lc_server.common.BottomSheetOrderFragment;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.common.MySwipeHelper;
import com.nta.lc_server.eventbus.ChangeMenuClick;
import com.nta.lc_server.eventbus.LoadOrderEvent;
import com.nta.lc_server.eventbus.PrintOrderEvent;
import com.nta.lc_server.model.FCMSendData;
import com.nta.lc_server.model.OrderModel;
import com.nta.lc_server.model.ShipperModel;
import com.nta.lc_server.model.ShippingOrderModel;
import com.nta.lc_server.model.TokenModel;
import com.nta.lc_server.remote.IFCMService;
import com.nta.lc_server.remote.RetrofitFCMClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class OrderFragment extends Fragment implements IShipperLoadCallbackListener {

    @BindView(R.id.recycler_order)
    RecyclerView recycler_order;
    @BindView(R.id.txt_order_filter)
    TextView txt_order_filter;

    RecyclerView recycler_shipper;

    Unbinder unbinder;
    LayoutAnimationController layoutAnimationController;
    MyOrderAdapter adapter;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IFCMService ifcmService;

    private IShipperLoadCallbackListener shipperLoadCallbackListener;


    private OrderViewModel orderViewModel;
    private MyShipperSelectionAdapter myShipperSelectedAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        orderViewModel =
                new ViewModelProvider(this).get(OrderViewModel.class);
        View root = inflater.inflate(R.layout.fragment_order, container, false);
        unbinder = ButterKnife.bind(this, root);
        initViews();
        orderViewModel.getMessageError().observe(getViewLifecycleOwner(), s -> {
            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
        });
        orderViewModel.getOrderModelMutableLiveData().observe(getViewLifecycleOwner(), orderModelList -> {
            if (orderModelList != null) {
                adapter = new MyOrderAdapter(getContext(), orderModelList);
                recycler_order.setAdapter(adapter);
                recycler_order.setLayoutAnimation(layoutAnimationController);

                updateTextCounter();
            }
        });
        return root;
    }

    private void initViews() {
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);
        shipperLoadCallbackListener = this;
        setHasOptionsMenu(true);

        recycler_order.setHasFixedSize(true);
        recycler_order.setLayoutManager(new LinearLayoutManager(getContext()));

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);

        //Get Size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;


        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_order, width / 6) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {

                //Button Print
                buf.add(new MyButton(getContext(), "Print", 35, 0, Color.parseColor("#8b0010"),
                        pos -> {
                            Dexter.withContext(getContext())
                                    .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    .withListener(new PermissionListener() {
                                        @Override
                                        public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                                            EventBus.getDefault().postSticky(new PrintOrderEvent(new StringBuilder(Common.getAppPath(getActivity()))
                                                    .append(Common.FILE_PRINT).toString(),
                                                    adapter.getItemAtPosition(pos)));
                                        }

                                        @Override
                                        public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                                            Toast.makeText(getContext(), "Please accept this permission", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                                           
                                        }
                                    }).check();
                        }));

                //Button Directions
                buf.add(new MyButton(getContext(), "Directions", 35, 0, Color.parseColor("#9b0000"),
                        pos -> {
                            OrderModel orderModel = ((MyOrderAdapter) recycler_order.getAdapter())
                                    .getItemAtPosition(pos);
                            if (orderModel.getOrderStatus() == 1) //Shipping
                            {
                                Common.currentOrderSelected = orderModel;
                                startActivity(new Intent(getContext(), TrackingOrderActivity.class));
                            } else {
                                Toast.makeText(getContext(), new StringBuilder("Your order is ")
                                        .append(Common.convertStatusToString(orderModel.getOrderStatus()))
                                        .append(". So you can't track directions"), Toast.LENGTH_SHORT).show();
                            }


                        }));

                //Button Call
                buf.add(new MyButton(getContext(), "Call", 35, 0, Color.parseColor("#560027"),
                        pos -> {
                            Dexter.withContext(getContext())
                                    .withPermission(Manifest.permission.CALL_PHONE)
                                    .withListener(new PermissionListener() {
                                        @Override
                                        public void onPermissionGranted(PermissionGrantedResponse response) {
                                            OrderModel orderModel = adapter.getItemAtPosition(pos);
                                            Intent intent = new Intent();
                                            intent.setAction(Intent.ACTION_DIAL);
                                            intent.setData(Uri.parse(new StringBuilder("tel: ")
                                                    .append(orderModel.getUserPhone()).toString()));
                                            startActivity(intent);
                                        }

                                        @Override
                                        public void onPermissionDenied(PermissionDeniedResponse response) {
                                            Toast.makeText(getContext(), "You must accept " + response.getPermissionName(), Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                                        }
                                    }).check(); //Don't forget call check
                        }));

                //Button Remove
                buf.add(new MyButton(getContext(), "Remove", 35, 0, Color.parseColor("#12005e"),
                        pos -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                                    .setTitle("Delete")
                                    .setMessage("Do you really want to delete this order?")
                                    .setNegativeButton("CANCEL", (dialogInterface, i) -> {
                                        dialogInterface.dismiss();
                                    })
                                    .setPositiveButton("DELETE", (dialogInterface, i) -> {
                                        OrderModel orderModel = adapter.getItemAtPosition(pos);
                                        FirebaseDatabase.getInstance()
                                                .getReference(Common.RESTAURANT_REF)
                                                .child(Common.currentServerUser.getRestaurant())
                                                .child(Common.ORDER_REF)
                                                .child(orderModel.getKey())
                                                .removeValue()
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnSuccessListener(aVoid -> {
                                                    adapter.removeItem(pos);
                                                    adapter.notifyItemRemoved(pos);
                                                    updateTextCounter();
                                                    dialogInterface.dismiss();
                                                    Toast.makeText(getContext(), "Order has been delete!", Toast.LENGTH_SHORT).show();
                                                });

                                    });

                            //Create dialog
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                            negativeButton.setTextColor(Color.GRAY);

                            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            positiveButton.setTextColor(Color.RED);
                        }));

                //Button Edit
                buf.add(new MyButton(getContext(), "Edit", 35, 0, Color.parseColor("#336699"),
                        pos -> {
                            showEditDialog(adapter.getItemAtPosition(pos), pos);
                        }));
            }
        };

    }

    private void showEditDialog(OrderModel orderModel, int pos) {
        View layout_dialog;
        AlertDialog.Builder builder;
        if (orderModel.getOrderStatus() == 0) {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipping, null);
            recycler_shipper = layout_dialog.findViewById(R.id.recycler_shippers);
            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
                    .setView(layout_dialog);

        } else if (orderModel.getOrderStatus() == -1) //Cancelled
        {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_cancelled, null);
            builder = new AlertDialog.Builder(getContext())
                    .setView(layout_dialog);
        } else //Shipped
        {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipped, null);
            builder = new AlertDialog.Builder(getContext())
                    .setView(layout_dialog);
        }

        //View
        Button btn_ok = layout_dialog.findViewById(R.id.btn_ok);
        Button btn_cancel = layout_dialog.findViewById(R.id.btn_cancel);

        RadioButton rdi_shipping = layout_dialog.findViewById(R.id.rdi_shipping);
        RadioButton rdi_shipped = layout_dialog.findViewById(R.id.rdi_shipped);
        RadioButton rdi_cancelled = layout_dialog.findViewById(R.id.rdi_cancelled);
        RadioButton rdi_delete = layout_dialog.findViewById(R.id.rdi_delete);
        RadioButton rdi_restore_placed = layout_dialog.findViewById(R.id.rdi_restore_placed);

        TextView txt_status = layout_dialog.findViewById(R.id.txt_status);

        //Set data
        txt_status.setText(new StringBuilder("Order Status (")
                .append(Common.convertStatusToString(orderModel.getOrderStatus()))
                .append(")"));

        //Create Dialog
        AlertDialog dialog = builder.create();
        if (orderModel.getOrderStatus() == 0) //Shipping
            loadShipperList(pos, orderModel, dialog, btn_ok, btn_cancel,
                    rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);
        else
            showDialog(pos, orderModel, dialog, btn_ok, btn_cancel,
                    rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);

    }

    private void loadShipperList(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        List<ShipperModel> tempList = new ArrayList<>();
        DatabaseReference shipperRef = FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPER_REF);
        Query shipperActive = shipperRef.orderByChild("active").equalTo(true); //Load only shipper active by Server app
        shipperActive.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot shipperSnapShot : dataSnapshot.getChildren()) {
                    ShipperModel shipperModel = shipperSnapShot.getValue(ShipperModel.class);
                    shipperModel.setKey(shipperSnapShot.getKey());
                    tempList.add(shipperModel);
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(pos, orderModel, tempList,
                        dialog,
                        btn_ok, btn_cancel,
                        rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                shipperLoadCallbackListener.onShipperLoadFailed(databaseError.getMessage());
            }
        });


    }

    private void showDialog(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        dialog.show();
        //Custom dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);

        btn_cancel.setOnClickListener(view -> {
            dialog.dismiss();
        });

        btn_ok.setOnClickListener(view -> {

            if (rdi_cancelled != null && rdi_cancelled.isChecked()) {
                updateOrder(pos, orderModel, -1);
                dialog.dismiss();
            } else if (rdi_shipping != null && rdi_shipping.isChecked()) //Shipping
            {
                //updateOrder(pos, orderModel, 1);
                ShipperModel shipperModel = null;
                if (myShipperSelectedAdapter != null) {
                    shipperModel = myShipperSelectedAdapter.getSelectedShipper();
                    if (shipperModel != null) {
                        createShippingOrder(pos, shipperModel, orderModel, dialog);
                    } else
                        Toast.makeText(getContext(), "Please select Shipper", Toast.LENGTH_SHORT).show();


                }
            } else if (rdi_shipped != null && rdi_shipped.isChecked()) {
                updateOrder(pos, orderModel, 2);
                dialog.dismiss();
            } else if (rdi_restore_placed != null && rdi_restore_placed.isChecked()) {
                updateOrder(pos, orderModel, 0);
                dialog.dismiss();
            } else if (rdi_delete != null && rdi_delete.isChecked()) {
                deleteOrder(pos, orderModel);
                dialog.dismiss();
            }
        });
    }

    //edit v101
    private void createShippingOrder(int pos, ShipperModel shipperModel, OrderModel orderModel, AlertDialog dialog) {

        ShippingOrderModel shippingOrderModel = new ShippingOrderModel();
        shippingOrderModel.setRestaurantKey(Common.currentServerUser.getRestaurant()); //v101
        shippingOrderModel.setShipperPhone(shipperModel.getPhone());
        shippingOrderModel.setShipperName(shipperModel.getName());
        shippingOrderModel.setOrderModel(orderModel);
        shippingOrderModel.setStartTrip(false);
        shippingOrderModel.setCurrentLat(-1.0);
        shippingOrderModel.setCurrentLng(-1.0);

        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser.getRestaurant())
                .child(Common.SHIPPING_ORDER_REF)
                .child(orderModel.getKey()) //v64: Change push() to child(orderModel.getKey())
                .setValue(shippingOrderModel)
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dialog.dismiss();
                        //updateOrder(pos, orderModel, 1);
                        //First, get token of user
                        FirebaseDatabase.getInstance()
                                .getReference(Common.TOKEN_REF)
                                .child(shipperModel.getKey())
                                .addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            TokenModel tokenModel = dataSnapshot.getValue(TokenModel.class);
                                            Map<String, String> notiData = new HashMap<>();
                                            notiData.put(Common.NOTI_TITLE, "You have new order need ship ");
                                            notiData.put(Common.NOTI_CONTENT, new StringBuilder("You have new order need ship to ")
                                                    .append(orderModel.getUserPhone()).toString());

                                            FCMSendData sendData = new FCMSendData(tokenModel.getToken(), notiData);

                                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(fcmResponse -> {
                                                        dialog.dismiss();
                                                        if (fcmResponse.getSuccess() == 1) {
                                                            updateOrder(pos, orderModel, 1);
                                                            Toast.makeText(getContext(), "Update order success!", Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Toast.makeText(getContext(), "Failed to send to shipper! Order wasn't update!", Toast.LENGTH_SHORT).show();
                                                        }

                                                    }, throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }));
                                        } else {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(), "Token not found", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        //Toast.makeText(getContext(), "Order has been sent to shipper", Toast.LENGTH_SHORT).show();
                    }

                });
    }

    private void deleteOrder(int pos, OrderModel orderModel) {
        if (!TextUtils.isEmpty(orderModel.getKey())) {
            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentServerUser.getRestaurant())
                    .child(Common.ORDER_REF)
                    .child(orderModel.getKey())
                    .removeValue()
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnSuccessListener(aVoid -> {
                        adapter.removeItem(pos);
                        adapter.notifyItemRemoved(pos);
                        updateTextCounter();
                        Toast.makeText(getContext(), "Delete order success!", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "Order number not be null or empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateOrder(int pos, OrderModel orderModel, int status) {
        if (!TextUtils.isEmpty(orderModel.getKey())) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("orderStatus", status);

            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentServerUser.getRestaurant())
                    .child(Common.ORDER_REF)
                    .child(orderModel.getKey())
                    .updateChildren(updateData)
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnSuccessListener(aVoid -> {

                        //Show dialog
                        AlertDialog dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
                        dialog.show();

                        //First, get token of user
                        FirebaseDatabase.getInstance()
                                .getReference(Common.TOKEN_REF)
                                .child(orderModel.getUserId())
                                .addListenerForSingleValueEvent(new ValueEventListener() {

                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            TokenModel tokenModel = dataSnapshot.getValue(TokenModel.class);
                                            Map<String, String> notiData = new HashMap<>();
                                            notiData.put(Common.NOTI_TITLE, "Your order was update");
                                            notiData.put(Common.NOTI_CONTENT, new StringBuilder("Your order ")
                                                    .append(orderModel.getKey())
                                                    .append(" was update to ")
                                                    .append(Common.convertStatusToString(status)).toString());

                                            FCMSendData sendData = new FCMSendData(tokenModel.getToken(), notiData);

                                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(fcmResponse -> {
                                                        dialog.dismiss();
                                                        if (fcmResponse.getSuccess() == 1) {
                                                            Toast.makeText(getContext(), "Update order success!", Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Toast.makeText(getContext(), "Update order success but failed to send notification!", Toast.LENGTH_SHORT).show();
                                                        }

                                                    }, throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }));
                                        } else {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(), "Token not found", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                        adapter.removeItem(pos);
                        adapter.notifyItemRemoved(pos);
                        updateTextCounter();

                    });
        } else {
            Toast.makeText(getContext(), "Order number not be null or empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTextCounter() {
        txt_order_filter.setText(new StringBuilder("Orders (")
                .append(adapter.getItemCount())
                .append(")"));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.order_filter_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_filter) {
            BottomSheetOrderFragment bottomSheetOrderFragment = BottomSheetOrderFragment.getInstance();
            bottomSheetOrderFragment.show(getActivity().getSupportFragmentManager(), "OrderFilter");
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent.class))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent.class);
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new ChangeMenuClick(true));
        super.onDestroy();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onLoadOrderEvent(LoadOrderEvent event) {
        orderViewModel.loadOrderByStatus(event.getStatus());
    }

    @Override
    public void onShipperLoadSuccess(List<ShipperModel> shipperModelList) {
        //Do nothing
    }

    @Override
    public void onShipperLoadSuccess(int pos, OrderModel orderModel, List<ShipperModel> shipperModels, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        if (recycler_shipper != null) {
            recycler_shipper.setHasFixedSize(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recycler_shipper.setLayoutManager(layoutManager);
            recycler_shipper.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

            myShipperSelectedAdapter = new MyShipperSelectionAdapter(getContext(), shipperModels);
            recycler_shipper.setAdapter(myShipperSelectedAdapter);
        }
        showDialog(pos, orderModel, dialog, btn_ok, btn_cancel,
                rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);
    }

    @Override
    public void onShipperLoadFailed(String message) {
        Toast.makeText(getContext(), "" + message, Toast.LENGTH_SHORT).show();
    }
}