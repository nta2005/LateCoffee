package com.nta.lc_server.adapter;

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
import com.nta.lc_server.R;
import com.nta.lc_server.callback.IRecyclerClickListener;
import com.nta.lc_server.common.Common;
import com.nta.lc_server.model.CartItem;
import com.nta.lc_server.model.OrderModel;

import java.text.SimpleDateFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyOrderAdapter extends RecyclerView.Adapter<MyOrderAdapter.MyViewHolder> {

    Context context;
    List<OrderModel> orderModelList;
    SimpleDateFormat simpleDateFormat;

    public MyOrderAdapter(Context context, List<OrderModel> orderModelList) {
        this.context = context;
        this.orderModelList = orderModelList;
        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_order_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context)
                .load(orderModelList.get(position).getCartItemList().get(0).getFoodImage())
                .into(holder.img_food_image);
        holder.txt_order_number.setText(orderModelList.get(position).getKey());
        Common.setSpanStringColor("Ngày: ", simpleDateFormat.format(orderModelList.get(position).getCreateDate()),
                holder.txt_time, Color.parseColor("#333639"));
        Common.setSpanStringColor("Trạng thái: ", Common.convertStatusToString(orderModelList.get(position).getOrderStatus()),
                holder.txt_order_status, Color.parseColor("#00579A"));
        Common.setSpanStringColor("Tên: ", orderModelList.get(position).getUserName(),
                holder.txt_name, Color.parseColor("#00574B"));
        Common.setSpanStringColor("Số lượng: ", orderModelList.get(position).getCartItemList() == null ? "0" :
                        String.valueOf(orderModelList.get(position).getCartItemList().size()),
                holder.txt_num_item, Color.parseColor("#4B647D"));

        holder.setRecyclerClickListener((view, pos) ->

                showDialog(orderModelList.get(pos).getCartItemList()));
    }

    private void showDialog(List<CartItem> cartItemList) {
        View layout_dialog = LayoutInflater.from(context).inflate(R.layout.layout_dialog_order_detail, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(layout_dialog);

        Button btn_ok = layout_dialog.findViewById(R.id.btn_ok);
        RecyclerView recycler_order_detail = layout_dialog.findViewById(R.id.recycler_order_detail);
        recycler_order_detail.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recycler_order_detail.setLayoutManager(layoutManager);
        recycler_order_detail.addItemDecoration(new DividerItemDecoration(context, layoutManager.getOrientation()));

        MyOrderDetailAdapter myOrderDetailAdapter = new MyOrderDetailAdapter(context, cartItemList);
        recycler_order_detail.setAdapter(myOrderDetailAdapter);

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
        return orderModelList.size();
    }

    public OrderModel getItemAtPosition(int pos) {
        return orderModelList.get(pos);
    }

    public void removeItem(int pos) {
        orderModelList.remove(pos);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.img_food_image)
        ImageView img_food_image;
        @BindView(R.id.txt_name)
        TextView txt_name;
        @BindView(R.id.txt_time)
        TextView txt_time;
        @BindView(R.id.txt_order_status)
        TextView txt_order_status;
        @BindView(R.id.txt_order_number)
        TextView txt_order_number;
        @BindView(R.id.txt_num_item)
        TextView txt_num_item;
        IRecyclerClickListener recyclerClickListener;
        private Unbinder unbinder;

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
