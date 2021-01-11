package com.nta.latecoffee.ui.view_orders;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidwidgets.formatedittext.widgets.FormatEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.latecoffee.R;
import com.nta.latecoffee.activity.TrackingOrderActivity;
import com.nta.latecoffee.adapter.MyOrdersAdapter;
import com.nta.latecoffee.callback.ILoadOrderCallbackListener;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.common.MySwipeHelper;
import com.nta.latecoffee.database.CartDataSource;
import com.nta.latecoffee.database.CartDatabase;
import com.nta.latecoffee.database.CartItem;
import com.nta.latecoffee.database.LocalCartDataSource;
import com.nta.latecoffee.eventbus.CounterCartEvent;
import com.nta.latecoffee.eventbus.MenuItemBack;
import com.nta.latecoffee.model.OrderModel;
import com.nta.latecoffee.model.RefundRequestModel;
import com.nta.latecoffee.model.ShippingOrderModel;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ViewOrderFragment extends Fragment implements ILoadOrderCallbackListener {

    CartDataSource cartDataSource;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    @BindView(R.id.recycler_orders)
    RecyclerView recycler_orders;
    AlertDialog dialog;
    private Unbinder unbinder;
    private ViewOrderViewModel viewOrderViewModel;

    private ILoadOrderCallbackListener listener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewOrderViewModel =
                new ViewModelProvider(this).get(ViewOrderViewModel.class);
        View root = inflater.inflate(R.layout.fragment_view_order, container, false);
        unbinder = ButterKnife.bind(this, root);

        initViews(root);
        loadOrdersFromFirebase();

        viewOrderViewModel.getMutableLiveDataOrderList().observe(getViewLifecycleOwner(), orderList -> {
            Collections.reverse(orderList); //Reverse order list to add lasted order to first
            MyOrdersAdapter adapter = new MyOrdersAdapter(getContext(), orderList);
            recycler_orders.setAdapter(adapter);


        });

        return root;
    }


    private void loadOrdersFromFirebase() {
        List<OrderModel> orderModelList = new ArrayList<>();
        FirebaseDatabase.getInstance()
                .getReference(Common.RESTAURANT_REF)
                .child(Common.currentRestaurant.getUid())
                .child(Common.ORDER_REF)
                .orderByChild("userId")
                .equalTo(Common.currentUser.getUid())
                .limitToLast(100)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot orderSnapShot : dataSnapshot.getChildren()) {
                            OrderModel orderModel = orderSnapShot.getValue(OrderModel.class);
                            orderModel.setOrderNumber(orderSnapShot.getKey()); //Remember set it
                            orderModelList.add(orderModel);
                        }
                        listener.onLoadOrderSuccess(orderModelList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onLoadOrderFailed(databaseError.getMessage());
                    }
                });
    }

    private void initViews(View root) {
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());
        listener = this;

        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(getContext()).build();

        recycler_orders.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_orders.setLayoutManager(layoutManager);
        recycler_orders.addItemDecoration(new DividerItemDecoration(getContext(),
                layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_orders, 250) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {

                //Cancel Order
                buf.add(new MyButton(getContext(), "Cancel Order", 35, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);
                            if (orderModel.getOrderStatus() == 0) {
                                if (orderModel.isCod()) {
                                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                    builder.setTitle("Cancel Order")
                                            .setMessage("Do you really want to cancel this order?")
                                            .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss())
                                            .setPositiveButton("Yes", (dialogInterface, i) -> {

                                                Map<String, Object> update_data = new HashMap<>();
                                                update_data.put("orderStatus", -1); //Cancel order

                                                FirebaseDatabase.getInstance()

                                                        .getReference(Common.RESTAURANT_REF)
                                                        .child(Common.currentRestaurant.getUid())
                                                        .child(Common.ORDER_REF)
                                                        .child(orderModel.getOrderNumber())
                                                        .updateChildren(update_data)
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnSuccessListener(aVoid -> {
                                                            orderModel.setOrderStatus(-1); //Local update
                                                            ((MyOrdersAdapter) recycler_orders.getAdapter()).setItemAtPosition(pos, orderModel);
                                                            recycler_orders.getAdapter().notifyItemChanged(pos);
                                                            Toast.makeText(getContext(), "Cancel order successfully!", Toast.LENGTH_SHORT).show();
                                                        });

                                            });
                                    androidx.appcompat.app.AlertDialog dialog = builder.create();
                                    dialog.show();
                                } else //Not COD
                                {
                                    View layout_refund_request = LayoutInflater.from(getContext())
                                            .inflate(R.layout.layout_refund_request, null);

                                    EditText edt_card_name = layout_refund_request.findViewById(R.id.edt_card_name);
                                    FormatEditText edt_card_number = layout_refund_request.findViewById(R.id.edt_card_number);
                                    FormatEditText edt_card_exp = layout_refund_request.findViewById(R.id.edt_card_exp);

                                    //Format credit card
                                    edt_card_number.setFormat("---- ---- ---- ----");
                                    edt_card_exp.setFormat("--/--");

                                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                    builder.setTitle("Cancel Order")
                                            .setMessage("Do you really want to cancel this order?")
                                            .setView(layout_refund_request)
                                            .setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss())
                                            .setPositiveButton("Yes", (dialogInterface, i) -> {

                                                RefundRequestModel refundRequestModel = new RefundRequestModel();
                                                refundRequestModel.setName(Common.currentUser.getName());
                                                refundRequestModel.setPhone(Common.currentUser.getPhone());
                                                refundRequestModel.setCardName(edt_card_name.getText().toString());
                                                refundRequestModel.setCardNumber(edt_card_number.getText().toString());
                                                refundRequestModel.setCardExp(edt_card_exp.getText().toString());
                                                refundRequestModel.setAmount(orderModel.getFinalPayment());


                                                FirebaseDatabase.getInstance()

                                                        .getReference(Common.RESTAURANT_REF)
                                                        .child(Common.currentRestaurant.getUid())
                                                        .child(Common.REQUEST_REFUND_REF)
                                                        .child(orderModel.getOrderNumber())
                                                        .setValue(refundRequestModel)
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnSuccessListener(aVoid -> {

                                                            //Update Firebase
                                                            Map<String, Object> update_data = new HashMap<>();
                                                            update_data.put("orderStatus", -1); //Cancel order

                                                            FirebaseDatabase.getInstance()
                                                                    .getReference(Common.RESTAURANT_REF)
                                                                    .child(Common.currentRestaurant.getUid())
                                                                    .child(Common.ORDER_REF)
                                                                    .child(orderModel.getOrderNumber())
                                                                    .updateChildren(update_data)
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnSuccessListener(a -> {
                                                                        orderModel.setOrderStatus(-1); //Local update
                                                                        ((MyOrdersAdapter) recycler_orders.getAdapter()).setItemAtPosition(pos, orderModel);
                                                                        recycler_orders.getAdapter().notifyItemChanged(pos);
                                                                        Toast.makeText(getContext(), "Cancel order successfully!", Toast.LENGTH_SHORT).show();
                                                                    });
                                                        });

                                            });
                                    androidx.appcompat.app.AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                            } else {
                                Toast.makeText(getContext(), new StringBuilder("Your order was change to ")
                                        .append(Common.convertStatusToText(orderModel.getOrderStatus()))
                                        .append(", so you can't cancel it!"), Toast.LENGTH_SHORT).show();
                            }
                        }));

                //Tracking Order
                buf.add(new MyButton(getContext(), "Tracking Order", 35, 0, Color.parseColor("#001970"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);

                            //Fetch from Firebase
                            FirebaseDatabase.getInstance()
                                    .getReference(Common.RESTAURANT_REF)
                                    .child(Common.currentRestaurant.getUid())
                                    .child(Common.SHIPPING_ORDER_REF)
                                    .child(orderModel.getOrderNumber())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                Common.currentShippingOrder = dataSnapshot.getValue(ShippingOrderModel.class);
                                                Common.currentShippingOrder.setKey(dataSnapshot.getKey());
                                                if (Common.currentShippingOrder.getCurrentLat() != -1 &&
                                                        Common.currentShippingOrder.getCurrentLng() != -1) {
                                                    startActivity(new Intent(getContext(), TrackingOrderActivity.class));
                                                } else {
                                                    Toast.makeText(getContext(), "Shipper not start ship your order, just wait", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(getContext(), "Your order just placed, must be wait it shipping", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            Toast.makeText(getContext(), "" + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }));

                //Repeat Order
                buf.add(new MyButton(getContext(), "Repeat Order", 35, 0, Color.parseColor("#5D4037"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);

                            dialog.show(); //show dialog if process is run on long time
                            cartDataSource.clearCart(Common.currentUser.getUid(),
                                    Common.currentRestaurant.getUid()) //Clear all item first
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(@io.reactivex.annotations.NonNull Integer integer) {
                                            //After clean cart, just add new
                                            CartItem[] cartItems = orderModel
                                                    .getCartItemList().toArray(new CartItem[orderModel.getCartItemList().size()]);

                                            //Insert new
                                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItems)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(()->{
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(), "Add all item in order to cart success!", Toast.LENGTH_SHORT).show();
                                                        EventBus.getDefault().postSticky(new CounterCartEvent(true)); //Count fab

                                                    },throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();

                                                    })


                                            );

                                        }

                                        @Override
                                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(), "[Error]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });


                        }));
            }
        };


    }

    @Override
    public void onLoadOrderSuccess(List<OrderModel> orderModelList) {
        dialog.dismiss();
        viewOrderViewModel.setMutableLiveDataOrderList(orderModelList);
    }

    @Override
    public void onLoadOrderFailed(String message) {
        dialog.dismiss();
        Toast.makeText(getContext(), "" + message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();
    }
}
