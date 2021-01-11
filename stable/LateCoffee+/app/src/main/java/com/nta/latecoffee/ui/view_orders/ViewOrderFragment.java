package com.nta.latecoffee.ui.view_orders;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.androidwidgets.formatedittext.widgets.FormatEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.latecoffee.R;
import com.nta.latecoffee.adapter.MyOrdersAdapter;
import com.nta.latecoffee.callback.ILoadOrderCallbackListener;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.common.MySwipeHelper;
import com.nta.latecoffee.database.CartDataSource;
import com.nta.latecoffee.database.CartDatabase;
import com.nta.latecoffee.database.CartItem;
import com.nta.latecoffee.database.LocalCartDataSource;
import com.nta.latecoffee.eventbus.CounterCartEvent;
import com.nta.latecoffee.model.FCMSendData;
import com.nta.latecoffee.model.OrderModel;
import com.nta.latecoffee.model.RefundRequestModel;
import com.nta.latecoffee.remote.IFCMService;
import com.nta.latecoffee.remote.RetrofitFCMClient;

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

//Sử dụng RxJava2: một Reactive dựa trên Java framework, thực hiện các tác vụ đồng bộ hoặc không đồng bộ trong chương trình.
// subscribeOn() Quyết định request trên thực hiện trên thread nào (thread được cung cấp bởi Scheduler).
// observeOn() Chỉ định thread mà operator hoặc subscriber được gọi sau nó.
// subscribe() sẽ sử dụng những item trên thread mà observeOn quyết định ở trên.
public class ViewOrderFragment extends Fragment implements ILoadOrderCallbackListener {

    CartDataSource cartDataSource;
    //Observable là nơi phát ra các phần tử 1 cách tuần tự.
    //Observer nhận các phần tử mà Observable chuyển đến.
    //*Disposable là sự liên kết giữa Observable và Observer
    CompositeDisposable compositeDisposable = new CompositeDisposable(); //Tập hợp các Disposable

    IFCMService ifcmService;

    @BindView(R.id.recycler_orders)
    RecyclerView recycler_orders;
    @BindView(R.id.refreshOrder)
    SwipeRefreshLayout refreshOrder;
    AlertDialog dialog;
    MySwipeHelper swipeButton;
    private Unbinder unbinder;
    private ViewOrderViewModel viewOrderViewModel;
    private ILoadOrderCallbackListener listener;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewOrderViewModel = new ViewModelProvider(this).get(ViewOrderViewModel.class);
        View root = inflater.inflate(R.layout.fragment_view_order, container, false);
        unbinder = ButterKnife.bind(this, root);

        initViews();
        loadOrdersFromFirebase();
        reloadOrder();
        viewOrderViewModel.getMutableLiveDataOrderList().observe(getViewLifecycleOwner(), orderList -> {
            Collections.reverse(orderList); //Reverse order list to add lasted order to first
            MyOrdersAdapter adapter = new MyOrdersAdapter(getContext(), orderList);
            recycler_orders.setAdapter(adapter);
        });

