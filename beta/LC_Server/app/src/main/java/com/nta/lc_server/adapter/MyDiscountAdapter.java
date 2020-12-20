package com.nta.lc_server.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nta.lc_server.R;
import com.nta.lc_server.model.DiscountModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyDiscountAdapter extends RecyclerView.Adapter<MyDiscountAdapter.MyViewHolder> {

    Context context;
    List<DiscountModel> discountModelList;
    SimpleDateFormat simpleDateFormat;

    public MyDiscountAdapter(Context context, List<DiscountModel> discountModelList) {
        this.context = context;
        this.discountModelList = discountModelList;
        simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_discount_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.txt_code.setText(new StringBuilder("Code: ").append(discountModelList.get(position).getKey()));
        holder.txt_percent.setText(new StringBuilder("Percent: ").append(discountModelList.get(position).getPercent()).append("%"));
        holder.txt_valid.setText(new StringBuilder("Valid until: ").append(simpleDateFormat.format(discountModelList.get(position).getUntilDate())));
    }

    @Override
    public int getItemCount() {
        return discountModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{
        @BindView(R.id.txt_code)
        TextView txt_code;
        @BindView(R.id.txt_percent)
        TextView txt_percent;
        @BindView(R.id.txt_valid)
        TextView txt_valid;

        private Unbinder unbinder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);
        }
    }
}
