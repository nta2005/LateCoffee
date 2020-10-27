package com.nta.latecoffee.ui.view_orders;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nta.latecoffee.R;
import com.nta.latecoffee.adapter.MyOrdersAdapter;
import com.nta.latecoffee.callback.ILoadOrderCallbackListener;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.model.OrderModel;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class ViewOrderFragment extends Fragment implements ILoadOrderCallbackListener {
    @BindView(R.id.recycler_orders)
    RecyclerView recycler_orders;
    AlertDialog dialog;
    private Unbinder unbinder;
    private ViewOrderViewModel viewOrderViewModel;

    private ILoadOrderCallbackListener listener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewOrderViewModel =
                ViewModelProviders.of(this).get(ViewOrderViewModel.class);
        View root = inflater.inflate(R.layout.fragment_view_order, container, false);
        unbinder = ButterKnife.bind(this, root);

        initViews(root);
        loadOrdersFromFirebase();

        viewOrderViewModel.getMutableLiveDataOrderList().observe(getViewLifecycleOwner(),orderList -> {
            MyOrdersAdapter adapter = new MyOrdersAdapter(getContext(),orderList);
            recycler_orders.setAdapter(adapter);
        });

        return root;
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
                        for (DataSnapshot orderSnapShot: dataSnapshot.getChildren()){
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
        listener = this;

        dialog = new SpotsDialog.Builder().setMessage("Đang tải...").setCancelable(false).setContext(getContext()).build();

        recycler_orders.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_orders.setLayoutManager(layoutManager);
        recycler_orders.addItemDecoration(new DividerItemDecoration(getContext(),
                layoutManager.getOrientation()));


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
}