        return root;
    }

    private void reloadOrder() {
        refreshOrder.setOnRefreshListener(() -> {
            loadOrdersFromFirebase();
            refreshOrder.setRefreshing(false);
        });
    }


    private void loadOrdersFromFirebase() {
        List<OrderModel> orderModelList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
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

    private void initViews() {
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);
        setHasOptionsMenu(true);
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());
        listener = this;

        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(getContext()).setMessage("Đang tải...").build();

        recycler_orders.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_orders.setLayoutManager(layoutManager);
        recycler_orders.addItemDecoration(new DividerItemDecoration(getContext(),
                layoutManager.getOrientation()));

        swipeButton = new MySwipeHelper(getContext(), recycler_orders, 250) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {

                //Huỷ đơn hàng (Chỉ có thể huỷ khi đang ở trạng thái 0: Chờ giao hàng)
                buf.add(new MyButton(getContext(), "Huỷ", 35, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);
                            if (orderModel.getOrderStatus() == 0) {
                                if (orderModel.isCod()) {
                                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                    builder.setTitle("HUỶ ĐƠN HÀNG")
                                            .setMessage("Bạn có muốn huỷ đơn hàng?")
                                            .setNegativeButton("Thoát", (dialogInterface, i) -> dialogInterface.dismiss())
                                            .setPositiveButton("Đồng ý", (dialogInterface, i) -> {

                                                Map<String, Object> update_data = new HashMap<>();
                                                update_data.put("orderStatus", -1); //Cancel order

                                                FirebaseDatabase.getInstance()
                                                        .getReference(Common.ORDER_REF)
                                                        .child(orderModel.getOrderNumber())
                                                        .updateChildren(update_data)
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnSuccessListener(aVoid -> {
                                                            orderModel.setOrderStatus(-1); //Local update
                                                            ((MyOrdersAdapter) recycler_orders.getAdapter()).setItemAtPosition(pos, orderModel);
                                                            recycler_orders.getAdapter().notifyItemChanged(pos);
                                                            //Toast.makeText(getContext(), "Đã huỷ đơn hàng!", Toast.LENGTH_SHORT).show();

                                                            Map<String, String> notiData = new HashMap<>();
                                                            notiData.put(Common.NOTI_TITLE, "Huỷ đơn hàng");
                                                            notiData.put(Common.NOTI_CONTENT, "Người dùng " + Common.currentUser.getPhone() + " đã huỷ 1 đơn hàng");

                                                            FCMSendData sendData = new FCMSendData(Common.createTopicOrder(), notiData);

                                                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                                    .subscribeOn(Schedulers.io())
                                                                    .observeOn(AndroidSchedulers.mainThread())
                                                                    .subscribe(fcmResponse -> {
                                                                        Toast.makeText(getContext(), "Đã huỷ đơn hàng!", Toast.LENGTH_SHORT).show();
                                                                        //Update cart count number after submit view_orders
                                                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                                                    }, throwable -> {
                                                                        Toast.makeText(getContext(), "Đã huỷ đơn hàng, gửi thông báo thất bại!", Toast.LENGTH_SHORT).show();
                                                                        //Update cart count number after submit view_orders
                                                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                                                    }));
                                                        });
                                            });
                                    androidx.appcompat.app.AlertDialog dialog = builder.create();
                                    dialog.show();
                                } else //Not COD (yêu cầu hoàn lại tiền)
                                {
                                    View layout_refund_request = LayoutInflater.from(getContext())
                                            .inflate(R.layout.layout_refund_request, null);

                                    EditText edt_card_name = layout_refund_request.findViewById(R.id.edt_card_name);
                                    FormatEditText edt_card_number = layout_refund_request.findViewById(R.id.edt_card_number);
                                    FormatEditText edt_card_exp = layout_refund_request.findViewById(R.id.edt_card_exp);

                                    //Format credit card
                                    edt_card_number.setFormat("---- ---- ---- ----");
                                    edt_card_exp.setFormat("--/--");

                                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                    builder.setTitle("HUỶ ĐƠN HÀNG")
                                            .setMessage("Bạn có muốn huỷ đơn hàng?")
                                            .setView(layout_refund_request)
                                            .setNegativeButton("Thoát", (dialogInterface, i) -> dialogInterface.dismiss())
                                            .setPositiveButton("Đồng ý", (dialogInterface, i) -> {

                                                RefundRequestModel refundRequestModel = new RefundRequestModel();
                                                refundRequestModel.setName(Common.currentUser.getName());
                                                refundRequestModel.setPhone(Common.currentUser.getPhone());
                                                refundRequestModel.setCardName(edt_card_name.getText().toString());
                                                refundRequestModel.setCardNumber(edt_card_number.getText().toString());
                                                refundRequestModel.setCardExp(edt_card_exp.getText().toString());
                                                refundRequestModel.setAmount(orderModel.getFinalPayment());


                                                FirebaseDatabase.getInstance()
                                                        .getReference(Common.REQUEST_REFUND_MODEL)
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
                                                                    .getReference(Common.ORDER_REF)
                                                                    .child(orderModel.getOrderNumber())
                                                                    .updateChildren(update_data)
                                                                    .addOnFailureListener(e -> {
                                                                        Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    })
                                                                    .addOnSuccessListener(a -> {
                                                                        orderModel.setOrderStatus(-1); //Local update
                                                                        ((MyOrdersAdapter) recycler_orders.getAdapter()).setItemAtPosition(pos, orderModel);
                                                                        recycler_orders.getAdapter().notifyItemChanged(pos);
                                                                        //Toast.makeText(getContext(), "Đã huỷ đơn hàng!", Toast.LENGTH_SHORT).show();

                                                                        Map<String, String> notiData = new HashMap<>();
                                                                        notiData.put(Common.NOTI_TITLE, "Huỷ đơn hàng");
                                                                        notiData.put(Common.NOTI_CONTENT, "Người dùng " + Common.currentUser.getPhone() + " đã huỷ 1 đơn hàng");

                                                                        FCMSendData sendData = new FCMSendData(Common.createTopicOrder(), notiData);

                                                                        compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                                                .subscribeOn(Schedulers.io())
                                                                                .observeOn(AndroidSchedulers.mainThread())
                                                                                .subscribe(fcmResponse -> {
                                                                                    Toast.makeText(getContext(), "Đã huỷ đơn hàng!", Toast.LENGTH_SHORT).show();
                                                                                    //Update cart count number after submit view_orders
                                                                                    EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                                                                }, throwable -> {
                                                                                    Toast.makeText(getContext(), "Đã huỷ đơn hàng, gửi thông báo thất bại!", Toast.LENGTH_SHORT).show();
                                                                                    //Update cart count number after submit view_orders
                                                                                    EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                                                                }));
                                                                    });
                                                        });
                                            });
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                }
                            } else {
                                Toast.makeText(getContext(), "Xin lỗi, bạn không thể huỷ!", Toast.LENGTH_SHORT).show();
                            }
                        }));

                //Lặp lại đơn hàng: lặp lại những đơn hàng đã đặt trước đó
                buf.add(new MyButton(getContext(), "Lặp lại", 35, 0, Color.parseColor("#5D4037"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPosition(pos);

                            dialog.show(); //show dialog if process is run on long time
                            cartDataSource.clearCart(Common.currentUser.getUid()) //Clear all item first
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
                                                    .subscribe(() -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(), "Đã thêm toàn bộ vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                                                        EventBus.getDefault().postSticky(new CounterCartEvent(true)); //Count fab

                                                    }, throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(), "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
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
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false); //Hide Home menu already inflate
        super.onPrepareOptionsMenu(menu);
    }
}
