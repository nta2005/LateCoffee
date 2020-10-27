package com.nta.latecoffee.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nta.latecoffee.R;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.model.OrderModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyOrdersAdapter extends RecyclerView.Adapter<MyOrdersAdapter.MyViewHolder> {

    private Context context;
    private List<OrderModel> orderModelList;
    private Calendar calendar;
    private SimpleDateFormat simpleDateFormat;

    public MyOrdersAdapter(Context context, List<OrderModel> orderModelList) {
        this.context = context;
        this.orderModelList = orderModelList;
        calendar = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_order_item,parent,false));

    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(orderModelList.get(position).getCartItemList().get(0).getFoodImage())
                .into(holder.img_order); //Load default image in cart
        calendar.setTimeInMillis(orderModelList.get(position).getCreateDate());
        Date date = new Date(orderModelList.get(position).getCreateDate());
        holder.txt_order_date.setText(new StringBuilder(Common.getDateOfWith(calendar.get(Calendar.DAY_OF_WEEK)))
        .append(" ")
        .append(simpleDateFormat.format(date)));
        holder.txt_order_number.setText(new StringBuilder("OrderModel number: ")
                .append(orderModelList.get(position).getOrderNumber()));
        holder.txt_order_comment.setText(new StringBuilder("Comment: ")
                .append(orderModelList.get(position).getComment()));
        holder.txt_order_status.setText(new StringBuilder("Status: ")
                .append(Common.convertStatusToText(orderModelList.get(position).getOrderStatus())));

    }

    @Override
    public int getItemCount() {
        return orderModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.txt_order_date)
        TextView txt_order_date;
        @BindView(R.id.txt_order_number)
        TextView txt_order_number;
        @BindView(R.id.txt_order_comment)
        TextView txt_order_comment;
        @BindView(R.id.txt_order_status)
        TextView txt_order_status;

        @BindView(R.id.igm_order)
        ImageView img_order;

        Unbinder unbinder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }
}
