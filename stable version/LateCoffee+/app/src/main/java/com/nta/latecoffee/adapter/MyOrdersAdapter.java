package com.nta.latecoffee.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nta.latecoffee.R;
import com.nta.latecoffee.callback.IRecyclerClickListener;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.database.CartItem;
import com.nta.latecoffee.model.OrderModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyOrdersAdapter extends RecyclerView.Adapter<MyOrdersAdapter.MyViewHolder> {

    private final Context context;
    private final List<OrderModel> orderList;
    private final Calendar calendar;
    private final SimpleDateFormat simpleDateFormat;
    RecyclerView recycler_order_detail;
    MyOrderDetailAdapter adapter;
    LinearLayoutManager layoutManager;
    Button btn_ok;

    public MyOrdersAdapter(Context context, List<OrderModel> orderList) {
        this.context = context;
        this.orderList = orderList;
        calendar = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    }

    public OrderModel getItemAtPosition(int pos) {
        return orderList.get(pos);
    }

    public void setItemAtPosition(int pos, OrderModel item) {
        orderList.set(pos, item);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_order_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(orderList.get(position).getCartItemList().get(0).getFoodImage())
                .into(holder.img_order); //Load default image in cart
        calendar.setTimeInMillis(orderList.get(position).getCreateDate());
        Date date = new Date(orderList.get(position).getCreateDate());
        holder.txt_order_date.setText(new StringBuilder(Common.getDateOfWeek(calendar.get(Calendar.DAY_OF_WEEK)))
                .append(" ")
                .append(simpleDateFormat.format(date)));
        holder.txt_order_number.setText(new StringBuilder("Đơn hàng: ")
                .append(orderList.get(position).getOrderNumber()));
        holder.txt_order_comment.setText(new StringBuilder("Ghi chú: ")
                .append(orderList.get(position).getComment()));
        holder.txt_order_status.setText(new StringBuilder("Trạng thái: ")
                .append(Common.convertStatusToText(orderList.get(position).getOrderStatus())));

        holder.setRecyclerClickListener((view, pos) -> {
            showDialog(orderList.get(pos).getCartItemList());
        });

    }

    private void showDialog(List<CartItem> cartItemList) {
        View layout_dialog = LayoutInflater.from(context).inflate(R.layout.layout_dialog_order_detail, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(layout_dialog);

        btn_ok = layout_dialog.findViewById(R.id.btn_ok);
        recycler_order_detail = layout_dialog.findViewById(R.id.recycler_order_detail);
        recycler_order_detail.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(context);
        recycler_order_detail.setLayoutManager(layoutManager);
        recycler_order_detail.addItemDecoration(new DividerItemDecoration(context, layoutManager.getOrientation()));

        adapter = new MyOrderDetailAdapter(context, cartItemList);
        recycler_order_detail.setAdapter(adapter);

        //Show dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        //Custom dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);

        btn_ok.setOnClickListener(view -> dialog.dismiss());
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.txt_order_date)
        TextView txt_order_date;
        @BindView(R.id.txt_order_number)
        TextView txt_order_number;
        @BindView(R.id.txt_order_comment)
        TextView txt_order_comment;
        @BindView(R.id.txt_order_status)
        TextView txt_order_status;
        @BindView(R.id.img_order)
        ImageView img_order;

        Unbinder unbinder;

        IRecyclerClickListener recyclerClickListener;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        public void setRecyclerClickListener(IRecyclerClickListener recyclerClickListener) {
            this.recyclerClickListener = recyclerClickListener;
        }

        @Override
        public void onClick(View view) {
            recyclerClickListener.onItemClickListener(view, getAdapterPosition());
        }
    }
}
