package com.nta.latecoffee.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nta.latecoffee.R;
import com.nta.latecoffee.callback.IRecyclerClickListener;
import com.nta.latecoffee.common.Common;
import com.nta.latecoffee.database.CartDataSource;
import com.nta.latecoffee.database.CartDatabase;
import com.nta.latecoffee.database.CartItem;
import com.nta.latecoffee.database.LocalCartDataSource;
import com.nta.latecoffee.eventbus.CounterCartEvent;
import com.nta.latecoffee.eventbus.FoodItemClick;
import com.nta.latecoffee.model.FoodModel;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MyFoodListAdapter extends RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder> {

    private Context context;
    private List<FoodModel> foodModelList;
    private CompositeDisposable compositeDisposable;
    private CartDataSource cartDataSource;

    public MyFoodListAdapter(Context context, List<FoodModel> foodModelList) {
        this.context = context;
        this.foodModelList = foodModelList;
        this.compositeDisposable = new CompositeDisposable();
        this.cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(context).cartDAO());

    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_food_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(foodModelList.get(position).getImage()).into(holder.img_food_image);
        holder.txt_food_price.setText(new StringBuilder("$")
                .append(foodModelList.get(position).getPrice()));
        holder.txt_food_name.setText(new StringBuilder("")
                .append(foodModelList.get(position).getName()));

        //Event
        holder.setListener((view, pos) -> {
            Common.selectedFood = foodModelList.get(pos);
            //To assign this data for 'key' of food, we will assign when we retrieve foods from Category
            Common.selectedFood.setKey(String.valueOf(pos));
            EventBus.getDefault().postSticky(new FoodItemClick(true, foodModelList.get(pos)));
        });

        holder.img_cart.setOnClickListener(view -> {
            CartItem cartItem = new CartItem();
            cartItem.setRestaurantId(Common.currentRestaurant.getUid()); //multi restaurant
            cartItem.setUid(Common.currentUser.getUid());
            cartItem.setUserPhone(Common.currentUser.getPhone());

            cartItem.setCategoryId(Common.categorySelected.getMenu_id());
            cartItem.setFoodId(foodModelList.get(position).getId());
            cartItem.setFoodName(foodModelList.get(position).getName());
            cartItem.setFoodImage(foodModelList.get(position).getImage());
            cartItem.setFoodPrice(Double.valueOf(String.valueOf(foodModelList.get(position).getPrice())));
            cartItem.setFoodQuantity(1);
            cartItem.setFoodExtraPrice(0.0); //Because default we not choose size + addon so extra price is 0
            cartItem.setFoodAddon("Default");
            cartItem.setFoodSize("Default");

            cartDataSource.getItemWithAllOptionsInCart(Common.currentUser.getUid(),
                    Common.categorySelected.getMenu_id(),
                    cartItem.getFoodId(),
                    cartItem.getFoodSize(),
                    cartItem.getFoodAddon(),
                    Common.currentRestaurant.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<CartItem>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                        }

                        @Override
                        public void onSuccess(@io.reactivex.annotations.NonNull CartItem cartItemFromDB) {
                            if (cartItemFromDB.equals(cartItem)) {
                                //Already in database, just update
                                cartItemFromDB.setFoodExtraPrice(cartItem.getFoodExtraPrice());
                                cartItemFromDB.setFoodSize(cartItem.getFoodSize());
                                cartItemFromDB.setFoodAddon(cartItem.getFoodAddon());
                                cartItemFromDB.setFoodQuantity(cartItemFromDB.getFoodQuantity() + cartItem.getFoodQuantity());

                                cartDataSource.updateCartItems(cartItemFromDB)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new SingleObserver<Integer>() {
                                            @Override
                                            public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                                            }

                                            @Override
                                            public void onSuccess(@io.reactivex.annotations.NonNull Integer integer) {
                                                Toast.makeText(context, "Update Cart success", Toast.LENGTH_SHORT).show();
                                                EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                            }

                                            @Override
                                            public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                                Toast.makeText(context, "[UPDATE CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                // Item not available in cart before, insert new
                                compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(() -> {
                                                    Toast.makeText(context, "Add to Cart success", Toast.LENGTH_SHORT).show();
                                                    EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                                }, throwable -> {
                                                    Toast.makeText(context, "[CART ERROR]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                        ));
                            }
                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                            if (e.getMessage().contains("empty")) {
                                //Default, if Cart is empty, this code will be fired
                                compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(() -> {
                                                    Toast.makeText(context, "Add to Cart success", Toast.LENGTH_SHORT).show();
                                                    EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                                }, throwable -> {
                                                    Toast.makeText(context, "[CART ERROR]" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                        ));
                            }
                            Toast.makeText(context, "[GET CART]" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        });
    }

    @Override
    public int getItemCount() {
        return foodModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.txt_food_name)
        TextView txt_food_name;
        @BindView(R.id.txt_food_price)
        TextView txt_food_price;
        @BindView(R.id.img_food_image)
        ImageView img_food_image;
        @BindView(R.id.img_fav)
        ImageView img_fav;
        @BindView(R.id.img_quick_cart)
        ImageView img_cart;

        IRecyclerClickListener listener;
        private Unbinder unbinder;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }

        @Override
        public void onClick(View view) {
            listener.onItemClickListener(view, getAdapterPosition());
        }
    }
}
