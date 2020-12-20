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
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nta.latecoffee.R;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.database.CartItem;
import com.nta.latecoffee.eventbus.UpdateItemInCart;
import com.nta.latecoffee.model.AddonModel;
import com.nta.latecoffee.model.SizeModel;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyCartAdaper extends RecyclerView.Adapter<MyCartAdaper.MyViewHolder> {


    Context context;
    List<CartItem> cartItemList;
    Gson gson;

    public MyCartAdaper(Context context, List<CartItem> cartItemList) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.gson = new Gson();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_cart_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(cartItemList.get(position).getFoodImage())
                .into(holder.img_cart);
        holder.txt_food_name.setText(new StringBuilder(cartItemList.get(position).getFoodName()));
        holder.txt_food_price.setText(new StringBuilder("").append(cartItemList.get(position).getFoodPrice() + cartItemList.get(position).getFoodExtraPrice()));

        if (cartItemList.get(position).getFoodSize() != null) {
            if (cartItemList.get(position).getFoodSize().equals("Default"))
                holder.txt_food_size.setText(new StringBuilder("Size: ").append("Default"));
            else {
                SizeModel sizeModel = gson.fromJson(cartItemList.get(position).getFoodSize(), new TypeToken<SizeModel>() {
                }.getType());
                holder.txt_food_size.setText(new StringBuilder("Size: ").append(sizeModel.getName()));
            }
        }

        if (cartItemList.get(position).getFoodAddon() != null) {
            if (cartItemList.get(position).getFoodAddon().equals("Default"))
                holder.txt_food_addon.setText(new StringBuilder("Addon: ").append("Default"));

            else {
                List<AddonModel> addonModels = gson.fromJson(cartItemList.get(position).getFoodAddon(),
                        new TypeToken<List<AddonModel>>() {
                        }.getType());
                holder.txt_food_addon.setText(new StringBuilder("Addon: ").append(Common.getListAddon(addonModels)));
            }
        }

        holder.numberButtonCart.setNumber(String.valueOf(cartItemList.get(position).getFoodQuantity()));

        //Event
        holder.numberButtonCart.setOnValueChangeListener((view, oldValue, newValue) -> {
            //When user click button, we will update database
            cartItemList.get(position).setFoodQuantity(newValue);
            EventBus.getDefault().postSticky(new UpdateItemInCart(cartItemList.get(position)));
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public CartItem getItemAtPosition(int pos) {
        return cartItemList.get(pos);
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.img_cart)
        ImageView img_cart;
        @BindView(R.id.txt_food_name)
        TextView txt_food_name;
        @BindView(R.id.txt_food_size)
        TextView txt_food_size;
        @BindView(R.id.txt_food_addon)
        TextView txt_food_addon;
        @BindView(R.id.txt_food_price)
        TextView txt_food_price;
        @BindView(R.id.number_button)
        ElegantNumberButton numberButtonCart;
        private Unbinder unbinder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }
}
