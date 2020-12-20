package com.nta.lc_server.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nta.lc_server.R;
import com.nta.lc_server.model.AddonModel;
import com.nta.lc_server.model.CartItem;
import com.nta.lc_server.model.SizeModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyOrderDetailAdapter extends RecyclerView.Adapter<MyOrderDetailAdapter.MyViewHolder> {

    Context context;
    List<CartItem> cartItemList;
    Gson gson;

    public MyOrderDetailAdapter(Context context, List<CartItem> cartItemList) {
        this.context = context;
        this.cartItemList = cartItemList;
        gson = new Gson();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_order_detail_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(cartItemList.get(position).getFoodImage()).into(holder.img_food_image);
        holder.txt_food_name.setText(new StringBuilder().append(cartItemList.get(position).getFoodName()));
        holder.txt_food_quantity.setText(new StringBuilder("Số lượng: ").append(cartItemList.get(position).getFoodQuantity()));
        SizeModel sizeModel = gson.fromJson(cartItemList.get(position).getFoodSize(), new TypeToken<SizeModel>() {
        }.getType());
        if (sizeModel != null)
            holder.txt_food_size.setText(new StringBuilder("Size: ").append(sizeModel.getName()));
        if (!cartItemList.get(position).getFoodAddon().equals("Mặc định")) {
            List<AddonModel> addonModels = gson.fromJson(cartItemList.get(position).getFoodAddon(),
                    new TypeToken<List<AddonModel>>() {
                    }.getType());
            StringBuilder addonString = new StringBuilder();
            if (addonModels != null) {
                for (AddonModel addonModel : addonModels)
                    addonString.append(addonModel.getName()).append(",");
                addonString.delete(addonString.length() - 1, addonString.length()); //Remove last "," character
                holder.txt_food_add_on.setText(new StringBuilder("Thêm: ").append(addonString));
            }
        } else holder.txt_food_add_on.setText(new StringBuilder("Thêm: Mặc định"));
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.txt_food_name)
        TextView txt_food_name;
        @BindView(R.id.txt_food_add_on)
        TextView txt_food_add_on;
        @BindView(R.id.txt_food_size)
        TextView txt_food_size;
        @BindView(R.id.txt_food_quantity)
        TextView txt_food_quantity;
        @BindView(R.id.img_food_image)
        ImageView img_food_image;

        private Unbinder unbinder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }
}
